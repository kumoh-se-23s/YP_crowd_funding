package ypfunding.server.network;

import lombok.Getter;
import lombok.Setter;
import ypfunding.common.protocol.TCPConnectionManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Getter
@Setter
public class PacketProcessor {
    private final SocketChannel channel;
    private ByteBuffer buffer;
    private int requiredSize;

    public PacketProcessor(SocketChannel srcSocket) throws IOException {
        srcSocket.configureBlocking(false);
        this.channel = srcSocket;
        this.buffer = ByteBuffer.allocate(TCPConnectionManager.MAX_BUFFER_SIZE);
        this.requiredSize = 0;
    }
    byte[] read() throws IOException {
        int size = channel.read(buffer);

        if(size == -1){
            return new byte[]{-1};
        }

        if(requiredSize == 0 && buffer.position() >= TCPConnectionManager.PACKET_LEN_HEADER_SIZE){
            requiredSize = Short.toUnsignedInt(buffer.getShort(0));

            if(requiredSize < TCPConnectionManager.PACKET_LEN_HEADER_SIZE){
                throw new IOException("Invalid packet size: " + requiredSize);
            }
            if (requiredSize > buffer.capacity()) {
                throw new IOException("Packet too large: " + requiredSize);
            }
        }

        if(requiredSize > 0 && buffer.position() >= requiredSize){
            byte[] result = new byte[requiredSize];
            System.arraycopy(buffer.array(), 0, result, 0, requiredSize);
            buffer.position(requiredSize);
            buffer.compact();
            requiredSize = 0;
            return result;
        }
        return new byte[]{0};
    }
}

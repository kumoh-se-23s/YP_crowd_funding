package ypfunding.common.protocol;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Getter
@Setter
public class PacketProcessor {
    private final SocketChannel socket;
    private ByteBuffer buffer;
    private int requiredSize;

    public PacketProcessor(SocketChannel srcSocket)  {
        this.socket = srcSocket;
        this.buffer = ByteBuffer.allocate(PacketManager.MAX_BUFFER_SIZE);
        this.requiredSize = 0;
    }
    public byte[] read() throws IOException {
        int size = socket.read(buffer);

        if(size == -1){
            return new byte[]{-1};
        }

        if(requiredSize == 0 && buffer.position() >= PacketManager.PACKET_LEN_HEADER_SIZE){
            requiredSize = Short.toUnsignedInt(buffer.getShort(0));

            if(requiredSize < PacketManager.PACKET_LEN_HEADER_SIZE){
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

package ypfunding.common.protocol;

import ypfunding.common.parallel.ThreadPool;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.protocol.packets.Packets;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class TCPConnectionManager {

    public static final int MAX_BUFFER_SIZE = 1024;
    public static final int PACKET_LEN_HEADER_SIZE = 2;
    public static final int MESSAGE_TYPE_HEADER_SIZE = 2;
    private final SocketChannel dstSocket;
    private final ThreadPool workerThread;
    private static final Map<Class<? extends Packet>, Packets> PKT_TO_REQUESTS_MAP = new HashMap<>();

    static{
        for (Packets value : Packets.values()) {
            PKT_TO_REQUESTS_MAP.put(value.getPacketClass(), value);
        }
    }

    public TCPConnectionManager(InetSocketAddress address) {
        try{
            dstSocket = SocketChannel.open();
            dstSocket.configureBlocking(true);
            dstSocket.connect(address);
            workerThread = new ThreadPool(1);
        }catch (IOException e){
            throw new IllegalStateException("Failed to create network connection", e);
        }
    }

    public void sendPacket(Packet packet) {
        workerThread.queue(() -> {
            try {
                ByteBuffer out = ByteBuffer.allocate(MAX_BUFFER_SIZE);
                out.putShort((short)0); //length of packet
                out.putShort((short)PKT_TO_REQUESTS_MAP.get(packet.getClass()).ordinal()); //request number
                packet.encode(out);
                int size = out.position();
                out.position(0);
                out.putInt(size);
                out.flip();
                dstSocket.write(out);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to send packet! ", e);
            }
        });
    }


}

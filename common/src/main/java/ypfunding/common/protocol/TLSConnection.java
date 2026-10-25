package ypfunding.common.protocol;

import lombok.Getter;
import lombok.Setter;
import ypfunding.common.protocol.packets.Packet;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

@Setter
@Getter
public class TLSConnection implements AutoCloseable {
    private final SSLEngine engine;
    private final SelectionKey selectionKey;
    private final SocketChannel socketChannel;
    private final AtomicReference<Packet> receivedPacket = new AtomicReference<>();
    private final Queue<Packet> sendPacketQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Class<? extends Packet>> receivedPacketQueue = new ConcurrentLinkedQueue<>();

    private ByteBuffer netIn;
    private ByteBuffer netOut;

    private ByteBuffer appIn;
    private ByteBuffer appOut;

    public TLSConnection(SSLEngine engine, SelectionKey selectionKey, SocketChannel socketChannel) {
        this.engine = engine;
        this.selectionKey = selectionKey;
        this.socketChannel = socketChannel;
        netIn = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        netOut = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        appIn = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
        appOut = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
    }

    public void readTLSData() throws IOException {
        if (socketChannel.read(netIn) == -1) {
            close();
            synchronized (this) {
                notifyAll();
            }
            throw new IOException("Connection closed");
        } else {
            netIn.flip();
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public void flushTLSData() throws IOException {

        Packet packet = sendPacketQueue.peek();
        if (packet == null) return;
        ByteBuffer appOut = getAppOut();
        ByteBuffer packetBuffer = PacketManager.makePacketBuffer(packet);
        if (appOut.remaining() < packetBuffer.remaining()) {
            return;
        }
        sendPacketQueue.poll();
        appOut.put(packetBuffer);
        appOut.flip();
    }

    public void driveTLSHandshake() throws IOException {

        switch (engine.getHandshakeStatus()) {
            case NEED_WRAP -> {

                // put existing data from previously generated encrypted buffer
                int netOutPosition = netOut.position();

                //  appOut requires read-mode
                //      |
                //      | encrypt
                //      v
                //  netOut requires write-mode

                assert appOut.isReadOnly();
                int appOutPosition = appOut.position();
                SSLEngineResult result = engine.wrap(appOut, netOut);

                switch (result.getStatus()) {
                    case OK -> {
                        netOut.flip();
                        appOut.compact();
                        socketChannel.write(netOut);

                        if (!netOut.hasRemaining()) {
                            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                        }
                        netOut.compact();
                    }
                    case BUFFER_OVERFLOW -> {
                        // buffer overflow. recover appOut and return
                        appOut.position(appOutPosition);
                        netOut.position(netOutPosition);
                    }
                    case BUFFER_UNDERFLOW -> throw new IOException("Unexpected status. cannot be happened from wrap()");
                    case CLOSED -> throw new IOException("TLS connection closed");
                }

            }
            case NEED_UNWRAP -> {

                //  netIn requires read-mode
                //      |
                //      | decrypt
                //      v
                //  appIn requires write-mode

                int appInPosition = appIn.position();
                int netInPosition = netIn.position();
                SSLEngineResult result = engine.unwrap(netIn, appIn);


                switch (result.getStatus()) {
                    case OK -> {
                        appIn.flip();

                        do {
                            receivedPacket.set(PacketManager.decodePacket(appIn, receivedPacketQueue.peek()));
                            if (receivedPacket.get() != null) {
                                receivedPacketQueue.poll();
                                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_READ);
                            }
                        } while (receivedPacket.get() != null);
                        synchronized (this) {
                            notifyAll();
                        }
                        appIn.compact();
                        netIn.compact();
                    }
                    case BUFFER_UNDERFLOW, BUFFER_OVERFLOW -> {
                        appIn.position(appInPosition);
                        netIn.position(netInPosition);
                    }
                    case CLOSED -> throw new IOException("TLS connection closed");
                }
            }
            case NEED_TASK -> {
                Runnable task;
                while ((task = engine.getDelegatedTask()) != null) {
                    task.run();
                }
            }
            case FINISHED, NOT_HANDSHAKING -> {
                //noop
            }
        }
    }

    @Override
    public void close() {
        receivedPacket.set(null);
    }
}

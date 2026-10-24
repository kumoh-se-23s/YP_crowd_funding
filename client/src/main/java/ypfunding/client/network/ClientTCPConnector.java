package ypfunding.client.network;

import ypfunding.common.protocol.PacketManager;
import ypfunding.common.protocol.PacketProcessor;
import ypfunding.common.protocol.packets.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class ClientTCPConnector implements ClientNetworkConnector {

    private final Selector selector;
    private final SocketChannel dstSocket;
    private final PacketProcessor packetProcessor;
    private final SelectionKey selectionKey;

    private final AtomicReference<Packet> receivedPacket = new AtomicReference<>();
    private final Queue<Packet> sendPacketQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Class<? extends Packet>> receivedPacketQueue = new ConcurrentLinkedQueue<>();

    //TODO : type the server address here
    private static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("127.0.0.1", 2222);


    public ClientTCPConnector() { // the server address is constant
        try {
            selector = Selector.open();
            dstSocket = SocketChannel.open();
            dstSocket.configureBlocking(false);
            dstSocket.connect(SERVER_ADDRESS);
            selectionKey = dstSocket.register(selector, SelectionKey.OP_CONNECT);
            packetProcessor = new PacketProcessor(dstSocket);
            start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create network connection", e);
        }
    }


    private void start(){
        new Thread(this::loop).start();
    }

    private void loop() {
        ByteBuffer buffer = null;
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isConnectable() && dstSocket.finishConnect()) {
                        key.interestOps(0);
                        if(!sendPacketQueue.isEmpty()){
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        }
                        if(!receivedPacketQueue.isEmpty()){
                            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                        }
                    }

                    if (key.isValid() && key.isWritable()) {
                        if (buffer == null) {
                            Packet packet = sendPacketQueue.peek();
                            if (packet != null) {
                                buffer = PacketManager.makePacketBuffer(packet);
                            }
                        }

                        if (buffer != null) {
                            dstSocket.write(buffer);
                            if (!buffer.hasRemaining()) {
                                sendPacketQueue.poll();
                                buffer = null;
                            }
                        }

                        if (buffer == null && sendPacketQueue.isEmpty()) {
                            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                        }
                    }
                    if (key.isValid() && key.isReadable()) {

                        byte[] bytesRead = packetProcessor.read();
                        if (bytesRead.length == 1) {
                            if (bytesRead[0] == -1) {
                                close();
                                synchronized (ClientTCPConnector.this){
                                    ClientTCPConnector.this.notifyAll();
                                }
                                return;
                            }
                        } else {
                            receivedPacket.set(PacketManager.decodePacket(ByteBuffer.wrap(bytesRead), receivedPacketQueue.poll()));
                            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                            synchronized (ClientTCPConnector.this){
                                ClientTCPConnector.this.notifyAll();
                            }
                        }

                    }
                    it.remove();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Error : ", e);
            }
        }
    }

    public <S extends Packet, R extends Packet> R sendAndWaitReply(S packetToSend, Class<R> packetToReceiveClass) throws TimeoutException{
        sendPacket(packetToSend);
        receivePacket(packetToReceiveClass);

        Packet pkt = null;
        long deadline = System.currentTimeMillis() + PacketManager.TIMEOUT_MS;

        synchronized (this){
            while(pkt == null){
                try {
                    long remaining = deadline - System.nanoTime();

                    if (remaining <= 0) {
                        throw new TimeoutException("Connection timed out");
                    }

                    wait(remaining);
                    pkt = receivedPacket.get();
                    receivedPacket.set(null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Error : ", e);
                }
            }
        }

        return packetToReceiveClass.cast(pkt);
    }

    public synchronized void sendPacket(Packet packet) {
        sendPacketQueue.offer(packet);
        if(selectionKey.isValid()) selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
    }


    private synchronized <P extends Packet> void receivePacket(Class<P> clazz) {
        receivedPacketQueue.offer(clazz);
        if(selectionKey.isValid()) selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);
        selector.wakeup();
    }


    @Override
    public void close() throws IOException {
        this.selectionKey.cancel();
        dstSocket.close();
        selector.close();
        receivedPacket.set(null);
    }
}

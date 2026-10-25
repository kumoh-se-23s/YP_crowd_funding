package ypfunding.client.network;

import ypfunding.common.constant.NetworkConstants;
import ypfunding.common.protocol.PacketManager;
import ypfunding.common.protocol.TLSConnection;
import ypfunding.common.protocol.packets.Packet;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

public class ClientTCPConnector implements ClientNetworkConnector {

    private final Selector selector;
    private final SocketChannel dstSocket;
    private final TLSConnection connection;
    private final SelectionKey selectionKey;
    private final SSLEngine engine;



    public ClientTCPConnector() { // the server address is constant
        try {
            engine = TLSClientConfig.create();
            selector = Selector.open();
            dstSocket = SocketChannel.open();
            dstSocket.configureBlocking(false);
            dstSocket.connect(NetworkConstants.SERVER_ADDRESS);
            selectionKey = dstSocket.register(selector, SelectionKey.OP_CONNECT);
            connection = new TLSConnection(engine, selectionKey, dstSocket);
            start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create network connection", e);
        }
    }


    private void start() {
        new Thread(this::loop).start();
    }



    private void loop() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    if (key.isConnectable() && dstSocket.finishConnect()) {
                        key.interestOps(SelectionKey.OP_READ);
                        if (connection.sendPacketCount() > 0) {
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        }
                        engine.beginHandshake();
                        connection.driveTLSHandshake();
                    }
                    if (key.isValid() && key.isWritable()) {
                        connection.flushTLSData();
                        connection.driveTLSHandshake();
                    }
                    if (key.isValid() && key.isReadable()) {
                        connection.readTLSData();
                        connection.driveTLSHandshake();
                    }
                    it.remove();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Error : ", e);
            }
        }
    }

    public <S extends Packet, R extends Packet> R sendAndWaitReply(S packetToSend, Class<R> packetToReceiveClass) throws TimeoutException {
        sendPacket(packetToSend);
        receivePacket(packetToReceiveClass);
        R receivedPacket = null;
        long deadline = System.currentTimeMillis() + PacketManager.TIMEOUT_MS;

        synchronized (this) {
            while (receivedPacket == null) {
                try {
                    long remaining = deadline - System.currentTimeMillis();

                    if (remaining <= 0) {
                        throw new TimeoutException("Connection timed out");
                    }

                    wait(remaining);
                    receivedPacket = connection.popReceivedPacket(packetToReceiveClass);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Error : ", e);
                }
            }
        }

        return packetToReceiveClass.cast(receivedPacket);
    }

    public synchronized void sendPacket(Packet packet) {
        connection.queuePacketToSend(packet);
        if (selectionKey.isValid()) selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
    }


    private synchronized <P extends Packet> void receivePacket(Class<P> clazz) {
        P packet = connection.popReceivedPacket(clazz);
        if (packet == null) return;
        if (selectionKey.isValid()) selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);
        selector.wakeup();
    }


    @Override
    public void close() throws IOException {
        this.selectionKey.cancel();
        connection.close();
        dstSocket.close();
        selector.close();
    }
}

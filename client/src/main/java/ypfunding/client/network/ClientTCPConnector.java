package ypfunding.client.network;

import ypfunding.common.constant.NetworkConstants;
import ypfunding.common.parallel.ThreadPool;
import ypfunding.common.protocol.PacketManager;
import ypfunding.common.protocol.TLSConnection;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.util.LogUtil;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ClientTCPConnector implements ClientNetworkConnector {

    private final Selector selector;
    private final SocketChannel dstSocket;
    private final TLSConnection connection;
    private final Scanner scanner;
    private final boolean connectedAtStart;
    private final ThreadPool threadPool;
    private final AtomicBoolean started = new AtomicBoolean();
    private final CountDownLatch loopDone = new CountDownLatch(1);
    private volatile boolean closeRequested;

    public ClientTCPConnector() { // the server address is constant
        try {
            SSLEngine engine = TLSClientConfig.create();    // 반드시 client mode
            selector = Selector.open();
            dstSocket = SocketChannel.open();
            dstSocket.configureBlocking(false);
            threadPool = new ThreadPool(1);

            connectedAtStart = dstSocket.connect(NetworkConstants.SERVER_ADDRESS);
            SelectionKey key = dstSocket.register(selector,
                    connectedAtStart ? SelectionKey.OP_READ : SelectionKey.OP_CONNECT);
            connection = new TLSConnection(engine, threadPool, key, dstSocket);
            scanner = new Scanner(System.in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create network connection", e);
        }
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            new Thread(this::loop, "client-io").start();
        }
    }

    public void loop() {
        try {
            if (connectedAtStart) connection.onConnected();     // 핸드셰이크는 IO 스레드에서 시작

            while (!closeRequested && !connection.isClosed()) {
                selector.select();
                if (closeRequested) break;

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) continue;

                    if (key.isConnectable()) {
                        if (dstSocket.finishConnect()) connection.onConnected();
                        continue;
                    }
                    if (key.isValid() && key.isWritable()) connection.onWritable();
                    if (key.isValid() && key.isReadable()) connection.onReadable();
                }
            }
        } catch (IOException e) {
            LogUtil.logError(e);
        } catch (ClosedSelectorException ignored) {
        } finally {
            connection.close();                                  // IO 스레드에서 종료
            try {
                selector.close();
            } catch (IOException ignored) {
            }
            loopDone.countDown();
        }
    }

    public <S extends Packet, R extends Packet> R sendAndWaitReply(S packetToSend, Class<R> packetToReceiveClass)
            throws TimeoutException {
        sendPacket(packetToSend);
        try {
            return connection.awaitPacket(packetToReceiveClass, NetworkConstants.TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for reply", e);
        }
    }

    public void sendPacket(Packet packet) {
        connection.queuePacketToSend(packet);                   // OP_WRITE 설정과 wakeup은 내부에서 처리
    }

    @Override
    public void close() {
        closeRequested = true;
        if (started.get()) {
            selector.wakeup();                                  // 정리는 loop의 finally가 수행
        } else {
            connection.close();
            try {
                selector.close();
            } catch (IOException ignored) {
            }
            loopDone.countDown();
        }
        threadPool.close();
        scanner.close();
    }

    @Override
    public void pause(Consumer<String> consoleInput) {
        while (loopDone.getCount() > 0 && scanner.hasNextLine()) {
            String input = scanner.nextLine();
            //TODO switch input
            consoleInput.accept(input);
            if (input.equals("exit")) break;
        }
        closeRequested = true;
        selector.wakeup();
        try {
            loopDone.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LogUtil.logError("Unexpected interrupt");
        }
    }
}
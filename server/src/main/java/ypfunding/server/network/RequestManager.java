package ypfunding.server.network;

import lombok.Getter;
import ypfunding.common.constant.NetworkConstants;
import ypfunding.common.parallel.ThreadPool;
import ypfunding.common.protocol.RawPacketTemp;
import ypfunding.common.protocol.TLSConnection;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.util.LogUtil;
import ypfunding.server.RuntimeServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestManager implements AutoCloseable {

    private static final RequestHandlers[] HANDLERS = RequestHandlers.values();

    /**
     * 클라이언트 한 명의 상태. key.attachment()로도 접근.
     */
    private static final class Session {
        final SocketChannel channel;
        final TLSConnection connection;
        final RequestEvent event;
        final AtomicBoolean draining = new AtomicBoolean();   // 연결당 핸들러 직렬 실행 보장
        volatile boolean abort;                               // 워커가 "이 연결 끊어라" 요청

        Session(SocketChannel channel, TLSConnection connection, RequestEvent event) {
            this.channel = channel;
            this.connection = connection;
            this.event = event;
        }
    }

    @Getter
    private final RuntimeServer runtimeServer;

    private final Selector selector;
    private final ServerSocketChannel serverSocket;
    private final ThreadPool threadPool;
    private final Map<SocketChannel, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private volatile boolean running = true;

    public RequestManager(RuntimeServer server) throws IOException {
        this.runtimeServer = server;
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(NetworkConstants.PORT));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        int threads = Runtime.getRuntime().availableProcessors();
        threadPool = new ThreadPool(threads);
    }

    /**
     * 블로킹. selector 스레드가 곧 호출 스레드.
     */
    public void start() {
        if (!started.compareAndSet(false, true)) throw new IllegalStateException("already started");
        try {
            while (running) {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();                                   // 처리 전에 제거 (예외 시 재처리 방지)
                    try {
                        processSelectedKey(key);
                    } catch (IOException | RuntimeException e) {
                        LogUtil.logError("Key processing failed: " + e);
                        dropKey(key);
                    }
                }
                runPendingEvents();                                // 키가 선택되지 않아도(wakeup) 항상 실행
                reapClosed();
            }
        } catch (IOException e) {
            LogUtil.logError("Selector failure: " + e);
        } catch (ClosedSelectorException ignored) {
        } finally {
            shutdown();
        }
    }

    private void processSelectedKey(SelectionKey key) throws IOException {
        if (!key.isValid()) return;
        if (key.isAcceptable()) {
            acceptAll();
            return;
        }
        Session s = (Session) key.attachment();
        if (s == null) return;

        if (key.isValid() && key.isWritable()) s.connection.onWritable();
        if (key.isValid() && key.isReadable()) {
            s.connection.onReadable();
            if (s.connection.hasReceivedPackets()) scheduleDrain(s);
        }
    }

    private void acceptAll() throws IOException {
        SocketChannel ch;
        while ((ch = serverSocket.accept()) != null) {           // non-blocking: null 가능
            try {
                ch.configureBlocking(false);
                SelectionKey clientKey = ch.register(selector, SelectionKey.OP_READ);
                runtimeServer.createConnection(ch, threadPool, clientKey);   // 서버 key가 아니라 클라이언트 key
                TLSConnection connection = runtimeServer.getConnection(ch);
                Session s = new Session(ch, connection, new RequestEvent(selector, runtimeServer, ch));
                clientKey.attach(s);
                sessions.put(ch, s);
                // 서버 핸드셰이크는 ClientHello 수신 시 onReadable → pump에서 시작됨
            } catch (IOException | RuntimeException e) {
                LogUtil.logError("Accept failed: " + e);
                try {
                    ch.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    // ================= 요청 처리 (워커 스레드) =================

    /**
     * 연결당 하나의 drain 작업만 실행 → 패킷 순서 보장, 한 번에 여러 패킷 처리.
     */
    private void scheduleDrain(Session s) {
        if (!s.draining.compareAndSet(false, true)) return;
        try {
            threadPool.queue(() -> {
                try {
                    RawPacketTemp raw;
                    while ((raw = s.connection.popReceivedPacket()) != null) {
                        try {
                            handle(s, raw);
                        } catch (RuntimeException e) {
                            LogUtil.logError("Request handler failed: " + e);
                        }
                    }
                } finally {
                    s.draining.set(false);
                }
                if (s.connection.hasReceivedPackets()) scheduleDrain(s);   // 플래그 해제 직전에 도착한 패킷
            });
        } catch (RuntimeException e) {
            s.draining.set(false);
            throw e;
        }
    }

    private void handle(Session s, RawPacketTemp raw) {
        int ord = raw.ordinal();
        if (ord < 0 || ord >= HANDLERS.length) {                   // 클라이언트 입력은 신뢰하지 않음
            LogUtil.logError("Unknown packet type: " + ord);
            s.abort = true;
        } else {
            HANDLERS[ord].handle(s.event, raw.payload());
        }
        selector.wakeup();                                         // selectorTasks/writeQueue 처리 요청
    }

    // ================= selector 스레드 =================

    /**
     * 핸들러가 남긴 selectorTasks 실행, writeQueue → 해당 연결로 전달.
     */
    private void runPendingEvents() {
        for (Session s : sessions.values()) {
            Runnable task;
            while ((task = s.event.getSelectorTasks().poll()) != null) {
                try {
                    task.run();                                    // not a request, it is request handling.
                } catch (RuntimeException e) {
                    LogUtil.logError("Selector task failed: " + e);
                }
            }
            Queue<Packet> writeQueue = s.event.getWriteQueue();
            Packet packet;
            while ((packet = writeQueue.poll()) != null) {
                s.connection.queuePacketToSend(packet);            // 이 세션의 연결로 (이전엔 다른 클라이언트로 갔음)
            }
        }
    }

    private void reapClosed() {
        for (Session s : sessions.values()) {
            if (s.abort) s.connection.close();                     // IO 스레드에서 종료
            if (s.connection.isClosed()) endSession(s);
        }
    }

    private void endSession(Session s) {
        if (sessions.remove(s.channel) == null) return;            // 한 번만
        s.connection.close();                                      // idempotent
        runtimeServer.endConnection(s.channel);
        LogUtil.log("Connection closed | socket " + s.channel.socket().getInetAddress() + ":" + s.channel.socket().getPort());
    }

    private void dropKey(SelectionKey key) {
        if (key.attachment() instanceof Session s) {
            s.connection.close();
            endSession(s);
        }                                                          // 서버 key는 그대로 유지
    }

    private void shutdown() {
        running = false;
        for (Session s : sessions.values()) endSession(s);
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
        try {
            selector.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close() {
        running = false;
        threadPool.close();
        if (started.get()) {
            selector.wakeup();                                     // 정리는 start()의 finally가 수행
        } else {
            shutdown();
        }
    }
}
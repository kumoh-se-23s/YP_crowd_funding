package ypfunding.common.protocol;

import ypfunding.common.parallel.ThreadPool;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.util.LogUtil;
import java.nio.channels.CancelledKeyException;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TLSConnection implements AutoCloseable {
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    private interface IOTask { void run() throws IOException; }

    private final SSLEngine engine;
    private final ThreadPool threadPool;
    private final SelectionKey selectionKey;
    private final SocketChannel socketChannel;

    private final Queue<Packet> sendPacketQueue = new ConcurrentLinkedQueue<>();
    private final Queue<RawPacketTemp> receivedPacketQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean taskRunning = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile Throwable taskError;

    private final Object opsLock = new Object();
    private final Object recvLock = new Object();

    // ---- IO 스레드 전용 (전부 평소 write-mode) ----
    private ByteBuffer netIn;
    private ByteBuffer netOut;
    private ByteBuffer appIn;
    private ByteBuffer appOut;
    private ByteBuffer pendingPlain;   // mux 결과 중 아직 appOut으로 못 옮긴 부분 (read-mode)

    public TLSConnection(SSLEngine engine, ThreadPool pool, SelectionKey selectionKey, SocketChannel socketChannel) {
        this.engine = engine;
        this.threadPool = pool;
        this.selectionKey = selectionKey;
        this.socketChannel = socketChannel;
        int pkt = Math.max(PacketManager.MAX_BUFFER_SIZE, engine.getSession().getPacketBufferSize());
        int app = Math.max(PacketManager.MAX_BUFFER_SIZE, engine.getSession().getApplicationBufferSize());
        netIn = ByteBuffer.allocate(pkt);
        netOut = ByteBuffer.allocate(pkt);
        appIn = ByteBuffer.allocate(app);
        appOut = ByteBuffer.allocate(app);
    }

    // ================= 앱 스레드 API =================

    public void queuePacketToSend(Packet packet) {
        if (closed.get()) return;
        sendPacketQueue.add(packet);
        setWriteInterest(true);                       // add 이후에 켜야 lost wakeup이 없음
        selectionKey.selector().wakeup();
    }

    public int sendPacketCount() {
        return sendPacketQueue.size();
    }

    public RawPacketTemp popReceivedPacket() {
        return receivedPacketQueue.poll();
    }

    /** 소비자가 하나라는 전제 (peek 후 poll). 타입이 다르면 패킷을 버리지 않고 null. */
    public <P extends Packet> P popReceivedPacket(Class<P> packetClass) {
        RawPacketTemp temp = receivedPacketQueue.peek();
        if (temp == null || packetClass != temp.packetClass()) return null;
        receivedPacketQueue.poll();
        try {
            P packet = PacketManager.createPacket(packetClass);
            packet.decode(temp.payload());
            return packet;
        } catch (IOException e) {
            LogUtil.logError("Error decoding packet: " + e.getMessage());
            return null;
        }
    }

    public RawPacketTemp awaitPacket(long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        synchronized (recvLock) {
            while (receivedPacketQueue.isEmpty() && !closed.get()) {
                long left = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (left <= 0) break;
                recvLock.wait(left);
            }
        }
        return receivedPacketQueue.poll();
    }

    // ================= IO 스레드 진입점 =================

    /** 클라이언트는 연결 완료 후 한 번 호출. 서버는 불필요. */
    public void startHandshake() {
        guard(() -> {
            engine.beginHandshake();
            pump();
        });
    }

    public void onReadable() {
        guard(() -> {
            boolean eof = !readTLSData();
            pump();                                    // EOF여도 버퍼에 남은 데이터를 먼저 처리
            if (eof) {
                try { engine.closeInbound(); } catch (SSLException ignored) { }
                close();
            }
        });
    }

    /** OP_WRITE: 소켓 쓰기 재개, 태스크 완료 후 재개, 새 패킷 전송 요청 모두 여기로 옴. */
    public void onWritable() {
        guard(() -> {
            flushNetOut();
            pump();
        });
    }

    private void guard(IOTask task) {
        if (closed.get()) return;
        try {
            task.run();
        } catch (IOException | RuntimeException e) {
            LogUtil.logError("TLS error: " + e.getMessage());
            close();
        }
    }

    // ================= 상태 머신 =================

    private void pump() throws IOException {
        boolean progress;
        do {
            if (taskRunning.get()) return;             // 태스크 실행 중엔 엔진 접근 금지
            Throwable err = taskError;
            if (err != null) throw new IOException("TLS delegated task failed", err);

            switch (engine.getHandshakeStatus()) {
                case NEED_WRAP -> progress = wrapHandshake();
                case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> progress = unwrapOnce();
                case NEED_TASK -> progress = startTasks();
                default -> {                           // NOT_HANDSHAKING (FINISHED는 result에만 나옴)
                    driveSend();
                    progress = unwrapOnce() || engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING;
                }
            }
        } while (progress);
    }

    private boolean readTLSData() throws IOException {
        return socketChannel.read(netIn) != -1;        // netIn은 항상 write-mode
    }

    // ---------- 핸드셰이크 wrap ----------

    private boolean wrapHandshake() throws IOException {
        if (netOut.position() > 0 && !flushNetOut()) return false;
        SSLEngineResult r = engine.wrap(EMPTY, netOut);
        switch (r.getStatus()) {
            case OK -> {
                flushNetOut();                         // 막히면 OP_WRITE만 켜지고 다음 루프에서 처리
                return true;
            }
            case CLOSED -> {
                flushNetOut();
                throw new IOException("TLS closed");
            }
            default -> throw new IOException("handshake wrap: " + r.getStatus());
        }
    }

    // ---------- 데이터 송신 ----------

    private void driveSend() throws IOException {
        if (netOut.position() > 0 && !flushNetOut()) return;
        while (true) {
            fillAppOut();
            if (appOut.position() == 0) return;        // 보낼 것 없음

            appOut.flip();
            boolean handshakeChanged = false;
            try {
                while (appOut.hasRemaining()) {
                    SSLEngineResult r = engine.wrap(appOut, netOut);
                    switch (r.getStatus()) {
                        case OK -> { }
                        case BUFFER_OVERFLOW -> {
                            if (netOut.position() == 0) throw new IOException("netOut too small");
                            if (!flushNetOut()) return;   // finally에서 appOut compact됨
                        }
                        case CLOSED -> {
                            flushNetOut();
                            throw new IOException("TLS closed");
                        }
                        default -> throw new IOException("unexpected " + r.getStatus());
                    }
                    if (engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
                        handshakeChanged = true;       // 재협상 등: 상위 루프가 상태에 맞게 처리
                        break;
                    }
                    if (r.getStatus() == SSLEngineResult.Status.OK
                        && r.bytesConsumed() == 0 && r.bytesProduced() == 0) {
                        throw new IOException("wrap made no progress");
                    }
                }
            } finally {
                appOut.compact();                      // 소비 안 된 부분 보존 + write-mode 복귀
            }
            if (!flushNetOut() || handshakeChanged) return;
        }
    }

    /** 큐의 패킷을 mux해서 appOut(write-mode)이 찰 때까지 채운다. 큰 패킷은 쪼개서 옮김. */
    private void fillAppOut() throws IOException {
        while (appOut.hasRemaining()) {
            if (pendingPlain == null || !pendingPlain.hasRemaining()) {
                Packet p = sendPacketQueue.poll();
                if (p == null) {
                    pendingPlain = null;
                    return;
                }
                pendingPlain = PacketManager.mux(p);
            }
            int n = Math.min(pendingPlain.remaining(), appOut.remaining());
            int oldLimit = pendingPlain.limit();
            pendingPlain.limit(pendingPlain.position() + n);
            appOut.put(pendingPlain);
            pendingPlain.limit(oldLimit);
        }
    }

    // ---------- 수신 (핸드셰이크/데이터 공용) ----------

    /** @return true: 진행했음(상태 재확인), false: 데이터 부족 또는 진전 없음 */
    private boolean unwrapOnce() throws IOException {
        if (netIn.position() == 0) return false;
        SSLEngineResult r;
        netIn.flip();
        try {
            r = engine.unwrap(netIn, appIn);
        } finally {
            netIn.compact();                           // 항상 write-mode 복귀 (확장은 이 뒤에)
        }

        switch (r.getStatus()) {
            case OK -> {
                demuxAppIn();
                return r.bytesConsumed() > 0 || r.bytesProduced() > 0;
            }
            case BUFFER_UNDERFLOW -> {
                if (netIn.hasRemaining()) return false;   // 더 읽으면 됨
                growNetIn();                              // 가득 찼는데도 부족 → 확장
                return true;
            }
            case BUFFER_OVERFLOW -> {
                demuxAppIn();
                int need = engine.getSession().getApplicationBufferSize();
                if (appIn.remaining() >= need) throw new IOException("unwrap overflow without cause");
                ByteBuffer bigger = ByteBuffer.allocate(appIn.position() + need);
                appIn.flip();
                bigger.put(appIn);
                appIn = bigger;
                return true;
            }
            case CLOSED -> {
                demuxAppIn();
                throw new IOException("TLS closed by peer");
            }
            default -> throw new IOException("unexpected " + r.getStatus());
        }
    }

    private void growNetIn() throws IOException {
        int need = engine.getSession().getPacketBufferSize();
        if (need <= netIn.capacity()) throw new IOException("netIn too small");
        ByteBuffer bigger = ByteBuffer.allocate(need);
        netIn.flip();
        bigger.put(netIn);
        netIn = bigger;
    }

    private void demuxAppIn() {
        boolean any = false;
        appIn.flip();
        try {
            RawPacketTemp t;
            while ((t = PacketManager.demux(appIn)) != null) {
                receivedPacketQueue.offer(t);
                any = true;
            }
        } finally {
            appIn.compact();                           // 불완전한 패킷은 남겨둠
        }
        if (any) {
            synchronized (recvLock) { recvLock.notifyAll(); }
        }
    }

    // ---------- 소켓 쓰기 ----------

    /** @return true: netOut 전부 전송됨 */
    private boolean flushNetOut() throws IOException {
        netOut.flip();
        try {
            while (netOut.hasRemaining() && socketChannel.write(netOut) > 0) { }
        } finally {
            netOut.compact();
        }
        boolean pending = netOut.position() > 0;
        setWriteInterest(pending);
        return !pending;
    }

    private void setWriteInterest(boolean on) {
        synchronized (opsLock) {                       // 앱 스레드와 IO 스레드의 RMW 경합 방지
            try {
                int ops = selectionKey.interestOps();
                selectionKey.interestOps(on ? ops | SelectionKey.OP_WRITE : ops & ~SelectionKey.OP_WRITE);
            } catch (CancelledKeyException ignored) { }
        }
    }

    // ---------- 위임 태스크 ----------

    private boolean startTasks() throws IOException {
        List<Runnable> tasks = new ArrayList<>();
        Runnable t;
        while ((t = engine.getDelegatedTask()) != null) tasks.add(t);
        if (tasks.isEmpty()) throw new IOException("NEED_TASK without delegated task");

        taskRunning.set(true);
        try {
            threadPool.queue(() -> {
                try {
                    tasks.forEach(Runnable::run);
                } catch (Throwable e) {
                    taskError = e;
                } finally {
                    taskRunning.set(false);
                    setWriteInterest(true);            // onWritable → pump로 재개
                    selectionKey.selector().wakeup();
                }
            });
        } catch (RuntimeException e) {
            taskRunning.set(false);
            throw e;
        }
        return false;
    }

    // ---------- 종료 ----------

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            engine.closeOutbound();
            if (!taskRunning.get()) {                  // best-effort close_notify
                engine.wrap(EMPTY, netOut);
                netOut.flip();
                socketChannel.write(netOut);
            }
        } catch (IOException | RuntimeException ignored) {
        } finally {
            selectionKey.cancel();
            try { socketChannel.close(); } catch (IOException ignored) { }
            synchronized (recvLock) { recvLock.notifyAll(); }
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    /** IO 스레드: finishConnect 직후(또는 즉시 연결된 경우) 호출. */
    public void onConnected() {
        synchronized (opsLock) {
            try {
                int ops = selectionKey.interestOps();
                selectionKey.interestOps((ops & ~SelectionKey.OP_CONNECT) | SelectionKey.OP_READ);
            } catch (CancelledKeyException ignored) { }
        }
        startHandshake();
    }

    /** 앱 스레드: 해당 타입의 패킷이 올 때까지 대기. */
    public <P extends Packet> P awaitPacket(Class<P> cls, long timeoutMs)
            throws InterruptedException, TimeoutException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        synchronized (recvLock) {
            while (true) {
                P p = popReceivedPacket(cls);              // 먼저 확인 (도착 후 대기 시작해도 안전)
                if (p != null) return p;
                if (closed.get()) throw new IllegalStateException("Connection closed");
                long leftMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (leftMs <= 0) throw new TimeoutException("Connection timed out");
                recvLock.wait(leftMs);
            }
        }
    }

    public boolean hasReceivedPackets() {
        return !receivedPacketQueue.isEmpty();
    }
}
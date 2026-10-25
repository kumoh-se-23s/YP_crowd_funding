package ypfunding.server.network;

import lombok.Getter;
import ypfunding.common.constant.NetworkConstants;
import ypfunding.common.parallel.ThreadPool;
import ypfunding.common.protocol.PacketManager;
import ypfunding.common.protocol.RawPacketTemp;
import ypfunding.common.protocol.TLSConnection;
import ypfunding.common.util.LogUtil;
import ypfunding.server.RuntimeServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class RequestManager implements AutoCloseable {

    @Getter
    private final RuntimeServer runtimeServer;

    private final Selector selector;
    private final ServerSocketChannel serverSocket;
    private final ThreadPool threadPool;
    private final Map<SocketChannel, RequestResolver> requestResolvers = new HashMap<>();

    public RequestManager(RuntimeServer server) throws IOException {
        this.runtimeServer = server;
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.bind(NetworkConstants.SERVER_ADDRESS);
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        int threads = Runtime.getRuntime().availableProcessors();
        threadPool = new ThreadPool(threads);
    }

    public void start() {
        new Thread(this::selectorLoop).start();
    }

    private void selectorLoop() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey selectedKey = it.next();
                    processSelectedKey(selectedKey);
                    it.remove();
                }

            } catch (IOException e) {
                LogUtil.logError(e);
            }
        }
    }

    private void processSelectedKey(SelectionKey selectionKey) throws IOException {
        for (RequestResolver value : requestResolvers.values()) {
            Runnable task;
            while ((task = value.getRequestEvent().getSelectorTasks().poll()) != null) {
                task.run(); // not a request, it is request handling.
            }
        }

        if (selectionKey.isAcceptable()) {
            SocketChannel srcSocket = serverSocket.accept();
            srcSocket.configureBlocking(false);
            srcSocket.register(selector, SelectionKey.OP_READ);

            requestResolvers.put(srcSocket, new RequestResolver(runtimeServer, selector, srcSocket));
        }
        if (selectionKey.isValid() && selectionKey.isReadable()) {
            SocketChannel srcSocket = (SocketChannel) selectionKey.channel();
            ByteBuffer result;

            while ((result = srcSocket.read()).length > 1) {
                byte[] packet = result;
                threadPool.queue(() -> receiveRequest(srcSocket, packet));
            }
            if (result[0] == -1) {
                selectionKey.cancel();
                requestResolvers.remove(srcSocket);
                srcSocket.close();
            }
        }

        if (selectionKey.isValid() && selectionKey.isWritable()) {
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            RequestResolver handler = requestResolvers.get(channel);
            Queue<ByteBuffer> queue = handler.getRequestEvent().getWriteQueue();

            while (!queue.isEmpty()) {
                ByteBuffer buf = queue.peek();
                channel.write(buf);

                if (buf.hasRemaining()) {
                    break;
                }
                queue.poll();
            }

            if (queue.isEmpty()) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            }
        }

    }

    private void receiveRequest(SocketChannel sender, ByteBuffer buffer) {
        RequestEvent event = requestResolvers.get(sender).getRequestEvent();
        RawPacketTemp raw = PacketManager.demux(buffer);
        if(raw == null) return;

        RequestHandlers.values()[raw.ordinal()].handle(event, raw.payload());
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        selector.close();
    }
}

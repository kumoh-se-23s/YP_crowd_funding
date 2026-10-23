package ypfunding.server.network;

import ypfunding.common.parallel.ThreadPool;
import ypfunding.common.protocol.PacketManager;
import ypfunding.common.protocol.RawPacketTemp;
import ypfunding.common.util.LogUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class RequestManager implements AutoCloseable {

    private final Selector selector;
    private final ServerSocketChannel serverSocket;
    private final ThreadPool threadPool;
    private final Map<SocketChannel, RequestHandler> requestHandlers = new HashMap<>();

    public RequestManager(int port) throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        threadPool = new ThreadPool(Runtime.getRuntime().availableProcessors());
        start();
    }

    private void start() {
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
        for (RequestHandler value : requestHandlers.values()) {
            Runnable task;
            while ((task = value.getRequestEvent().getSelectorTasks().poll()) != null) {
                task.run(); // not a request
            }
        }

        if (selectionKey.isAcceptable()) {
            SocketChannel srcSocket = serverSocket.accept();
            srcSocket.configureBlocking(false);
            srcSocket.register(selector, SelectionKey.OP_READ);
            requestHandlers.put(srcSocket, new RequestHandler(selector, srcSocket));
        }
        if (selectionKey.isValid() && selectionKey.isReadable()) {
            SocketChannel srcSocket = (SocketChannel) selectionKey.channel();
            RequestHandler handler = requestHandlers.get(srcSocket);

            byte[] result;

            while ((result = handler.getPacketProcessor().read()).length > 1) {
                byte[] packet = result;
                threadPool.queue(() -> receiveRequest(srcSocket, packet));
            }
            if (result[0] == -1) {
                selectionKey.cancel();
                requestHandlers.remove(srcSocket);
                srcSocket.close();
            }
        }

        if (selectionKey.isValid() && selectionKey.isWritable()) {
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            RequestHandler handler = requestHandlers.get(channel);
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

    private void receiveRequest(SocketChannel sender, byte[] buffer) {
        RequestEvent event = requestHandlers.get(sender).getRequestEvent();
        RawPacketTemp raw = PacketManager.demultiplex(buffer);
        RequestLinkers.values()[raw.ordinal()].handle(event, raw.payload());
    }

    @Override
    public void close() throws Exception {
        selector.close();
    }
}

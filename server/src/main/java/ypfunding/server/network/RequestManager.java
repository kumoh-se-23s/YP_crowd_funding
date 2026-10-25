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
                threadPool.queue(() -> {
                    Queue<Packet> writeQueue = value.getRequestEvent().getWriteQueue();
                    while (!writeQueue.isEmpty()) {
                        Packet packet = writeQueue.poll();
                        SocketChannel srcSocket = (SocketChannel) selectionKey.channel();
                        TLSConnection connection = runtimeServer.getConnection(srcSocket);
                        connection.queuePacketToSend(packet);
                    }

                });
            }
        }

        if (selectionKey.isAcceptable()) {
            SocketChannel srcSocket = serverSocket.accept();
            srcSocket.configureBlocking(false);
            srcSocket.register(selector, SelectionKey.OP_READ);
            runtimeServer.createConnection(srcSocket, selectionKey);
            requestResolvers.put(srcSocket, new RequestResolver(runtimeServer, selector, srcSocket));

            TLSConnection connection = runtimeServer.getConnection(srcSocket);
            connection.driveTLSHandshake();
        }
        if (selectionKey.isValid() && selectionKey.isReadable()) {
            SocketChannel srcSocket = (SocketChannel) selectionKey.channel();
            TLSConnection connection = runtimeServer.getConnection(srcSocket);
            connection.readTLSData();
            connection.driveTLSHandshake();
            threadPool.queue(() -> receiveRequest(srcSocket, connection.popReceivedPacket()));

        }

        if (selectionKey.isValid() && selectionKey.isWritable()) {
            SocketChannel srcSocket = (SocketChannel) selectionKey.channel();
            TLSConnection connection = runtimeServer.getConnection(srcSocket);
            connection.flushTLSData();
            connection.driveTLSHandshake();
        }

    }

    private void receiveRequest(SocketChannel sender, RawPacketTemp raw) {
        if(raw == null) return;
        RequestEvent event = requestResolvers.get(sender).getRequestEvent();
        RequestHandlers.values()[raw.ordinal()].handle(event, raw.payload());
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        selector.close();
    }
}

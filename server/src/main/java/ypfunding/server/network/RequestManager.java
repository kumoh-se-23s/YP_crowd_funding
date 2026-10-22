package ypfunding.server.network;

import ypfunding.common.protocol.TCPConnectionManager;
import ypfunding.common.parallel.ThreadPool;
import ypfunding.common.util.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
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
        threadPool.start();
        start();
    }

    private void start() {
        new Thread(this::selectorLoop).start();
    }
    private void selectorLoop(){
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey selectedKey = it.next();

                    if (selectedKey.isAcceptable()) {
                        SocketChannel srcSocket = serverSocket.accept();
                        srcSocket.configureBlocking(false);
                        srcSocket.register(selector, SelectionKey.OP_READ);
                        requestHandlers.put(srcSocket, new RequestHandler(srcSocket));
                    }
                    if (selectedKey.isReadable()) {
                        SocketChannel srcSocket = (SocketChannel) selectedKey.channel();
                        RequestHandler handler = requestHandlers.get(srcSocket);

                        byte[] result;
                        while ((result = handler.getPacketProcessor().read()) != null) {
                            byte[] packet = result;
                            threadPool.queue(() -> receiveRequest(srcSocket, packet));
                        }
                    }
                    it.remove();
                }

            } catch (IOException e) {
                LogUtil.logError(e);
            }
        }
    }

    private void receiveRequest(SocketChannel sender, byte[] buffer) {
        RequestEvent event = requestHandlers.get(sender).getRequestEvent();

        try {

            byte[] payloadBuf = new byte[buffer.length - TCPConnectionManager.PACKET_LEN_HEADER_SIZE - TCPConnectionManager.MESSAGE_TYPE_HEADER_SIZE];
            DataInputStream payload = new DataInputStream(new ByteArrayInputStream(payloadBuf));
            int requestNum = (buffer[TCPConnectionManager.PACKET_LEN_HEADER_SIZE] & 0xff) | ((buffer[TCPConnectionManager.PACKET_LEN_HEADER_SIZE + 1] & 0xff) << 8); // the size of request number is 2 byte
            System.arraycopy(buffer, TCPConnectionManager.PACKET_LEN_HEADER_SIZE + TCPConnectionManager.MESSAGE_TYPE_HEADER_SIZE, payloadBuf, 0, payloadBuf.length);

            if (requestNum >= RequestLinkers.values().length) {
                // manipulated-client can send invalid request number
                throw new IOException("Invalid request number: " + requestNum);
            }

            RequestLinkers.values()[requestNum].handle(event, payload);

        } catch (SocketException _) {
            // no data provided
        } catch (IOException e) {
            event.reply(e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        selector.close();
    }
}

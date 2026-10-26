package ypfunding.server.network;

import lombok.Getter;
import ypfunding.common.protocol.packets.MessagePacket;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.server.RuntimeServer;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RequestEvent {
    private final Selector selector;

    @Getter
    private final RuntimeServer server;
    @Getter
    private final SocketChannel srcSocket;
    @Getter
    private final Queue<Packet> writeQueue = new ConcurrentLinkedQueue<>();
    @Getter
    private final Queue<Runnable> selectorTasks = new ConcurrentLinkedQueue<>();

    public RequestEvent(Selector selector, RuntimeServer server, SocketChannel srcSocket) {
        this.selector = selector;
        this.server = server;
        this.srcSocket = srcSocket;
    }

    public void reply(Packet packet) {
        selectorTasks.offer(() -> {
            SelectionKey key = srcSocket.keyFor(selector);
            if (key == null || !key.isValid()) {
                throw new IllegalStateException("Invalid reply target");
            }
            writeQueue.offer(packet);
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        });
    }

    public void reply(String message) {

        MessagePacket msgPkt = new MessagePacket();
        msgPkt.setMessage(message);

        reply(msgPkt);
    }


}

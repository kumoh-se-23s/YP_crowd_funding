package ypfunding.server.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class RequestHandler {
    private final RequestEvent requestEvent;
    private final PacketProcessor packetProcessor;

    public RequestHandler(SocketChannel channel) throws IOException {
        this.requestEvent = new RequestEvent(channel);
        this.packetProcessor = new PacketProcessor(channel);
    }


    public PacketProcessor getPacketProcessor() {
        return packetProcessor;
    }

    public RequestEvent getRequestEvent() {
        return requestEvent;
    }
}

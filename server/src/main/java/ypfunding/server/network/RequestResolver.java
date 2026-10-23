package ypfunding.server.network;

import lombok.Getter;
import ypfunding.common.protocol.PacketProcessor;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

@Getter
public class RequestResolver {
    private final RequestEvent requestEvent;
    private final PacketProcessor packetProcessor;

    public RequestResolver(Selector selector, SocketChannel channel) throws IOException {
        this.requestEvent = new RequestEvent(selector, channel);
        this.packetProcessor = new PacketProcessor(channel);
    }


}

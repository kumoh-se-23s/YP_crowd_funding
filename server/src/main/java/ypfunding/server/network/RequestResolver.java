package ypfunding.server.network;

import lombok.Getter;
import ypfunding.server.RuntimeServer;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

@Getter
public class RequestResolver {
    private final RuntimeServer runtimeServer;
    private final RequestEvent requestEvent;

    public RequestResolver(RuntimeServer runtimeServer, Selector selector, SocketChannel channel) {
        this.runtimeServer = runtimeServer;
        this.requestEvent = new RequestEvent(selector, channel);
    }


}

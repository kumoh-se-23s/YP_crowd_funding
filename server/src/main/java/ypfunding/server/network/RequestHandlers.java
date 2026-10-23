package ypfunding.server.network;

import ypfunding.common.protocol.packets.Packets;

import java.nio.ByteBuffer;

public enum RequestHandlers {
    MESSAGE(Packets.MESSAGE, new MessagePacketHandler()),
    NOTIFICATION(Packets.NOTIFICATION, new NotificationPacketHandler()),
    ;

    private final Packets packets;
    private final RequestHandler handler;

    RequestHandlers(Packets packets, RequestHandler handler) {
        this.packets = packets;
        this.handler = handler;
    }

    public Packets getRequests() {
        return packets;
    }

    public void handle(RequestEvent event, ByteBuffer payload) {
        handler.handle(event, payload);
    }
}

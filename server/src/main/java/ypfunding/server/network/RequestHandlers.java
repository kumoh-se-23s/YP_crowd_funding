package ypfunding.server.network;

import ypfunding.common.protocol.PacketManager;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.protocol.packets.Packets;

import java.nio.ByteBuffer;

public enum RequestHandlers {
    MESSAGE(Packets.MESSAGE, new MessagePacketHandler()),
    NOTIFICATION(Packets.NOTIFICATION, new NotificationPacketHandler()),
    ;

    private final Class<? extends Packet> packetClass;
    private final RequestHandler<Packet> handler;

    @SuppressWarnings("unchecked")
    <P extends Packet> RequestHandlers(Packets packets, RequestHandler<P> handler) {
        this.packetClass = packets.getPacketClass();
        this.handler = (event, packet) -> handler.handle(event, (P) packet);
    }

    public void handle(RequestEvent event, ByteBuffer payload) {
        Packet p = PacketManager.decodePacket(payload, packetClass);
        handler.handle(event, p);
    }
}

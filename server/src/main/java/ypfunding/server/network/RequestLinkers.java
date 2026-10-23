package ypfunding.server.network;

import ypfunding.common.protocol.packets.Packets;

import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public enum RequestLinkers {
        MESSAGE(Packets.MESSAGE, (event, payload) -> {}),
        NOTIFICATION(Packets.NOTIFICATION, (event, payload) -> {}),
    ;

    private final Packets packets;
    private final BiConsumer<RequestEvent, ByteBuffer> managerConsumer;

    RequestLinkers(Packets packets, BiConsumer<RequestEvent, ByteBuffer> managerConsumer) {
        this.packets = packets;
        this.managerConsumer = managerConsumer;
    }

    public Packets getRequests() {
        return packets;
    }

    public void handle(RequestEvent event, ByteBuffer payload) {
        managerConsumer.accept(event, payload);
    }
}

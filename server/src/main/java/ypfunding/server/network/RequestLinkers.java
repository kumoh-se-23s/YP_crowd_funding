package ypfunding.server.network;

import ypfunding.common.protocol.packets.Packets;

import java.io.DataInputStream;
import java.util.function.BiConsumer;

public enum RequestLinkers {

    ;

    private final Packets packets;
    private final BiConsumer<RequestEvent, DataInputStream> managerConsumer;

    RequestLinkers(Packets packets, BiConsumer<RequestEvent, DataInputStream> managerConsumer) {
        this.packets = packets;
        this.managerConsumer = managerConsumer;
    }

    public Packets getRequests() {
        return packets;
    }

    public void handle(RequestEvent event, DataInputStream payload) {
        managerConsumer.accept(event, payload);
    }
}

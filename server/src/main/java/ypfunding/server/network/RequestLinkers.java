package ypfunding.server.network;

import ypfunding.common.protocol.Requests;

import java.io.DataInputStream;
import java.util.function.BiConsumer;

public enum RequestLinkers {

    ;

    private final Requests requests;
    private final BiConsumer<RequestEvent, DataInputStream> managerConsumer;

    RequestLinkers(Requests requests, BiConsumer<RequestEvent, DataInputStream> managerConsumer) {
        this.requests = requests;
        this.managerConsumer = managerConsumer;
    }

    public Requests getRequests() {
        return requests;
    }

    public void handle(RequestEvent event, DataInputStream payload) {
        managerConsumer.accept(event, payload);
    }
}

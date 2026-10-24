package ypfunding.server.network;

import ypfunding.common.protocol.packets.Packet;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface RequestHandler<P extends Packet> {

    void handle(RequestEvent event, P packet);
}

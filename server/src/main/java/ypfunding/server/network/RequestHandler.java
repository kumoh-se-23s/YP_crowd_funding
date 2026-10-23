package ypfunding.server.network;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface RequestHandler {

    void handle(RequestEvent event, ByteBuffer buffer);
}

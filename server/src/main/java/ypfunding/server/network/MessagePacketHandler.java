package ypfunding.server.network;

import java.nio.ByteBuffer;

public class MessagePacketHandler implements RequestHandler{
    @Override
    public void handle(RequestEvent event, ByteBuffer buffer) {
        //TODO
        event.reply("IRECEIVEDMESSAGE");
    }
}

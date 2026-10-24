package ypfunding.server.network;

import ypfunding.common.protocol.packets.MessagePacket;

import java.nio.ByteBuffer;

public class MessagePacketHandler implements RequestHandler<MessagePacket>{
    @Override
    public void handle(RequestEvent event, MessagePacket packet) {
        //TODO
        event.reply("IRECEIVEDMESSAGE");
    }
}

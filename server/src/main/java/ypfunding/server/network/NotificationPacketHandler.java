package ypfunding.server.network;

import ypfunding.common.protocol.packets.MessagePacket;

import java.nio.ByteBuffer;

public class NotificationPacketHandler implements RequestHandler{
    @Override
    public void handle(RequestEvent event, ByteBuffer buffer) {
        //TODO
        event.reply("WOJASOIDJOIDJVOAISDJN OISDA");
    }
}

package ypfunding.server.network;

import ypfunding.common.protocol.packets.MessagePacket;
import ypfunding.common.protocol.packets.NotificationPacket;

import java.nio.ByteBuffer;

public class NotificationPacketHandler implements RequestHandler<NotificationPacket>{
    @Override
    public void handle(RequestEvent event, NotificationPacket packet) {
        //TODO
        event.reply("WOJASOIDJOIDJVOAISDJN OISDA");
    }
}

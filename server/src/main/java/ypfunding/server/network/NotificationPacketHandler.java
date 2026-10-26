package ypfunding.server.network;

import ypfunding.common.protocol.packets.NotificationPacket;

public class NotificationPacketHandler implements RequestHandler<NotificationPacket>{
    @Override
    public void handle(RequestEvent event, NotificationPacket packet) {
        //TODO
        event.reply("WOJASOIDJOIDJVOAISDJN OISDA");
    }
}

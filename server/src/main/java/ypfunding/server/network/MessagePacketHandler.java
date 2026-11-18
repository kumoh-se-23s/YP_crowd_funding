package ypfunding.server.network;

import ypfunding.common.protocol.packets.request.MessagePacket;
import ypfunding.common.util.LogUtil;

public class MessagePacketHandler implements RequestHandler<MessagePacket>{
    @Override
    public void handle(RequestEvent event, MessagePacket packet) {
        //TODO
        event.reply("Success");
        LogUtil.log("Received message: " + packet.getMessage());
    }
}

package ypfunding.server.network;

import ypfunding.common.protocol.packets.LoginPacket;

import java.nio.channels.SocketChannel;

public class LoginPacketHandler implements RequestHandler<LoginPacket> {
    @Override
    public void handle(RequestEvent event, LoginPacket packet) {
        SocketChannel socket = event.getSrcSocket();


    }
}

package ypfunding.server.network;

import ypfunding.common.protocol.packets.LoginPacket;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;

import java.nio.channels.SocketChannel;

public class LoginPacketHandler implements RequestHandler<LoginPacket> {
    @Override
    public void handle(RequestEvent event, LoginPacket packet) {
        LoginUser user;
        try {
            user = event.getServer().getAppContext().getAuthService().login(packet.getId(), packet.getPassword());
        } catch (BusinessException e) {
            event.reply(e.getMessage());
            return;
        }

        SocketChannel socket = event.getSrcSocket();
        event.getServer().addUser(socket, user);
        event.reply("Login successful!");
    }
}

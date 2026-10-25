package ypfunding.server.network;

import ypfunding.common.protocol.packets.LoginPacket;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.persistence.dto.UserLoginDTO;
import ypfunding.server.util.SaltEncryptUtils;

import java.nio.channels.SocketChannel;

public class LoginPacketHandler implements RequestHandler<LoginPacket> {
    @Override
    public void handle(RequestEvent event, LoginPacket packet) {
        UserLoginDTO userLoginDTO = event.getServer().getAppContext().getManagedService().getUserByLoginId(packet.getId());
        if(userLoginDTO == null){
            event.reply("Invalid login id or password!");
            return;
        }

        String lhs = SaltEncryptUtils.encrypt(packet.getPassword(), userLoginDTO.getSalt());
        String rhs = userLoginDTO.getEncryptedPassword();

        if(!lhs.equals(rhs)){
            event.reply("Invalid login id or password!");
            return;
        }

        SocketChannel socket = event.getSrcSocket();
        event.getServer().addUser(socket, new LoginUser(userLoginDTO.getUserId(), userLoginDTO.getUserType()));
        event.reply("Login successful!");
    }
}

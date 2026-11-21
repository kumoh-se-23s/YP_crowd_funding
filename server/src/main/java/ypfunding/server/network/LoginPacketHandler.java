package ypfunding.server.network;

import ypfunding.common.dto.UserDTO;
import ypfunding.common.protocol.packets.request.RequestLoginPacket;
import ypfunding.common.protocol.packets.response.ResponseLoginPacket;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;

public class LoginPacketHandler implements RequestHandler<RequestLoginPacket> {
    static final String ALREADY_LOGGED_IN_MESSAGE = "이미 로그인되어 있습니다. 로그아웃 후 다시 로그인하세요.";

    @Override
    public void handle(RequestEvent event, RequestLoginPacket packet) {
        if (event.getLoginUser() != null) {
            event.fail(ALREADY_LOGGED_IN_MESSAGE);
            return;
        }

        AppContext context = event.getServer().getAppContext();
        LoginUser user = context.getAuthService().login(packet.getId(), packet.getPassword());
        UserDTO info = context.getUserService().getMyInfo(user);

        event.login(user);                             // 이 연결 = 이 사용자 (이후 요청은 userId 없이 처리)

        ResponseLoginPacket response = new ResponseLoginPacket();
        response.setUser(info);
        event.reply(response);
    }
}

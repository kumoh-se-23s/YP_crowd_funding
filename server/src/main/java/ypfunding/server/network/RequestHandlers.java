package ypfunding.server.network;

import ypfunding.common.protocol.PacketManager;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.protocol.packets.RequestPackets;
import ypfunding.common.util.LogUtil;
import ypfunding.server.exception.BusinessException;

import java.nio.ByteBuffer;

public enum RequestHandlers {
    LOGIN(RequestPackets.LOGIN, new LoginPacketHandler()),
    FUND(RequestPackets.FUND, new FundPacketHandler()),
    MESSAGE(RequestPackets.MESSAGE, new MessagePacketHandler()),
    ;

    static final String LOGIN_REQUIRED_MESSAGE = "로그인이 필요합니다.";
    static final String SERVER_ERROR_MESSAGE = "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";

    static { // RequestPackets 순서와 다르면 다른 기능의 핸들러가 실행되므로 시작 단계에서 검증하고 동작 막기
        RequestPackets[] packets = RequestPackets.values();
        RequestHandlers[] handlers = values();
        if (packets.length != handlers.length) {
            throw new IllegalStateException("RequestHandlers must cover every RequestPackets entry");
        }
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i].packets != packets[i]) {
                throw new IllegalStateException("RequestHandlers order mismatch at " + i + ": " + handlers[i]);
            }
        }
    }

    private final RequestPackets packets;
    private final Class<? extends Packet> packetClass;
    private final RequestHandler<Packet> handler;

    @SuppressWarnings("unchecked")
    <P extends Packet> RequestHandlers(RequestPackets packets, RequestHandler<P> handler) {
        this.packets = packets;
        this.packetClass = packets.getPacketClass();
        this.handler = (event, packet) -> handler.handle(event, (P) packet);
    }

    //로그인 안 해도 쓸 수 있는 기능인지 리턴
    private boolean isPublic() {
        return this == LOGIN || this == MESSAGE;
    }

    public void handle(RequestEvent event, ByteBuffer payload) {
        if (!isPublic() && event.getLoginUser() == null) {
            event.fail(LOGIN_REQUIRED_MESSAGE);
            return;
        }
        try {
            Packet p = PacketManager.decodePacket(payload, packetClass);
            handler.handle(event, p);
        } catch (BusinessException e) {
            event.fail(e.getMessage()); // 업무 규칙 위반
        } catch (RuntimeException e) {
            LogUtil.logError("Request " + this + " failed: " + e);
            event.fail(SERVER_ERROR_MESSAGE); // 내부 오류 내용은 클라이언트에 보내지 않음
        }
    }
}

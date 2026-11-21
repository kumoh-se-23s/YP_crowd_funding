package ypfunding.common.protocol;

import ypfunding.common.dto.UserDTO;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.protocol.packets.PacketType;
import ypfunding.common.protocol.packets.RequestPackets;
import ypfunding.common.protocol.packets.ResponsePackets;
import ypfunding.common.protocol.packets.ResultPackets;
import ypfunding.common.protocol.packets.request.MessagePacket;
import ypfunding.common.protocol.packets.request.RequestFundPacket;
import ypfunding.common.protocol.packets.request.RequestLoginPacket;
import ypfunding.common.protocol.packets.response.NotificationPacket;
import ypfunding.common.protocol.packets.response.ResponseLoginPacket;
import ypfunding.common.protocol.packets.result.ResultFailedPacket;
import ypfunding.common.protocol.packets.result.ResultSuccessPacket;
import ypfunding.common.util.LogUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

//패킷 내용 로거
public class PacketLogger {
    public static final boolean ENABLED = true;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int BYTES_PER_LINE = 16;
    private static final String INDENT = "        ";

    // mux에서 호출. frame = 헤더 + 바디 전체 (flip된 상태)
    static void logSend(Packet packet, ByteBuffer frame) {
        if (!ENABLED) return;
        ByteBuffer f = frame.duplicate(); // 원본 position 손 안댐
        int type = Byte.toUnsignedInt(f.get());
        int code = Byte.toUnsignedInt(f.get());
        f.getShort(); // LENGTH는 바디 크기로 다시 계산
        print("SEND", type, code, f.slice(), packet);
    }

    // demux에서 호출. 바디를 한 번 더 디코딩해서 로그 (원본 버퍼는 그대로)
    static void logReceive(RawPacketTemp raw) {
        if (!ENABLED) return;
        Packet decoded = null;
        try {
            decoded = PacketManager.decodePacket(raw.payload().duplicate(), raw.packetClass());
        } catch (RuntimeException ignored) {
            // 내용 해석에 실패해도 바이트는 출력
        }
        print("RECV", raw.packetType().ordinal(), raw.ordinal(), raw.payload().duplicate(), decoded);
    }

    private static void print(String direction, int type, int code, ByteBuffer body, Packet packet) {
        PacketType packetType = PacketType.values()[type];
        byte[] bytes = new byte[body.remaining()];
        body.get(bytes);
        int[] masked = passwordRange(packet);

        StringBuilder sb = new StringBuilder();
        sb.append('[').append(LocalTime.now().format(TIME)).append("] ")
                .append(direction).append(' ').append(packetType).append(' ').append(codeName(packetType, code))
                .append("  (TYPE=").append(type).append(", CODE=").append(code).append(", LENGTH=").append(bytes.length).append(")\n");
        sb.append(INDENT).append("내용 : ").append(describe(packet)).append('\n');
        sb.append(INDENT).append("헤더 : ").append(String.format("%02X %02X %02X %02X",
                type, code, (bytes.length >> 8) & 0xFF, bytes.length & 0xFF)).append('\n');
        sb.append(INDENT).append("바디 : ");
        if (bytes.length == 0) {
            sb.append("(없음)");
        }
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && i % BYTES_PER_LINE == 0) sb.append('\n').append(INDENT).append("       ");
            boolean hide = masked != null && i >= masked[0] && i < masked[0] + masked[1];
            sb.append(hide ? "**" : String.format("%02X", bytes[i])).append(' ');
        }
        LogUtil.log(sb.toString().stripTrailing());
    }

    // CODE → 기능 이름 (TYPE마다 표가 다름)
    private static String codeName(PacketType type, int code) {
        Enum<?>[] table = switch (type) {
            case REQUEST -> RequestPackets.values();
            case RESPONSE -> ResponsePackets.values();
            case RESULT -> ResultPackets.values();
        };
        return code < table.length ? table[code].name() : "UNKNOWN";
    }

    // 패킷별 내용. 새 패킷을 추가하면 여기에 한 줄 추가 (없으면 클래스 이름만 출력)
    private static String describe(Packet p) {
        return switch (p) {
            case null -> "(해석 실패)";
            case RequestLoginPacket r -> "id=" + r.getId() + ", password=" + "*".repeat(r.getPassword().length());
            case RequestFundPacket r -> "rewardId=" + r.getRewardId() + ", quantity=" + r.getQuantity();
            case ResponseLoginPacket r -> {
                UserDTO u = r.getUser();
                yield "userId=" + u.getUserId() + ", loginId=" + u.getLoginId() + ", name=" + u.getName()
                        + ", address=" + u.getAddress() + ", userType=" + u.getUserType();
            }
            case ResultSuccessPacket ignored -> "(바디 없음)";
            case ResultFailedPacket r -> "reason=" + r.getReason();
            case MessagePacket r -> "message=" + r.getMessage();
            case NotificationPacket r -> "type=" + r.getNotification().getType() + ", message=" + r.getNotification().getMessage();
            default -> p.getClass().getSimpleName();
        };
    }

    // 로그인 요청 바디에서 비밀번호 바이트 위치 {시작, 길이}. 구조 : idLen(4) id passwordLen(4) password
    private static int[] passwordRange(Packet p) {
        if (!(p instanceof RequestLoginPacket r)) return null;
        int idBytes = r.getId().getBytes(StandardCharsets.UTF_8).length;
        int pwBytes = r.getPassword().getBytes(StandardCharsets.UTF_8).length;
        return new int[]{4 + idBytes + 4, pwBytes};
    }
}

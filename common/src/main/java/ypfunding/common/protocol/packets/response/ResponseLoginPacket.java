package ypfunding.common.protocol.packets.response;

import lombok.Getter;
import lombok.Setter;
import ypfunding.common.dto.UserDTO;
import ypfunding.common.protocol.packets.ResponsePacket;
import ypfunding.common.type.UserType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class ResponseLoginPacket implements ResponsePacket{
    private UserDTO user;

    @Override
    public void decode(ByteBuffer in) {
        user = new UserDTO();
        user.setUserId(in.getLong());
        user.setLoginId(readString(in));
        user.setName(readString(in));
        user.setAddress(readString(in));
        user.setUserType(UserType.valueOf(readString(in)));
    }

    @Override
    public void encode(ByteBuffer out) {
        out.putLong(user.getUserId());
        writeString(out, user.getLoginId());
        writeString(out, user.getName());
        writeString(out, user.getAddress());
        writeString(out, user.getUserType().name());
    }


    private static String readString(ByteBuffer in) {
        byte[] raw = new byte[in.getInt()];
        in.get(raw);
        return new String(raw, StandardCharsets.UTF_8);
    }

    private static void writeString(ByteBuffer out, String value) {
        byte[] raw = value.getBytes(StandardCharsets.UTF_8);
        out.putInt(raw.length);
        out.put(raw);
    }
}

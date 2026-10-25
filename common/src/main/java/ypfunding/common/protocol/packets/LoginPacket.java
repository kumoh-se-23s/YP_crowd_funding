package ypfunding.common.protocol.packets;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class LoginPacket implements Packet {

    String id;
    String password;

    @Override
    public void decode(ByteBuffer in) {
        int idLength = in.getInt();
        byte[] idRaw = new byte[idLength];
        in.get(idRaw);
        id = new String(idRaw, StandardCharsets.UTF_8);

        int passwordLength = in.getInt();
        byte[] passwordRaw = new byte[passwordLength];
        in.get(passwordRaw);
        password = new String(passwordRaw, StandardCharsets.UTF_8);
    }

    @Override
    public void encode(ByteBuffer out) {
        byte[] idRaw = id.getBytes(StandardCharsets.UTF_8);
        out.putInt(idRaw.length);
        out.put(idRaw);
        byte[] passwordRaw = password.getBytes(StandardCharsets.UTF_8);
        out.putInt(passwordRaw.length);
        out.put(passwordRaw);
    }
}

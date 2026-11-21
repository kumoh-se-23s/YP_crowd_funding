package ypfunding.common.protocol.packets.result;

import lombok.Getter;
import lombok.Setter;
import ypfunding.common.protocol.packets.ResultPacket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class ResultFailedPacket implements ResultPacket {

    private String reason;

    @Override
    public void decode(ByteBuffer in) {
        byte[] raw = new byte[in.getInt()];
        in.get(raw);
        reason = new String(raw, StandardCharsets.UTF_8);
    }

    @Override
    public void encode(ByteBuffer out) {
        byte[] raw = reason.getBytes(StandardCharsets.UTF_8);
        out.putInt(raw.length);
        out.put(raw);

    }


}

package ypfunding.common.protocol.packets;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class MessagePacket implements Packet{

    @Setter
    @Getter
    private String message;


    @Override
    public void decode(ByteBuffer in) {
        byte[] raw = new byte[in.getInt()];
        in.get(raw);
        message = new String(raw, StandardCharsets.UTF_8);
    }

    @Override
    public void encode(ByteBuffer out) {
        byte[] raw = message.getBytes(StandardCharsets.UTF_8);
        out.putInt(raw.length);
        out.put(raw);
    }
}

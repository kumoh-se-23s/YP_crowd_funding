package ypfunding.common.protocol.packets;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.ByteBuffer;

@Setter
public class LoginPacket implements Packet {

    String id;
    String password;


    @Override
    public void decode(ByteBuffer in) throws IOException {
        //noop
    }

    @Override
    public void encode(ByteBuffer out) throws IOException {
        //noop
    }
}

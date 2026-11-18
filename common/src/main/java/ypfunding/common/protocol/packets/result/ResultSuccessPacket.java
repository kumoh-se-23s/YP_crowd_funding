package ypfunding.common.protocol.packets.result;

import lombok.Setter;
import ypfunding.common.protocol.packets.ResultPacket;

import java.nio.ByteBuffer;

@Setter
public class ResultSuccessPacket implements ResultPacket {


    @Override
    public void decode(ByteBuffer in) {
    }

    @Override
    public void encode(ByteBuffer out) {
    }


}

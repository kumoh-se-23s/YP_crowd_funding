package ypfunding.common.protocol.packets.request;

import lombok.Getter;
import lombok.Setter;
import ypfunding.common.protocol.packets.RequestPacket;

import java.nio.ByteBuffer;

@Getter
@Setter
public class RequestFundPacket implements  RequestPacket{
    private long rewardId;
    private int quantity;

    @Override
    public void decode(ByteBuffer in) {
        rewardId = in.getLong();
        quantity = in.getInt();
    }

    @Override
    public void encode(ByteBuffer out) {
        out.putLong(rewardId);
        out.putInt(quantity);
    }
}

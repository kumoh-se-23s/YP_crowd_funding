package ypfunding.common.protocol.packets;

import lombok.Getter;
import ypfunding.common.protocol.packets.request.LoginPacket;
import ypfunding.common.protocol.packets.request.MessagePacket;
import ypfunding.common.protocol.packets.result.ResultFailedPacket;
import ypfunding.common.protocol.packets.result.ResultSuccessPacket;

@Getter
public enum ResultPackets implements Packets{
    SUCCESS(ResultSuccessPacket.class),
    FAILED(ResultFailedPacket.class),
    ;
    private final Class<? extends ResultPacket> packetClass;

    ResultPackets(Class<? extends ResultPacket> packetClass) {
        this.packetClass = packetClass;
    }

    @Override
    public Class<? extends ResultPacket> getPacketClass() {
        return packetClass;
    }
}

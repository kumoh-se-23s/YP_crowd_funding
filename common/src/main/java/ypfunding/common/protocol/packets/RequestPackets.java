package ypfunding.common.protocol.packets;

import lombok.Getter;
import ypfunding.common.protocol.packets.request.RequestLoginPacket;
import ypfunding.common.protocol.packets.request.RequestFundPacket;
import ypfunding.common.protocol.packets.request.MessagePacket;

@Getter
public enum RequestPackets implements Packets{
    LOGIN(RequestLoginPacket.class),
    FUND(RequestFundPacket.class),
    MESSAGE(MessagePacket.class),
    ;
    private final Class<? extends RequestPacket> packetClass;

    RequestPackets(Class<? extends RequestPacket> packetClass) {
        this.packetClass = packetClass;
    }

    @Override
    public Class<? extends RequestPacket> getPacketClass() {
        return packetClass;
    }
}

package ypfunding.common.protocol.packets;

import lombok.Getter;
import ypfunding.common.protocol.packets.response.ResponseLoginPacket;
import ypfunding.common.protocol.packets.response.NotificationPacket;

@Getter
public enum ResponsePackets implements Packets{
    LOGIN(ResponseLoginPacket.class),
    NOTIFICATION(NotificationPacket.class),
    ;
    private final Class<? extends ResponsePacket> packetClass;

    ResponsePackets(Class<? extends ResponsePacket> packetClass) {
        this.packetClass = packetClass;
    }

    @Override
    public Class<? extends ResponsePacket> getPacketClass() {
        return packetClass;
    }
}

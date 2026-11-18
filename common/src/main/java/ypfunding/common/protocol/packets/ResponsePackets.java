package ypfunding.common.protocol.packets;

import lombok.Getter;
import ypfunding.common.protocol.packets.request.LoginPacket;
import ypfunding.common.protocol.packets.request.MessagePacket;
import ypfunding.common.protocol.packets.response.NotificationPacket;

@Getter
public enum ResponsePackets implements Packets{
    LOGIN(LoginPacket.class),
    MESSAGE(MessagePacket.class),
    NOTIFICATION(NotificationPacket.class),
    ;
    private final Class<? extends Packet> packetClass;

    ResponsePackets(Class<? extends Packet> packetClass) {
        this.packetClass = packetClass;
    }

    @Override
    public Class<? extends Packet> getPacketClass() {
        return packetClass;
    }
}

package ypfunding.common.protocol.packets;

import lombok.Getter;
import ypfunding.common.protocol.packets.request.LoginPacket;
import ypfunding.common.protocol.packets.request.MessagePacket;

@Getter
public enum RequestPackets implements Packets{
    LOGIN(LoginPacket.class),
    MESSAGE(MessagePacket.class),
    ;
    private final Class<? extends Packet> packetClass;

    RequestPackets(Class<? extends Packet> packetClass) {
        this.packetClass = packetClass;
    }

    @Override
    public Class<? extends Packet> getPacketClass() {
        return packetClass;
    }
}

package ypfunding.common.protocol.packets;

import lombok.Getter;

@Getter
public enum Packets {
    LOGIN(LoginPacket.class),
    MESSAGE(MessagePacket.class),
    NOTIFICATION(NotificationPacket.class),
    ;
    private final Class<? extends Packet> packetClass;

    Packets(Class<? extends Packet> packetClass) {
        this.packetClass = packetClass;
    }

}

package ypfunding.common.protocol.packets;

import lombok.Getter;

@Getter
public enum Packets {
    LOGIN(LoginPacket.class),
    NOTIFICATION(NotificationPacket.class),
    MESSAGE(MessagePacket.class),
    ;
    private final Class<? extends Packet> packetClass;

    Packets(Class<? extends Packet> packetClass) {
        this.packetClass = packetClass;
    }

}

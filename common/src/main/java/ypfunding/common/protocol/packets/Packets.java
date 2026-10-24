package ypfunding.common.protocol.packets;

public enum Packets {
    NOTIFICATION(NotificationPacket.class),
    MESSAGE(MessagePacket.class)
    ;
    private final Class<? extends Packet> packetClass;

    Packets(Class<? extends Packet> packetClass) {
        this.packetClass = packetClass;
    }

    public Class<? extends Packet> getPacketClass() {
        return packetClass;
    }
}

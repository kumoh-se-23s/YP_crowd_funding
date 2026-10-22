package ypfunding.common.protocol;

public enum Requests {
    NOTIFICATION(NotificationPacket.class)
    ;
    private final Class<? extends Packet> packetClass;

    Requests(Class<? extends Packet> packetClass) {
        this.packetClass = packetClass;
    }

    public Class<? extends Packet> getPacketClass() {
        return packetClass;
    }
}

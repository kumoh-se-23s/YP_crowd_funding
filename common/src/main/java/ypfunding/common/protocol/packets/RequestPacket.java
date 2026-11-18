package ypfunding.common.protocol.packets;

public interface RequestPacket extends Packet {

    @Override
    default PacketType getType() {
        return PacketType.REQUEST;
    }
}

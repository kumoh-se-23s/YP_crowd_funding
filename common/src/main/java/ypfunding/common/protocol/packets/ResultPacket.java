package ypfunding.common.protocol.packets;

public interface ResultPacket extends Packet{

    @Override
    default PacketType getType() {
        return PacketType.RESULT;
    }
}

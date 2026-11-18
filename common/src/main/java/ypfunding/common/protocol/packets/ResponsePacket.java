package ypfunding.common.protocol.packets;

public interface ResponsePacket extends Packet{

    @Override
    default PacketType getType() {
        return PacketType.RESPONSE;
    }
}

package ypfunding.common.protocol;

import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.protocol.packets.PacketType;

import java.nio.ByteBuffer;

public record RawPacketTemp(PacketType packetType, int ordinal, Class<? extends Packet> packetClass, ByteBuffer payload) {
}

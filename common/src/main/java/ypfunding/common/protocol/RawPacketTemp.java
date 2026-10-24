package ypfunding.common.protocol;

import ypfunding.common.protocol.packets.Packet;

import java.nio.ByteBuffer;

public record RawPacketTemp(int ordinal, Class<? extends Packet> packetClass, ByteBuffer payload) {
}

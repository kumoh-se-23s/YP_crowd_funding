package ypfunding.common.protocol.packets;

import lombok.Getter;

public interface Packets {

    Class<? extends Packet> getPacketClass();

}

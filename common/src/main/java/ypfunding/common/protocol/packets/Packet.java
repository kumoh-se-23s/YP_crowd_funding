package ypfunding.common.protocol.packets;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Packet {



    void decode(ByteBuffer in) throws IOException;

    void encode(ByteBuffer out) throws IOException;
}

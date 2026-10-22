package ypfunding.common.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Packet {



    void decode(ByteBuffer in) throws IOException;

    void encode(ByteBuffer out) throws IOException;
}

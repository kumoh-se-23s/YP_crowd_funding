package ypfunding.client.network;

import ypfunding.common.protocol.packets.Packet;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface ClientNetworkConnector extends AutoCloseable{

    <S extends Packet, R extends Packet> R sendAndWaitReply(S packetToSend, Class<R> packetToReceiveClass) throws TimeoutException;

    void sendPacket(Packet packet);

    @Override
    void close() throws IOException;
}

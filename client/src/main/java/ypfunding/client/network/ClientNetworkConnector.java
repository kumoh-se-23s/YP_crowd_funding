package ypfunding.client.network;

import ypfunding.common.protocol.packets.Packet;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public interface ClientNetworkConnector extends AutoCloseable{

    <S extends Packet, R extends Packet> R sendAndWaitReply(S packetToSend, Class<R> packetToReceiveClass) throws TimeoutException;

    void sendPacket(Packet packet);

    void start();
    @Override
    void close() throws IOException;

    void pause(Consumer<String> consoleInput);
}

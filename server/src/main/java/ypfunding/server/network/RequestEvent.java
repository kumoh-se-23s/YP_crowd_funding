package ypfunding.server.network;

import ypfunding.common.protocol.Packet;
import ypfunding.common.protocol.TCPConnectionManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class RequestEvent {

    public SocketChannel srcSocket;

    public RequestEvent(SocketChannel srcSocket){
        this.srcSocket = srcSocket;
    }

    public void reply(String message){
        //TODO
        ByteBuffer buf = ByteBuffer.allocate(TCPConnectionManager.MAX_BUFFER_SIZE);
        byte[] arr = message.getBytes(StandardCharsets.UTF_8);
        buf.putInt(arr.length);
        buf.put(arr);
        try {
            srcSocket.write(buf);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reply " + e);
        }
    }
}

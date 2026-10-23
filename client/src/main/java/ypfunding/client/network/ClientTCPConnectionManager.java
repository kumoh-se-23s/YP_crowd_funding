package ypfunding.client.network;

import ypfunding.common.parallel.ThreadPool;
import ypfunding.common.protocol.PacketManager;
import ypfunding.common.protocol.PacketProcessor;
import ypfunding.common.protocol.packets.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class ClientTCPConnectionManager {

    private final SocketChannel dstSocket;
    private final ThreadPool workerThread;
    private final PacketProcessor packetProcessor;

    //TODO : type the server address here
    private static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("127.0.0.1", 2222);



    public ClientTCPConnectionManager() { // the server address is constant
        try {
            dstSocket = SocketChannel.open();
            dstSocket.configureBlocking(true);
            dstSocket.connect(SERVER_ADDRESS);
            workerThread = new ThreadPool(1);
            packetProcessor = new PacketProcessor(dstSocket);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create network connection", e);
        }
    }

    public void sendPacket(Packet packet) {
        workerThread.queue(() -> {
            try {
                dstSocket.write(PacketManager.makePacketBuffer(packet));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to send packet! ", e);
            }
        });
    }


    public void receivePacket(){
        try {
            byte[] bytesRead;
            while((bytesRead = packetProcessor.read()).length == 1){
                if(bytesRead[0] == -1){
                    dstSocket.close();
                    return;
                }
            }

        }catch (IOException e){
            throw new IllegalStateException("Failed to receive packet! ", e);
        }
    }



}

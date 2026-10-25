package ypfunding.client;

import ypfunding.client.network.ClientNetworkConnector;
import ypfunding.client.network.ClientTCPConnector;
import ypfunding.common.protocol.packets.MessagePacket;
import ypfunding.common.util.LogUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    static void main() {
        // TODO
        try(ClientNetworkConnector connector = new ClientTCPConnector()){
            connector.start();
            MessagePacket pkt = new MessagePacket();
            pkt.setMessage("HELLO WORLD!");
            pkt = connector.sendAndWaitReply(pkt, MessagePacket.class);
            System.out.println("Response : " + pkt.getMessage());
            connector.pause();
        }catch (IOException | TimeoutException e){
            LogUtil.log("Error : " + e.getMessage());
        }
    }
}

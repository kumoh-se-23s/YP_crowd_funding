package ypfunding.client;

import ypfunding.client.network.ClientNetworkConnector;
import ypfunding.client.network.ClientTCPConnector;
import ypfunding.common.protocol.packets.MessagePacket;
import ypfunding.common.util.LogUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    static void main() {
        // Minimal example for client
        // TODO
        try(ClientNetworkConnector connector = new ClientTCPConnector()){
            connector.start();
            System.out.println("[ Enter the message here ]");
            connector.pause(e -> {
                if(e.isBlank()) return;

                MessagePacket pkt = new MessagePacket();
                pkt.setMessage(e);

                try {
                    pkt = connector.sendAndWaitReply(pkt, MessagePacket.class);
                } catch (TimeoutException ex) {
                    LogUtil.log("Timeout : " + ex.getMessage());
                }

                pkt.setMessage(e);
                System.out.println("Response : " + pkt.getMessage());
            });
        }catch (IOException e){
            LogUtil.log("Error : " + e.getMessage());
        }
    }
}

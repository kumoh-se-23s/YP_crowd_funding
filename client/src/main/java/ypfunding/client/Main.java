package ypfunding.client;

import ypfunding.client.network.ClientNetworkConnector;
import ypfunding.client.network.ClientTCPConnector;
import ypfunding.common.protocol.packets.request.MessagePacket;
import ypfunding.common.protocol.packets.response.NotificationPacket;
import ypfunding.common.util.LogUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    static void main() {
        // Minimal example for client
        // TODO
        try(ClientNetworkConnector connector = new ClientTCPConnector()){
            System.out.println("Connecting...");
            connector.start();
            System.out.println("Connected!");
            System.out.println("[ Enter the message here ]");
            connector.pause(e -> {
                if(e.isBlank()) return;

                MessagePacket pkt = new MessagePacket();
                pkt.setMessage(e);

                try {
                    NotificationPacket pkt2 = connector.sendAndWaitReply(pkt, NotificationPacket.class);
                    String msg = pkt2.getNotification().getMessage();
                    System.out.println("Response : " + msg);
                } catch (TimeoutException ex) {
                    LogUtil.log("Timeout : " + ex.getMessage());
                }

            });
        }catch (IOException e){
            LogUtil.log("Error : " + e.getMessage());
        }
    }
}

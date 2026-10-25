package ypfunding.client;

import ypfunding.client.network.ClientNetworkConnector;
import ypfunding.client.network.ClientTCPConnector;
import ypfunding.common.protocol.packets.MessagePacket;
import ypfunding.common.util.LogUtil;

import java.io.IOException;

public class Main {
    static void main() {
        // TODO
        try(ClientNetworkConnector connector = new ClientTCPConnector()){
            connector.start();
            MessagePacket pkt = new MessagePacket();
            pkt.setMessage("HELLO WORLD!");
            connector.sendPacket(pkt);
            connector.pause();
        }catch (IOException e){
            LogUtil.log("Error : " + e.getMessage());
        }
    }
}

package ypfunding.common.constant;

import java.net.InetSocketAddress;

public class NetworkConstants {
    private NetworkConstants() {
    }

    public static final int PORT = 7777;
    public static final String SERVER_ADDRESS_IP = "59.151.239.227";
    public static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress(SERVER_ADDRESS_IP, PORT);
}

package ypfunding.common.constant;

import java.net.InetSocketAddress;

public class NetworkConstants {
    private NetworkConstants() {
    }

    public static final int PORT = 7777;
    public static final String SERVER_ADDRESS_IP = "127.0.0.1";
    public static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress(SERVER_ADDRESS_IP, PORT);
}

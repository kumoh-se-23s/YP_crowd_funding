package ypfunding.common.constant;

import ypfunding.common.config.AppProperties;

import java.net.InetSocketAddress;

public class NetworkConstants {
    private NetworkConstants() {
    }

    private static final String KEY_SERVER_ADDRESS_IP = "server.address.ip";
    private static final String KEY_SERVER_PORT = "server.port";

    public static final int PORT;
    public static final String SERVER_ADDRESS_IP;
    public static final InetSocketAddress SERVER_ADDRESS;

    public static final int MAX_BUFFER_SIZE = 2048;
    public static final int PACKET_LEN_HEADER_SIZE = 2;
    public static final int MESSAGE_TYPE_HEADER_SIZE = 2;
    public static final int TIMEOUT_MS = 5000;


    static {
        AppProperties properties = AppProperties.load(NetworkConstants.class, "config/server.properties");
        SERVER_ADDRESS_IP = properties.getValue(KEY_SERVER_ADDRESS_IP);
        PORT = Integer.parseInt(properties.getValue(KEY_SERVER_PORT));
        SERVER_ADDRESS = new InetSocketAddress(SERVER_ADDRESS_IP, PORT);
    }
}

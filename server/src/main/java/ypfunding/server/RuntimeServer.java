package ypfunding.server;

import ypfunding.common.constant.NetworkConstants;
import ypfunding.common.protocol.TLSConnection;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.common.util.LogUtil;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.network.RequestManager;

import javax.net.ssl.SSLContext;
import java.io.Console;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RuntimeServer implements AutoCloseable {

    // server configuration
    private final SSLContext sslContext;

    // connection pool
    private final Map<SocketChannel, TLSConnection> tlsConnectionMap;

    // login user pool
    private final Map<SocketChannel, LoginUser> userMap;


    private final AppContext context;

    private RuntimeServer(SSLContext sslContext) {
        this.sslContext = sslContext;
        userMap = new HashMap<>();
        tlsConnectionMap = new HashMap<>();
        context = new AppContext();
    }

    public void addUser(SocketChannel socket, LoginUser user) {
        userMap.put(socket, user);
    }

    public LoginUser getUser(SocketChannel socket) {
        return userMap.get(socket);
    }

    public void endConnection(SocketChannel socket) {
        tlsConnectionMap.get(socket).close();
        tlsConnectionMap.remove(socket);
        userMap.remove(socket);
    }

    public AppContext getAppContext() {
        return context;
    }

    public void createConnection(SocketChannel socket, SelectionKey selectionKey){
        tlsConnectionMap.computeIfAbsent(socket, _ -> new TLSConnection(TLSServerConfig.createServerEngine(sslContext), selectionKey, socket));
    }

    public TLSConnection getConnection(SocketChannel socket){
        return tlsConnectionMap.get(socket);
    }

    public static void start() {
        SSLContext context = null;
        while (context == null) {
            Console console = System.console();
            if (console == null) {
                throw new IllegalStateException("console unavailable");
            }

            char[] password = console.readPassword("Enter password: ");
            if (password == null) {
                throw new IllegalStateException("password input closed");
            }

            try {
                context = TLSServerConfig.createSSLContext(password);
            } catch (Exception e) {
                LogUtil.logError("Sorry, try again.");
            } finally {
                Arrays.fill(password, '\0');
            }
        }

        RuntimeServer server = new RuntimeServer(context);
        try (RequestManager manager = new RequestManager(server)) {
            manager.start();
            LogUtil.log("Server started at " + DateTimeUtil.now() + ".");
            LogUtil.log("Address : " + NetworkConstants.SERVER_ADDRESS_IP);
            LogUtil.log("Listening on (Port) : " + NetworkConstants.PORT);
        } catch (IOException e) {
            LogUtil.logError("Failed to start server! \n" + e);
        }
    }

    @Override
    public void close() {
        context.close();
    }
}

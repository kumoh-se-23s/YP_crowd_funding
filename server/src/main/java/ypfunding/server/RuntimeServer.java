package ypfunding.server;

import ypfunding.common.constant.NetworkConstants;
import ypfunding.common.protocol.TLSConnection;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.common.util.LogUtil;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.network.RequestManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.swing.*;
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
    private final Map<SocketChannel, SSLEngine> sslEngineMap;
    private final Map<SocketChannel, TLSConnection> tlsConnectionMap;

    // login user pool
    private final Map<SocketChannel, LoginUser> userMap;


    private final AppContext context;

    private RuntimeServer(SSLContext sslContext) {
        this.sslContext = sslContext;
        userMap = new HashMap<>();
        sslEngineMap = new HashMap<>();
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
        sslEngineMap.get(socket).closeOutbound();
        sslEngineMap.remove(socket);
        tlsConnectionMap.get(socket).close();
        tlsConnectionMap.remove(socket);
        userMap.remove(socket);
    }

    public AppContext getAppContext() {
        return context;
    }

    public void createConnection(SocketChannel socket, SelectionKey selectionKey){
        SSLEngine engine = TLSServerConfig.createServerEngine(sslContext);
        sslEngineMap.put(socket, engine);
        tlsConnectionMap.computeIfAbsent(socket, _ -> new TLSConnection(engine, selectionKey, socket));
    }

    public TLSConnection getConnection(SocketChannel socket){
        return tlsConnectionMap.get(socket);
    }

    public static void start() {
        SSLContext context = null;


        while (context == null) {
            JPasswordField passwordField = new JPasswordField();

            int result = JOptionPane.showConfirmDialog(null,
                    passwordField,
                    "Input Password",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            char[] password = passwordField.getPassword();

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            try {
                context = TLSServerConfig.createSSLContext(password);
            } catch (Exception e) {
                LogUtil.logError("Sorry, try again.");
            } finally {
                Arrays.fill(password, '\0');
            }
        }


        try(RuntimeServer server = new RuntimeServer(context)){
            RequestManager manager = new RequestManager(server);
            LogUtil.log("Server started at " + DateTimeUtil.now() + ".");
            LogUtil.log("Address : " + NetworkConstants.SERVER_ADDRESS_IP);
            LogUtil.log("Listening on (Port) : " + NetworkConstants.PORT);
            manager.start();
        } catch (IOException e) {
            LogUtil.logError("Failed to start server! \n" + e);
        }
    }

    @Override
    public void close() {
        context.close();
    }
}

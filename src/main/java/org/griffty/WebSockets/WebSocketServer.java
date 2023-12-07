package org.griffty.WebSockets;

import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import org.glassfish.tyrus.server.Server;
import org.griffty.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebSocketServer {
    private static WebSocketServer instance;
    public static WebSocketServer getInstance() {
        if (instance == null){
            instance = new WebSocketServer();
        }

        return instance;
    }
    private WebSocketServer(){}
    private Server mainServer;
    private int port = 1309;
    public void runServer() {
        if (mainServer != null){
            return;
        }
        String contextPath = "/cloud_storage";
        Logger.getInstance().saveLogMessage("WebSocketServer", "Initializing WebSocket server on: " + port + " contextPath: " + contextPath);
        mainServer = new Server("localhost",  port, contextPath, null, WebSocketEndpoint.class); //todo: add logging for each Configuration added
        Logger.getInstance().saveLogMessage("WebSocketServer", "WebSocket server initialized; port: " + port + " contextPath: " + contextPath);
        try {
            mainServer.start();
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
        }
    }
    public void stopServer() {
        if (mainServer == null){
            return;
        }
        Logger.getInstance().saveLogMessage("WebSocketServer", "Stopping WebSocket server; port: " + port );
        System.out.println("Stopping Server");
        mainServer.stop();
    }

    private final List<WebSocketEndpoint> activeConnections = new ArrayList<>();

    public List<WebSocketEndpoint> getActiveConnections() {
        return activeConnections;
    }
    public boolean addActiveConnection(WebSocketEndpoint endpoint) {
        for (WebSocketEndpoint connection : getActiveConnections()){
            if (connection.getSession().getId().equals(endpoint.getSession().getId())){
                return false;
            }
        }
        getActiveConnections().add(endpoint);
        return true;
    }
    public void removeActiveConnection(WebSocketEndpoint endpoint) {
        getActiveConnections().removeIf(connection -> connection.getSession().getId().equals(endpoint.getSession().getId()));
    }


    private final int maxIdNumbers = 16;
    private int availableIdCount = 0;
    public String getAvailableUId() {
        StringBuilder idBuilder = new StringBuilder(availableIdCount + "");

        for (int i = 0; i < maxIdNumbers - idBuilder.toString().length(); i++){
            idBuilder.insert(0, "0");
        }
        availableIdCount++;
        return idBuilder.toString();
    }

    public void changePort(int port) {
        stopServer();
        Logger.getInstance().saveLogMessage("WebSocketServer", "Port changed; old port: " + this.port + "; new port: " + port);
        this.port = port;
        runServer();
    }

    public void terminate() {
        for (WebSocketEndpoint endpoint: getActiveConnections()) {
            try {
                endpoint.getSession().close(new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART, ""));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        stopServer();
    }
}

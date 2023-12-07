package org.griffty;

import jakarta.websocket.CloseReason;
import org.griffty.WebSockets.WebSocketEndpoint;
import org.griffty.WebSockets.WebSocketServer;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    private static final Scanner adminInput = new Scanner(System.in);
    private static WebSocketServer webSocketServer;
    private static DiscordServer discordServer;
    private static Logger logger;

    public static void main(String[] args) {
        logger = Logger.getInstance();
        discordServer = DiscordServer.getInstance();
        webSocketServer = WebSocketServer.getInstance();
        webSocketServer.runServer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> terminate()));
        checkForInput();
    }

    private static void terminate() {
        webSocketServer.terminate();
        discordServer.terminate();
        logger.terminate();
    }

    private static void checkForInput() {
        String command;
        while (!(command = adminInput.nextLine()).equals("q"))
        {
            Logger.getInstance().saveLogMessage("[Command]", command);
            String[] split = command.toLowerCase().split(" ");
            switch (split[0]){
                case "websocket" -> {
                    switch (split[1]){
                        case "port" ->{
                            int port;
                            try {
                                port = Integer.parseInt(split[2]);
                                if (port < 0 || port > 65535){
                                    throw new RuntimeException();
                                }
                            } catch (Exception e){
                                System.out.println("Cannot use " + split[2] + " as a port");
                                continue;
                            }
                            webSocketServer.changePort(port);
                        }
                        case "reboot" -> {
                            webSocketServer.stopServer();
                            webSocketServer.runServer();
                        }
                        case "closeconnection" -> {
                            for (WebSocketEndpoint endpoint :
                                    webSocketServer.getActiveConnections()){
                                if (endpoint.getSession().getId().equals(split[3])){
                                    try {
                                        endpoint.getSession().close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "")); //todo: check if on closure object is removed from active connections
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    break;
                                }
                            }
                        }
                        case "clearconnections" -> {
                            for (WebSocketEndpoint endpoint :
                                    webSocketServer.getActiveConnections()){
                                try {
                                    endpoint.getSession().close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        default -> System.out.println("Cannot recognize command " + split[1]);
                    }
                }
                case "shutdown" -> {
                    webSocketServer.stopServer();
                    System.exit(69);
                }
                default -> System.out.println("Cannot recognize command " + command);
            }
        }
    }
}
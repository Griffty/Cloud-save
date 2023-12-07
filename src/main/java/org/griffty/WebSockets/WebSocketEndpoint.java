package org.griffty.WebSockets;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.griffty.DiscordServer;
import org.griffty.Logger;

import java.io.IOException;

@ServerEndpoint(WebSocketEndpoint.endpointContextPath)
public class WebSocketEndpoint {
    static final String endpointContextPath = "/fileIO";
    private Session session;
    private ServerConnectionState state;
    private String fileName;
    private String UId = "-";
    private int fileSize;
    private int currentChunk;

    @OnMessage
    public void onMessage(String message) throws IOException {
        Logger.getInstance().saveLogMessage(String.format("[Client %S]", session.getId()), message);
        if (message.contains("Request")){
            if (message.contains("End")){
                EndFileTransition(false);
                return;
            }
            if (state != ServerConnectionState.WAITING){
                sendConnectionOccupied();
                return;
            }
            String[] requestHeader = message.split(":");
            if (requestHeader.length != 3){
                sendImproperHeaderStructure();
                return;
            }
            if (message.contains("FileUploadRequest")){
                startUploadProcess(requestHeader);
            }
        }
    }

    @OnMessage
    public void onMessage(byte[] file) {
        Logger.getInstance().saveLogMessage("Server", String.format("Received file chunk on %S; size: " + (file.length/1024f/1024f) +" MB(" + (file.length/1024f) +"KB)", UId));
        DiscordServer.getInstance().uploadFile(file, fileName + "__" + currentChunk).thenRun(() -> {
            try {
                currentChunk++;
                session.getBasicRemote().sendText("Received");
            } catch (IOException e) {
                throw new RuntimeException(e); //todo: stop transition on error
            }
        });
    }
    @OnOpen
    public void onOpen(Session session) throws IOException {
        this.session = session;
        state = ServerConnectionState.WAITING;
        if (!WebSocketServer.getInstance().addActiveConnection(this)){
            sendSessionAlreadyOpen();
            return;
        }
        Logger.getInstance().saveLogMessage("Server", String.format("Connection ESTABLISHED: %S", session.getId()));
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        WebSocketServer.getInstance().removeActiveConnection(this);
        Logger.getInstance().saveLogMessage("Server", String.format("Connection CLOSED: %S; reason: %S", session.getId(), closeReason.getCloseCode()));
    }
    @OnError
    public void onError(Session session, Throwable thr) {
        if (state != ServerConnectionState.WAITING){
            Logger.getInstance().saveLogMessage("Server", String.format("Terminating UPLOAD because of error; UId: %S", UId));
            EndFileTransition(true);
        }
        StringBuilder message = new StringBuilder(thr.toString());
        for (StackTraceElement element : thr.getStackTrace()){
            message.append("\n").append(element.toString());
        }
        Logger.getInstance().saveLogMessage("Server", String.format("Error in session %S \n%S", session.getId(), message));
    }
    public Session getSession() {
        return session;
    }

    private void EndFileTransition(boolean terminated) {
        Logger.getInstance().saveLogMessage("Server", String.format("File upload finished; name: %S; %S", fileName, session.getId()));
        //FileHandler.getInstance().addNewFile(); todo
        state = ServerConnectionState.WAITING;
    }
    private void sendConnectionOccupied() throws IOException {
        session.getBasicRemote().sendText("Error:ConnectionOccupied:" + state);
        Logger.getInstance().saveImportantLogMessage("Server", String.format("Attempt to use occupied connection: %S; currently: %S", session.getId(), state));
    }
    private void sendImproperHeaderStructure() throws IOException {
        session.getBasicRemote().sendText("Error:ImproperHeaderStructure");
        Logger.getInstance().saveImportantLogMessage("Server", String.format("Exception during file upload request: %S; Improper header structure.", session.getId()));
    }
    private void sendSessionAlreadyOpen() throws IOException {
        session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Session with this Client is already active"));
        Logger.getInstance().saveImportantLogMessage("Server", String.format("Exception during opening session: %S is already in active list.", session.getId()));
    }
    private void startUploadProcess(String[] requestHeader) throws IOException {
        state = ServerConnectionState.UPLOADING;
        fileName = requestHeader[1];
        fileSize = Integer.parseInt(requestHeader[2]);
        currentChunk = 0;
        UId = WebSocketServer.getInstance().getAvailableUId();
        Logger.getInstance().saveLogMessage("Server", String.format("New file UPLOAD request: %S; uploadId: %S; filename: %S; fileSize: %S", session.getId(), UId, fileName, fileSize));
        session.getBasicRemote().sendText(UId + ":" + determineChunkSize(fileSize)); //todo: does JS need to know UDId?
    }
    private int determineChunkSize(int fileSize) {
        return fileSize > 5 * 1024 * 1024 ? 25 * 1024 * 1024 : 5 * 1024 * 1024; // 25MB - max Discord file size
    }
}

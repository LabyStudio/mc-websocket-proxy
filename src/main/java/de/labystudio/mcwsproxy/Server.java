package de.labystudio.mcwsproxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.Proxy;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Web socket server that handles the communication between the web socket and the socket
 * It accepts new connections and creates a new session for each connection
 *
 * @author LabyStudio
 */
public class Server extends WebSocketServer {

    private static final Gson GSON = new Gson();

    private final Map<WebSocket, Session> sessions = new HashMap<>();
    private final MinecraftSessionService sessionService;

    /**
     * Create a new web socket server
     */
    public Server() {
        super(new java.net.InetSocketAddress(30023));

        YggdrasilAuthenticationService service = new YggdrasilAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString());
        this.sessionService = service.createMinecraftSessionService();
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        try {
            JsonObject object = GSON.fromJson(message, JsonObject.class);

            int packetId = object.get("id").getAsInt();
            JsonObject payload = object.get("payload").getAsJsonObject();

            if (packetId == 0) {
                // Create new session packet
                String host = payload.get("host").getAsString();
                int port = payload.get("port").getAsInt();

                Session session = new Session(this, webSocket, host, port);
                this.sessions.put(webSocket, session);
            } else {
                Session session = this.sessions.get(webSocket);
                if (session != null) {
                    session.handlePacket(packetId, payload);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            webSocket.close();
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteBuffer message) {
        super.onMessage(webSocket, message);

        // Send message to session
        Session session = this.sessions.get(webSocket);
        if (session != null) {
            session.send(message);
        }
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        Session session = this.sessions.remove(webSocket);
        if (session != null) {
            session.close();
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception ex) {
        Session session = this.sessions.remove(webSocket);
        if (session != null) {
            session.close();
        }
    }

    public MinecraftSessionService getSessionService() {
        return this.sessionService;
    }
}

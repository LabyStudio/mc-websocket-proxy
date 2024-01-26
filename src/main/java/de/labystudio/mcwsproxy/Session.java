package de.labystudio.mcwsproxy;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Session class that handles the communication between the web socket and the socket
 * It redirects the packets from the web socket to the socket and vice versa
 * <p>
 * It also handles the authentication with the Mojang session server
 *
 * @author LabyStudio
 */
public class Session {

    private final Server server;
    private final WebSocket webSocket;
    private final Socket socket;

    /**
     * Create a new session
     *
     * @param server    The server instance
     * @param webSocket The socket instance for this connection
     * @param host      The Minecraft server host to connect to
     * @param port      The Minecraft server port to connect to
     * @throws IOException If an I/O error occurs when creating the socket
     */
    public Session(Server server, WebSocket webSocket, String host, int port) throws IOException {
        this.server = server;
        this.webSocket = webSocket;
        this.socket = new Socket(host, port);

        System.out.println("New session from " + webSocket.getRemoteSocketAddress().getHostName() + " to " + host + ":" + port);

        // Read from socket and write to web socket
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                while (this.socket.isConnected() && this.webSocket.isOpen()) {
                    ByteBuffer buffer = ByteBuffer.allocate(0x1FFFFF);
                    int read = this.socket.getInputStream().read(buffer.array());
                    if (read == -1) {
                        break;
                    }
                    buffer.limit(read);
                    webSocket.send(buffer);
                }
            } catch (IOException e) {
                System.out.println("Error while reading from socket: " + e.getMessage());
            }
        });
    }

    /**
     * Handle a packet from the web socket
     *
     * @param id      The packet id
     * @param payload The packet payload
     */
    public void handlePacket(int id, JsonObject payload) {
        // Join server
        if (id == 1) {
            // Note: The session token should never be sent to the proxy for security reasons,
            // but there is no other way for the javascript to authenticate with the Mojang because
            // you can't communicate with the session server from the browser because of CORS
            String accessToken = payload.get("accessToken").getAsString();
            String selectedProfile = payload.get("selectedProfile").getAsString();
            String serverId = payload.get("serverId").getAsString();

            UUID uuid = UUID.fromString(selectedProfile.replaceAll(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
            ));
            GameProfile profile = new GameProfile(uuid, "Unknown");
            try {
                this.server.getSessionService().joinServer(profile, accessToken, serverId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close the session
     */
    public void close() {
        System.out.println("Session of " + this.webSocket.getRemoteSocketAddress().getHostName() + " closed");

        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a message to the socket
     */
    public void send(ByteBuffer message) {
        try {
            this.socket.getOutputStream().write(message.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

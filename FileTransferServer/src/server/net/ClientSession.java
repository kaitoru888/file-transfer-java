package server.net;

import java.io.DataOutputStream;
import java.net.Socket;

public class ClientSession {
    public final String username;
    public final int userId;
    public final Socket socket;
    public final DataOutputStream out;

    public ClientSession(String username, int userId, Socket socket, DataOutputStream out) {
        this.username = username;
        this.userId = userId;
        this.socket = socket;
        this.out = out;
    }
}
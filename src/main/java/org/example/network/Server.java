package org.example.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static PeerConnection listen(int port) throws IOException {
        System.out.println("Waiting for opponent on port " + port + "...");
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept(); // waits for a connection to other peer
        serverSocket.close(); // closes after connecting to other peer
        System.out.println("Opponent connected: " + socket.getInetAddress());
        return new PeerConnection(socket);
    }
}

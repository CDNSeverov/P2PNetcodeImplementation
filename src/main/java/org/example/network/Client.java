package org.example.network;

import java.io.IOException;
import java.net.Socket;

public class Client {
    public static PeerConnection connect(String host, int port) throws IOException {
        System.out.println("Connecting to " + host + ":" + port + "...");
        Socket socket = new Socket(host, port);
        System.out.println("Connected!");
        return new PeerConnection(socket);
    }
}

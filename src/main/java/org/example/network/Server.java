package org.example.network;

import java.io.IOException;
import java.net.*;

public class Server {
    public static PeerConnection listen(int port) throws IOException {
        InetAddress localhost = InetAddress.getLocalHost();
        System.out.println("System IP Address: " + localhost.getHostAddress().trim());
        System.out.println("Waiting for opponent on port " + port + "...");

        DatagramSocket socket = new DatagramSocket(port);

        // Don't set peerAddress yet — we learn it from the first incoming packet
        PeerConnection conn = new PeerConnection(socket);

        System.out.println("Listening for first UDP packet...");
        return conn;
    }
}
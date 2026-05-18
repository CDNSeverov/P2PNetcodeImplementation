package org.example.network;

import java.io.IOException;
import java.net.*;

public class Client {
    public static PeerConnection connect(String host, int port) throws IOException {
        System.out.println("Connecting to " + host + ":" + port + "...");

        InetAddress address = InetAddress.getByName(host);
        DatagramSocket socket = new DatagramSocket(); // OS assigns local port

        System.out.println("Connected (UDP)!");
        return new PeerConnection(socket, address, port);
    }
}
package org.example;

import org.example.network.Client;
import org.example.network.PeerConnection;
import org.example.network.Server;

import javax.swing.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // Parse args : java Main [host] [port] [delay|rollback]
        boolean isServer = args.length == 0 || args[0].equals("server");
        String host = isServer ? null : args[0];
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7777;
        boolean netcodeType = args.length > 2 && args[2].equals("rollback"); // true for rollback, false for delay

        // Connecting
        PeerConnection peer = isServer ? Server.listen(port) : Client.connect(host, port);

        // UI
        GUI gui = new GUI(!isServer);
        InputHandler input = new InputHandler();

        JFrame frame = new JFrame(host);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(gui);
        frame.pack();
        frame.addKeyListener(input);
        frame.setVisible(true);

        // Run
        System.out.println("Using " + (netcodeType ? "rollback" : "delay-based") + " netcode");

        if (isServer) {
            System.out.println("Waiting for client to send first packet...");
            peer.waitForPeer(); // blocks until first UDP packet arrives
            System.out.println("Client found: ");
        }

        System.out.println("Waiting for ready confirmation");
        peer.sendReady();
        peer.waitForReady();
        System.out.println("Starting");

        new Game(peer, netcodeType, input, gui).run();
    }
}
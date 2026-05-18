package org.example.network;

import org.example.Message;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class PeerConnection {
    private static final int PACKET_SIZE = 16; // 4 ints * 4 bytes
    private final CountDownLatch addressLatch = new CountDownLatch(1);

    private final DatagramSocket socket;
    private volatile InetAddress peerAddress;
    private volatile int peerPort;
    private final ConcurrentLinkedQueue<Message> inbox = new ConcurrentLinkedQueue<>();
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    // Server-side constructor: we know our port, peer address comes from first packet
    public PeerConnection(DatagramSocket socket) {
        this.socket = socket;
        startReader();
    }

    // Client-side constructor: we know exactly who we're talking to
    public PeerConnection(DatagramSocket socket, InetAddress peerAddress, int peerPort) {
        this.socket = socket;
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        startReader();
    }

    private void startReader() {
        Thread reader = new Thread(() -> {
            byte[] buf = new byte[PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, PACKET_SIZE);
            try {
                while (!socket.isClosed()) {
                    socket.receive(packet);

                    // First packet from peer tells us their address (server side)
                    if (peerAddress == null) {
                        peerAddress = packet.getAddress();
                        peerPort = packet.getPort();
                        addressLatch.countDown(); // now safe to send
                    }

                    ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, PACKET_SIZE);
                    int frame  = bb.getInt();
                    int left   = bb.getInt();
                    int right  = bb.getInt();
                    int attack = bb.getInt();

                    if (frame == Integer.MIN_VALUE) {
                        readyLatch.countDown();
                        continue;
                    }

                    inbox.add(new Message(frame, new int[]{left, right, attack}, 0f));
                }
            } catch (Exception e) {
                if (!socket.isClosed()) {
                    System.out.println("Connection error: " + e.getMessage());
                }
            }
        });
        reader.setDaemon(true);
        reader.start();
    }

    public void send(Message msg) {
        try {
            ByteBuffer bb = ByteBuffer.allocate(PACKET_SIZE);
            bb.putInt(msg.frame);
            bb.putInt(msg.inputs[0]);
            bb.putInt(msg.inputs[1]);
            bb.putInt(msg.inputs[2]);

            byte[] data = bb.array();
            DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
        }
    }

    public Message poll() {
        return inbox.poll();
    }

    public void close() {
        socket.close();
    }

    public void sendReady() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(PACKET_SIZE);
        bb.putInt(Integer.MIN_VALUE);
        bb.putInt(0);
        bb.putInt(0);
        bb.putInt(0);

        byte[] data = bb.array();
        DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
        socket.send(packet);
    }

    public void waitForReady() throws InterruptedException {
        readyLatch.await();
    }
    public void waitForPeer() throws InterruptedException {
        addressLatch.await();
    }
}
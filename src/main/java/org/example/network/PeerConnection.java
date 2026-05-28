package org.example.network;

import org.example.Message;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class PeerConnection {
    private final Socket socket;
    private final DataOutputStream out;
    private final ConcurrentLinkedQueue<Message> inbox = new ConcurrentLinkedQueue<>();
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    public PeerConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        Thread reader = new Thread(() -> {
            DataInputStream in = null;
            try {
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                while(!socket.isClosed()) {
                    int frame = in.readInt();
                    int left = in.readInt();
                    int right = in.readInt();
                    int attack = in.readInt();

                    if (frame == Integer.MIN_VALUE) {
                        readyLatch.countDown();
                        continue;
                    }

                    inbox.add(new Message(frame, new int[]{left, right, attack}));
                }
            } catch (Exception e) {
                System.out.println("Connection closed: " + e.getMessage());
            }

        });
        reader.setDaemon(true);
        reader.start();
    }

    public void send(Message msg) {
        try {
            out.writeInt(msg.frame);
            out.writeInt(msg.inputs[0]);
            out.writeInt(msg.inputs[1]);
            out.writeInt(msg.inputs[2]);
            out.flush();
        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
        }
    }

    public Message poll() {
        return inbox.poll();
    }

    public void close() throws IOException {
        socket.close();
    }

    public void sendReady() throws IOException {
        out.writeInt(Integer.MIN_VALUE);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(0);
        out.flush();
    }

    public void waitForReady() throws InterruptedException {
        readyLatch.await();
    }
}

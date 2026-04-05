package org.example.network;

import org.example.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PeerConnection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ConcurrentLinkedQueue<Message> inbox = new ConcurrentLinkedQueue<>();

    public PeerConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());

        Thread reader = new Thread(() -> {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(socket.getInputStream());
                while(!socket.isClosed()) {
                    Message msg = (Message) in.readObject();
                    inbox.add(msg);
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
            out.writeObject(msg);
            out.flush();
            out.reset();
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
}

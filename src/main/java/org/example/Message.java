package org.example;

public class Message {
    public int frame;
    public int[] inputs = new int[3];
    public Message(int frame, int[] inputs) {
        this.frame = frame;
        this.inputs = inputs;
    }
}

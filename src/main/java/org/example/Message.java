package org.example;

public class Message {
    public int frame;
    public int[] inputs = new int[3];
    public float posX;
    public Message(int frame, int[] inputs, float posX) {
        this.frame = frame;
        this.inputs = inputs;
        this.posX = posX;
    }
}

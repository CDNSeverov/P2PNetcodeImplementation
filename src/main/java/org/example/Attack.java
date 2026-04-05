package org.example;

public class Attack {
    public float posX;
    public float posY;
    public int counter;

    public Attack(float posX, float posY) {
        this.posX = posX;
        this.posY = posY;
        this.counter = 0;
    }

    public boolean isExtended() {
        return counter > 40;
    }

    public Attack copy() {
        Attack a = new Attack(posX, posY);
        a.counter = this.counter;
        return a;
    }
}

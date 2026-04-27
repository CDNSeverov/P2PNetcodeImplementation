package org.example;

public class Player {
    public float posX;
    public float posY;
    public Attack attack;
    public boolean facingRight;

    public static final float WIDTH = 100f;
    public static final float HEIGHT = 100f;
    public static final float ATTACK_W = 100f;
    public static final float ATTACK_H = 10f;

    public Player(float posX, float posY, boolean facingRight) {
        //System.out.println("Player Created");
        this.posX = posX;
        this.posY = posY;
        this.attack = new Attack(posX, posY);
        this.facingRight = facingRight;
    }

    public void update(int[] inputs) {
        if (inputs[0] == 1 && posX >= 50f) {
            posX -= 10f;
        }
        if (inputs[1] == 1 && posX <= 1230f) {
            posX += 10f;
        }
        if (inputs[2] == 1 && attack.counter <= 0) {
            attack.counter = 60;
        }

        if (attack.isExtended()) {
            attack.posX = facingRight ? posX + 50f : posX - 50f;
        } else {
            attack.posX = facingRight ? posX - 50f : posX + 50f;
        }
        attack.posY = 450f;

        if (attack.counter > 0) {
            attack.counter--;
        }
    }

    public Player copy() {
        Player p = new Player(posX, posY, facingRight);
        p.attack = attack.copy();
        return p;
    }

    // For AABB
    public float left() {
        return posX - WIDTH/2f;
    }
    public float right() {
        return posX + WIDTH/2f;
    }
    public float top() {
        return posY - HEIGHT/2f;
    }
    public float bottom() {
        return posY + HEIGHT/2f;
    }

    public float attackLeft() {
        return attack.posX;
    }
    public float attackRight() {
        return attack.posX + ATTACK_W;
    }
    public float attackTop() {
        return attack.posY - ATTACK_H/2f;
    }
    public float attackBottom() {
        return attack.posY + ATTACK_H/2f;
    }
}

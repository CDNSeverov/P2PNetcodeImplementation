package org.example;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

public class InputHandler implements KeyListener {
    private final Set<Integer> held = new HashSet<>();

    @Override
    public void keyTyped(KeyEvent e) {

    }
    @Override
    public void keyPressed(KeyEvent e) {
        held.add(e.getKeyCode());
    }
    @Override
    public void keyReleased(KeyEvent e) {
        held.remove(e.getKeyCode());
    }

    public int[] readLocalInputs() {
        return new int[]{
                held.contains(KeyEvent.VK_A) ? 1 : 0,
                held.contains(KeyEvent.VK_D) ? 1 : 0,
                held.contains(KeyEvent.VK_S) ? 1 : 0
        };
    }

    public int[] readP2Inputs() {
        return new int[]{
                held.contains(KeyEvent.VK_LEFT) ? 1 : 0,
                held.contains(KeyEvent.VK_RIGHT) ? 1 : 0,
                held.contains(KeyEvent.VK_DOWN) ? 1 : 0
        };
    }
}

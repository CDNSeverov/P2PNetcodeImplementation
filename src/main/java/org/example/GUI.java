package org.example;

import javax.swing.*;
import java.awt.*;

public class GUI extends JPanel {
    private volatile GameState state;
    private final boolean mirror; // true for player 2

    public GUI(boolean mirror) {
        this.mirror = mirror;
        setPreferredSize(new Dimension(1280,500));
        setBackground(Color.BLACK);
    }

    public void updateState(GameState state) {
        this.state = state;
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        GameState snapshot = state;
        if (snapshot == null) {
            return;
        }

        if (mirror) {
            drawPlayer(g, snapshot.opponent, Color.WHITE);
            drawPlayer(g, snapshot.player, Color.RED);
        } else {
            drawPlayer(g, snapshot.player, Color.WHITE);
            drawPlayer(g, snapshot.opponent, Color.RED);
        }


        if (snapshot.gameOver) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 64));
            g.drawString("GAME OVER!", 450, 360);
        }
    }

    private void drawPlayer(Graphics g, Player p, Color color) {
        if (p == null) {
            return;
        }

        float drawX = mirror ? mirrorX(p.posX) : p.posX;

        g.setColor(color);
        // Body with origin as center
        int bx = (int)(p.posX - Player.WIDTH / 2f);
        int by = (int)(p.posY - Player.HEIGHT / 2f);
        g.fillRect(bx, by, (int)Player.WIDTH, (int)Player.HEIGHT);

        // Attack
        g.setColor(color.darker());
        int ax = (int) p.attack.posX;
        int ay = (int)(p.attack.posY - Player.ATTACK_H / 2f);
        g.fillRect(ax, ay, (int)Player.ATTACK_W, (int)Player.ATTACK_H);
    }

    private float mirrorX(float x) {
        return 1280f - x;
    }
}

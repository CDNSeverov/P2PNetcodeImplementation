package org.example;

import org.example.netcode.DelayBasedNetcode;
import org.example.netcode.Netcode;
import org.example.netcode.RollbackNetcode;
import org.example.network.PeerConnection;

public class Game {
    private static final double TICK_RATE = 1.0 / 60.0;
    private final Netcode netcode;
    private final InputHandler input;
    private final GUI gui;

    public Game(PeerConnection peer, boolean netcodeType, InputHandler input, GUI gui) {
        this.netcode = netcodeType ? new RollbackNetcode(peer) : new DelayBasedNetcode(peer);
        this.input = input;
        this.gui = gui;
    }

    public void run() {
        double accumulator = 0.0;
        long last = System.nanoTime();

        while (!netcode.getDisplayState().gameOver) {
            long now = System.nanoTime();
            double delta = (now - last) / 1_000_000_000.0;
            last = now;
            accumulator += delta;

            while (accumulator >= TICK_RATE) {
                int[] localInputs = input.readLocalInputs();
                netcode.tick(localInputs);
                accumulator -= TICK_RATE;
            }

            gui.updateState(netcode.getDisplayState());

            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {

            }
        }
    }
}

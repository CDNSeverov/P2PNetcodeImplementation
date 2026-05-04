package org.example;

import org.example.netcode.DelayBasedNetcode;
import org.example.netcode.Netcode;
import org.example.netcode.RollbackNetcode;
import org.example.network.PeerConnection;

public class Game {
    private static final double TICK_RATE = 1.0 / 60.0;
    private static final double MAX_ACCUMULATOR = TICK_RATE * 3;
    private final Netcode netcode;
    private final InputHandler input;
    private final GUI gui;

    public Game(PeerConnection peer, boolean netcodeType, InputHandler input, GUI gui) {
        this.netcode = netcodeType ? new RollbackNetcode(peer) : new DelayBasedNetcode(peer);
        //System.out.println(this.netcode);
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

            if (delta > 0.1) {
                delta = 0.1;
            }

            // Cap delta to prevent huge jumps after pauses/GC
            accumulator += delta;

            // Cap accumulator so one instance can never run more than 3 frames
            // ahead even if it was stalled
            if (accumulator > MAX_ACCUMULATOR) {
                accumulator = MAX_ACCUMULATOR;
            }

            while (accumulator >= TICK_RATE) {
                int[] localInputs = input.readLocalInputs();
                netcode.tick(localInputs);
                accumulator -= TICK_RATE;
            }

            gui.updateState(netcode.getDisplayState());

            // Sleep precisely until next tick instead of a fixed 1ms
            double timeUntilNextTick = TICK_RATE - accumulator;
            if (timeUntilNextTick > 0.002) { // only sleep if more than 2ms away
                try {
                    long sleepMs = (long)(timeUntilNextTick * 1000) - 1;
                    if (sleepMs > 0) {
                        Thread.sleep(sleepMs);
                    }
                } catch (InterruptedException ignored) {}
            }
            // Busy-wait the last ~1ms for precision
            while (System.nanoTime() - last < (long)(TICK_RATE * 1_000_000_000L)) {
                Thread.onSpinWait();
            }
        }
    }
}

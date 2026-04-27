package org.example.netcode;

// Local inputs are applied immediately,
// remote inputs are predicted (repeat from last known input)
// When error occurs in prediction engine rills back to last confirmed snapshot,
// re-simulates up to current frame using the correct inputs and continues.

import org.example.GameState;
import org.example.network.PeerConnection;

public class RollbackNetcode implements Netcode{
    private final GameState current;
    private final PeerConnection peer;

    public RollbackNetcode(PeerConnection peer) {
        this.peer = peer;
        this.current = GameState.createInitial();
    }

    @Override
    public boolean tick(int[] localInputs) {
        return false;
    }

    @Override
    public GameState getDisplayState() {
        return null;
    }
}

package org.example.netcode;

import org.example.GameState;

public interface Netcode {
    // Interface for both netcode implementations
    // They decide when it's safe to advance the game state.
    // Returns true if the game state was advanced this call
    // (maybe even 0 or multiple times for rollback). Callers re-render after each call.
    boolean tick(int[] localInputs);
    GameState getDisplayState();
}

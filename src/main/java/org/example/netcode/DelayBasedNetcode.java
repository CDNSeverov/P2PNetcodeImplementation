package org.example.netcode;

// Both players buffer their inputs for a set amount of frames before applying them,
// guaranteeing that when frame N is simulated both player inputs for frame N are already available

import org.example.GameState;
import org.example.Message;
import org.example.network.PeerConnection;

import java.util.ArrayDeque;
import java.util.Deque;

public class DelayBasedNetcode implements Netcode{
    public static final int DELAY = 3;

    private final GameState state;
    private final PeerConnection peer;
    private final Deque<int[]> localBuffer = new ArrayDeque<>();
    private final Deque<int[]> remoteBuffer = new ArrayDeque<>();

    public DelayBasedNetcode(PeerConnection peer) {
        this.state = new GameState();
        this.peer = peer;
    }

    @Override
    public boolean tick(int[] localInputs) {
        localBuffer.addLast(localInputs); // 1. Enqueue this frame's local inputs (will be used after a delay)
        peer.send(new Message(state.frame + DELAY, localInputs)); // 2. Send inputs to peer immediately

        Message msg; // 3. Drain any messages that arrived from the peer
        while((msg = peer.poll()) != null) {
            remoteBuffer.addLast(msg.inputs);
        }

        // 4. Only simulate when BOTH buffers have DELAY frames queued
        if (localBuffer.size() < DELAY || remoteBuffer.isEmpty()) {
            return false; // stall - wait for peer
        }

        int[] local = localBuffer.pollFirst();
        int[] remote = remoteBuffer.pollFirst();
        state.update(local, remote);
        return true;
    }

    @Override
    public GameState getDisplayState() {
        return state;
    }
}

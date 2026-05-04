package org.example.netcode;

// Local inputs are applied immediately,
// remote inputs are predicted (repeat from last known input)
// When error occurs in prediction engine rills back to last confirmed snapshot,
// re-simulates up to current frame using the correct inputs and continues.

import org.example.GameState;
import org.example.Message;
import org.example.network.PeerConnection;

import java.util.*;

public class RollbackNetcode implements Netcode{
    private GameState current;
    private final PeerConnection peer;
    private static final int MAX_ROLLBACK = 8;
    private final Deque<GameState> snapshots = new ArrayDeque<>(); // oldest -> ... -> newest
    private final Map<Integer, int[]> predictedRemote = new HashMap<>();
    private final Map<Integer, int[]> confirmedRemote = new HashMap<>();
    private int[] lastRemote = {0, 0, 0};
    private final Deque<int[]> localHistory = new ArrayDeque<>(); // localHistory[i] = inputs used on frame (currentFrame - localHistory.size() + i)
    private int confirmedFrame = -1;

    public RollbackNetcode(PeerConnection peer) {
        this.peer = peer;
        this.current = GameState.createInitial();
    }

    @Override
    public boolean tick(int[] localInputs) {
        int frame = current.frame;

        // Save snapshot before simulating the frame
        snapshots.addLast(current.copy());
        localHistory.addLast(localInputs);
        if (snapshots.size() > MAX_ROLLBACK) {
            snapshots.pollFirst();
            localHistory.pollFirst();
        }

        // Receive any remote inputs that arrived
        Message msg;
        while ((msg = peer.poll()) != null) {
            //System.out.println("Received msg for frame " + msg.frame + " current frame=" + frame);
            msg.inputs = reverseRemoteInputs(msg.inputs);
            confirmedRemote.put(msg.frame, msg.inputs);
            if (msg.frame > confirmedFrame) {
                confirmedFrame = msg.frame;
                lastRemote = msg.inputs;
            }
        }

        // Check if we need to rollback
        int rollbackTo = findRollbackFrame(frame);
        if (rollbackTo >= 0) {
            System.out.println("rollbackTo = " + rollbackTo);
            doRollback(rollbackTo, frame);
            System.out.println("did rollback");
        }

        // Predict remote input for the frame (repeat last known)
        int[] remote = confirmedRemote.getOrDefault(frame, lastRemote.clone());
        predictedRemote.put(frame, remote.clone());

        // Advance one frame
        peer.send(new Message(frame, localInputs));
        current.update(localInputs, remote);
        return true;
    }

    private int findRollbackFrame(int currentFrame) {
        int earliest = -1;
        for (Map.Entry<Integer, int[]> entry : confirmedRemote.entrySet()) {
            int f = entry.getKey();
            if(f >= currentFrame || f < currentFrame - MAX_ROLLBACK) {
                continue;
            }

            int[] predicted = predictedRemote.get(f);
            int[] confirmed = entry.getValue();

            if (predicted == null || !Arrays.equals(predicted, confirmed)) {
                // Check whether our prediction for that frame was wrong
                earliest = (earliest == -1) ? f : Math.min(earliest, f);
                // Update prediciton so we don't rollback this frame again
                predictedRemote.put(f, confirmed.clone());
            }
        }
        if (earliest >= 0) {
            System.out.println("Rollback triggered on frame " + currentFrame + " back to " + earliest);
        }
        return earliest;
    }

    private void doRollback(int targetFrame, int presentFrame) {
        int stepsBack = presentFrame - targetFrame;
        if (stepsBack <= 0 || stepsBack > snapshots.size()) {
            return;
        }

        List<GameState> snapshotList = new ArrayList<>(snapshots);
        List<int[]> localList = new ArrayList<>(localHistory);

        int idx = snapshotList.size() - stepsBack;
        if (idx < 0) {
            return;
        }

        current = snapshotList.get(idx).copy();

        for (int f = targetFrame; f < presentFrame; f++) {
            int localIdx = f - (presentFrame - localList.size());
            int[] loc = (localIdx >= 0 && localIdx < localList.size()) ? localList.get(localIdx) : new int[]{0,0,0};
            int[] rem = confirmedRemote.getOrDefault(f, lastRemote.clone());
            current.update(loc, rem);
        }

        while (snapshots.size() > idx) {
            snapshots.pollLast();
        }
        while (localHistory.size() > idx) {
            localHistory.pollLast();
        }
    }

    public int[] reverseRemoteInputs(int[] remote) {

        if (remote[0] == 0 && remote[1] == 1) {
            remote[0] = 1;
            remote[1] = 0;
        } else if (remote[0] == 1 && remote[1] == 0) {
            remote[0] = 0;
            remote[1] = 1;
        } else {
            remote[0] = 1;
            remote[1] = 1;
        }

        return remote;
    }

    @Override
    public GameState getDisplayState() {
        return current;
    }
}

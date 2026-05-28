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
    private static final int MAX_ROLLBACK = 120;
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

        drainNetwork(frame);

        // Save snapshot before simulating the frame
        snapshots.addLast(current.copy());
        localHistory.addLast(localInputs.clone());


        // Check if we need to rollback
        int rollbackTo = findRollbackFrame(frame);
        if (rollbackTo > 0) {
            doRollback(rollbackTo, frame);
        }

        trimHistory();

        // Predict remote input for the frame (repeat last known)
        int[] remote = confirmedRemote.getOrDefault(frame, lastRemote.clone());
        predictedRemote.put(frame, remote.clone());

        // Advance one frame
        peer.send(new Message(frame, localInputs));
        current.update(localInputs, remote);

        pruneOldFrames(frame);

        return true;
    }

    private void drainNetwork(int currentFrame) {
        Message msg;
        int latestFrame = -1;
        while ((msg = peer.poll()) != null) {
            int[] mirroredInputs = reverseRemoteInputs(msg.inputs);

            confirmedRemote.put(msg.frame, mirroredInputs);

            if (msg.frame > latestFrame) {
                latestFrame = msg.frame;
                lastRemote = mirroredInputs.clone();
            }
        }
        if (latestFrame > confirmedFrame) {
            confirmedFrame = latestFrame;
        }
    }

    private int findRollbackFrame(int currentFrame) {
        int earliest = -1;
        for (Map.Entry<Integer, int[]> entry : confirmedRemote.entrySet()) {
            int f = entry.getKey();
            if (f >= currentFrame || f < currentFrame - MAX_ROLLBACK) {
                continue;
            }

            int[] confirmed = entry.getValue();
            int[] predicted = predictedRemote.get(f);
            System.out.println(Arrays.equals(predicted, confirmed));

            boolean inputMismatch = (predicted == null || !Arrays.equals(predicted, confirmed));

            if (inputMismatch) {
                earliest = (earliest == -1) ? f : Math.min(earliest, f);
                System.out.println("Earliest: " + earliest);
            }
        }
        return earliest;
    }

    private void doRollback(int targetFrame, int presentFrame) {
        List<GameState> snapshotList = new ArrayList<>(snapshots);
        List<int[]> localList = new ArrayList<>(localHistory);

        int baseFrame = presentFrame - snapshotList.size() + 1;
        int idx = targetFrame - baseFrame;

        if (idx < 0 || idx >= snapshotList.size()) {
            return;
        }

        current = snapshotList.get(idx).copy();

        for (int f = targetFrame; f < presentFrame; f++) {
            int localIdx = f - baseFrame;
            int[] loc = (localIdx >= 0 && localIdx < localList.size()) ? localList.get(localIdx) : new int[]{0,0,0};
            int[] rem = confirmedRemote.getOrDefault(f, lastRemote.clone());
            current.update(loc, rem);
        }
    }

    private void trimHistory() {
        while (snapshots.size()   > MAX_ROLLBACK) snapshots.pollFirst();
        while (localHistory.size() > MAX_ROLLBACK) localHistory.pollFirst();
    }

    private void pruneOldFrames(int currentFrame) {
        int cutoff = currentFrame - MAX_ROLLBACK - 1;
        confirmedRemote.entrySet().removeIf(e -> e.getKey() < cutoff);
        predictedRemote.entrySet().removeIf(e -> e.getKey() < cutoff);
    }

    public int[] reverseRemoteInputs(int[] remote) {

        if (remote[0] == 0 && remote[1] == 1) {
            remote[0] = 1;
            remote[1] = 0;
        } else if (remote[0] == 1 && remote[1] == 0) {
            remote[0] = 0;
            remote[1] = 1;
        }

        return remote;
    }

    @Override
    public GameState getDisplayState() {
        return current;
    }
}

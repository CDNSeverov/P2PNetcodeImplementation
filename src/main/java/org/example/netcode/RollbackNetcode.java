package org.example.netcode;

import org.example.GameState;
import org.example.Message;
import org.example.network.PeerConnection;

import java.util.*;

public class RollbackNetcode implements Netcode {
    private GameState current;
    private final PeerConnection peer;
    private static final int MAX_ROLLBACK = 120;
    private final Deque<GameState> snapshots = new ArrayDeque<>();
    private final Map<Integer, int[]> predictedRemote = new HashMap<>();
    private final Map<Integer, int[]> confirmedRemote = new HashMap<>();
    private int[] lastRemote = {0, 0, 0};
    private final Deque<int[]> localHistory = new ArrayDeque<>();
    private int confirmedFrame = -1;

    public RollbackNetcode(PeerConnection peer) {
        this.peer = peer;
        this.current = GameState.createInitial();
    }

    @Override
    public boolean tick(int[] localInputs) {
        int frame = current.frame;

        drainNetwork(frame);

        snapshots.addLast(current.copy());
        localHistory.addLast(localInputs.clone());

        int rollbackTo = findRollbackFrame(frame);
        if (rollbackTo > 0) {
            doRollback(rollbackTo, frame);
        }

        trimHistory();

        int[] remote = confirmedRemote.getOrDefault(frame, lastRemote.clone());
        predictedRemote.put(frame, remote.clone());

        peer.send(new Message(frame, localInputs));
        current.update(localInputs, remote);

        pruneOldFrames(frame);

        return true;
    }

    private void drainNetwork(int currentFrame) {
        Message msg;
        int latestFrame = -1;
        while ((msg = peer.poll()) != null) {
            // Reverse the remote inputs so the opponent moves correctly
            // on the local screen (local player always on the left)
            int[] reversed = reverseRemoteInputs(msg.inputs);
            confirmedRemote.put(msg.frame, reversed);

            if (msg.frame > latestFrame) {
                latestFrame = msg.frame;
                lastRemote = reversed.clone();
            }
        }
        if (latestFrame > confirmedFrame) {
            confirmedFrame = latestFrame;
        }
    }

    private int[] reverseRemoteInputs(int[] raw) {
        int[] reversed = raw.clone();
        // swap left (index 0) and right (index 1)
        int temp = reversed[0];
        reversed[0] = reversed[1];
        reversed[1] = temp;
        return reversed;
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
            boolean inputMismatch = (predicted == null || !Arrays.equals(predicted, confirmed));

            if (inputMismatch) {
                earliest = (earliest == -1) ? f : Math.min(earliest, f);
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
            int[] loc = (localIdx >= 0 && localIdx < localList.size())
                    ? localList.get(localIdx)
                    : new int[]{0, 0, 0};

            // Use confirmed input if available, otherwise fall back to the original prediction
            int[] rem;
            if (confirmedRemote.containsKey(f)) {
                rem = confirmedRemote.get(f);
            } else {
                rem = predictedRemote.get(f);
                if (rem == null) {
                    rem = lastRemote.clone();
                }
            }

            // Store the remote input actually used for this frame in the prediction map
            predictedRemote.put(f, rem.clone());

            current.update(loc, rem);
        }
    }

    private void trimHistory() {
        while (snapshots.size() > MAX_ROLLBACK) snapshots.pollFirst();
        while (localHistory.size() > MAX_ROLLBACK) localHistory.pollFirst();
    }

    private void pruneOldFrames(int currentFrame) {
        int cutoff = currentFrame - MAX_ROLLBACK - 1;
        confirmedRemote.entrySet().removeIf(e -> e.getKey() < cutoff);
        predictedRemote.entrySet().removeIf(e -> e.getKey() < cutoff);
    }

    @Override
    public GameState getDisplayState() {
        return current;
    }
}
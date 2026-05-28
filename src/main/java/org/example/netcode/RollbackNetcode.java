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
    private final Map<Integer, Float> confirmedPosX = new HashMap<>();
    private int[] lastRemote = {0, 0, 0};
    private final Deque<int[]> localHistory = new ArrayDeque<>(); // localHistory[i] = inputs used on frame (currentFrame - localHistory.size() + i)
    private int confirmedFrame = -1;
    private static final float SCREEN_WIDTH = 1280f;
    private static final float POS_TOLERANCE = 0f;

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

//        trimHistory();


        // Check if we need to rollback
        int rollbackTo = findRollbackFrame(frame);
        if (rollbackTo > 0) {
//            System.out.println("rollbackTo: " + rollbackTo);
//            System.out.println("current frame: " + frame);
            doRollback(rollbackTo, frame);
        }

        trimHistory();

//        if (snapshots.size() > MAX_ROLLBACK) {
//            snapshots.pollFirst();
//            localHistory.pollFirst();
//        }

        // Predict remote input for the frame (repeat last known)
        int[] remote = confirmedRemote.getOrDefault(frame, lastRemote.clone());
        predictedRemote.put(frame, remote.clone());

        // Advance one frame
        peer.send(new Message(frame, localInputs, current.player.posX));
        current.update(localInputs, remote);

        pruneOldFrames(frame);

        return true;
    }

    private void drainNetwork(int currentFrame) {
        Message msg;
        while ((msg = peer.poll()) != null) {
            //System.out.println("Received msg for frame " + msg.frame + " current frame=" + frame);
            int[] mirroredInputs = reverseRemoteInputs(msg.inputs);
            float mirroredPosX = reversePosX(msg.posX);

            confirmedRemote.put(msg.frame, mirroredInputs);
            confirmedPosX.put(msg.frame, mirroredPosX);

            if (msg.frame > currentFrame) {
                confirmedFrame = msg.frame;
                lastRemote = mirroredInputs.clone();
            }
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
//            boolean positionMismatch = hasPositionMismatch(f);

            if (inputMismatch) {
                earliest = (earliest == -1) ? f : Math.min(earliest, f);
                System.out.println("Earliest: " + earliest);
            }

//            if (predicted == null || !Arrays.equals(predicted, confirmed)) {
//                // Check whether our prediction for that frame was wrong
//                earliest = (earliest == -1) ? f : Math.min(earliest, f);
//                // Update prediciton so we don't rollback this frame again
//                predictedRemote.put(f, confirmed.clone());
//            }
//        }
//        if (earliest >= 0) {
//            System.out.println("Rollback triggered on frame " + currentFrame + " back to " + earliest);
//        }
        }
        return earliest;
    }

    private boolean hasPositionMismatch(int f) {
        Float reportedPosX = confirmedPosX.get(f);
        if (reportedPosX == null) {
            return false;
        }

        List<GameState> list = new ArrayList<>(snapshots);
        int baseFrame = current.frame - list.size() + 1;
        int idx = f - baseFrame;
        if (idx < 0 || idx >= list.size()) {
            return false;
        }

        return Math.abs(list.get(idx).opponent.posX - reportedPosX) > POS_TOLERANCE;
    }


    private void doRollback(int targetFrame, int presentFrame) {
        List<GameState> snapshotList = new ArrayList<>(snapshots);
        List<int[]> localList = new ArrayList<>(localHistory);

        int baseFrame = presentFrame - snapshotList.size() + 1;
        int idx = targetFrame - baseFrame;

//        System.out.println("presentFrame: " + presentFrame);
//        System.out.println("idx: " + idx);
//        System.out.println("targetFrame: " + targetFrame);
//        System.out.println("baseFrame: " + baseFrame);

        if (idx < 0 || idx >= snapshotList.size()) {
            return;
        }

        current = snapshotList.get(idx).copy();

        for (int f = targetFrame; f < presentFrame; f++) {
            int localIdx = f - baseFrame;
            int[] loc = (localIdx >= 0 && localIdx < localList.size()) ? localList.get(localIdx) : new int[]{0,0,0};
            int[] rem = confirmedRemote.getOrDefault(f, lastRemote.clone());
            current.update(loc, rem);

//            Float peerPosX = confirmedPosX.get(f);
//            if (peerPosX != null && current.opponent.posX != peerPosX) {
//                current.opponent.posX = peerPosX;
//            }
        }

//        snapshots = new ArrayDeque<>(snapshotList.subList(0, idx));
//        localHistory = new ArrayDeque<>(localList.subList(0, idx));

//        trimHistory();

//        while (snapshots.size() > idx) {
//            snapshots.pollLast();
//        }
//        while (localHistory.size() > idx) {
//            localHistory.pollLast();
//        }
    }

    private void trimHistory() {
        while (snapshots.size()   > MAX_ROLLBACK) snapshots.pollFirst();
        while (localHistory.size() > MAX_ROLLBACK) localHistory.pollFirst();
    }

    private void pruneOldFrames(int currentFrame) {
        int cutoff = currentFrame - MAX_ROLLBACK - 1;
        confirmedRemote.entrySet().removeIf(e -> e.getKey() < cutoff);
        confirmedPosX.entrySet().removeIf(e -> e.getKey() < cutoff);
        predictedRemote.entrySet().removeIf(e -> e.getKey() < cutoff);
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

    private float reversePosX(float posX) {
        return SCREEN_WIDTH - posX;
    }

    @Override
    public GameState getDisplayState() {
        return current;
    }
}

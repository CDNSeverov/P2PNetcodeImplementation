package org.example;

public class GameState {
    public Player player;
    public Player opponent;
    public boolean gameOver;
    public int frame;
    private int pendingGameOverFrames = 0;
    private static final int GAME_OVER_CONFIRM_FRAMES = 3;

    public static GameState createInitial() {
        GameState gs = new GameState();
        gs.player = new Player(130f, 410f, true);
        gs.opponent = new Player(1150f, 410f, false);
        gs.gameOver = false;
        gs.frame = 0;
        return gs;
    }

    private GameState() {}

    private GameState(Player player, Player opponent, boolean gameOver, int frame) {
        this.player = player;
        this.opponent = opponent;
        this.gameOver = gameOver;
        this.frame = frame;
    }

    public void update(int[] localInputs, int[] remoteInputs) {
        if (gameOver) {
            return;
        }
        player.update(localInputs);
        opponent.update(remoteInputs);
        resolveCollisions();
        frame++;

        if (pendingGameOverFrames > 0) {
            pendingGameOverFrames--;
            if (pendingGameOverFrames == 0) {
                gameOver = true;
            }
        }
    }

    private void resolveCollisions() {
        if (aabb(player.left(), player.top(), player.right(), player.bottom(), opponent.left(), opponent.top(), opponent.right(), opponent.bottom())) {
            if (player.posX >= 50f) {
                player.posX -= 10f;
            }
            if (opponent.posX <= 1230f) {
                opponent.posX += 10f;
            }
        }

        boolean shouldEnd = checkGameOverCondition();
        if (shouldEnd) {
            if (pendingGameOverFrames == 0) {
                pendingGameOverFrames = GAME_OVER_CONFIRM_FRAMES;
            }
        } else {
            pendingGameOverFrames = 0;
        }
    }

    private boolean checkGameOverCondition() {
        if (player.attack.isExtended() && aabb(player.attackLeft(), player.attackTop(), player.attackRight(), player.attackBottom(), opponent.left(), opponent.top(), opponent.right(), opponent.bottom())) {
            return true;
        }

        if (opponent.attack.isExtended() && aabb(opponent.attackLeft(), opponent.attackTop(), opponent.attackRight(), opponent.attackBottom(), player.left(), player.top(), player.right(), player.bottom())) {
            return true;
        }

        if (player.attack.isExtended() && opponent.attack.isExtended() && aabb(player.attackLeft(), player.attackTop(), player.attackRight(), player.attackBottom(), opponent.attackLeft(), opponent.attackTop(), opponent.attackRight(), opponent.attackBottom())) {
            return true;
        }

        return false;
    }

    private boolean aabb(float al, float at, float ar, float ab, float bl, float bt, float br, float bb) {
        return al < br && ar > bl && at < bb && ab > bt;
    }

    public GameState copy() {
        return new GameState(player.copy(), opponent.copy(), gameOver, frame);
    }
}

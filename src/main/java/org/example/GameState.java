package org.example;

public class GameState {
    public Player player;
    public Player opponent;
    public boolean gameOver;
    public int frame;

    public GameState() {
        this.player = player;
        this.opponent = opponent;
        gameOver = false;
        frame = 0;
    }

    private GameState(Player player, Player opponent, boolean gameOver, int frame) {
        this.player = player;
        this.opponent = opponent;
        this.gameOver = gameOver;
        this.frame = frame;
    }

    public void update(int[] localInputs, int[] remoteInputs) {
        if (gameOver) return;
        player.update(localInputs);
        opponent.update(remoteInputs);
        resolveCollisions();
        frame++;
    }

    private void resolveCollisions() {
        if (aabb(player.left(), player.top(), player.right(), player.bottom(), opponent.left(), opponent.top(), opponent.right(), opponent.bottom())) {
            if (player.posX >= 50f) {
                player.posX -= 10f;
            }
            if (opponent.posX <= 1230f) {
                player.posX += 10f;
            }
        }
        if (player.attack.isExtended() && aabb(player.left(), player.top(), player.right(), player.bottom(), opponent.left(), opponent.top(), opponent.right(), opponent.bottom())) {
            gameOver = true;
        }
        if (opponent.attack.isExtended() && aabb(player.left(), player.top(), player.right(), player.bottom(), opponent.left(), opponent.top(), opponent.right(), opponent.bottom())) {
            gameOver = true;
        }
        if (player.attack.isExtended() && opponent.attack.isExtended() && aabb(player.left(), player.top(), player.right(), player.bottom(), opponent.left(), opponent.top(), opponent.right(), opponent.bottom())) {
            gameOver = true;
        }
    }

    private boolean aabb(float al, float at, float ar, float ab, float bl, float bt, float br, float bb) {
        return al < br && ar > bl && at < bb && ab > bt;
    }

    public GameState copy() {
        return new GameState(player.copy(), opponent.copy(), gameOver, frame);
    }
}

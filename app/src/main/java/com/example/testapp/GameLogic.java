package com.example.testapp;

import java.util.Random;

public class GameLogic {

    private static final int TOTAL_JUMPS = 5;

    public PlatformGlass[] platforms;
    public Player player;

    private int currentJump = 0;
    private Random random = new Random();

    private long startTimeMs;
    private long pausedTimeStartMs = 0;
    private long pausedAccumMs = 0;
    private boolean running = false;

    private long bestTime = Long.MAX_VALUE;

    // respawn delay state (when wrong jump)
    private boolean waitingToRespawn = false;
    private long respawnStartMs = 0;
    private long respawnDelayMs = 900; // short delay so fall animation shows

    public GameLogic() {
        resetGameState();
    }

    public void resetGameState() {
        platforms = new PlatformGlass[TOTAL_JUMPS];
        for (int i = 0; i < TOTAL_JUMPS; i++) {
            boolean correctIsLeft = random.nextBoolean();
            platforms[i] = new PlatformGlass(i, correctIsLeft);
        }
        player = new Player();
        currentJump = 0;
        running = false;
        pausedAccumMs = 0;
        waitingToRespawn = false;
    }

    public void start() {
        resetGameState();
        running = true;
        startTimeMs = System.currentTimeMillis();
    }

    public boolean isRunning() {
        return running && !waitingToRespawn;
    }

    public void pause() {
        if (!running) return;
        pausedTimeStartMs = System.currentTimeMillis();
        running = false;
    }

    public void resume() {
        if (pausedTimeStartMs == 0) {
            running = true;
            return;
        }
        long now = System.currentTimeMillis();
        pausedAccumMs += (now - pausedTimeStartMs);
        pausedTimeStartMs = 0;
        running = true;
    }

    public void restart() {
        resetGameState();
        start();
    }

    public void jumpLeft() { handleJump(true); }
    public void jumpRight() { handleJump(false); }

    private void handleJump(boolean left) {
        if (!running) return;
        if (waitingToRespawn) return;
        if (currentJump >= TOTAL_JUMPS) return;

        PlatformGlass p = platforms[currentJump];

        if (p.isCorrect(left)) {
            player.jumpTo(p.getX(left), p.getY());
            currentJump++;
            if (currentJump == TOTAL_JUMPS) {
                // finished run
                long finishMs = System.currentTimeMillis() - startTimeMs - pausedAccumMs;
                if (finishMs < bestTime) bestTime = finishMs;
                running = false;
            }
        } else {
            // wrong: break platform and start fall/respawn timer
            p.breakPlatform();
            player.fall();
            waitingToRespawn = true;
            respawnStartMs = System.currentTimeMillis();
        }
    }

    public void update() {
        // update pieces and player
        long now = System.currentTimeMillis();

        // update platform pieces if broken
        for (PlatformGlass p : platforms) {
            if (p != null) p.update();
        }

        player.update();

        // check respawn timer
        if (waitingToRespawn) {
            if (now - respawnStartMs >= respawnDelayMs) {
                // respawn player at start but keep broken platforms broken
                player = new Player(); // fresh player at start
                currentJump = 0;
                waitingToRespawn = false;
                // do not restart timer; run continues
            }
        }
    }

    public void draw(float[] vpMatrix) {
        // draw platforms then player
        for (PlatformGlass p : platforms) {
            if (p != null) p.draw(vpMatrix);
        }
        if (player != null) player.draw(vpMatrix);
    }

    // timer accessors used by UI
    public double getElapsedSeconds() {
        if (startTimeMs == 0) return 0.0;
        long now = System.currentTimeMillis();
        long elapsed = now - startTimeMs - pausedAccumMs;
        if (!running && waitingToRespawn) {
            // still count elapsed during respawn (optional): keep counting
        }
        if (elapsed < 0) elapsed = 0;
        return elapsed / 1000.0;
    }

    public long getBestTimeMs() {
        return bestTime;
    }
}

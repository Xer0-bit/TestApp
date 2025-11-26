package com.example.testapp;

import java.util.Random;

public class GameLogic {

    private static final int TOTAL_JUMPS = 5;

    public PlatformGlass[] platforms;
    public Player player;

    private int currentJump = 0;
    private Random random = new Random();

    private long startTime;
    private long pausedTime;
    private boolean running = false;

    public long bestTime = Long.MAX_VALUE;

    public GameLogic() {
        resetGame();
    }

    public void resetGame() {
        platforms = new PlatformGlass[TOTAL_JUMPS];
        for (int i = 0; i < TOTAL_JUMPS; i++) {
            boolean correctIsLeft = random.nextBoolean();
            platforms[i] = new PlatformGlass(i, correctIsLeft);
        }

        player = new Player();
        currentJump = 0;

        startTime = System.currentTimeMillis();
        running = false;
    }

    public void jumpLeft() {
        handleJump(true);
    }

    public void jumpRight() {
        handleJump(false);
    }

    private void handleJump(boolean left) {
        if (!running) return; // ignore input if not running
        if (currentJump >= TOTAL_JUMPS) return;

        PlatformGlass p = platforms[currentJump];

        if (p.isCorrect(left)) {
            player.jumpTo(p.getX(left), p.getY(), p.getZ());
            currentJump++;

            if (currentJump == TOTAL_JUMPS) {
                long finish = System.currentTimeMillis() - startTime;
                if (finish < bestTime) bestTime = finish;
                running = false;
            }

        } else {
            player.fall();
            p.breakSide(left); // Only break the incorrect side
        }
    }

    public void update() {
        player.update();
    }

    public void draw(float[] vpMatrix) {
        for (PlatformGlass p : platforms)
            p.update();
    }

    // --- New methods for MainActivity --- //

    public void start() {
        resetGame();
        running = true;
        startTime = System.currentTimeMillis();
    }

    public void pause() {
        if (running) {
            pausedTime = System.currentTimeMillis();
            running = false;
        }
    }

    public void resume() {
        if (!running) {
            long now = System.currentTimeMillis();
            startTime += (now - pausedTime);
            running = true;
        }
    }

    public void restart() {
        resetGame();
        running = true;
    }

    public boolean isRunning() {
        return running;
    }

    public double getElapsedSeconds() {
        if (running) return (System.currentTimeMillis() - startTime) / 1000.0;
        else return (pausedTime - startTime) / 1000.0;
    }

    public long getBestTimeMs() {
        return bestTime;
    }
}

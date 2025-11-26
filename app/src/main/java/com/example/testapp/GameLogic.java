package com.example.testapp;

import android.os.Handler;
import java.util.Random;

public class GameLogic {

    private static final int TOTAL_PLATFORMS = 6; // 5 platforms + 1 finish
    private static final float PLATFORM_Y = 1.0f;
    private static final float PLATFORM_Z_SPACING = 5f;

    public PlatformGlass[] platforms;
    public Player player;

    private int nextPlatform = 0;
    private Random random = new Random();
    private Handler handler = new Handler();

    private boolean running = true;
    private boolean gameWon = false;
    private long startTime = System.currentTimeMillis();
    private long pausedTime = 0;
    private double bestTime = Double.MAX_VALUE;

    public GameLogic() {
        resetGame();
    }

    public void resetGame() {
        platforms = new PlatformGlass[TOTAL_PLATFORMS];
        float startZ = 0f;

        for (int i = 0; i < TOTAL_PLATFORMS - 1; i++) {
            boolean leftIsCorrect = random.nextBoolean();
            platforms[i] = new PlatformGlass(i, leftIsCorrect, PLATFORM_Y, startZ + i * PLATFORM_Z_SPACING);
        }

        // Last platform is the finish line (both sides are correct)
        platforms[TOTAL_PLATFORMS - 1] = new PlatformGlass(TOTAL_PLATFORMS - 1, true, PLATFORM_Y, startZ + (TOTAL_PLATFORMS - 1) * PLATFORM_Z_SPACING);
        platforms[TOTAL_PLATFORMS - 1].setIsFinish(true);

        player = new Player(0f, PLATFORM_Y, -2f); // start behind first platform
        nextPlatform = 0;
        startTime = System.currentTimeMillis();
        running = true;
        gameWon = false;
    }

    public void jumpLeft() {
        handleJump(true);
    }

    public void jumpRight() {
        handleJump(false);
    }

    private void handleJump(boolean left) {
        if (nextPlatform >= TOTAL_PLATFORMS) return;

        PlatformGlass p = platforms[nextPlatform];

        if (p.isCorrect(left) || p.isFinish()) {
            player.jumpTo(p.getX(left), PLATFORM_Y, p.getZ());
            nextPlatform++;

            // Check if reached finish line
            if (nextPlatform >= TOTAL_PLATFORMS) {
                winGame();
            }
        } else {
            // mark the broken side
            p.breakSide(left);

            // make player fall and reset to start
            player.fall();
            handler.postDelayed(() -> player.respawn(), 500);

            // reset next platform index but keep platforms as they are
            nextPlatform = 0;
        }
    }

    private void winGame() {
        running = false;
        gameWon = true;
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        if (elapsed < bestTime) {
            bestTime = elapsed;
        }
    }

    public void update() {
        if (!running) return;

        player.update();

        for (PlatformGlass p : platforms) p.update();
    }

    public void draw(float[] vpMatrix) {
        for (PlatformGlass p : platforms) p.draw(vpMatrix);
        player.draw(vpMatrix);
    }

    public double getElapsedSeconds() {
        if (gameWon) {
            return (pausedTime - startTime) / 1000.0;
        }
        return running ? (System.currentTimeMillis() - startTime) / 1000.0 : (pausedTime - startTime) / 1000.0;
    }

    public boolean isRunning() { return running; }

    public boolean isGameWon() { return gameWon; }

    public double getBestTime() { return bestTime; }

    public void start() { resetGame(); }

    public void pause() {
        if (running) {
            pausedTime = System.currentTimeMillis();
            running = false;
        }
    }

    public void resume() {
        if (!running && !gameWon) {
            long now = System.currentTimeMillis();
            startTime += (now - pausedTime);
            running = true;
        }
    }

    public void restart() { resetGame(); }

    public int getNextPlatformIndex() { return nextPlatform; }
}
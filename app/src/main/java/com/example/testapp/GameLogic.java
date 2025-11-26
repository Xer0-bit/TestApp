package com.example.testapp;

import android.os.Handler;
import java.util.Random;

public class GameLogic {

    private static final int TOTAL_PLATFORMS = 6; // 5 platforms + 1 finish
    private static final float PLATFORM_Y = 1.0f;
    private static final float PLATFORM_Z_SPACING = 5f;
    private static final long INPUT_DELAY_MS = 600; // delay between jumps in milliseconds

    public PlatformGlass[] platforms;
    public Player player;
    public ParticleSystem particles;

    private int nextPlatform = 0;
    private Random random = new Random();
    private Handler handler = new Handler();

    private boolean running = true;
    private boolean gameWon = false;
    private long startTime = System.currentTimeMillis();
    private long pausedTime = 0;
    private long winTime = 0;
    private double bestTime = Double.MAX_VALUE;

    // Screen shake
    private float shakeAmount = 0f;
    private float shakeDecay = 0f;

    // Input delay
    private long lastJumpTime = 0;

    public GameLogic() {
        particles = new ParticleSystem();
        running = false; // start paused
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
        pausedTime = 0;
        winTime = 0;
        running = true;
        gameWon = false;
        shakeAmount = 0f;
        lastJumpTime = 0;
        particles = new ParticleSystem();
    }

    public void jumpLeft() {
        handleJump(true);
    }

    public void jumpRight() {
        handleJump(false);
    }

    private void handleJump(boolean left) {
        // Check input delay
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastJumpTime < INPUT_DELAY_MS) {
            return; // ignore input
        }
        lastJumpTime = currentTime;

        if (nextPlatform >= TOTAL_PLATFORMS) return;

        PlatformGlass p = platforms[nextPlatform];

        if (p.isCorrect(left) || p.isFinish()) {
            player.jumpTo(p.getX(left), PLATFORM_Y, p.getZ());
            nextPlatform++;

            // Land effect
            handler.postDelayed(() -> {
                particles.spawnLandEffect(player.x, PLATFORM_Y, player.z);
                shakeAmount = 0.15f;
                shakeDecay = 0.15f;
            }, 300);

            // Check if reached finish line
            if (nextPlatform >= TOTAL_PLATFORMS) {
                winGame();
            }
        } else {
            // mark the broken side
            p.breakSide(left);

            // Break effect immediately
            particles.spawnBreakEffect(p.getX(left), p.getY(), p.getZ());
            shakeAmount = 0.25f;
            shakeDecay = 0.25f;

            // make player fall and reset to start
            player.fall();
            handler.postDelayed(() -> player.respawn(), 500);

            // reset next platform index but keep platforms as they are
            nextPlatform = 0;
            lastJumpTime = 0; // allow immediate jump after respawn
        }
    }

    private void winGame() {
        running = false;
        gameWon = true;
        winTime = System.currentTimeMillis();
        double elapsed = (winTime - startTime) / 1000.0;
        if (elapsed < bestTime) {
            bestTime = elapsed;
        }

        // Win effect
        particles.spawnLandEffect(player.x, PLATFORM_Y, player.z);
        particles.spawnLandEffect(player.x + 0.5f, PLATFORM_Y, player.z);
        particles.spawnLandEffect(player.x - 0.5f, PLATFORM_Y, player.z);
    }

    public void update() {
        if (!running && !gameWon) return;

        player.update();

        for (PlatformGlass p : platforms) p.update();

        particles.update();

        // Decay screen shake
        if (shakeAmount > 0) {
            shakeAmount -= shakeDecay * 0.016f; // ~60fps
        }
    }

    public void draw(float[] vpMatrix) {
        for (PlatformGlass p : platforms) p.draw(vpMatrix);
        player.draw(vpMatrix);
        particles.draw(vpMatrix);
    }

    public float getShakeAmount() {
        return shakeAmount;
    }

    public double getElapsedSeconds() {
        if (gameWon) {
            return (winTime - startTime) / 1000.0;
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
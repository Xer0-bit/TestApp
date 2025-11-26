package com.example.testapp;

import java.util.Random;
import android.os.Handler;

public class GameLogic {

    private static final int TOTAL_PLATFORMS = 5;
    private static final float PLATFORM_Y = 1.0f;
    private static final float PLATFORM_Z_SPACING = 5f; // distance between platforms

    public PlatformGlass[] platforms;
    public Player player;

    private int nextPlatform = 0;
    private Random random = new Random();
    private Handler handler = new Handler();

    private boolean running = true; // always running
    private long startTime = System.currentTimeMillis();
    private long pausedTime = 0;

    public GameLogic() {
        resetGame();
    }

    public void resetGame() {
        platforms = new PlatformGlass[TOTAL_PLATFORMS];
        float startZ = -PLATFORM_Z_SPACING; // first platform in front of player
        for (int i = 0; i < TOTAL_PLATFORMS; i++) {
            boolean leftIsCorrect = random.nextBoolean();
            platforms[i] = new PlatformGlass(i, leftIsCorrect, PLATFORM_Y, startZ - i * PLATFORM_Z_SPACING);
        }

        player = new Player(0f, PLATFORM_Y, 2f); // start slightly in front
        nextPlatform = 0;
        startTime = System.currentTimeMillis();
        running = true;
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

        if (p.isCorrect(left)) {
            player.jumpTo(p.getX(left), PLATFORM_Y, p.getZ());
            nextPlatform++;
        } else {
            player.fall();
            // respawn player without restarting game
            handler.postDelayed(() -> player.respawn(), 500);
        }
    }

    public void update() {
        player.update();

        for (PlatformGlass p : platforms) {
            p.update();
        }

        // recycle platforms endlessly
        for (int i = 0; i < TOTAL_PLATFORMS; i++) {
            if (platforms[i].getZ() > player.z + 10f) { // behind player
                boolean leftIsCorrect = random.nextBoolean();
                float newZ = getFarthestPlatformZ() - PLATFORM_Z_SPACING;
                platforms[i] = new PlatformGlass(i, leftIsCorrect, PLATFORM_Y, newZ);
            }
        }
    }

    private float getFarthestPlatformZ() {
        float minZ = Float.MAX_VALUE;
        for (PlatformGlass p : platforms) {
            if (p.getZ() < minZ) minZ = p.getZ();
        }
        return minZ;
    }

    public void draw(float[] vpMatrix) {
        for (PlatformGlass p : platforms) {
            p.draw(vpMatrix);
        }
        player.draw(vpMatrix);
    }

    // --- Timer / survival methods for MainActivity ---
    public double getElapsedSeconds() {
        if (running) return (System.currentTimeMillis() - startTime) / 1000.0;
        else return (pausedTime - startTime) / 1000.0;
    }

    public long getBestTimeMs() {
        return Long.MAX_VALUE; // not used
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        resetGame();
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
            startTime += (now - pausedTime); // adjust timer
            running = true;
        }
    }

    public void restart() {
        resetGame();
    }

    // Add this getter at the bottom of GameLogic.java
    public int getNextPlatformIndex() {
        return nextPlatform;
    }

}

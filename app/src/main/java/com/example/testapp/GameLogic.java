package com.example.testapp;

import android.os.Handler;
import java.util.Random;

public class GameLogic {

    private static final int TOTAL_PLATFORMS = 5;
    private static final float PLATFORM_Y = 1.0f;
    private static final float PLATFORM_Z_SPACING = 5f;

    public PlatformGlass[] platforms;
    public Player player;

    private int nextPlatform = 0;
    private Random random = new Random();
    private Handler handler = new Handler();

    private boolean running = true;
    private long startTime = System.currentTimeMillis();
    private long pausedTime = 0;

    public GameLogic() {
        resetGame();
    }

    public void resetGame() {
        platforms = new PlatformGlass[TOTAL_PLATFORMS];
        float startZ = 0f;

        for (int i = 0; i < TOTAL_PLATFORMS; i++) {
            boolean leftIsCorrect = random.nextBoolean();
            platforms[i] = new PlatformGlass(i, leftIsCorrect, PLATFORM_Y, startZ + i * PLATFORM_Z_SPACING);
        }

        player = new Player(0f, PLATFORM_Y, -2f); // start behind first platform
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
            handler.postDelayed(() -> player.respawn(), 500);
        }
    }

    public void update() {
        player.update();

        for (PlatformGlass p : platforms) p.update();

        // recycle platforms ahead
        for (int i = 0; i < TOTAL_PLATFORMS; i++) {
            if (platforms[i].getZ() < player.z - 10f) { // behind player
                boolean leftIsCorrect = random.nextBoolean();
                float newZ = getFarthestPlatformZ() + PLATFORM_Z_SPACING;
                platforms[i] = new PlatformGlass(i, leftIsCorrect, PLATFORM_Y, newZ);
            }
        }
    }

    private float getFarthestPlatformZ() {
        float maxZ = Float.NEGATIVE_INFINITY;
        for (PlatformGlass p : platforms) if (p.getZ() > maxZ) maxZ = p.getZ();
        return maxZ;
    }

    public void draw(float[] vpMatrix) {
        for (PlatformGlass p : platforms) p.draw(vpMatrix);
        player.draw(vpMatrix);
    }

    public double getElapsedSeconds() {
        return running ? (System.currentTimeMillis() - startTime) / 1000.0 : (pausedTime - startTime) / 1000.0;
    }

    public boolean isRunning() { return running; }

    public void start() { resetGame(); }

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

    public void restart() { resetGame(); }

    public int getNextPlatformIndex() { return nextPlatform; }
}

package com.example.testapp;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Random;

public class GameLogic {

    // Constants
    private static final int TOTAL_PLATFORMS = 6;
    private static final float PLATFORM_Y = 1.0f;
    private static final float PLATFORM_Z_SPACING = 5f;
    private static final long INPUT_DELAY_MS = 150;
    private static final float JUMP_LAND_SHAKE = 0.15f;
    private static final float GLASS_BREAK_SHAKE = 0.25f;
    private static final float SHAKE_DECAY_RATE = 3.0f;
    private static final long RESPAWN_DELAY_MS = 400;
    private static final long LAND_EFFECT_DELAY_MS = 250;
    private static final float MAX_DELTA_TIME = 0.05f;
    private boolean hasStartedTimer = false;

    public enum GameState {
        MENU, PLAYING, PAUSED, WON
    }

    public PlatformGlass[] platforms;
    public Player player;

    private int nextPlatform = 0;
    private Random random = new Random();

    private GameState state = GameState.MENU;
    private long gameStartTime = 0;
    private long pauseStartTime = 0;
    private long totalPausedTime = 0;
    private long winTime = 0;
    private double bestTime = Double.MAX_VALUE;

    private float shakeAmount = 0f;
    private long lastJumpTime = 0;
    private long lastFrameTime = 0;
    private boolean isRespawning = false;
    private volatile boolean isActive = true;

    // Scheduled events system
    private static class ScheduledEvent {
        long executeAt;
        Runnable action;
        ScheduledEvent(long executeAt, Runnable action) {
            this.executeAt = executeAt;
            this.action = action;
        }
    }

    private final ArrayList<ScheduledEvent> scheduledEvents = new ArrayList<>();

    public GameLogic() {
        initializeGame();
    }

    private void initializeGame() {
        if (platforms == null) {
            platforms = new PlatformGlass[TOTAL_PLATFORMS];
        }
        float startZ = 0f;

        // First platform - starting platform (black, centered, full width)
        platforms[0] = new PlatformGlass(0, true, PLATFORM_Y, startZ);
        platforms[0].setIsStart(true);

        // Middle platforms - regular glass bridge sections
        for (int i = 1; i < TOTAL_PLATFORMS - 1; i++) {
            boolean leftIsCorrect = random.nextBoolean();
            platforms[i] = new PlatformGlass(i, leftIsCorrect, PLATFORM_Y, startZ + i * PLATFORM_Z_SPACING);
        }

        // Last platform - finish platform (black, centered, full width)
        platforms[TOTAL_PLATFORMS - 1] = new PlatformGlass(TOTAL_PLATFORMS - 1, true, PLATFORM_Y, startZ + (TOTAL_PLATFORMS - 1) * PLATFORM_Z_SPACING);
        platforms[TOTAL_PLATFORMS - 1].setIsFinish(true);

        if (player == null) {
            player = new Player(0f, PLATFORM_Y, startZ);
        } else {
            player.respawnToStart(startZ);
        }

        nextPlatform = 1; // Start at platform 1 (first glass bridge platform)
        shakeAmount = 0f;
        lastJumpTime = 0;
        lastFrameTime = SystemClock.uptimeMillis();
        isRespawning = false;
        hasStartedTimer = false;

        scheduledEvents.clear();
    }

    public void startGame() {
        initializeGame();
        state = GameState.PLAYING;
        totalPausedTime = 0;
        winTime = 0;
        lastFrameTime = SystemClock.uptimeMillis();
        lastJumpTime = SystemClock.uptimeMillis() - INPUT_DELAY_MS;
    }

    public void restartGame() {
        initializeGame();
        gameStartTime = SystemClock.uptimeMillis();
        totalPausedTime = 0;
        winTime = 0;
        state = GameState.PLAYING;
        lastFrameTime = SystemClock.uptimeMillis();
        lastJumpTime = SystemClock.uptimeMillis() - INPUT_DELAY_MS;
        hasStartedTimer = true;
    }

    public void pauseGame() {
        if (state != GameState.PLAYING) return;
        pauseStartTime = SystemClock.uptimeMillis();
        state = GameState.PAUSED;
    }

    public void resumeGame() {
        if (state != GameState.PAUSED) return;
        long pauseDuration = SystemClock.uptimeMillis() - pauseStartTime;
        totalPausedTime += pauseDuration;
        state = GameState.PLAYING;
        lastFrameTime = SystemClock.uptimeMillis();
    }

    public void returnToMenu() {
        state = GameState.MENU;
        initializeGame();
        totalPausedTime = 0;
        winTime = 0;
    }

    private void scheduleOnGLThread(Runnable action, long delayMs) {
        long runAt = SystemClock.uptimeMillis() + Math.max(0, delayMs);
        synchronized (scheduledEvents) {
            scheduledEvents.add(new ScheduledEvent(runAt, action));
        }
    }

    public void jumpLeft() {
        handleJump(true);
    }

    public void jumpRight() {
        handleJump(false);
    }

    private void handleJump(boolean left) {
        if (!isActive || platforms == null || player == null || nextPlatform >= TOTAL_PLATFORMS) {
            return;
        }

        if (isRespawning) {
            return;
        }

        long currentTime = SystemClock.uptimeMillis();
        if (currentTime - lastJumpTime < INPUT_DELAY_MS) {
            return;
        }
        lastJumpTime = currentTime;

        // Start timer on first jump
        if (!hasStartedTimer) {
            hasStartedTimer = true;
            gameStartTime = currentTime;
        }

        PlatformGlass p = platforms[nextPlatform];

        // Always jump to the platform first
        player.jumpTo(p.getX(left), PLATFORM_Y, p.getZ());
        nextPlatform++;

        if (p.isCorrect(left) || p.isFinish()) {
            // Correct platform - just land with shake
            shakeAmount = JUMP_LAND_SHAKE;

            if (nextPlatform >= TOTAL_PLATFORMS) {
                scheduleOnGLThread(this::winGame, 0);
            }
        } else {
            // Wrong platform - wait for player to land, then break it
            scheduleOnGLThread(() -> {
                if (isActive && player != null && state == GameState.PLAYING) {
                    // Break the platform
                    p.breakSide(left);
                    shakeAmount = GLASS_BREAK_SHAKE;

                    // Make player fall
                    player.fall();
                    isRespawning = true;

                    // Schedule respawn after falling
                    scheduleOnGLThread(() -> {
                        if (isActive && player != null && state == GameState.PLAYING) {
                            player.respawn();
                            nextPlatform = 1; // Reset to first glass platform
                            isRespawning = false;
                            lastJumpTime = SystemClock.uptimeMillis() - INPUT_DELAY_MS;
                        }
                    }, RESPAWN_DELAY_MS);
                }
            }, 300); // Wait 300ms for player to land before breaking
        }
    }

    private void winGame() {
        if (state != GameState.PLAYING) return;

        state = GameState.WON;
        winTime = SystemClock.uptimeMillis();
        double elapsed = getElapsedSeconds();
        if (elapsed < bestTime) {
            bestTime = elapsed;
        }
    }

    public void update() {
        if (state != GameState.PLAYING || !isActive) return;
        if (player == null || platforms == null) return;

        long currentTime = SystemClock.uptimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000.0f;
        lastFrameTime = currentTime;
        deltaTime = Math.min(deltaTime, MAX_DELTA_TIME);

        // Execute scheduled events
        synchronized (scheduledEvents) {
            for (int i = scheduledEvents.size() - 1; i >= 0; i--) {
                ScheduledEvent ev = scheduledEvents.get(i);
                if (ev.executeAt <= currentTime) {
                    try {
                        ev.action.run();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    scheduledEvents.remove(i);
                }
            }
        }

        player.update();

        for (PlatformGlass p : platforms) {
            if (p != null) {
                p.update();
            }
        }

        if (shakeAmount > 0) {
            shakeAmount = Math.max(0f, shakeAmount - SHAKE_DECAY_RATE * deltaTime);
        }
    }

    public void draw(float[] vpMatrix) {
        if (vpMatrix == null || platforms == null || player == null) {
            return;
        }

        for (PlatformGlass p : platforms) {
            if (p != null) {
                p.draw(vpMatrix);
            }
        }
        player.draw(vpMatrix);
    }

    public float getShakeAmount() {
        return Math.max(0, shakeAmount);
    }

    public double getElapsedSeconds() {
        if (!hasStartedTimer) return 0;
        if (state == GameState.MENU) {
            return 0;
        }
        if (state == GameState.WON) {
            if (winTime == 0) return 0;
            return (winTime - gameStartTime - totalPausedTime) / 1000.0;
        }
        if (state == GameState.PAUSED) {
            if (pauseStartTime == 0) return 0;
            return (pauseStartTime - gameStartTime - totalPausedTime) / 1000.0;
        }
        return (SystemClock.uptimeMillis() - gameStartTime - totalPausedTime) / 1000.0;
    }

    public GameState getGameState() { return state; }
    public boolean isPlaying() { return state == GameState.PLAYING; }
    public boolean isGameWon() { return state == GameState.WON; }
    public double getBestTime() { return bestTime == Double.MAX_VALUE ? 0 : bestTime; }

    public void cleanup() {
        isActive = false;
        synchronized (scheduledEvents) {
            scheduledEvents.clear();
        }
    }
}
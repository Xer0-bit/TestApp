package com.example.testapp;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.Random;

public class GameLogic {

    private static final int TOTAL_PLATFORMS = 6;
    private static final float PLATFORM_Y = 1.0f;
    private static final float PLATFORM_Z_SPACING = 5f;
    private static final long INPUT_DELAY_MS = 200;
    private static final float SHAKE_DECAY_RATE = 3.0f;
    private static final long RESPAWN_DELAY_MS = 1000;
    private static final long LAND_EFFECT_DELAY_MS = 300;
    private static final float MAX_DELTA_TIME = 0.05f;

    public enum GameState {
        MENU, PLAYING, PAUSED, WON
    }

    public PlatformGlass[] platforms;
    public Player player;
    public ParticleSystem particles;

    private int nextPlatform = 0;
    private Random random = new Random();
    private Handler handler;

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
    private boolean hasStartedTimer = false;
    private volatile boolean isActive = true;

    // Deferred effects - simple queue for land/break effects
    private long landEffectTime = 0;
    private float landEffectX, landEffectY, landEffectZ;
    private long breakEffectTime = 0;
    private long respawnTime = 0;

    public GameLogic() {
        handler = new Handler(Looper.getMainLooper());
        particles = new ParticleSystem();
        initializeGame();
    }

    private void initializeGame() {
        if (platforms == null) {
            platforms = new PlatformGlass[TOTAL_PLATFORMS];
        }
        float startZ = 0f;

        for (int i = 0; i < TOTAL_PLATFORMS - 1; i++) {
            boolean leftIsCorrect = random.nextBoolean();
            platforms[i] = new PlatformGlass(i, leftIsCorrect, PLATFORM_Y, startZ + i * PLATFORM_Z_SPACING);
        }

        platforms[TOTAL_PLATFORMS - 1] = new PlatformGlass(TOTAL_PLATFORMS - 1, true, PLATFORM_Y, startZ + (TOTAL_PLATFORMS - 1) * PLATFORM_Z_SPACING);
        platforms[TOTAL_PLATFORMS - 1].setIsFinish(true);

        if (player == null) {
            player = new Player(0f, PLATFORM_Y, -2f);
        } else {
            player.respawn();
        }

        nextPlatform = 0;
        shakeAmount = 0f;
        lastJumpTime = 0;
        lastFrameTime = SystemClock.uptimeMillis();
        isRespawning = false;
        hasStartedTimer = false;
        landEffectTime = 0;
        breakEffectTime = 0;
        respawnTime = 0;

        if (particles == null) {
            particles = new ParticleSystem();
        } else {
            particles.clear();
        }
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

        // Start timer when first jump happens
        if (!hasStartedTimer) {
            hasStartedTimer = true;
            gameStartTime = currentTime;
        }

        PlatformGlass p = platforms[nextPlatform];

        if (p.isCorrect(left) || p.isFinish()) {
            player.jumpTo(p.getX(left), PLATFORM_Y, p.getZ());
            nextPlatform++;

            // Schedule land effect to run in update() instead of on main thread
            landEffectTime = currentTime + LAND_EFFECT_DELAY_MS;
            landEffectX = player.x;
            landEffectY = PLATFORM_Y;
            landEffectZ = player.z;

            if (nextPlatform >= TOTAL_PLATFORMS) {
                winGame();
            }
        } else {
            p.breakSide(left);

            // Spawn break effect immediately (no threading)
            if (particles != null) {
                particles.spawnBreakEffect(p.getX(left), p.getY(), p.getZ());
            }

            shakeAmount = 0.25f;

            if (player != null) {
                player.fall();
                isRespawning = true;
                respawnTime = currentTime + RESPAWN_DELAY_MS;
            }
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

        if (particles != null) {
            particles.spawnLandEffect(player.x, PLATFORM_Y, player.z);
            particles.spawnLandEffect(player.x + 0.5f, PLATFORM_Y, player.z);
            particles.spawnLandEffect(player.x - 0.5f, PLATFORM_Y, player.z);
        }
    }

    public void update() {
        if (state != GameState.PLAYING || !isActive) return;

        if (player == null || platforms == null) return;

        long currentTime = SystemClock.uptimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000.0f;
        lastFrameTime = currentTime;

        deltaTime = Math.min(deltaTime, MAX_DELTA_TIME);

        // Process deferred land effect
        if (landEffectTime > 0 && currentTime >= landEffectTime) {
            if (particles != null) {
                particles.spawnLandEffect(landEffectX, landEffectY, landEffectZ);
            }
            shakeAmount = 0.15f;
            landEffectTime = 0;
        }

        // Process deferred respawn
        if (isRespawning && respawnTime > 0 && currentTime >= respawnTime) {
            if (player != null && state == GameState.PLAYING) {
                player.respawn();
                nextPlatform = 0;
                isRespawning = false;
                lastJumpTime = currentTime - INPUT_DELAY_MS;
            }
            respawnTime = 0;
        }

        player.update();

        for (PlatformGlass p : platforms) {
            if (p != null) {
                p.update();
            }
        }

        if (particles != null) {
            particles.update();
        }

        if (shakeAmount > 0) {
            shakeAmount = Math.max(0f, shakeAmount - SHAKE_DECAY_RATE * deltaTime);
        }
    }

    public void draw(float[] vpMatrix) {
        if (vpMatrix == null || platforms == null || player == null || particles == null) {
            return;
        }

        for (PlatformGlass p : platforms) {
            if (p != null) {
                p.draw(vpMatrix);
            }
        }
        player.draw(vpMatrix);
        particles.draw(vpMatrix);
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
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
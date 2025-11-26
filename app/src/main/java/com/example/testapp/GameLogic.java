package com.example.testapp;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Random;

public class GameLogic {

    // Constants
    private static final int TOTAL_PLATFORMS = 6;
    private static final float PLATFORM_Y = 1.0f;
    private static final float PLATFORM_Z_SPACING = 5f;
    private static final long INPUT_DELAY_MS = 200;
    private static final float GRAVITY = 0.02f;
    private static final float FALL_SPEED = 0.05f;
    private static final float JUMP_LAND_SHAKE = 0.15f;
    private static final float GLASS_BREAK_SHAKE = 0.25f;
    private static final float SHAKE_DECAY_RATE = 3.0f;
    private static final long RESPAWN_DELAY_MS = 200;
    private static final long POST_RESPAWN_COOLDOWN_MS = 250;
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

    // Track if handler is active to prevent memory leaks
    private volatile boolean isActive = true;

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

        if (particles == null) {
            particles = new ParticleSystem();
        } else {
            particles.clear();
        }
    }

    public void startGame() {
        if (state != GameState.MENU) return;

        initializeGame();
        gameStartTime = SystemClock.uptimeMillis();
        totalPausedTime = 0;
        winTime = 0;
        state = GameState.PLAYING;
        lastFrameTime = SystemClock.uptimeMillis();
        // Set lastJumpTime to past to allow immediate first jump
        lastJumpTime = SystemClock.uptimeMillis() - INPUT_DELAY_MS;
    }

    public void restartGame() {
        initializeGame();
        gameStartTime = SystemClock.uptimeMillis();
        totalPausedTime = 0;
        winTime = 0;
        state = GameState.PLAYING;
        lastFrameTime = SystemClock.uptimeMillis();
        // Set lastJumpTime to past to allow immediate first jump
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
        if (state != GameState.PLAYING) return;
        handleJump(true);
    }

    public void jumpRight() {
        if (state != GameState.PLAYING) return;
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

        PlatformGlass p = platforms[nextPlatform];

        if (p.isCorrect(left) || p.isFinish()) {
            player.jumpTo(p.getX(left), PLATFORM_Y, p.getZ());
            nextPlatform++;

            if (isActive) {
                handler.postDelayed(() -> {
                    if (isActive && player != null && particles != null && state == GameState.PLAYING) {
                        particles.spawnLandEffect(player.x, PLATFORM_Y, player.z);
                        shakeAmount = JUMP_LAND_SHAKE;
                    }
                }, LAND_EFFECT_DELAY_MS);
            }

            if (nextPlatform >= TOTAL_PLATFORMS) {
                winGame();
            }
        } else {
            p.breakSide(left);

            if (particles != null) {
                particles.spawnBreakEffect(p.getX(left), p.getY(), p.getZ());
            }

            shakeAmount = GLASS_BREAK_SHAKE;

            if (player != null) {
                player.fall();
                isRespawning = true;

                if (isActive) {
                    handler.postDelayed(() -> {
                        if (isActive && player != null && state == GameState.PLAYING) {
                            player.respawn();
                            nextPlatform = 0;
                            isRespawning = false;
                            // Set lastJumpTime to past to allow immediate jump after respawn
                            lastJumpTime = SystemClock.uptimeMillis() - INPUT_DELAY_MS;
                        }
                    }, RESPAWN_DELAY_MS);
                }
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
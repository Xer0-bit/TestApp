package com.example.testapp;

import android.os.Handler;
import java.util.Random;

public class GameLogic {

    private static final int TOTAL_PLATFORMS = 6;
    private static final float PLATFORM_Y = 1.0f;
    private static final float PLATFORM_Z_SPACING = 5f;
    private static final long INPUT_DELAY_MS = 200;

    public enum GameState {
        MENU, PLAYING, PAUSED, WON
    }

    public PlatformGlass[] platforms;
    public Player player;
    public ParticleSystem particles;

    private int nextPlatform = 0;
    private Random random = new Random();
    private Handler handler = new Handler();

    private GameState state = GameState.MENU;
    private long gameStartTime = 0;
    private long pauseStartTime = 0;
    private long totalPausedTime = 0;
    private long winTime = 0;
    private double bestTime = Double.MAX_VALUE;

    private float shakeAmount = 0f;
    private float shakeDecay = 0f;
    private long lastJumpTime = 0;

    public GameLogic() {
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
        shakeDecay = 0f;
        lastJumpTime = 0;

        if (particles == null) {
            particles = new ParticleSystem();
        }
    }

    public void startGame() {
        if (state != GameState.MENU) return;

        initializeGame();
        gameStartTime = System.currentTimeMillis();
        totalPausedTime = 0;
        winTime = 0;
        state = GameState.PLAYING;
    }

    public void restartGame() {
        initializeGame();
        gameStartTime = System.currentTimeMillis();
        totalPausedTime = 0;
        winTime = 0;
        state = GameState.PLAYING;
    }

    public void pauseGame() {
        if (state != GameState.PLAYING) return;
        pauseStartTime = System.currentTimeMillis();
        state = GameState.PAUSED;
    }

    public void resumeGame() {
        if (state != GameState.PAUSED) return;
        long pauseDuration = System.currentTimeMillis() - pauseStartTime;
        totalPausedTime += pauseDuration;
        state = GameState.PLAYING;
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
        // Safety check
        if (platforms == null || player == null || nextPlatform >= TOTAL_PLATFORMS) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastJumpTime < INPUT_DELAY_MS) {
            return;
        }
        lastJumpTime = currentTime;

        PlatformGlass p = platforms[nextPlatform];

        if (p.isCorrect(left) || p.isFinish()) {
            player.jumpTo(p.getX(left), PLATFORM_Y, p.getZ());
            nextPlatform++;

            handler.postDelayed(() -> {
                if (player != null && particles != null) {
                    particles.spawnLandEffect(player.x, PLATFORM_Y, player.z);
                    shakeAmount = 0.15f;
                    shakeDecay = 0.15f;
                }
            }, 300);

            if (nextPlatform >= TOTAL_PLATFORMS) {
                winGame();
            }
        } else {
            p.breakSide(left);

            if (particles != null) {
                particles.spawnBreakEffect(p.getX(left), p.getY(), p.getZ());
            }

            shakeAmount = 0.25f;
            shakeDecay = 0.25f;

            if (player != null) {
                player.fall();
                handler.postDelayed(() -> {
                    if (player != null && state == GameState.PLAYING) {
                        player.respawn();
                        nextPlatform = 0;
                        lastJumpTime = System.currentTimeMillis() - INPUT_DELAY_MS;
                    }
                }, 1000);
            }
        }
    }

    private void winGame() {
        if (state != GameState.PLAYING) return; // Prevent double-win

        state = GameState.WON;
        winTime = System.currentTimeMillis();
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
        if (state != GameState.PLAYING) return;

        if (player == null || platforms == null) return;

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
            shakeAmount -= shakeDecay * 0.016f;
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
        return Math.max(0, shakeAmount); // Prevent negative shake
    }

    public double getElapsedSeconds() {
        if (state == GameState.MENU) {
            return 0;
        }
        if (state == GameState.WON) {
            if (winTime == 0) return 0; // Safety check
            return (winTime - gameStartTime - totalPausedTime) / 1000.0;
        }
        if (state == GameState.PAUSED) {
            if (pauseStartTime == 0) return 0; // Safety check
            return (pauseStartTime - gameStartTime - totalPausedTime) / 1000.0;
        }
        // PLAYING state
        return (System.currentTimeMillis() - gameStartTime - totalPausedTime) / 1000.0;
    }

    public GameState getGameState() { return state; }
    public boolean isPlaying() { return state == GameState.PLAYING; }
    public boolean isGameWon() { return state == GameState.WON; }
    public double getBestTime() { return bestTime == Double.MAX_VALUE ? 0 : bestTime; }
}
package com.example.testapp;

import java.util.Random;
import android.os.Handler;

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

    private Handler handler = new Handler();

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
        if (!running) return;
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
            p.breakSide(left);

            // Reset after fall
            handler.postDelayed(this::resetGame, 1000);
        }
    }

    public void update() {
        player.update();

        // Update platforms animations
        for (PlatformGlass p : platforms) {
            p.update();
        }

        // Reset player if he fell below visible
        if (!player.isJumping() && !player.isFalling()) return;
        if (player.y < -4f) {
            handler.postDelayed(this::resetGame, 1000);
        }
    }

    public void draw(float[] vpMatrix) {
        for (PlatformGlass p : platforms) {
            p.draw(vpMatrix);
        }
        player.draw(vpMatrix);
    }

    // --- Controls for MainActivity ---
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

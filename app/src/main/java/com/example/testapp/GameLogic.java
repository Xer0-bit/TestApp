package com.example.testapp;

import java.util.Random;

public class GameLogic {

    private static final int TOTAL_JUMPS = 5;

    public PlatformGlass[] platforms;
    public Player player;

    private int currentJump = 0;
    private Random random = new Random();

    private long startTime;
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
    }

    public void jumpLeft() {
        handleJump(true);
    }

    public void jumpRight() {
        handleJump(false);
    }

    private void handleJump(boolean left) {
        if (currentJump >= TOTAL_JUMPS) return;

        PlatformGlass p = platforms[currentJump];

        if (p.isCorrect(left)) {
            player.jumpTo(p.getX(left), p.getY());
            currentJump++;

            if (currentJump == TOTAL_JUMPS) {
                long finish = System.currentTimeMillis() - startTime;
                if (finish < bestTime) bestTime = finish;
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
            p.draw(vpMatrix);

        player.draw(vpMatrix);
    }
}

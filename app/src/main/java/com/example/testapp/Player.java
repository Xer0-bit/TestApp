package com.example.testapp;

public class Player {

    private static final float JUMP_SPEED = 0.15f;
    private static final float FALL_SPEED = 0.3f;
    private static final float FALL_THRESHOLD = -2f;
    private static final float POSITION_EPSILON = 0.01f;
    private static final float PLAYER_SIZE = 0.4f;

    public float x, y, z;

    private float targetX, targetY, targetZ;
    private boolean jumping = false;
    private boolean falling = false;

    // Save start position to reset after wrong step
    private float startX, startY, startZ;

    // Player color
    private final float[] playerColor = {1f, 0.8f, 0.1f, 1f}; // Gold/yellow

    public Player(float startX, float startY, float startZ) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;

        x = startX;
        y = startY;
        z = startZ;

        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public boolean isJumping() {
        return jumping;
    }

    public boolean isFalling() {
        return falling;
    }

    public void jumpTo(float tX, float tY, float tZ) {
        targetX = tX;
        targetY = tY;
        targetZ = tZ;
        jumping = true;
        falling = false;
    }

    public void fall() {
        jumping = false;
        falling = true;
    }

    public void respawn() {
        jumping = false;
        falling = false;

        // Reset to start position
        x = startX;
        y = startY;
        z = startZ;

        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public void respawnToStart(float newStartZ) {
        this.startZ = newStartZ;
        respawn();
    }

    public void update() {
        if (jumping) {
            float dx = targetX - x;
            float dy = targetY - y;
            float dz = targetZ - z;

            // Check if player has reached target
            if (Math.abs(dx) < POSITION_EPSILON &&
                    Math.abs(dy) < POSITION_EPSILON &&
                    Math.abs(dz) < POSITION_EPSILON) {
                x = targetX;
                y = targetY;
                z = targetZ;
                jumping = false;
            } else {
                x += dx * JUMP_SPEED;
                y += dy * JUMP_SPEED;
                z += dz * JUMP_SPEED;
            }
        }

        if (falling) {
            y -= FALL_SPEED;
            if (y < FALL_THRESHOLD) {
                respawn();
            }
        }
    }

    public void draw(float[] vpMatrix) {
        Cube c = new Cube(x, y, z);
        c.size = PLAYER_SIZE;
        c.modelRotationX = 0;
        c.draw(vpMatrix, playerColor);
    }
}
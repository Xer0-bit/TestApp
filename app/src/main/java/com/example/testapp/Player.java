package com.example.testapp;

public class Player {

    public float x, y, z;

    private float jumpSpeed = 0.15f;
    private float targetX, targetY, targetZ;
    private boolean jumping = false;
    private boolean falling = false;

    // Save start position to reset after wrong step
    private final float startX, startY, startZ;

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

    public boolean isJumping() { return jumping; }
    public boolean isFalling() { return falling; }

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

        // reset to start
        x = startX;
        y = startY;
        z = startZ;

        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public void update() {
        if (jumping) {
            float dx = targetX - x;
            float dy = targetY - y;
            float dz = targetZ - z;

            if (Math.abs(dx) < 0.01f && Math.abs(dy) < 0.01f && Math.abs(dz) < 0.01f) {
                x = targetX;
                y = targetY;
                z = targetZ;
                jumping = false;
            } else {
                x += dx * jumpSpeed;
                y += dy * jumpSpeed;
                z += dz * jumpSpeed;
            }
        }

        if (falling) {
            y -= 0.3f;
            if (y < -2f) respawn();
        }
    }

    public void draw(float[] vpMatrix) {
        float[] color = {1f, 0.8f, 0.1f, 1f};
        Cube c = new Cube(x, y, z);
        c.size = 0.4f;
        c.modelRotationX = 0;
        c.draw(vpMatrix, color);
    }
}

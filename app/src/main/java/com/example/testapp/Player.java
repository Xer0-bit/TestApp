package com.example.testapp;

public class Player {

    public float x = 0;
    public float y = 1.0f;
    public float z = 0f;

    private float jumpSpeed = 0.2f;
    private float targetX, targetY, targetZ;
    private boolean jumping = false;
    private boolean falling = false;

    // --- Getters ---
    public boolean isJumping() { return jumping; }
    public boolean isFalling() { return falling; }

    public void jumpTo(float targetX, float targetY, float targetZ) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        jumping = true;
        falling = false;
    }

    public void fall() {
        jumping = false;
        falling = true;
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
        if (falling) y -= 0.2f;
    }

    public void draw(float[] vpMatrix) {
        float[] color = {1f, 0.8f, 0.1f, 1f};
        Cube c = new Cube(x, y, z);
        c.size = 0.4f;
        c.draw(vpMatrix, color);
    }
}

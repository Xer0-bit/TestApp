package com.example.testapp;

public class Player {

    public float x = 0;
    public float y = 1.0f;
    public float z = 0f;

    private boolean falling = false;

    public void jumpTo(float targetX, float targetY, float targetZ) {
        x = targetX;
        y = targetY;
        z = targetZ;
        falling = false;
    }

    public void fall() {
        falling = true;
    }

    public void update() {
        if (falling) {
            y -= 0.2f;
        }
    }

    public void draw(float[] vpMatrix) {
        // body color
        float[] color = {1f, 0.8f, 0.1f, 1f};

        // draw a single cube for simplicity
        Cube c = new Cube(x, y, z);
        c.size = 0.4f;
        c.draw(vpMatrix, color);
    }
}
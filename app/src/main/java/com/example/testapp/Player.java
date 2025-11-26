package com.example.testapp;

public class Player {

    public float x = 0;
    public float y = 0;
    public float z = -4.6f;

    private boolean falling = false;

    // animation targets
    private float startX = 0f, startY = 0.5f;
    private float targetX = 0f, targetY = 0.5f;
    private boolean jumping = false;
    private float t = 0f;

    public Player() {
        x = 0f;
        y = 0.5f;
    }

    public void jumpTo(float targetX, float targetY) {
        this.startX = x;
        this.startY = y;
        this.targetX = targetX;
        this.targetY = targetY;
        t = 0f;
        jumping = true;
        falling = false;
    }

    public void fall() {
        falling = true;
        jumping = false;
    }

    public void update() {
        if (jumping) {
            t += 0.06f;
            if (t >= 1f) {
                t = 1f;
                jumping = false;
                x = targetX;
                y = targetY;
            } else {
                // horizontal lerp
                x = lerp(startX, targetX, t);
                // arc for y
                float arc = (float)(4 * (t - t * t)); // parabola
                y = lerp(startY, targetY, t) + arc * 0.8f;
            }
        } else if (falling) {
            y -= 0.18f;
            if (y < -12f) {
                // keep falling offscreen — actual respawn handled by GameLogic
                falling = false;
            }
        }
    }

    private float lerp(float a, float b, float v) {
        return a + (b - a) * v;
    }

    public void draw(float[] vpMatrix) {
        // body: tall thin cube
        Cube body = new Cube(x, y + 0.2f, z);
        body.size = 0.4f; // x/z scale
        // we need y scale -> Cube.draw currently scales Y to 0.05f; to make taller,
        // we can trick by making multiple cubes stacked or use scale param - keep it simple:
        // draw several small cubes stacked to form taller body
        float[] bodyColor = {1f, 0.8f, 0.1f, 1f};
        // draw 3 stacked boxes to simulate height
        for (int i = 0; i < 3; i++) {
            Cube part = new Cube(x, y + 0.2f + i * 0.18f, z);
            part.size = 0.35f;
            part.draw(vpMatrix, bodyColor);
        }
        // head
        Cube head = new Cube(x, y + 0.85f, z);
        head.size = 0.25f;
        float[] headColor = {1f, 0.9f, 0.6f, 1f};
        head.draw(vpMatrix, headColor);
    }
}

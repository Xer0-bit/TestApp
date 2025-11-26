package com.example.testapp;

public class PlatformGlass {

    private float y, z;
    private int index;
    private boolean leftIsCorrect;

    private boolean leftBroken = false;
    private boolean rightBroken = false;
    private float breakProgressLeft = 0f;
    private float breakProgressRight = 0f;
    private static final float FALL_SPEED = 0.05f;

    private float xLeft = -1.5f;
    private float xRight = 1.5f;

    public PlatformGlass(int idx, boolean correctLeft, float y, float z) {
        this.index = idx;
        this.leftIsCorrect = correctLeft;
        this.y = y;
        this.z = z;
    }

    public boolean isCorrect(boolean left) { return left == leftIsCorrect; }

    public float getX(boolean left) { return left ? xLeft : xRight; }
    public float getY() { return y; }
    public float getZ() { return z; }

    public void breakSide(boolean left) {
        if (left) leftBroken = true;
        else rightBroken = true;
    }

    public void update() {
        if (leftBroken) breakProgressLeft += FALL_SPEED;
        if (rightBroken) breakProgressRight += FALL_SPEED;
    }

    public void draw(float[] vpMatrix) {
        float[] glassColor = {0.4f, 0.8f, 1f, 0.45f};

        if (!leftBroken || breakProgressLeft < 1f) {
            Cube c = new Cube(xLeft, y - breakProgressLeft * 3f, z);
            c.size = 1.5f;
            c.modelRotationX = 0;
            c.drawWithRotation(vpMatrix, glassColor);
        }

        if (!rightBroken || breakProgressRight < 1f) {
            Cube c = new Cube(xRight, y - breakProgressRight * 3f, z);
            c.size = 1.5f;
            c.modelRotationX = 0;
            c.drawWithRotation(vpMatrix, glassColor);
        }
    }
}

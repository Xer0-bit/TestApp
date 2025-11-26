package com.example.testapp;

public class PlatformGlass {

    private static final float FALL_SPEED = 0.05f;
    private static final float PLATFORM_SIZE = 1.5f;
    private static final float X_LEFT = -1.5f;
    private static final float X_RIGHT = 1.5f;
    private static final float MAX_FALL_DISTANCE = 3f;

    private float y, z;
    private int index;
    private boolean leftIsCorrect;
    private boolean isFinish = false;

    private boolean leftBroken = false;
    private boolean rightBroken = false;
    private float breakProgressLeft = 0f;
    private float breakProgressRight = 0f;

    private float xLeft = X_LEFT;
    private float xRight = X_RIGHT;

    public PlatformGlass(int idx, boolean correctLeft, float y, float z) {
        this.index = idx;
        this.leftIsCorrect = correctLeft;
        this.y = y;
        this.z = z;
    }

    public void setIsFinish(boolean finish) {
        this.isFinish = finish;
    }

    public boolean isCorrect(boolean left) {
        return left == leftIsCorrect || isFinish;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public float getX(boolean left) {
        return left ? xLeft : xRight;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public void breakSide(boolean left) {
        if (left) {
            leftBroken = true;
        } else {
            rightBroken = true;
        }
    }

    public void update() {
        if (leftBroken && breakProgressLeft < 1f) {
            breakProgressLeft += FALL_SPEED;
            if (breakProgressLeft > 1f) {
                breakProgressLeft = 1f;
            }
        }

        if (rightBroken && breakProgressRight < 1f) {
            breakProgressRight += FALL_SPEED;
            if (breakProgressRight > 1f) {
                breakProgressRight = 1f;
            }
        }
    }

    public void draw(float[] vpMatrix) {
        float[] glassColor = isFinish
                ? new float[]{0.2f, 1f, 0.3f, 0.45f}  // Green for finish
                : new float[]{0.4f, 0.8f, 1f, 0.45f}; // Blue for regular

        // Draw left platform
        if (!leftBroken || breakProgressLeft < 1f) {
            Cube c = new Cube(xLeft, y - breakProgressLeft * MAX_FALL_DISTANCE, z);
            c.size = PLATFORM_SIZE;
            c.modelRotationX = 0;
            c.drawWithRotation(vpMatrix, glassColor);
        }

        // Draw right platform
        if (!rightBroken || breakProgressRight < 1f) {
            Cube c = new Cube(xRight, y - breakProgressRight * MAX_FALL_DISTANCE, z);
            c.size = PLATFORM_SIZE;
            c.modelRotationX = 0;
            c.drawWithRotation(vpMatrix, glassColor);
        }
    }
}
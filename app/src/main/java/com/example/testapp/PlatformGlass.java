package com.example.testapp;

import android.opengl.Matrix;

public class PlatformGlass {

    private int index;
    private boolean leftIsCorrect;

    private boolean leftBroken = false;
    private boolean rightBroken = false;

    // animation progress (0 = intact, 1 = fully fallen)
    private float breakProgressLeft = 0f;
    private float breakProgressRight = 0f;

    private static final float FALL_SPEED = 0.05f;
    private static final float ROTATE_SPEED = 2f; // degrees per frame

    public PlatformGlass(int idx, boolean correctLeft) {
        index = idx;
        leftIsCorrect = correctLeft;
    }

    public boolean isCorrect(boolean left) {
        return left == leftIsCorrect;
    }

    public float getX(boolean left) {
        return left ? -1.5f : 1.5f;
    }

    public float getY() {
        return 1.0f + index * 1.5f;
    }

    public float getZ() {
        return -5f - index * 3f;
    }

    public void breakSide(boolean left) {
        if (left) leftBroken = true;
        else rightBroken = true;
    }

    public void update() {
        if (leftBroken && breakProgressLeft < 1f) breakProgressLeft += FALL_SPEED;
        if (rightBroken && breakProgressRight < 1f) breakProgressRight += FALL_SPEED;
    }

    public void draw(float[] vpMatrix) {
        float y = getY();
        float z = getZ();
        float[] glassColor = {0.4f, 0.8f, 1f, 0.45f};

        // LEFT PLATFORM
        if (!leftBroken || breakProgressLeft < 1f) {
            Cube leftCube = new Cube(-1.5f, y, z);
            leftCube.size = 1.5f;
            // apply falling animation
            leftCube.y = y - breakProgressLeft * 3f; // slide down
            leftCube.modelRotationX = breakProgressLeft * 45f; // tilt
            leftCube.drawWithRotation(vpMatrix, glassColor);
        }

        // RIGHT PLATFORM
        if (!rightBroken || breakProgressRight < 1f) {
            Cube rightCube = new Cube(1.5f, y, z);
            rightCube.size = 1.5f;
            rightCube.y = y - breakProgressRight * 3f;
            rightCube.modelRotationX = breakProgressRight * 45f;
            rightCube.drawWithRotation(vpMatrix, glassColor);
        }
    }

    public boolean allFallen() {
        return breakProgressLeft >= 1f && breakProgressRight >= 1f;
    }
}

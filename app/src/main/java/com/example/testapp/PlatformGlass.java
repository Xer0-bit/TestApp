package com.example.testapp;

public class PlatformGlass {

    private int index;
    private boolean leftIsCorrect;

    private boolean leftBroken = false;
    private boolean rightBroken = false;

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

    public void draw(float[] vpMatrix) {
        float y = getY();
        float z = -5f - index * 3f; // platforms extend forward away from camera
        float[] glassColor = {0.4f, 0.8f, 1f, 0.45f};

        if (!leftBroken) {
            Cube left = new Cube(-1.5f, y, z);
            left.size = 1.5f;
            left.draw(vpMatrix, glassColor);
        }

        if (!rightBroken) {
            Cube right = new Cube(1.5f, y, z);
            right.size = 1.5f;
            right.draw(vpMatrix, glassColor);
        }
    }
}

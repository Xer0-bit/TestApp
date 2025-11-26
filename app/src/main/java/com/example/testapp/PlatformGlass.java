package com.example.testapp;

public class PlatformGlass {

    private int index;
    private boolean leftIsCorrect;

    private boolean leftBroken = false;
    private boolean rightBroken = false;

    // animation progress (0 = intact, 1 = fully fallen)
    private float breakProgressLeft = 0f;
    private float breakProgressRight = 0f;

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

    // call when player jumps on a side
    public void breakSide(boolean left) {
        if (left) leftBroken = true;
        else rightBroken = true;
    }

    // call every frame
    public void update() {
        float speed = 0.05f;
        if (leftBroken && breakProgressLeft < 1f) breakProgressLeft += speed;
        if (rightBroken && breakProgressRight < 1f) breakProgressRight += speed;
    }

    public void draw(float[] vpMatrix) {
        float y = getY();
        float z = getZ();

        float[] glassColor = {0.4f, 0.8f, 1f, 0.45f};

        // LEFT PLATFORM
        float leftY = y - breakProgressLeft;
        if (!leftBroken || breakProgressLeft < 1f) {
            Cube leftCube = new Cube(-1.5f, leftY, z);
            leftCube.size = 1.5f;
            leftCube.draw(vpMatrix, glassColor);
        }

        // RIGHT PLATFORM
        float rightY = y - breakProgressRight;
        if (!rightBroken || breakProgressRight < 1f) {
            Cube rightCube = new Cube(1.5f, rightY, z);
            rightCube.size = 1.5f;
            rightCube.draw(vpMatrix, glassColor);
        }
    }
}

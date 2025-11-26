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


    float breakProgress = 0f; // 0 = intact, 1 = fully fallen
    public void breakSide(boolean left) {
        if (!leftBroken) {
            Cube leftCube = new Cube(-1.5f, y - breakProgress, z);
            leftCube.size = 1.5f;
            leftCube.draw(vpMatrix, glassColor);
        }

        if (!rightBroken) {
            Cube rightCube = new Cube(1.5f, y - breakProgress, z);
            rightCube.size = 1.5f;
            rightCube.draw(vpMatrix, glassColor);
        }

    }


    public void draw(float[] vpMatrix) {
        float y = getY();
        float z = -5f - index * 3f; // platforms extend forward away from camera
        float platformSpacing = 3.0f;
        float z = player.z - (index + 1) * platformSpacing; // platforms extend away from player

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

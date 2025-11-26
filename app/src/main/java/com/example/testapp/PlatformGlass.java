package com.example.testapp;

public class PlatformGlass {

    private static final float FALL_SPEED = 0.05f;
    private static final float PLATFORM_SIZE = 1.5f;
    private static final float X_LEFT = -1.5f;
    private static final float X_RIGHT = 1.5f;
    private static final float MAX_FALL_DISTANCE = 3f;
    private static final float BREAK_ROTATION_SPEED = 8f;

    private float y, z;
    private int index;
    private boolean leftIsCorrect;
    private boolean isFinish = false;
    private boolean isStart = false;

    private boolean leftBroken = false;
    private boolean rightBroken = false;
    private float breakProgressLeft = 0f;
    private float breakProgressRight = 0f;
    private float rotationLeft = 0f;
    private float rotationRight = 0f;

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

    public void setIsStart(boolean start) {
        this.isStart = start;
    }

    public boolean isCorrect(boolean left) {
        return left == leftIsCorrect || isFinish || isStart;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public boolean isStart() {
        return isStart;
    }

    public float getX(boolean left) {
        // For start and finish platforms, always return center
        if (isStart || isFinish) {
            return 0f;
        }
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
            rotationLeft += BREAK_ROTATION_SPEED;
            if (breakProgressLeft > 1f) {
                breakProgressLeft = 1f;
            }
        }

        if (rightBroken && breakProgressRight < 1f) {
            breakProgressRight += FALL_SPEED;
            rotationRight += BREAK_ROTATION_SPEED;
            if (breakProgressRight > 1f) {
                breakProgressRight = 1f;
            }
        }
    }

    public void draw(float[] vpMatrix) {
        // Start and finish platforms are black and full-width
        if (isStart || isFinish) {
            float[] platformColor = {0.1f, 0.1f, 0.1f, 0.9f}; // Black/dark gray
            Cube c = new Cube(0f, y, z); // Centered
            c.size = PLATFORM_SIZE * 2.2f; // Wide enough to cover both sides
            c.modelRotationX = 0;
            c.drawWithRotation(vpMatrix, platformColor);
            return;
        }

        // Regular glass platforms
        float[] glassColor = {0.4f, 0.8f, 1f, 0.45f}; // Blue

        // Draw left platform
        if (!leftBroken || breakProgressLeft < 1f) {
            if (leftBroken) {
                // Draw shattered pieces
                drawShatteredPieces(vpMatrix, xLeft, y - breakProgressLeft * MAX_FALL_DISTANCE, z,
                        rotationLeft, breakProgressLeft);
            } else {
                // Draw intact platform
                Cube c = new Cube(xLeft, y, z);
                c.size = PLATFORM_SIZE;
                c.modelRotationX = 0;
                c.drawWithRotation(vpMatrix, glassColor);
            }
        }

        // Draw right platform
        if (!rightBroken || breakProgressRight < 1f) {
            if (rightBroken) {
                // Draw shattered pieces
                drawShatteredPieces(vpMatrix, xRight, y - breakProgressRight * MAX_FALL_DISTANCE, z,
                        rotationRight, breakProgressRight);
            } else {
                // Draw intact platform
                Cube c = new Cube(xRight, y, z);
                c.size = PLATFORM_SIZE;
                c.modelRotationX = 0;
                c.drawWithRotation(vpMatrix, glassColor);
            }
        }
    }

    private void drawShatteredPieces(float[] vpMatrix, float baseX, float baseY, float baseZ,
                                     float rotation, float progress) {
        // Red color for breaking glass
        float alpha = 0.45f * (1f - progress * 0.7f);
        float[] breakColor = {0.8f, 0.2f, 0.2f, alpha};

        // Spread multiplier - pieces fly apart as they fall
        float spread = progress * 0.8f;

        // Draw 4 pieces in a 2x2 grid pattern
        float pieceSize = PLATFORM_SIZE * 0.4f;
        float offset = PLATFORM_SIZE * 0.25f;

        // Piece positions relative to center
        float[][] pieceOffsets = {
                {-offset, 0, -offset},  // Top-left
                {offset, 0, -offset},   // Top-right
                {-offset, 0, offset},   // Bottom-left
                {offset, 0, offset}     // Bottom-right
        };

        // Different rotation speeds for each piece
        float[] rotationMultipliers = {1.2f, 0.8f, 1.5f, 0.9f};

        for (int i = 0; i < 4; i++) {
            float px = baseX + pieceOffsets[i][0] * (1 + spread);
            float py = baseY + pieceOffsets[i][1];
            float pz = baseZ + pieceOffsets[i][2] * (1 + spread);

            Cube c = new Cube(px, py, pz);
            c.size = pieceSize;
            c.modelRotationX = rotation * rotationMultipliers[i];
            c.drawWithRotation(vpMatrix, breakColor);
        }
    }
}
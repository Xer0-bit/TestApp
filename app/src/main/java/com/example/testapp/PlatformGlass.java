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
        // Start and finish platforms - Ancient stone platforms
        if (isStart || isFinish) {
            float[] stoneColor = {0.25f, 0.2f, 0.15f, 0.95f}; // Dark weathered stone
            float[] glowColor = isFinish
                    ? new float[]{0.4f, 0.8f, 0.3f, 0.6f}  // Green magical glow for finish
                    : new float[]{0.5f, 0.5f, 0.8f, 0.6f}; // Blue magical glow for start

            // Main stone platform
            Cube c = new Cube(0f, y, z);
            c.size = PLATFORM_SIZE * 2.2f;
            c.modelRotationX = 0;
            c.drawWithRotation(vpMatrix, stoneColor);

            // Glowing runes underneath
            Cube glow = new Cube(0f, y - 0.15f, z);
            glow.size = PLATFORM_SIZE * 2.0f;
            glow.modelRotationX = 0;
            glow.drawWithRotation(vpMatrix, glowColor);

            return;
        }

        // Mystical glass platforms with magical energy
        float time = android.os.SystemClock.uptimeMillis() / 1000f;
        float pulse = (float) Math.sin(time * 2f + z * 0.5f) * 0.1f + 0.35f;

        float[] glassColor = {0.3f, 0.6f, 0.9f, pulse}; // Shimmering blue-cyan
        float[] edgeGlow = {0.5f, 0.8f, 1f, pulse * 0.5f}; // Bright edge glow

        // Draw left platform
        if (!leftBroken || breakProgressLeft < 1f) {
            if (leftBroken) {
                drawShatteredPieces(vpMatrix, xLeft, y - breakProgressLeft * MAX_FALL_DISTANCE, z,
                        rotationLeft, breakProgressLeft);
            } else {
                // Main glass platform
                Cube c = new Cube(xLeft, y, z);
                c.size = PLATFORM_SIZE;
                c.modelRotationX = 0;
                c.drawWithRotation(vpMatrix, glassColor);

                // Glowing magical border
                Cube border = new Cube(xLeft, y - 0.08f, z);
                border.size = PLATFORM_SIZE * 1.1f;
                border.modelRotationX = 0;
                border.drawWithRotation(vpMatrix, edgeGlow);
            }
        }

        // Draw right platform
        if (!rightBroken || breakProgressRight < 1f) {
            if (rightBroken) {
                drawShatteredPieces(vpMatrix, xRight, y - breakProgressRight * MAX_FALL_DISTANCE, z,
                        rotationRight, breakProgressRight);
            } else {
                // Main glass platform
                Cube c = new Cube(xRight, y, z);
                c.size = PLATFORM_SIZE;
                c.modelRotationX = 0;
                c.drawWithRotation(vpMatrix, glassColor);

                // Glowing magical border
                Cube border = new Cube(xRight, y - 0.08f, z);
                border.size = PLATFORM_SIZE * 1.1f;
                border.modelRotationX = 0;
                border.drawWithRotation(vpMatrix, edgeGlow);
            }
        }
    }

    private void drawShatteredPieces(float[] vpMatrix, float baseX, float baseY, float baseZ,
                                     float rotation, float progress) {
        // Mystical breaking effect - pieces turn from blue to purple/red as they shatter
        float alpha = 0.6f * (1f - progress * 0.7f);

        // Color shift from blue to fiery red-purple as it breaks
        float r = 0.3f + progress * 0.6f;  // Gets more red
        float g = 0.6f - progress * 0.5f;  // Loses green
        float b = 0.9f - progress * 0.3f;  // Slightly loses blue

        float[] breakColor = {r, g, b, alpha};

        // Add magical particle trail effect
        float[] trailColor = {0.9f, 0.4f, 0.9f, alpha * 0.3f}; // Purple trail

        float spread = progress * 0.8f;
        float pieceSize = PLATFORM_SIZE * 0.4f;
        float offset = PLATFORM_SIZE * 0.25f;

        float[][] pieceOffsets = {
                {-offset, 0, -offset},
                {offset, 0, -offset},
                {-offset, 0, offset},
                {offset, 0, offset}
        };

        float[] rotationMultipliers = {1.2f, 0.8f, 1.5f, 0.9f};

        for (int i = 0; i < 4; i++) {
            float px = baseX + pieceOffsets[i][0] * (1 + spread);
            float py = baseY + pieceOffsets[i][1];
            float pz = baseZ + pieceOffsets[i][2] * (1 + spread);

            // Main shattered piece
            Cube c = new Cube(px, py, pz);
            c.size = pieceSize;
            c.modelRotationX = rotation * rotationMultipliers[i];
            c.drawWithRotation(vpMatrix, breakColor);

            // Magical trail effect behind each piece
            if (progress > 0.2f) {
                Cube trail = new Cube(px, py + 0.2f, pz - 0.3f);
                trail.size = pieceSize * 0.7f;
                trail.modelRotationX = rotation * rotationMultipliers[i] * 0.5f;
                trail.drawWithRotation(vpMatrix, trailColor);
            }
        }
    }
}
package com.example.testapp;

import android.opengl.GLES20;

public class Player {

    private static final float JUMP_SPEED = 0.15f;
    private static final float FALL_SPEED = 0.3f;
    private static final float FALL_THRESHOLD = -2f;
    private static final float POSITION_EPSILON = 0.01f;
    private static final float PLAYER_SIZE = 0.4f;

    public float x, y, z;

    private float targetX, targetY, targetZ;
    private volatile boolean jumping = false;
    private volatile boolean falling = false;

    // Save start position to reset after wrong step
    private float startX, startY, startZ;

    public Player(float startX, float startY, float startZ) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;

        x = startX;
        y = startY;
        z = startZ;

        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public boolean isJumping() {
        return jumping;
    }

    public boolean isFalling() {
        return falling;
    }

    public void jumpTo(float tX, float tY, float tZ) {
        targetX = tX;
        targetY = tY;
        targetZ = tZ;
        jumping = true;
        falling = false;
    }

    public void fall() {
        android.util.Log.d("PLAYER_FALL", "fall() called - setting falling=true");
        jumping = false;
        falling = true;
        targetY = -10f;
    }

    public void respawn() {
        jumping = false;
        falling = false;

        x = startX;
        y = startY;
        z = startZ;

        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public void respawnToStart(float newStartZ) {
        this.startZ = newStartZ;
        respawn();
    }

    public void update() {
        if (jumping) {
            float dx = targetX - x;
            float dy = targetY - y;
            float dz = targetZ - z;

            if (Math.abs(dx) < POSITION_EPSILON &&
                    Math.abs(dy) < POSITION_EPSILON &&
                    Math.abs(dz) < POSITION_EPSILON) {
                x = targetX;
                y = targetY;
                z = targetZ;
                jumping = false;
            } else {
                x += dx * JUMP_SPEED;
                y += dy * JUMP_SPEED;
                z += dz * JUMP_SPEED;
            }
        }

        if (falling) {
            y -= FALL_SPEED;
        }
    }

    public void draw(float[] vpMatrix) {
        float time = android.os.SystemClock.uptimeMillis() / 1000f;
        float bobAmount = falling ? 0f : (float) Math.sin(time * 3f) * 0.03f;
        float starPulse = (float) Math.sin(time * 4f) * 0.15f + 0.85f;

        // Create transformation matrix for the entire wizard
        float[] wizardMatrix = new float[16];
        float[] finalVPMatrix = new float[16];

        if (falling) {
            // Apply rotation to entire wizard when falling
            android.opengl.Matrix.setIdentityM(wizardMatrix, 0);
            android.opengl.Matrix.translateM(wizardMatrix, 0, x, y, z);
            android.opengl.Matrix.rotateM(wizardMatrix, 0, time * 300f, 0f, 1f, 0f); // Spin
            android.opengl.Matrix.rotateM(wizardMatrix, 0, 80f, 1f, 0f, 0f); // Tilt

            // Create modified VP matrix that includes wizard transform
            float[] tempMatrix = new float[16];
            android.opengl.Matrix.setIdentityM(tempMatrix, 0);
            android.opengl.Matrix.translateM(tempMatrix, 0, -x, -y, -z); // Undo position
            android.opengl.Matrix.multiplyMM(finalVPMatrix, 0, wizardMatrix, 0, tempMatrix, 0);
            android.opengl.Matrix.multiplyMM(wizardMatrix, 0, vpMatrix, 0, finalVPMatrix, 0);
            finalVPMatrix = wizardMatrix;
        } else {
            finalVPMatrix = vpMatrix;
        }

        // Draw wizard using original Cube-based method
        float[] robeColor = {0.25f, 0.2f, 0.45f, 1f};

        // Lower robe
        Cube robeLower = new Cube(x, y + PLAYER_SIZE * 0.25f + bobAmount, z);
        robeLower.size = PLAYER_SIZE * 1.05f;
        robeLower.modelRotationX = 0;
        robeLower.draw(finalVPMatrix, robeColor);

        // Bottom trim (black)
        float[] bottomTrimColor = {0.1f, 0.1f, 0.15f, 1f};
        Cube bottomTrim = new Cube(x, y + PLAYER_SIZE * 0.1f + bobAmount, z);
        bottomTrim.size = PLAYER_SIZE * 1.15f;
        bottomTrim.modelRotationX = 0;
        bottomTrim.draw(finalVPMatrix, bottomTrimColor);

        // Mid robe
        Cube robeMid = new Cube(x, y + PLAYER_SIZE * 0.5f + bobAmount, z);
        robeMid.size = PLAYER_SIZE * 1.0f;
        robeMid.modelRotationX = 0;
        robeMid.draw(finalVPMatrix, robeColor);

        // Upper robe
        Cube robeUpper = new Cube(x, y + PLAYER_SIZE * 0.75f + bobAmount, z);
        robeUpper.size = PLAYER_SIZE * 0.95f;
        robeUpper.modelRotationX = 0;
        robeUpper.draw(finalVPMatrix, robeColor);

        // Shoulders
        float[] shoulderColor = {0.2f, 0.15f, 0.4f, 1f};
        Cube shoulders = new Cube(x, y + PLAYER_SIZE * 0.95f + bobAmount, z);
        shoulders.size = PLAYER_SIZE * 1.1f;
        shoulders.modelRotationX = 0;
        shoulders.draw(finalVPMatrix, shoulderColor);

        // Collar (black)
        float[] collarColor = {0.1f, 0.1f, 0.15f, 1f};
        Cube collar = new Cube(x, y + PLAYER_SIZE * 1.0f + bobAmount, z);
        collar.size = PLAYER_SIZE * 1.0f;
        collar.modelRotationX = 0;
        collar.draw(finalVPMatrix, collarColor);

        // Belt (gold - middle trim)
        float[] beltColor = {0.7f, 0.6f, 0.2f, 1f};
        Cube belt = new Cube(x, y + PLAYER_SIZE * 0.6f + bobAmount, z);
        belt.size = PLAYER_SIZE * 1.0f;
        belt.modelRotationX = 0;
        belt.draw(finalVPMatrix, beltColor);

        // Sleeves
        float[] sleeveColor = {0.22f, 0.18f, 0.42f, 1f};
        Cube leftSleeve = new Cube(x - PLAYER_SIZE * 0.55f, y + PLAYER_SIZE * 0.8f + bobAmount, z);
        leftSleeve.size = PLAYER_SIZE * 0.35f;
        leftSleeve.modelRotationX = 0;
        leftSleeve.draw(finalVPMatrix, sleeveColor);

        Cube rightSleeve = new Cube(x + PLAYER_SIZE * 0.55f, y + PLAYER_SIZE * 0.8f + bobAmount, z);
        rightSleeve.size = PLAYER_SIZE * 0.35f;
        rightSleeve.modelRotationX = 0;
        rightSleeve.draw(finalVPMatrix, sleeveColor);

        // Hands
        float[] handColor = {0.9f, 0.75f, 0.6f, 1f};
        Cube leftHand = new Cube(x - PLAYER_SIZE * 0.7f, y + PLAYER_SIZE * 0.65f + bobAmount, z);
        leftHand.size = PLAYER_SIZE * 0.3f;
        leftHand.modelRotationX = 0;
        leftHand.draw(finalVPMatrix, handColor);

        Cube rightHand = new Cube(x + PLAYER_SIZE * 0.7f, y + PLAYER_SIZE * 0.65f + bobAmount, z);
        rightHand.size = PLAYER_SIZE * 0.3f;
        rightHand.modelRotationX = 0;
        rightHand.draw(finalVPMatrix, handColor);

        // Neck and Head
        Cube neck = new Cube(x, y + PLAYER_SIZE * 1.05f + bobAmount, z);
        neck.size = PLAYER_SIZE * 0.5f;
        neck.modelRotationX = 0;
        neck.draw(finalVPMatrix, handColor);

        Cube head = new Cube(x, y + PLAYER_SIZE * 1.3f + bobAmount, z);
        head.size = PLAYER_SIZE * 0.6f;
        head.modelRotationX = 0;
        head.draw(finalVPMatrix, handColor);

        // Hat
        float[] hatColor = {0.2f, 0.15f, 0.35f, 1f};
        Cube hatBrim = new Cube(x, y + PLAYER_SIZE * 1.55f + bobAmount, z);
        hatBrim.size = PLAYER_SIZE * 0.9f;
        hatBrim.modelRotationX = 0;
        hatBrim.draw(finalVPMatrix, hatColor);

        Cube hatBase = new Cube(x, y + PLAYER_SIZE * 1.75f + bobAmount, z);
        hatBase.size = PLAYER_SIZE * 0.7f;
        hatBase.modelRotationX = 0;
        hatBase.draw(finalVPMatrix, hatColor);

        Cube hatMid = new Cube(x, y + PLAYER_SIZE * 2.0f + bobAmount, z);
        hatMid.size = PLAYER_SIZE * 0.5f;
        hatMid.modelRotationX = 0;
        hatMid.draw(finalVPMatrix, hatColor);

        Cube hatTop = new Cube(x, y + PLAYER_SIZE * 2.25f + bobAmount, z);
        hatTop.size = PLAYER_SIZE * 0.3f;
        hatTop.modelRotationX = 0;
        hatTop.draw(finalVPMatrix, hatColor);

        // Stars on back of hat (using drawWithRotation for sparkle effect)
        float[] starColor = {1f, 0.9f, 0.3f, starPulse};

        Cube star1 = new Cube(x - PLAYER_SIZE * 0.2f, y + PLAYER_SIZE * 1.7f + bobAmount, z - PLAYER_SIZE * 0.35f);
        star1.size = PLAYER_SIZE * 0.12f;
        star1.modelRotationX = time * 80f;
        star1.drawWithRotation(finalVPMatrix, starColor);

        Cube star2 = new Cube(x + PLAYER_SIZE * 0.15f, y + PLAYER_SIZE * 2.0f + bobAmount, z - PLAYER_SIZE * 0.3f);
        star2.size = PLAYER_SIZE * 0.1f;
        star2.modelRotationX = -time * 100f;
        star2.drawWithRotation(finalVPMatrix, starColor);

        Cube star3 = new Cube(x, y + PLAYER_SIZE * 2.35f + bobAmount, z - PLAYER_SIZE * 0.2f);
        star3.size = PLAYER_SIZE * 0.08f;
        star3.modelRotationX = time * 120f;
        star3.drawWithRotation(finalVPMatrix, starColor);
    }

}
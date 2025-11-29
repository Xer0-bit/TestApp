package com.example.testapp;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.nio.FloatBuffer;
import java.util.Random;

public class GameRenderer implements GLSurfaceView.Renderer {

    private GameLogic logic;
    private Context context;
    private Random shakeRandom = new Random();

    private float cachedShakeX = 0;
    private float cachedShakeY = 0;
    private float cachedShakeZ = 0;

    public static float[] projectionMatrix = new float[16];
    public static float[] viewMatrix = new float[16];
    public static float[] vpMatrix = new float[16];

    private static final float CAMERA_HEIGHT = 6f;
    private static final float CAMERA_DISTANCE = 12f;
    private static final float LOOK_AHEAD_DISTANCE = 8f;
    private static final float SHAKE_DAMPING = 0.5f;

    // Library environment
    private Cube[] leftBookshelves;
    private Cube[] rightBookshelves;
    private Cube[] leftBooks;
    private Cube[] rightBooks;
    private static final int BOOKSHELF_COUNT = 8;
    private static final int BOOKS_PER_SHELF = 5;

    // Floating magical books
    private MagicalBook[] floatingBooks;
    private static final int FLOATING_BOOK_COUNT = 15;

    // Candles on shelves
    private Candle[] leftCandles;
    private Candle[] rightCandles;

    // Animated time for effects
    private float animTime = 0f;

    private static class MagicalBook {
        float x, y, z;
        float orbitRadius;
        float orbitSpeed;
        float bobSpeed;
        float bobOffset;
        float size;
        float rotationAngle;
        float tiltAngle;
        float spinSpeed;
        float pageFlipSpeed;
        float[] coverColor;
        float[] pageColor;
        int bookStyle; // 0=ancient, 1=mystical, 2=glowing

        MagicalBook(float x, float y, float z, Random rand) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.orbitRadius = rand.nextFloat() * 2f + 0.5f;
            this.orbitSpeed = rand.nextFloat() * 0.3f + 0.2f;
            this.bobSpeed = rand.nextFloat() * 0.8f + 0.4f;
            this.bobOffset = rand.nextFloat() * 6.28f;
            this.size = rand.nextFloat() * 0.3f + 0.25f;
            this.rotationAngle = rand.nextFloat() * 360f;
            this.tiltAngle = rand.nextFloat() * 30f - 15f;
            this.spinSpeed = rand.nextFloat() * 20f + 10f;
            this.pageFlipSpeed = rand.nextFloat() * 2f + 1f;
            this.bookStyle = rand.nextInt(3);

            // Generate varied cover colors
            switch(bookStyle) {
                case 0: // Ancient leather
                    this.coverColor = new float[]{
                            0.3f + rand.nextFloat() * 0.2f,
                            0.15f + rand.nextFloat() * 0.1f,
                            0.08f,
                            0.95f
                    };
                    break;
                case 1: // Mystical blue/purple
                    this.coverColor = new float[]{
                            0.2f + rand.nextFloat() * 0.2f,
                            0.15f + rand.nextFloat() * 0.2f,
                            0.5f + rand.nextFloat() * 0.3f,
                            0.95f
                    };
                    break;
                case 2: // Glowing gold/red
                    this.coverColor = new float[]{
                            0.6f + rand.nextFloat() * 0.2f,
                            0.3f + rand.nextFloat() * 0.2f,
                            0.1f,
                            0.95f
                    };
                    break;
            }

            this.pageColor = new float[]{
                    0.95f - rand.nextFloat() * 0.1f,
                    0.9f - rand.nextFloat() * 0.1f,
                    0.75f - rand.nextFloat() * 0.1f,
                    0.95f
            };
        }
    }

    private static class Candle {
        float x, y, z;
        float flickerOffset;

        Candle(float x, float y, float z, Random rand) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.flickerOffset = rand.nextFloat() * 6.28f;
        }
    }

    public GameRenderer(Context ctx) {
        context = ctx;
        logic = new GameLogic();
        initializeLibrary();
    }

    private void initializeLibrary() {
        Random rand = new Random();

        // Create towering bookshelves along the sides
        leftBookshelves = new Cube[BOOKSHELF_COUNT];
        rightBookshelves = new Cube[BOOKSHELF_COUNT];
        leftBooks = new Cube[BOOKSHELF_COUNT * BOOKS_PER_SHELF];
        rightBooks = new Cube[BOOKSHELF_COUNT * BOOKS_PER_SHELF];

        for (int i = 0; i < BOOKSHELF_COUNT; i++) {
            leftBookshelves[i] = new Cube(-5f, 0f, i * 5f);
            rightBookshelves[i] = new Cube(5f, 0f, i * 5f);

            // Add books to each shelf
            for (int j = 0; j < BOOKS_PER_SHELF; j++) {
                int idx = i * BOOKS_PER_SHELF + j;
                float bookY = j * 1.2f + 0.5f;
                leftBooks[idx] = new Cube(-5.3f, bookY, i * 5f + (rand.nextFloat() - 0.5f) * 0.3f);
                rightBooks[idx] = new Cube(5.3f, bookY, i * 5f + (rand.nextFloat() - 0.5f) * 0.3f);
            }
        }

        // Create floating magical books
        floatingBooks = new MagicalBook[FLOATING_BOOK_COUNT];
        for (int i = 0; i < FLOATING_BOOK_COUNT; i++) {
            float x = (rand.nextFloat() - 0.5f) * 12f;
            float y = rand.nextFloat() * 6f + 2f;
            float z = rand.nextFloat() * 30f - 5f;
            floatingBooks[i] = new MagicalBook(x, y, z, rand);
        }

        // Create candles
        leftCandles = new Candle[BOOKSHELF_COUNT];
        rightCandles = new Candle[BOOKSHELF_COUNT];
        for (int i = 0; i < BOOKSHELF_COUNT; i++) {
            leftCandles[i] = new Candle(-5f, 6f, i * 5f, rand);
            rightCandles[i] = new Candle(5f, 6f, i * 5f, rand);
        }
    }

    public GameLogic getLogic() { return logic; }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Warm library atmosphere - amber/candlelit
        GLES20.glClearColor(0.12f, 0.08f, 0.05f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        ShaderHelper.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float) width / height;
        Matrix.perspectiveM(projectionMatrix, 0, 50f, aspect, 0.1f, 150f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Update animation time
        animTime += 0.016f;

        // Update game logic
        logic.update();

        Player player = logic.player;
        if (player == null) return;

        // Calculate shake
        float shake = logic.getShakeAmount();
        if (shake > 0) {
            cachedShakeX = (shakeRandom.nextFloat() - 0.5f) * shake;
            cachedShakeY = (shakeRandom.nextFloat() - 0.5f) * shake;
            cachedShakeZ = (shakeRandom.nextFloat() - 0.5f) * shake;
        } else {
            cachedShakeX = 0;
            cachedShakeY = 0;
            cachedShakeZ = 0;
        }

        // Camera positioned above and behind
        float camX = cachedShakeX;
        float camY = player.y + CAMERA_HEIGHT + cachedShakeY;
        float camZ = player.z - CAMERA_DISTANCE + cachedShakeZ;

        float lookX = cachedShakeX * SHAKE_DAMPING;
        float lookY = player.y + cachedShakeY * SHAKE_DAMPING;
        float lookZ = player.z + LOOK_AHEAD_DISTANCE + cachedShakeZ * SHAKE_DAMPING;

        Matrix.setLookAtM(viewMatrix, 0,
                camX, camY, camZ,
                lookX, lookY, lookZ,
                0f, 1f, 0f);

        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        // Draw library environment
        drawBookshelves(vpMatrix);
        drawCandles(vpMatrix);
        drawFloatingBooks(vpMatrix);

        // Draw game objects
        logic.draw(vpMatrix);
    }

    private void drawBookshelves(float[] vpMatrix) {
        // Dark wood color for shelves
        float[] woodColor = {0.25f, 0.15f, 0.08f, 0.95f};
        float[] bookColors = {
                0.6f, 0.2f, 0.15f, 0.9f,  // Red leather
                0.15f, 0.3f, 0.15f, 0.9f,  // Green leather
                0.4f, 0.3f, 0.2f, 0.9f,   // Brown leather
                0.15f, 0.2f, 0.5f, 0.9f,  // Blue leather
                0.5f, 0.4f, 0.2f, 0.9f    // Tan leather
        };

        for (int i = 0; i < BOOKSHELF_COUNT; i++) {
            // Left bookshelf structure
            Cube leftShelf = leftBookshelves[i];
            leftShelf.size = 1.5f;
            leftShelf.modelRotationX = 0;

            // Vertical supports
            for (int h = 0; h < 6; h++) {
                Cube support = new Cube(leftShelf.x, h * 1.2f, leftShelf.z);
                support.size = 1.5f;
                support.modelRotationX = 0;
                support.draw(vpMatrix, woodColor);
            }

            // Right bookshelf structure
            Cube rightShelf = rightBookshelves[i];
            rightShelf.size = 1.5f;
            rightShelf.modelRotationX = 0;

            for (int h = 0; h < 6; h++) {
                Cube support = new Cube(rightShelf.x, h * 1.2f, rightShelf.z);
                support.size = 1.5f;
                support.modelRotationX = 0;
                support.draw(vpMatrix, woodColor);
            }

            // Draw books on shelves
            for (int j = 0; j < BOOKS_PER_SHELF; j++) {
                int idx = i * BOOKS_PER_SHELF + j;

                // Left books
                Cube leftBook = leftBooks[idx];
                float[] bookColor = {
                        bookColors[j * 4],
                        bookColors[j * 4 + 1],
                        bookColors[j * 4 + 2],
                        bookColors[j * 4 + 3]
                };
                leftBook.size = 0.25f;
                leftBook.modelRotationX = 0;
                leftBook.draw(vpMatrix, bookColor);

                // Right books
                Cube rightBook = rightBooks[idx];
                rightBook.size = 0.25f;
                rightBook.modelRotationX = 0;
                rightBook.draw(vpMatrix, bookColor);
            }
        }
    }

    private void drawCandles(float[] vpMatrix) {
        for (int i = 0; i < BOOKSHELF_COUNT; i++) {
            // Left candles
            Candle leftCandle = leftCandles[i];
            float leftFlicker = (float) Math.sin(animTime * 3f + leftCandle.flickerOffset) * 0.05f + 0.95f;

            // Candle stick
            float[] candleColor = {0.9f, 0.9f, 0.8f, 0.9f};
            Cube leftStick = new Cube(leftCandle.x, leftCandle.y, leftCandle.z);
            leftStick.size = 0.15f;
            leftStick.modelRotationX = 0;
            leftStick.draw(vpMatrix, candleColor);

            // Flame glow
            float[] flameColor = {1f, 0.7f, 0.2f, 0.7f * leftFlicker};
            Cube leftFlame = new Cube(leftCandle.x, leftCandle.y + 0.3f, leftCandle.z);
            leftFlame.size = 0.2f * leftFlicker;
            leftFlame.modelRotationX = 0;
            leftFlame.draw(vpMatrix, flameColor);

            // Right candles
            Candle rightCandle = rightCandles[i];
            float rightFlicker = (float) Math.sin(animTime * 3.2f + rightCandle.flickerOffset) * 0.05f + 0.95f;

            Cube rightStick = new Cube(rightCandle.x, rightCandle.y, rightCandle.z);
            rightStick.size = 0.15f;
            rightStick.modelRotationX = 0;
            rightStick.draw(vpMatrix, candleColor);

            float[] rightFlameColor = {1f, 0.7f, 0.2f, 0.7f * rightFlicker};
            Cube rightFlame = new Cube(rightCandle.x, rightCandle.y + 0.3f, rightCandle.z);
            rightFlame.size = 0.2f * rightFlicker;
            rightFlame.modelRotationX = 0;
            rightFlame.draw(vpMatrix, rightFlameColor);
        }
    }

    private void drawFloatingBooks(float[] vpMatrix) {
        Cube cube = new Cube(0, 0, 0);

        for (MagicalBook book : floatingBooks) {
            // --- world/animation ---
            float angle = animTime * book.orbitSpeed + book.bobOffset;
            float bookX = book.x + (float) Math.cos(angle) * book.orbitRadius * 0.5f;
            float bookY = book.y + (float) Math.sin(animTime * book.bobSpeed + book.bobOffset) * 0.4f;
            float bookZ = book.z + (float) Math.sin(angle * 0.6f) * book.orbitRadius * 0.5f;

            float bookSize = book.size;
            float currentRotation = animTime * book.spinSpeed;
            float pageTurnAngle = (float) Math.sin(animTime * book.pageFlipSpeed) * 15f;

            // dimensions (Cube scale order: X=thickness, Y=height, Z=width)
            float bookHeight = bookSize * 1.4f;
            float bookWidth = bookSize * 1.2f;
            float bookThickness = bookSize * 0.15f;
            float coverThickness = bookSize * 0.02f;
            float spineWidth = bookSize * 0.03f;   // thickness along Z for the small spine block
            float pageThickness = bookSize * 0.08f;

            // === IMPORTANT: use half-widths so pieces don't overlap ===
            float pageWidth = bookWidth * 0.5f;
            float coverWidth = bookWidth * 0.5f;
            float halfSpine = spineWidth * 0.5f;

            // === world transform (position + spin + tilt) ===
            float[] worldTransform = new float[16];
            Matrix.setIdentityM(worldTransform, 0);
            Matrix.translateM(worldTransform, 0, bookX, bookY, bookZ);
            Matrix.rotateM(worldTransform, 0, currentRotation, 0f, 1f, 0f); // spin around Y
            Matrix.rotateM(worldTransform, 0, book.tiltAngle, 1f, 0f, 0f);  // tilt around X

            // Opening angle (pages/covers rotate around the vertical spine -> Y axis)
            float openAngle = -(40f + pageTurnAngle);

            // --- SPINE (center cube) ---
            float[] spineLocal = new float[16];
            Matrix.setIdentityM(spineLocal, 0);
            Matrix.scaleM(spineLocal, 0, bookThickness, bookHeight, spineWidth);
            float[] spineModel = new float[16];
            Matrix.multiplyMM(spineModel, 0, worldTransform, 0, spineLocal, 0);
            cube.drawWithModel(vpMatrix, spineModel, book.coverColor);

            // Helper pivot distances:
            // spineFace = distance from spine center to the outer face where pages hinge
            float spineFace = halfSpine;
            // pageCenterFromSpine = from spine face to page center (half the page width)
            float pageCenterFromSpine = pageWidth * 0.5f;

            // === LEFT PAGE (hinge at left spine face, rotate around Y) ===
            {
                float[] leftPageLocal = new float[16];
                Matrix.setIdentityM(leftPageLocal, 0);

                // 1) translate so hinge is at the origin for rotation (move local origin to spine face)
                Matrix.translateM(leftPageLocal, 0, 0f, 0f, -spineFace);

                // 2) rotate around Y (vertical hinge) - left side rotates negative around Y
                Matrix.rotateM(leftPageLocal, 0, -openAngle, 0f, 1f, 0f);

                // 3) after rotating, move outward so the page center is positioned correctly:
                //    move by -pageCenterFromSpine (because left is negative Z in our coordinate choice)
                Matrix.translateM(leftPageLocal, 0, 0f, 0f, -pageCenterFromSpine);

                // 4) scale to page dimensions (X = thickness, Y = height, Z = pageWidth)
                Matrix.scaleM(leftPageLocal, 0, pageThickness, bookHeight * 0.96f, pageWidth);

                float[] leftPageModel = new float[16];
                Matrix.multiplyMM(leftPageModel, 0, worldTransform, 0, leftPageLocal, 0);
                cube.drawWithModel(vpMatrix, leftPageModel, book.pageColor);
            }

            // === LEFT COVER (attached to back of left page) ===
            {
                float[] leftCoverLocal = new float[16];
                Matrix.setIdentityM(leftCoverLocal, 0);

                // 1. Move pivot to left spine face (same as page)
                Matrix.translateM(leftCoverLocal, 0, 0f, 0f, -spineFace);

                // 2. Rotate same as page
                Matrix.rotateM(leftCoverLocal, 0, -openAngle, 0f, 1f, 0f);

                // 3. Move to page center position first
                Matrix.translateM(leftCoverLocal, 0, 0f, 0f, -pageCenterFromSpine);

                // 4. Then move outward from page center to attach cover to back of page
                //    Move by: half page thickness + half cover thickness
                float coverOffset = (pageThickness + coverThickness) * 0.5f;
                Matrix.translateM(leftCoverLocal, 0, coverOffset, 0f, 0f);  // FLIPPED SIGN

                // 5. Scale into a cover (slightly larger than page)
                Matrix.scaleM(leftCoverLocal, 0, coverThickness, bookHeight, pageWidth * 1.25f);

                float[] leftCoverModel = new float[16];
                Matrix.multiplyMM(leftCoverModel, 0, worldTransform, 0, leftCoverLocal, 0);
                cube.drawWithModel(vpMatrix, leftCoverModel, book.coverColor);
            }

            // === RIGHT PAGE (hinge at right spine face on +Z) ===
            {
                float[] rightPageLocal = new float[16];
                Matrix.setIdentityM(rightPageLocal, 0);

                // hinge: move to +spineFace
                Matrix.translateM(rightPageLocal, 0, 0f, 0f, spineFace);

                // rotate around Y (opposite direction for right side)
                Matrix.rotateM(rightPageLocal, 0, openAngle, 0f, 1f, 0f);

                // move outward to page center (positive Z)
                Matrix.translateM(rightPageLocal, 0, 0f, 0f, pageCenterFromSpine);

                // scale same as left page (Z = pageWidth)
                Matrix.scaleM(rightPageLocal, 0, pageThickness, bookHeight * 0.96f, pageWidth);

                float[] rightPageModel = new float[16];
                Matrix.multiplyMM(rightPageModel, 0, worldTransform, 0, rightPageLocal, 0);
                cube.drawWithModel(vpMatrix, rightPageModel, book.pageColor);
            }

            // === RIGHT COVER (attached to back of right page) ===
            {
                float[] rightCoverLocal = new float[16];
                Matrix.setIdentityM(rightCoverLocal, 0);

                // 1. Move pivot to right spine face (same as page)
                Matrix.translateM(rightCoverLocal, 0, 0f, 0f, spineFace);

                // 2. Rotate same as page
                Matrix.rotateM(rightCoverLocal, 0, openAngle, 0f, 1f, 0f);

                // 3. Move to page center position first
                Matrix.translateM(rightCoverLocal, 0, 0f, 0f, pageCenterFromSpine);

                // 4. Then move outward from page center to attach cover to back of page
                //    Move by: half page thickness + half cover thickness
                float coverOffset = (pageThickness + coverThickness) * 0.5f;
                Matrix.translateM(rightCoverLocal, 0, coverOffset, 0f, 0f);

                // 5. Scale into a cover (slightly larger than page)
                Matrix.scaleM(rightCoverLocal, 0, coverThickness, bookHeight, pageWidth * 1.25f);

                float[] rightCoverModel = new float[16];
                Matrix.multiplyMM(rightCoverModel, 0, worldTransform, 0, rightCoverLocal, 0);
                cube.drawWithModel(vpMatrix, rightCoverModel, book.coverColor);
            }

            // === MAGICAL EFFECTS (placed relative to worldTransform) ===
            float glowPulse = (float) Math.sin(animTime * 3f + book.bobOffset) * 0.3f + 0.7f;
            switch (book.bookStyle) {
                case 0: {
                    // Ancient runes on covers
                    float[] runeColor = {0.9f, 0.75f, 0.2f, 0.7f * glowPulse};
                    for (int i = 0; i < 3; i++) {
                        float runeY = (i - 1) * bookHeight * 0.35f;

                        float[] runeLocal = new float[16];
                        Matrix.setIdentityM(runeLocal, 0);

                        // place runes on left cover
                        Matrix.translateM(runeLocal, 0, 0f, runeY, -spineFace - (coverThickness * 0.5f));
                        Matrix.rotateM(runeLocal, 0, -(openAngle + 5f), 0f, 1f, 0f);
                        Matrix.scaleM(runeLocal, 0, coverThickness * 0.3f, bookSize * 0.15f, bookSize * 0.15f);

                        float[] runeModel = new float[16];
                        Matrix.multiplyMM(runeModel, 0, worldTransform, 0, runeLocal, 0);
                        cube.drawWithModel(vpMatrix, runeModel, runeColor);
                    }

                    for (int i = 0; i < 3; i++) {
                        float runeY = (i - 1) * bookHeight * 0.35f;

                        float[] runeLocal = new float[16];
                        Matrix.setIdentityM(runeLocal, 0);
                        Matrix.translateM(runeLocal, 0, 0f, runeY, spineFace + (coverThickness * 0.5f));
                        Matrix.rotateM(runeLocal, 0, openAngle + 5f, 0f, 1f, 0f);
                        Matrix.scaleM(runeLocal, 0, coverThickness * 0.3f, bookSize * 0.15f, bookSize * 0.15f);

                        float[] runeModel = new float[16];
                        Matrix.multiplyMM(runeModel, 0, worldTransform, 0, runeLocal, 0);
                        cube.drawWithModel(vpMatrix, runeModel, runeColor);
                    }
                    break;
                }
                case 1: {
                    // Mystical orbiting particles
                    float[] particleColor = {0.4f, 0.6f, 1f, 0.8f * glowPulse};
                    for (int i = 0; i < 4; i++) {
                        float particleAngle = animTime * 2f + i * (3.14159f * 0.5f);
                        float particleRadius = bookSize * 0.8f;
                        float px = (float) Math.cos(particleAngle) * particleRadius;
                        float pz = (float) Math.sin(particleAngle) * particleRadius;

                        float[] particleLocal = new float[16];
                        Matrix.setIdentityM(particleLocal, 0);
                        Matrix.translateM(particleLocal, 0, px, 0f, pz);
                        Matrix.scaleM(particleLocal, 0, bookSize * 0.08f, bookSize * 0.08f, bookSize * 0.08f);

                        float[] particleModel = new float[16];
                        Matrix.multiplyMM(particleModel, 0, worldTransform, 0, particleLocal, 0);
                        cube.drawWithModel(vpMatrix, particleModel, particleColor);
                    }
                    break;
                }
                case 2: {
                    // Golden sparkles orbiting the book
                    float[] sparkleColor = {1f, 0.8f, 0.3f, 0.9f * glowPulse};
                    for (int i = 0; i < 6; i++) {
                        float sparkleAngle = animTime * 2.5f + i * (3.14159f / 3f);
                        float sparkleRadius = bookSize * 0.7f;
                        float sparkleHeight = (float) Math.sin(animTime * 1.5f + i) * bookHeight * 0.3f;
                        float px = (float) Math.cos(sparkleAngle) * sparkleRadius;
                        float py = sparkleHeight;
                        float pz = (float) Math.sin(sparkleAngle) * sparkleRadius;

                        float[] sparkleLocal = new float[16];
                        Matrix.setIdentityM(sparkleLocal, 0);
                        Matrix.translateM(sparkleLocal, 0, px, py, pz);
                        Matrix.rotateM(sparkleLocal, 0, animTime * 100f + i * 60f, 0f, 1f, 0f);
                        Matrix.scaleM(sparkleLocal, 0, bookSize * 0.05f, bookSize * 0.05f, bookSize * 0.05f);

                        float[] sparkleModel = new float[16];
                        Matrix.multiplyMM(sparkleModel, 0, worldTransform, 0, sparkleLocal, 0);
                        cube.drawWithModel(vpMatrix, sparkleModel, sparkleColor);
                    }
                    break;
                }
            }
        }
    }

    public void release() {
        if (logic != null) {
            logic.cleanup();
        }
        ShaderHelper.release();
    }
}

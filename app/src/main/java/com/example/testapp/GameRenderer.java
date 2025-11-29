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
        for (MagicalBook book : floatingBooks) {
            // Position + orbit
            float angle = animTime * book.orbitSpeed + book.bobOffset;
            float bookX = book.x + (float) Math.cos(angle) * book.orbitRadius * 0.5f;
            float bookY = book.y + (float) Math.sin(animTime * book.bobSpeed + book.bobOffset) * 0.4f;
            float bookZ = book.z + (float) Math.sin(angle * 0.6f) * book.orbitRadius * 0.5f;

            float bookSize = book.size;

            // Rotation for dynamic floating
            float currentRotation = animTime * book.spinSpeed;
            float pageTurnAngle = (float) Math.sin(animTime * book.pageFlipSpeed) * 15f; // Pages flutter

            // Book dimensions - thin, realistic open book
            float bookHeight = bookSize * 1.4f;      // Height (Y) - taller
            float bookWidth = bookSize * 1.2f;       // Width when open (Z)
            float bookThickness = bookSize * 0.15f;  // Thickness (X) - MUCH thinner!

            float coverThickness = bookSize * 0.02f;
            float spineWidth = bookSize * 0.06f;     // Thin spine
            float pageThickness = bookSize * 0.08f;  // Thin page block

            // Create book transformation matrix
            float[] bookMatrix = new float[16];
            float[] tempMatrix = new float[16];
            float[] finalMatrix = new float[16];

            Matrix.setIdentityM(bookMatrix, 0);
            Matrix.translateM(bookMatrix, 0, bookX, bookY, bookZ);
            Matrix.rotateM(bookMatrix, 0, currentRotation, 0f, 1f, 0f); // Spin around Y
            Matrix.rotateM(bookMatrix, 0, book.tiltAngle, 1f, 0f, 0f); // Tilt

            // Transform VP matrix
            Matrix.multiplyMM(tempMatrix, 0, vpMatrix, 0, bookMatrix, 0);

            // === SPINE (center, connecting the two sides) ===
            float[] spinePos = new float[16];
            Matrix.setIdentityM(spinePos, 0);
            Matrix.multiplyMM(finalMatrix, 0, tempMatrix, 0, spinePos, 0);

            Cube spine = new Cube(0, 0, 0);
            spine.drawCustomScale(finalMatrix, book.coverColor, bookThickness, bookHeight, spineWidth);

            // === LEFT PAGE (rotated out from spine) ===
            float openAngle = 40f + pageTurnAngle; // Pages open at 40 degrees + flutter

            float[] leftPageMatrix = new float[16];
            Matrix.setIdentityM(leftPageMatrix, 0);
            Matrix.rotateM(leftPageMatrix, 0, openAngle, 0f, 1f, 0f); // Rotate around spine
            Matrix.translateM(leftPageMatrix, 0, 0f, 0f, -bookWidth * 0.5f); // Move out from spine
            Matrix.multiplyMM(finalMatrix, 0, tempMatrix, 0, leftPageMatrix, 0);

            // Left page block
            Cube leftPages = new Cube(0, 0, 0);
            leftPages.drawCustomScale(finalMatrix, book.pageColor, pageThickness, bookHeight * 0.96f, bookWidth);

            // Left cover
            float[] leftCoverMatrix = new float[16];
            Matrix.setIdentityM(leftCoverMatrix, 0);
            Matrix.rotateM(leftCoverMatrix, 0, openAngle + 5f, 0f, 1f, 0f); // Slightly more open
            Matrix.translateM(leftCoverMatrix, 0, 0f, 0f, -bookWidth * 0.5f);
            Matrix.multiplyMM(finalMatrix, 0, tempMatrix, 0, leftCoverMatrix, 0);

            Cube leftCover = new Cube(0, 0, 0);
            leftCover.drawCustomScale(finalMatrix, book.coverColor, coverThickness, bookHeight, bookWidth);

            // === RIGHT PAGE (rotated out from spine) ===
            float[] rightPageMatrix = new float[16];
            Matrix.setIdentityM(rightPageMatrix, 0);
            Matrix.rotateM(rightPageMatrix, 0, -openAngle, 0f, 1f, 0f); // Rotate other direction
            Matrix.translateM(rightPageMatrix, 0, 0f, 0f, bookWidth * 0.5f);
            Matrix.multiplyMM(finalMatrix, 0, tempMatrix, 0, rightPageMatrix, 0);

            // Right page block
            Cube rightPages = new Cube(0, 0, 0);
            rightPages.drawCustomScale(finalMatrix, book.pageColor, pageThickness, bookHeight * 0.96f, bookWidth);

            // Right cover
            float[] rightCoverMatrix = new float[16];
            Matrix.setIdentityM(rightCoverMatrix, 0);
            Matrix.rotateM(rightCoverMatrix, 0, -(openAngle + 5f), 0f, 1f, 0f); // Slightly more open
            Matrix.translateM(rightCoverMatrix, 0, 0f, 0f, bookWidth * 0.5f);
            Matrix.multiplyMM(finalMatrix, 0, tempMatrix, 0, rightCoverMatrix, 0);

            Cube rightCover = new Cube(0, 0, 0);
            rightCover.drawCustomScale(finalMatrix, book.coverColor, coverThickness, bookHeight, bookWidth);

            // === MAGICAL EFFECTS based on book style ===
            float glowPulse = (float) Math.sin(animTime * 3f + book.bobOffset) * 0.3f + 0.7f;

            switch(book.bookStyle) {
                case 0: // Ancient - glowing runes on the cover
                    float[] runeColor = {0.9f, 0.75f, 0.2f, 0.7f * glowPulse};

                    // Runes on left cover
                    for (int i = 0; i < 3; i++) {
                        float runeY = (i - 1) * bookHeight * 0.35f;
                        float[] runePos = new float[16];
                        Matrix.setIdentityM(runePos, 0);
                        Matrix.rotateM(runePos, 0, openAngle + 5f, 0f, 1f, 0f);
                        Matrix.translateM(runePos, 0, coverThickness * 1.5f, runeY, -bookWidth * 0.5f);
                        Matrix.multiplyMM(finalMatrix, 0, tempMatrix, 0, runePos, 0);

                        Cube rune = new Cube(0, 0, 0);
                        rune.drawCustomScale(finalMatrix, runeColor,
                                coverThickness * 0.2f, bookSize * 0.15f, bookSize * 0.15f);
                    }

                    // Runes on right cover
                    for (int i = 0; i < 3; i++) {
                        float runeY = (i - 1) * bookHeight * 0.35f;
                        float[] runePos = new float[16];
                        Matrix.setIdentityM(runePos, 0);
                        Matrix.rotateM(runePos, 0, -(openAngle + 5f), 0f, 1f, 0f);
                        Matrix.translateM(runePos, 0, coverThickness * 1.5f, runeY, bookWidth * 0.5f);
                        Matrix.multiplyMM(finalMatrix, 0, tempMatrix, 0, runePos, 0);

                        Cube rune = new Cube(0, 0, 0);
                        rune.drawCustomScale(finalMatrix, runeColor,
                                coverThickness * 0.2f, bookSize * 0.15f, bookSize * 0.15f);
                    }
                    break;

                case 1: // Mystical - orbiting particles
                    float[] particleColor = {0.4f, 0.6f, 1f, 0.8f * glowPulse};
                    for (int i = 0; i < 4; i++) {
                        float particleAngle = animTime * 2f + i * (3.14159f * 0.5f);
                        float particleRadius = bookSize * 0.8f;
                        float px = (float) Math.cos(particleAngle) * particleRadius;
                        float pz = (float) Math.sin(particleAngle) * particleRadius;

                        float[] particlePos = new float[16];
                        Matrix.setIdentityM(particlePos, 0);
                        Matrix.translateM(particlePos, 0, px, 0f, pz);
                        Matrix.multiplyMM(finalMatrix, 0, tempMatrix, 0, particlePos, 0);

                        Cube particle = new Cube(0, 0, 0);
                        particle.drawCustomScale(finalMatrix, particleColor,
                                bookSize * 0.08f, bookSize * 0.08f, bookSize * 0.08f);
                    }
                    break;

                case 2: // Glowing - subtle aura effect
                    float[] auraColor = {
                            book.coverColor[0] * 1.5f,
                            book.coverColor[1] * 1.3f,
                            book.coverColor[2] * 0.8f,
                            0.2f * glowPulse
                    };

                    Cube aura = new Cube(0, 0, 0);
                    aura.drawCustomScale(tempMatrix, auraColor,
                            bookThickness * 1.5f, bookHeight * 1.2f, bookWidth * 1.8f);
                    break;
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
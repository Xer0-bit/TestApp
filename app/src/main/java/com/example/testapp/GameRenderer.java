package com.example.testapp;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
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

        MagicalBook(float x, float y, float z, Random rand) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.orbitRadius = rand.nextFloat() * 2f + 0.5f;
            this.orbitSpeed = rand.nextFloat() * 0.3f + 0.2f;
            this.bobSpeed = rand.nextFloat() * 0.8f + 0.4f;
            this.bobOffset = rand.nextFloat() * 6.28f;
            this.size = rand.nextFloat() * 0.2f + 0.15f;
            this.rotationAngle = rand.nextFloat() * 360f;
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
            // Calculate orbital position
            float angle = animTime * book.orbitSpeed + book.bobOffset;
            float bookX = book.x + (float) Math.cos(angle) * book.orbitRadius * 0.5f;
            float bookY = book.y + (float) Math.sin(animTime * book.bobSpeed + book.bobOffset) * 0.4f;
            float bookZ = book.z + (float) Math.sin(angle * 0.6f) * book.orbitRadius * 0.5f;

            // Magical glow around books
            float pulse = (float) Math.sin(animTime * 2f + book.bobOffset) * 0.2f + 0.8f;

            // Aged book colors
            float[] bookColor = {0.4f, 0.3f, 0.2f, 0.85f}; // Brown leather
            float[] glowColor = {0.7f, 0.6f, 0.9f, 0.4f * pulse}; // Purple magical aura

            // Book with rotation
            Cube bookCube = new Cube(bookX, bookY, bookZ);
            bookCube.size = book.size;
            bookCube.modelRotationX = book.rotationAngle + animTime * 20f;
            bookCube.drawWithRotation(vpMatrix, bookColor);

            // Magical aura
            Cube aura = new Cube(bookX, bookY, bookZ);
            aura.size = book.size * 1.3f;
            aura.modelRotationX = -book.rotationAngle - animTime * 15f;
            aura.drawWithRotation(vpMatrix, glowColor);
        }
    }

    public void release() {
        if (logic != null) {
            logic.cleanup();
        }
        ShaderHelper.release();
    }
}
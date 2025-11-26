package com.example.testapp;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

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

    private static final float CAMERA_HEIGHT = 4f;
    private static final float CAMERA_DISTANCE = 8f;
    private static final float LOOK_AHEAD_DISTANCE = 5f;
    private static final float SHAKE_DAMPING = 0.5f;

    public GameRenderer(Context ctx) {
        context = ctx;
        logic = new GameLogic();
    }

    public GameLogic getLogic() { return logic; }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.6f, 0.7f, 0.9f, 1f);
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
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 150f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Update game logic
        logic.update();

        Player player = logic.player;
        if (player == null) return;

        // Calculate shake once per frame
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

        // Look at point
        float lookX = cachedShakeX * SHAKE_DAMPING;
        float lookY = player.y + cachedShakeY * SHAKE_DAMPING;
        float lookZ = player.z + LOOK_AHEAD_DISTANCE + cachedShakeZ * SHAKE_DAMPING;

        Matrix.setLookAtM(viewMatrix, 0,
                camX, camY, camZ,
                lookX, lookY, lookZ,
                0f, 1f, 0f);

        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        // Draw solids
        logic.draw(vpMatrix);
    }

    public void release() {
        if (logic != null) {
            logic.cleanup();
        }
        ShaderHelper.release();
    }
}
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

        // Calculate shake ONCE per frame and cache it
        float shake = logic.getShakeAmount();
        cachedShakeX = (shakeRandom.nextFloat() - 0.5f) * shake;
        cachedShakeY = (shakeRandom.nextFloat() - 0.5f) * shake;
        cachedShakeZ = (shakeRandom.nextFloat() - 0.5f) * shake;

        // Camera positioned above and behind, looking down the platforms
        float camHeight = 4f;
        float camDistance = 8f;

        float camX = cachedShakeX;
        float camY = player.y + camHeight + cachedShakeY;
        float camZ = player.z - camDistance + cachedShakeZ;

        // Look at point: centered between platforms, slightly ahead of player
        float lookX = cachedShakeX * 0.5f;
        float lookY = player.y + cachedShakeY * 0.5f;
        float lookZ = player.z + 5f + cachedShakeZ * 0.5f;

        Matrix.setLookAtM(viewMatrix, 0,
                camX, camY, camZ,
                lookX, lookY, lookZ,
                0f, 1f, 0f);

        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        // Draw solids with depth write
        logic.draw(vpMatrix);

        // Draw particles without depth write (so they don't hide behind glass)
        GLES20.glDepthMask(false);
        if (logic.particles != null) {
            logic.particles.draw(vpMatrix);
        }
        GLES20.glDepthMask(true);
    }

    public void release() {
        ShaderHelper.release();
    }
}
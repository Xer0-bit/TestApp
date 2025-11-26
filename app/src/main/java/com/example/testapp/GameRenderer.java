package com.example.testapp;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {

    public GameLogic logic;
    private Context context;

    public static float[] projectionMatrix = new float[16];
    public static float[] viewMatrix = new float[16];
    public static float[] vpMatrix = new float[16];

    public GameRenderer(Context ctx) {
        context = ctx;
        logic = new GameLogic();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // soft sky background
        GLES20.glClearColor(0.6f, 0.7f, 0.9f, 1f);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float aspect = (float) width / height;

        // Perspective camera
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 150f);

        // Camera looking forward at platforms
        Matrix.setLookAtM(viewMatrix, 0,
                0f, 1.2f, 4f,       // eye
                0f, 1.2f, -20f,     // look at
                0f, 1f, 0f);        // up
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Build view-projection matrix
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        logic.update();
        logic.draw(vpMatrix);
    }
}

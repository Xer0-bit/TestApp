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

    // combined projection * view matrix
    private final float[] vpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    public GameRenderer(Context ctx) {
        context = ctx;
        logic = new GameLogic();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // background black
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // enable blending for glass transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // init shader program
        ShaderHelper.init();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // clear
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // update game logic
        logic.update();

        // draw using the cached vpMatrix
        logic.draw(vpMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // projection: perspective frustum
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1f;
        final float top = 1f;
        final float near = 1f;
        final float far = 50f;
        Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);

        // view: camera positioned slightly behind and above, looking toward origin
        Matrix.setLookAtM(viewMatrix, 0,
                0f, 3f, 6f,   // eyeX, eyeY, eyeZ
                0f, 1f, 0f,   // centerX, centerY, centerZ
                0f, 1f, 0f);  // upX, upY, upZ

        // vp = projection * view
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }
}

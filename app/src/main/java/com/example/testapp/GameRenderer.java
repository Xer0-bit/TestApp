package com.example.testapp;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {

    private GameLogic logic;
    private Context context;

    public static float[] projectionMatrix = new float[16];
    public static float[] viewMatrix = new float[16];
    public static float[] vpMatrix = new float[16];

    public GameRenderer(Context ctx) {
        context = ctx;
        logic = new GameLogic();
    }

    public GameLogic getLogic() {
        return logic;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.6f, 0.7f, 0.9f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

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

        // Update logic first
        logic.update();

        // Camera: center between next two platforms
        PlatformGlass[] platforms = logic.platforms;
        int next = Math.min(logic.getNextPlatformIndex(), logic.platforms.length - 1);
        PlatformGlass p1 = platforms[next];
        PlatformGlass p2 = platforms[Math.min(next + 1, platforms.length - 1)];

        float camX = (p1.getX(true) + p2.getX(true)) / 2f;
        float camY = p1.getY() + 3f;
        float camZ = (p1.getZ() + p2.getZ()) / 2f + 6f;

        // Look at midpoint
        float lookX = (p1.getX(true) + p2.getX(true)) / 2f;
        float lookY = p1.getY();
        float lookZ = (p1.getZ() + p2.getZ()) / 2f;

        Matrix.setLookAtM(viewMatrix, 0,
                camX, camY, camZ,
                lookX, lookY, lookZ,
                0f, 1f, 0f);

        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        logic.draw(vpMatrix);
    }
}

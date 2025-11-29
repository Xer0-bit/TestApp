package com.example.testapp;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Cube {

    private static final int POSITION_SIZE = 3;
    private static final int NORMAL_SIZE = 3;
    private static final int STRIDE = (POSITION_SIZE + NORMAL_SIZE) * 4;

    private static final int TRIANGLES_PER_SIDE = 2;
    private static final int SIDES = 6;
    private static final int VERTICES_PER_TRIANGLE = 3;
    private static final int TOTAL_VERTICES = SIDES * TRIANGLES_PER_SIDE * VERTICES_PER_TRIANGLE;

    // Interleaved vertices (pos + normal)
    private static final float[] VERTICES = {

            // FRONT (0, 0, 1)
            -0.5f,-0.5f,0.5f,   0,0,1,
            0.5f,-0.5f,0.5f,   0,0,1,
            0.5f, 0.5f,0.5f,   0,0,1,
            -0.5f,-0.5f,0.5f,   0,0,1,
            0.5f, 0.5f,0.5f,   0,0,1,
            -0.5f, 0.5f,0.5f,   0,0,1,

            // RIGHT (1, 0, 0)
            0.5f,-0.5f,0.5f,   1,0,0,
            0.5f,-0.5f,-0.5f,  1,0,0,
            0.5f, 0.5f,-0.5f,  1,0,0,
            0.5f,-0.5f,0.5f,   1,0,0,
            0.5f, 0.5f,-0.5f,  1,0,0,
            0.5f, 0.5f,0.5f,   1,0,0,

            // BACK (0, 0, -1)
            0.5f,-0.5f,-0.5f,  0,0,-1,
            -0.5f,-0.5f,-0.5f,  0,0,-1,
            -0.5f, 0.5f,-0.5f,  0,0,-1,
            0.5f,-0.5f,-0.5f,  0,0,-1,
            -0.5f, 0.5f,-0.5f,  0,0,-1,
            0.5f, 0.5f,-0.5f,  0,0,-1,

            // LEFT (-1, 0, 0)
            -0.5f,-0.5f,-0.5f, -1,0,0,
            -0.5f,-0.5f,0.5f,  -1,0,0,
            -0.5f, 0.5f,0.5f,  -1,0,0,
            -0.5f,-0.5f,-0.5f, -1,0,0,
            -0.5f, 0.5f,0.5f,  -1,0,0,
            -0.5f, 0.5f,-0.5f, -1,0,0,

            // TOP (0, 1, 0)
            -0.5f,0.5f,0.5f,    0,1,0,
            0.5f,0.5f,0.5f,    0,1,0,
            0.5f,0.5f,-0.5f,   0,1,0,
            -0.5f,0.5f,0.5f,    0,1,0,
            0.5f,0.5f,-0.5f,   0,1,0,
            -0.5f,0.5f,-0.5f,   0,1,0,

            // BOTTOM (0, -1, 0)
            -0.5f,-0.5f,-0.5f,  0,-1,0,
            0.5f,-0.5f,-0.5f,  0,-1,0,
            0.5f,-0.5f,0.5f,   0,-1,0,
            -0.5f,-0.5f,-0.5f,  0,-1,0,
            0.5f,-0.5f,0.5f,   0,-1,0,
            -0.5f,-0.5f,0.5f,   0,-1,0,
    };

    private static final FloatBuffer buffer;

    static {
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        buffer = bb.asFloatBuffer();
        buffer.put(VERTICES);
        buffer.position(0);
    }

    public Cube(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float x, y, z;
    public float size = 1f;

    private final float[] model = new float[16];
    private final float[] mvp = new float[16];

    // -------------------------------------------------------------------------------------
    // 2-sided draw: supply separate OUTER and INNER color
    // -------------------------------------------------------------------------------------
    public void drawTwoSided(float[] vpMatrix, float[] modelMatrix,
                             float[] outerColor, float[] innerColor) {

        GLES20.glUseProgram(ShaderHelper.program);

        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvp, 0);

        GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
        GLES20.glEnableVertexAttribArray(ShaderHelper.aNormalHandle);

        buffer.position(0);
        GLES20.glVertexAttribPointer(
                ShaderHelper.aPositionHandle,
                POSITION_SIZE,
                GLES20.GL_FLOAT,
                false,
                STRIDE,
                buffer
        );

        buffer.position(POSITION_SIZE);
        GLES20.glVertexAttribPointer(
                ShaderHelper.aNormalHandle,
                NORMAL_SIZE,
                GLES20.GL_FLOAT,
                false,
                STRIDE,
                buffer
        );

        // FRONT faces (outer color)
        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, outerColor, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, TOTAL_VERTICES / 2);

        // BACK faces (inner color)
        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, innerColor, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, TOTAL_VERTICES / 2, TOTAL_VERTICES / 2);

        GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
        GLES20.glDisableVertexAttribArray(ShaderHelper.aNormalHandle);
    }
}

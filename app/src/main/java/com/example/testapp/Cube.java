package com.example.testapp;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Cube {

    // Keep old glass thinness behavior for legacy draw() calls
    private static final float GLASS_THICKNESS = 0.05f;

    // Interleaved: position (3) + normal (3)
    private static final int POSITION_SIZE = 3;
    private static final int NORMAL_SIZE = 3;
    private static final int FLOATS_PER_VERTEX = POSITION_SIZE + NORMAL_SIZE;
    private static final int STRIDE = FLOATS_PER_VERTEX * 4; // bytes

    // 36 vertices (12 triangles) * (pos + normal)
    private static final float[] VERTICES = {
            // FRONT (0,0,1)
            -0.5f,-0.5f, 0.5f,   0f,0f,1f,
            0.5f,-0.5f, 0.5f,   0f,0f,1f,
            0.5f, 0.5f, 0.5f,   0f,0f,1f,
            -0.5f,-0.5f, 0.5f,   0f,0f,1f,
            0.5f, 0.5f, 0.5f,   0f,0f,1f,
            -0.5f, 0.5f, 0.5f,   0f,0f,1f,

            // RIGHT (1,0,0)
            0.5f,-0.5f, 0.5f,   1f,0f,0f,
            0.5f,-0.5f,-0.5f,   1f,0f,0f,
            0.5f, 0.5f,-0.5f,   1f,0f,0f,
            0.5f,-0.5f, 0.5f,   1f,0f,0f,
            0.5f, 0.5f,-0.5f,   1f,0f,0f,
            0.5f, 0.5f, 0.5f,   1f,0f,0f,

            // BACK (0,0,-1)
            0.5f,-0.5f,-0.5f,   0f,0f,-1f,
            -0.5f,-0.5f,-0.5f,   0f,0f,-1f,
            -0.5f, 0.5f,-0.5f,   0f,0f,-1f,
            0.5f,-0.5f,-0.5f,   0f,0f,-1f,
            -0.5f, 0.5f,-0.5f,   0f,0f,-1f,
            0.5f, 0.5f,-0.5f,   0f,0f,-1f,

            // LEFT (-1,0,0)
            -0.5f,-0.5f,-0.5f,   -1f,0f,0f,
            -0.5f,-0.5f, 0.5f,   -1f,0f,0f,
            -0.5f, 0.5f, 0.5f,   -1f,0f,0f,
            -0.5f,-0.5f,-0.5f,   -1f,0f,0f,
            -0.5f, 0.5f, 0.5f,   -1f,0f,0f,
            -0.5f, 0.5f,-0.5f,   -1f,0f,0f,

            // TOP (0,1,0)
            -0.5f, 0.5f, 0.5f,   0f,1f,0f,
            0.5f, 0.5f, 0.5f,   0f,1f,0f,
            0.5f, 0.5f,-0.5f,   0f,1f,0f,
            -0.5f, 0.5f, 0.5f,   0f,1f,0f,
            0.5f, 0.5f,-0.5f,   0f,1f,0f,
            -0.5f, 0.5f,-0.5f,   0f,1f,0f,

            // BOTTOM (0,-1,0)
            -0.5f,-0.5f,-0.5f,   0f,-1f,0f,
            0.5f,-0.5f,-0.5f,   0f,-1f,0f,
            0.5f,-0.5f, 0.5f,   0f,-1f,0f,
            -0.5f,-0.5f,-0.5f,   0f,-1f,0f,
            0.5f,-0.5f, 0.5f,   0f,-1f,0f,
            -0.5f,-0.5f, 0.5f,   0f,-1f,0f
    };

    private static final FloatBuffer buffer;
    static {
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        buffer = bb.asFloatBuffer();
        buffer.put(VERTICES);
        buffer.position(0);
    }

    // vertex count
    private static final int VERTEX_COUNT = VERTICES.length / FLOATS_PER_VERTEX;

    public float x, y, z;            // position
    public float size = 1f;         // uniform scale
    public float modelRotationX = 0f; // legacy rotation field used in your code

    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    public Cube(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }

    // ------------------------
    // Legacy draw() - thin glass style used around project
    // ------------------------
    public void draw(float[] vpMatrix, float[] colorRGBA) {
        if (ShaderHelper.program == -1) return;
        GLES20.glUseProgram(ShaderHelper.program);

        // Build model matrix
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        if (modelRotationX != 0f) {
            Matrix.rotateM(modelMatrix, 0, modelRotationX, 1f, 0f, 0f);
        }
        Matrix.scaleM(modelMatrix, 0, size, GLASS_THICKNESS, size);

        // compute mvp
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // set up attributes (position + normal)
        // position attribute: starts at buffer position 0 (floats -> bytes offset = 0)
        buffer.position(0);
        GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
        GLES20.glVertexAttribPointer(
                ShaderHelper.aPositionHandle,
                POSITION_SIZE,
                GLES20.GL_FLOAT,
                false,
                STRIDE,
                buffer
        );

        // normal attribute: offset by POSITION_SIZE floats
        if (ShaderHelper.aNormalHandle != -1) {
            buffer.position(POSITION_SIZE);
            GLES20.glEnableVertexAttribArray(ShaderHelper.aNormalHandle);
            GLES20.glVertexAttribPointer(
                    ShaderHelper.aNormalHandle,
                    NORMAL_SIZE,
                    GLES20.GL_FLOAT,
                    false,
                    STRIDE,
                    buffer
            );
        }

        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, colorRGBA, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
        if (ShaderHelper.aNormalHandle != -1) GLES20.glDisableVertexAttribArray(ShaderHelper.aNormalHandle);
    }

    // ------------------------
    // Legacy draw with rotation (used a lot)
    // ------------------------
    public void drawWithRotation(float[] vpMatrix, float[] colorRGBA) {
        if (ShaderHelper.program == -1) return;
        GLES20.glUseProgram(ShaderHelper.program);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, modelRotationX, 1f, 0f, 0f);
        Matrix.scaleM(modelMatrix, 0, size, GLASS_THICKNESS, size);

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        buffer.position(0);
        GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
        GLES20.glVertexAttribPointer(
                ShaderHelper.aPositionHandle,
                POSITION_SIZE,
                GLES20.GL_FLOAT, false,
                STRIDE, buffer
        );

        if (ShaderHelper.aNormalHandle != -1) {
            buffer.position(POSITION_SIZE);
            GLES20.glEnableVertexAttribArray(ShaderHelper.aNormalHandle);
            GLES20.glVertexAttribPointer(
                    ShaderHelper.aNormalHandle,
                    NORMAL_SIZE,
                    GLES20.GL_FLOAT, false,
                    STRIDE, buffer
            );
        }

        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, colorRGBA, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
        if (ShaderHelper.aNormalHandle != -1) GLES20.glDisableVertexAttribArray(ShaderHelper.aNormalHandle);
    }

    // ------------------------
    // Legacy drawWithModel: accept a full model matrix (many callers use this)
    // ------------------------
    public void drawWithModel(float[] vpMatrix, float[] modelMat, float[] colorRGBA) {
        if (ShaderHelper.program == -1) return;
        GLES20.glUseProgram(ShaderHelper.program);

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMat, 0);
        GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        buffer.position(0);
        GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
        GLES20.glVertexAttribPointer(
                ShaderHelper.aPositionHandle,
                POSITION_SIZE,
                GLES20.GL_FLOAT, false,
                STRIDE, buffer
        );

        if (ShaderHelper.aNormalHandle != -1) {
            buffer.position(POSITION_SIZE);
            GLES20.glEnableVertexAttribArray(ShaderHelper.aNormalHandle);
            GLES20.glVertexAttribPointer(
                    ShaderHelper.aNormalHandle,
                    NORMAL_SIZE,
                    GLES20.GL_FLOAT, false,
                    STRIDE, buffer
            );
        }

        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, colorRGBA, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
        if (ShaderHelper.aNormalHandle != -1) GLES20.glDisableVertexAttribArray(ShaderHelper.aNormalHandle);
    }

    // ------------------------
    // Legacy drawCustomScale: accept custom scale (x,y,z) relative to Cube position
    // ------------------------
    public void drawCustomScale(float[] vpMatrix, float[] colorRGBA, float scaleX, float scaleY, float scaleZ) {
        if (ShaderHelper.program == -1) return;
        GLES20.glUseProgram(ShaderHelper.program);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, scaleZ);

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        buffer.position(0);
        GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
        GLES20.glVertexAttribPointer(
                ShaderHelper.aPositionHandle,
                POSITION_SIZE,
                GLES20.GL_FLOAT, false,
                STRIDE, buffer
        );

        if (ShaderHelper.aNormalHandle != -1) {
            buffer.position(POSITION_SIZE);
            GLES20.glEnableVertexAttribArray(ShaderHelper.aNormalHandle);
            GLES20.glVertexAttribPointer(
                    ShaderHelper.aNormalHandle,
                    NORMAL_SIZE,
                    GLES20.GL_FLOAT, false,
                    STRIDE, buffer
            );
        }

        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, colorRGBA, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
        if (ShaderHelper.aNormalHandle != -1) GLES20.glDisableVertexAttribArray(ShaderHelper.aNormalHandle);
    }

    // ------------------------
    // NEW: drawTwoSided - front faces using outerColor, back half using innerColor
    //    (assumes the mesh is organized with first half of vertices as "front-facing" sides)
    // ------------------------
    public void drawTwoSided(float[] vpMatrix, float[] modelMat, float[] outerColor, float[] innerColor) {
        if (ShaderHelper.program == -1) return;
        GLES20.glUseProgram(ShaderHelper.program);

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMat, 0);
        GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvpMatrix, 0);

        buffer.position(0);
        GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
        GLES20.glVertexAttribPointer(
                ShaderHelper.aPositionHandle,
                POSITION_SIZE,
                GLES20.GL_FLOAT, false,
                STRIDE, buffer
        );

        if (ShaderHelper.aNormalHandle != -1) {
            buffer.position(POSITION_SIZE);
            GLES20.glEnableVertexAttribArray(ShaderHelper.aNormalHandle);
            GLES20.glVertexAttribPointer(
                    ShaderHelper.aNormalHandle,
                    NORMAL_SIZE,
                    GLES20.GL_FLOAT, false,
                    STRIDE, buffer
            );
        }

        int half = VERTEX_COUNT / 2;

        // Draw first half (outer)
        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, outerColor, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, half);

        // Draw second half (inner)
        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, innerColor, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, half, VERTEX_COUNT - half);

        GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
        if (ShaderHelper.aNormalHandle != -1) GLES20.glDisableVertexAttribArray(ShaderHelper.aNormalHandle);
    }
}

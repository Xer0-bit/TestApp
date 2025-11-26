package com.example.testapp;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Cube {

    private static final int COORDS_PER_VERTEX = 3;
    private static final int VERTEX_COUNT = 36; // 12 triangles * 3 vertices
    private static final float GLASS_THICKNESS = 0.05f;

    // 36 vertices (12 triangles) * 3 coords
    private static final float[] VERTICES = {
            // front
            -0.5f, -0.5f,  0.5f,   0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
            // right
            0.5f, -0.5f,  0.5f,   0.5f, -0.5f, -0.5f,   0.5f,  0.5f, -0.5f,
            0.5f, -0.5f,  0.5f,   0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f,
            // back
            0.5f, -0.5f, -0.5f,  -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,   0.5f,  0.5f, -0.5f,
            // left
            -0.5f, -0.5f, -0.5f,  -0.5f, -0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f,  0.5f,  -0.5f,  0.5f, -0.5f,
            // top
            -0.5f,  0.5f,  0.5f,   0.5f,  0.5f,  0.5f,   0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f,  0.5f,   0.5f,  0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,
            // bottom
            -0.5f, -0.5f, -0.5f,   0.5f, -0.5f, -0.5f,   0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,   0.5f, -0.5f,  0.5f,  -0.5f, -0.5f,  0.5f
    };

    private static final FloatBuffer vertexBuffer;

    static {
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);
    }

    public float x, y, z;
    public float size = 1f;
    public float modelRotationX = 0f;

    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    public Cube(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void draw(float[] vpMatrix, float[] colorRGBA) {
        if (ShaderHelper.program == -1) return;
        GLES20.glUseProgram(ShaderHelper.program);

        // Build model matrix
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.scaleM(modelMatrix, 0, size, GLASS_THICKNESS, size); // Thin glass-like

        // Compute MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        // Pass uniforms
        GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, colorRGBA, 0);

        // Set vertex attributes
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
        GLES20.glVertexAttribPointer(ShaderHelper.aPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VERTEX_COUNT);

        GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
    }

    public void drawWithRotation(float[] vpMatrix, float[] colorRGBA) {
        if (ShaderHelper.program == -1) return;
        GLES20.glUseProgram(ShaderHelper.program);

        // Build model matrix with rotation
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, modelRotationX, 1f, 0f, 0f);
        Matrix.scaleM(modelMatrix, 0, size, GLASS_THICKNESS, size);

        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, colorRGBA, 0);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
        GLES20.glVertexAttribPointer(ShaderHelper.aPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VERTEX_COUNT);
        GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
    }
}
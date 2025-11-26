package com.example.testapp;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

public class ParticleSystem {

    private static final float GRAVITY = 0.02f;
    private static final float PARTICLE_LIFETIME_STEP = 0.016f; // ~60fps
    private static final int COORDS_PER_VERTEX = 3;

    // Particle counts
    private static final int BREAK_PARTICLE_COUNT = 12;
    private static final int LAND_PARTICLE_COUNT = 8;

    // Colors
    private static final float[] BREAK_COLOR = {0.4f, 0.8f, 1f, 0.8f}; // Blue glass
    private static final float[] LAND_COLOR = {1f, 0.8f, 0.1f, 0.7f}; // Gold

    private static class Particle {
        float x, y, z;
        float vx, vy, vz;
        float life, maxLife;
        float[] color;
        float size;

        Particle(float x, float y, float z, float vx, float vy, float vz,
                 float life, float[] color, float size) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.life = life;
            this.maxLife = life;
            this.color = color.clone();
            this.size = size;
        }

        void update() {
            x += vx;
            y += vy;
            z += vz;
            vy -= GRAVITY;
            life -= PARTICLE_LIFETIME_STEP;
        }

        boolean isAlive() {
            return life > 0;
        }

        float getAlpha() {
            return life / maxLife;
        }
    }

    private ArrayList<Particle> particles = new ArrayList<>();
    private Random random = new Random();
    private float[] modelMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    // Simple cube for particles (more efficient than spheres)
    private static final float PARTICLE_SIZE = 0.1f;
    private static final float[] CUBE_VERTICES = {
            // Front face
            -PARTICLE_SIZE, -PARTICLE_SIZE,  PARTICLE_SIZE,
            PARTICLE_SIZE, -PARTICLE_SIZE,  PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            -PARTICLE_SIZE, -PARTICLE_SIZE,  PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            -PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            // Back face
            -PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            -PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            -PARTICLE_SIZE,  PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE, -PARTICLE_SIZE,
            // Top face
            -PARTICLE_SIZE,  PARTICLE_SIZE, -PARTICLE_SIZE,
            -PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            -PARTICLE_SIZE,  PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE, -PARTICLE_SIZE,
            // Bottom face
            -PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE, -PARTICLE_SIZE,  PARTICLE_SIZE,
            -PARTICLE_SIZE, -PARTICLE_SIZE,  PARTICLE_SIZE,
            -PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE, -PARTICLE_SIZE,  PARTICLE_SIZE,
            // Right face
            PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            PARTICLE_SIZE, -PARTICLE_SIZE,  PARTICLE_SIZE,
            PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE, -PARTICLE_SIZE,
            PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            // Left face
            -PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            -PARTICLE_SIZE, -PARTICLE_SIZE,  PARTICLE_SIZE,
            -PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            -PARTICLE_SIZE, -PARTICLE_SIZE, -PARTICLE_SIZE,
            -PARTICLE_SIZE,  PARTICLE_SIZE,  PARTICLE_SIZE,
            -PARTICLE_SIZE,  PARTICLE_SIZE, -PARTICLE_SIZE
    };

    private static FloatBuffer particleBuffer;

    static {
        ByteBuffer bb = ByteBuffer.allocateDirect(CUBE_VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        particleBuffer = bb.asFloatBuffer();
        particleBuffer.put(CUBE_VERTICES);
        particleBuffer.position(0);
    }

    public void spawnBreakEffect(float x, float y, float z) {
        for (int i = 0; i < BREAK_PARTICLE_COUNT; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float speed = random.nextFloat() * 0.15f + 0.05f;
            float vx = (float) Math.cos(angle) * speed;
            float vz = (float) Math.sin(angle) * speed;
            float vy = random.nextFloat() * 0.1f + 0.05f;
            particles.add(new Particle(x, y, z, vx, vy, vz, 1.2f, BREAK_COLOR, 0.08f));
        }
    }

    public void spawnLandEffect(float x, float y, float z) {
        for (int i = 0; i < LAND_PARTICLE_COUNT; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float speed = random.nextFloat() * 0.1f + 0.03f;
            float vx = (float) Math.cos(angle) * speed;
            float vz = (float) Math.sin(angle) * speed;
            float vy = random.nextFloat() * 0.05f + 0.02f;
            particles.add(new Particle(x, y, z, vx, vy, vz, 0.8f, LAND_COLOR, 0.06f));
        }
    }

    public void clear() {
        particles.clear();
    }

    public void update() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (!p.isAlive()) {
                particles.remove(i);
            }
        }
    }

    public void draw(float[] vpMatrix) {
        if (particles.isEmpty() || ShaderHelper.program == -1) return;

        GLES20.glUseProgram(ShaderHelper.program);

        for (Particle p : particles) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z);
            Matrix.scaleM(modelMatrix, 0, p.size, p.size, p.size);
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);

            float[] color = p.color.clone();
            color[3] *= p.getAlpha(); // Fade out over time

            GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvpMatrix, 0);
            GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, color, 0);

            particleBuffer.position(0);
            GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
            GLES20.glVertexAttribPointer(ShaderHelper.aPositionHandle, COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false, 0, particleBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, CUBE_VERTICES.length / COORDS_PER_VERTEX);
            GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
        }
    }
}
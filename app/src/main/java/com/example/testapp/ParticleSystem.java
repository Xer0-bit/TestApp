package com.example.testapp;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class ParticleSystem {

    private static class Particle {
        float x, y, z;
        float vx, vy, vz;
        float life, maxLife;
        float[] color;
        float size;

        Particle(float x, float y, float z, float vx, float vy, float vz, float life, float[] color, float size) {
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
            vy -= 0.02f; // gravity
            life -= 0.016f; // ~60fps
        }

        boolean isAlive() {
            return life > 0;
        }
    }

    private ArrayList<Particle> particles = new ArrayList<>();
    private float[] modelMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    private static final float[] SPHERE_VERTICES = generateSphere(0.1f, 6, 6);
    private static FloatBuffer sphereBuffer;

    static {
        ByteBuffer bb = ByteBuffer.allocateDirect(SPHERE_VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        sphereBuffer = bb.asFloatBuffer();
        sphereBuffer.put(SPHERE_VERTICES);
        sphereBuffer.position(0);
    }

    private static float[] generateSphere(float radius, int lats, int lons) {
        ArrayList<Float> vertices = new ArrayList<>();
        for (int i = 0; i <= lats; i++) {
            float lat = (float) (Math.PI * i / lats);
            float sinLat = (float) Math.sin(lat);
            float cosLat = (float) Math.cos(lat);

            for (int j = 0; j <= lons; j++) {
                float lon = (float) (2 * Math.PI * j / lons);
                float sinLon = (float) Math.sin(lon);
                float cosLon = (float) Math.cos(lon);

                vertices.add(radius * sinLat * cosLon);
                vertices.add(radius * cosLat);
                vertices.add(radius * sinLat * sinLon);
            }
        }

        float[] result = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            result[i] = vertices.get(i);
        }
        return result;
    }

    public void spawnBreakEffect(float x, float y, float z) {
        float[] color = {0.4f, 0.8f, 1f, 0.8f};
        for (int i = 0; i < 12; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float speed = (float) (Math.random() * 0.15f + 0.05f);
            float vx = (float) Math.cos(angle) * speed;
            float vz = (float) Math.sin(angle) * speed;
            float vy = (float) (Math.random() * 0.1f + 0.05f);
            particles.add(new Particle(x, y, z, vx, vy, vz, 1.2f, color, 0.08f));
        }
    }

    public void spawnLandEffect(float x, float y, float z) {
        float[] color = {1f, 0.8f, 0.1f, 0.7f};
        for (int i = 0; i < 8; i++) {
            float angle = (float) (Math.random() * Math.PI * 2);
            float speed = (float) (Math.random() * 0.1f + 0.03f);
            float vx = (float) Math.cos(angle) * speed;
            float vz = (float) Math.sin(angle) * speed;
            float vy = (float) (Math.random() * 0.05f + 0.02f);
            particles.add(new Particle(x, y, z, vx, vy, vz, 0.8f, color, 0.06f));
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
            color[3] *= (p.life / p.maxLife); // fade out

            GLES20.glUniformMatrix4fv(ShaderHelper.uMVPMatrixHandle, 1, false, mvpMatrix, 0);
            GLES20.glUniform4fv(ShaderHelper.uColorHandle, 1, color, 0);

            sphereBuffer.position(0);
            GLES20.glEnableVertexAttribArray(ShaderHelper.aPositionHandle);
            GLES20.glVertexAttribPointer(ShaderHelper.aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, sphereBuffer);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, SPHERE_VERTICES.length / 3);
            GLES20.glDisableVertexAttribArray(ShaderHelper.aPositionHandle);
        }
    }
}
package com.example.testapp;

import java.util.ArrayList;
import java.util.List;

public class PlatformGlass {

    private int index;
    private boolean leftIsCorrect;

    // cubes representing the intact platform
    private final Cube leftCube;
    private final Cube rightCube;

    // broken pieces (spawned only when broken)
    private boolean broken = false;
    private final List<Fragment> fragments = new ArrayList<>();

    public PlatformGlass(int idx, boolean correctLeft) {
        index = idx;
        leftIsCorrect = correctLeft;

        float y = getY();
        leftCube = new Cube(-1.5f, y, -12f);
        rightCube = new Cube(1.5f, y, -12f);
        leftCube.size = 1.5f;
        rightCube.size = 1.5f;
    }

    public boolean isCorrect(boolean left) {
        return left == leftIsCorrect;
    }

    public float getX(boolean left) {
        return left ? -1.5f : 1.5f;
    }

    public float getY() {
        return index * 1.3f + 1.0f;
    }

    public void draw(float[] vpMatrix) {
        if (!broken) {
            float[] glassColor = {0.4f, 0.8f, 1f, 0.45f};
            leftCube.draw(vpMatrix, glassColor);
            rightCube.draw(vpMatrix, glassColor);
        } else {
            // draw fragments
            for (Fragment f : fragments) f.draw(vpMatrix);
        }
    }

    public void breakPlatform() {
        if (broken) return;
        broken = true;
        // spawn several small fragments from both sides
        spawnFragmentsAt(leftCube.x, leftCube.y, leftCube.z);
        spawnFragmentsAt(rightCube.x, rightCube.y, rightCube.z);
    }

    private void spawnFragmentsAt(float cx, float cy, float cz) {
        int pieces = 6;
        for (int i = 0; i < pieces; i++) {
            float ox = (i - pieces/2) * 0.12f;
            float oz = (float)(Math.random() * 0.2f - 0.1f);
            Fragment f = new Fragment(cx + ox, cy + 0.02f, cz + oz);
            // give random velocity
            f.vx = (float)(Math.random() * 0.06f - 0.03f);
            f.vy = (float)(Math.random() * 0.06f + 0.02f);
            f.vz = (float)(Math.random() * 0.06f - 0.03f);
            fragments.add(f);
        }
    }

    public void update() {
        if (!broken) return;
        for (Fragment f : fragments) f.update();
    }

    // small fragment class that falls and rotates
    private static class Fragment {
        float x,y,z;
        float vx,vy,vz;
        float angle = 0f;
        Cube cube;

        Fragment(float x, float y, float z) {
            this.x = x; this.y = y; this.z = z;
            cube = new Cube(x,y,z);
            cube.size = 0.25f;
        }

        void update() {
            vy -= 0.0025f; // gravity
            x += vx;
            y += vy;
            z += vz;
            angle += 4f;
            cube.x = x;
            cube.y = y;
            cube.z = z;
        }

        void draw(float[] vpMatrix) {
            float[] color = {0.6f, 0.9f, 1f, 1f};
            cube.draw(vpMatrix, color);
        }
    }
}

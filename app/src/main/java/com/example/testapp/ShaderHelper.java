package com.example.testapp;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderHelper {
    private static final String TAG = "ShaderHelper";

    // ============================================================
    // NEW SHADERS — supports normals and soft lighting
    // ============================================================

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec3 aPosition;\n" +
                    "attribute vec3 aNormal;\n" +
                    "varying vec3 vNormal;\n" +
                    "void main() {\n" +
                    "    vNormal = aNormal;\n" +
                    "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "uniform vec4 uColor;\n" +
                    "varying vec3 vNormal;\n" +
                    "void main() {\n" +
                    "    // Soft directional light from above/front\n" +
                    "    vec3 lightDir = normalize(vec3(0.2, 0.7, 1.0));\n" +
                    "    float light = dot(normalize(vNormal), lightDir);\n" +
                    "    light = clamp(light * 0.5 + 0.5, 0.0, 1.0);\n" +
                    "    gl_FragColor = vec4(uColor.rgb * light, uColor.a);\n" +
                    "}\n";

    // ============================================================

    public static int program = -1;

    // Vertex attrs
    public static int aPositionHandle = -1;
    public static int aNormalHandle = -1;

    // Uniforms
    public static int uMVPMatrixHandle = -1;
    public static int uColorHandle = -1;

    // ============================================================

    public static void init() {
        if (program != -1) return; // already created

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vs == 0 || fs == 0) {
            Log.e(TAG, "Failed to load shaders");
            return;
        }

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking program: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = -1;
            return;
        }

        // ---- GET ALL HANDLES ----
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        aNormalHandle = GLES20.glGetAttribLocation(program, "aNormal");
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        uColorHandle = GLES20.glGetUniformLocation(program, "uColor");

        // Clean up after linking
        GLES20.glDeleteShader(vs);
        GLES20.glDeleteShader(fs);
    }

    public static void release() {
        if (program != -1) {
            GLES20.glDeleteProgram(program);
            program = -1;
            aPositionHandle = -1;
            aNormalHandle = -1;
            uMVPMatrixHandle = -1;
            uColorHandle = -1;
        }
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            Log.e(TAG, "Shader compile error: " +
                    GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}

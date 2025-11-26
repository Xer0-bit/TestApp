package com.example.testapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class GameSurfaceView extends GLSurfaceView {

    private final GameRenderer renderer;

    public GameSurfaceView(Context context) {
        this(context, null);
    }

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        renderer = new GameRenderer(context);
        setRenderer(renderer);
        setPreserveEGLContextOnPause(true);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public GameRenderer getRenderer() {
        return renderer;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            GameLogic logic = renderer.getLogic();
            if (logic == null) return true;

            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                return true;
            }

            if (!logic.isPlaying()) {
                return true;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_A:
                    logic.jumpRight();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_D:
                    logic.jumpLeft();
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            GameLogic logic = renderer.getLogic();
            if (logic == null) return true;
            if (!logic.isPlaying()) return true;

            // Only process ACTION_DOWN to prevent lag from repeated events
            int action = event.getActionMasked();
            if (action != MotionEvent.ACTION_DOWN) {
                return true;
            }

            final float x = event.getX();
            final float w = getWidth();

            // Queue to GL thread to ensure thread safety with OpenGL state
            queueEvent(() -> {
                if (logic.isPlaying()) {
                    if (x < w / 2f) {
                        logic.jumpRight();
                    } else {
                        logic.jumpLeft();
                    }
                }
            });

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
}
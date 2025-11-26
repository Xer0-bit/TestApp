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

            // Escape key is handled by MainActivity
            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                return true;
            }

            // Only allow gameplay input during PLAYING state
            if (!logic.isPlaying()) {
                return true;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_A:
                    logic.jumpRight();  // Left key jumps RIGHT (flipped for user perspective)
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_D:
                    logic.jumpLeft();  // Right key jumps LEFT (flipped for user perspective)
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
            if (event.getAction() != MotionEvent.ACTION_DOWN) {
                return true;
            }

            GameLogic logic = renderer.getLogic();
            if (logic == null) return true;

            // Only process touch during PLAYING state
            if (!logic.isPlaying()) {
                return true;
            }

            float x = event.getX();
            float w = getWidth();

            // Left side of screen = jump RIGHT, Right side = jump LEFT (flipped for user perspective)
            if (x < w / 2f) {
                logic.jumpRight();
            } else {
                logic.jumpLeft();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
}
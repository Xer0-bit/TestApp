package com.example.testapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class GameSurfaceView extends GLSurfaceView {

    private final GameRenderer renderer;

    public GameSurfaceView(Context context) { this(context, null); }

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        renderer = new GameRenderer(context);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public GameRenderer getRenderer() { return renderer; }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        GameLogic logic = renderer.getLogic();

        // Only allow escape to work in any state, other keys only work during gameplay
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            return true; // let MainActivity handle it
        }

        // Block all other input if not playing
        if (!logic.isPlaying()) {
            return true; // consume input but don't use it
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                logic.jumpRight(); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                logic.jumpLeft(); return true;
            default: return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        GameLogic logic = renderer.getLogic();

        // Only process touch input during gameplay
        if (!logic.isPlaying()) {
            return true; // consume but ignore
        }

        float x = event.getX();
        float w = getWidth();
        if (x < w / 2f) logic.jumpRight();
        else logic.jumpLeft();
        return true;
    }
}
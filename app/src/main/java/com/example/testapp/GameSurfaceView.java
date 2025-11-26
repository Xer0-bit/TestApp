package com.example.testapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class GameSurfaceView extends GLSurfaceView {

    private final GameRenderer renderer;
    private long lastTouchInputTime = 0;
    private static final long TOUCH_DEBOUNCE_MS = 200;

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
            if (logic == null || !logic.isPlaying()) {
                return keyCode == KeyEvent.KEYCODE_ESCAPE;
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
                case KeyEvent.KEYCODE_ESCAPE:
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
            if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
                return true;
            }

            GameLogic logic = renderer.getLogic();

            // ONLY process touch if game is actively playing
            if (logic == null || !logic.isPlaying()) {
                return false; // Don't consume - let UI handle it
            }

            // Debounce touch input on UI thread BEFORE queuing
            long now = System.currentTimeMillis();
            if (now - lastTouchInputTime < TOUCH_DEBOUNCE_MS) {
                return true; // Ignore rapid taps
            }
            lastTouchInputTime = now;

            float x = event.getX();
            float w = getWidth();

            // Queue the jump on GL thread
            queueEvent(() -> {
                if (x < w / 2f) {
                    logic.jumpRight();
                } else {
                    logic.jumpLeft();
                }
            });

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
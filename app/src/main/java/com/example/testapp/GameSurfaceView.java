package com.example.testapp;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.os.SystemClock;

public class GameSurfaceView extends GLSurfaceView {

    private final GameRenderer renderer;
    private long lastTouchTime = 0;

    public GameSurfaceView(Context context) {
        this(context, null);
    }

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        renderer = new GameRenderer(context);
        setRenderer(renderer);
        // Prevent expensive UI event throttling on some devices
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
            long now = SystemClock.uptimeMillis();

            Log.d("DBG_TOUCH",
                    "ACTION=" + event.getActionMasked() +
                            "  time=" + now +
                            "  thread=" + Thread.currentThread().getName()
            );

            GameLogic logic = renderer.getLogic();
            if (logic == null) return true;
            if (!logic.isPlaying()) return true;

            if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
                return true;
            }

            final float x = event.getX();
            final float w = getWidth();

            // Capture timestamp for GL-thread delta
            final long inputTimestamp = now;

            queueEvent(() -> {
                Log.d("DBG_INPUT_GL",
                        "GL thread got input at " + SystemClock.uptimeMillis() +
                                "  delta=" + (SystemClock.uptimeMillis() - inputTimestamp) +
                                "ms  thread=" + Thread.currentThread().getName()
                );

                if (x < w / 2f) {
                    logic.jumpRight();
                } else {
                    logic.jumpLeft();
                }
            });

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
}
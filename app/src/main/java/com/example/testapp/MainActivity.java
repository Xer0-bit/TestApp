package com.example.testapp;

import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GameSurfaceView gameView;
    private Handler uiHandler = new Handler();
    private TextView tvTimer;
    private Runnable tickRunnable;

    private LinearLayout mainMenu, pauseMenu;
    private Button btnStartGame, btnResume, btnRestartPause, btnReturnMenu;
    private GameLogic logic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);
        tvTimer = findViewById(R.id.tvTimer);

        mainMenu = findViewById(R.id.mainMenu);
        pauseMenu = findViewById(R.id.pauseMenu);

        btnStartGame = findViewById(R.id.btnStartGame);
        btnResume = findViewById(R.id.btnResume);
        btnRestartPause = findViewById(R.id.btnRestartPause);
        btnReturnMenu = findViewById(R.id.btnReturnMenu);

        logic = gameView.getRenderer().getLogic();

        btnStartGame.setOnClickListener(v -> {
            logic.start();
            mainMenu.setVisibility(View.GONE);
        });

        btnResume.setOnClickListener(v -> {
            logic.resume();
            pauseMenu.setVisibility(View.GONE);
        });

        btnRestartPause.setOnClickListener(v -> {
            logic.restart();
            pauseMenu.setVisibility(View.GONE);
        });

        btnReturnMenu.setOnClickListener(v -> {
            logic.resetGame();
            pauseMenu.setVisibility(View.GONE);
            mainMenu.setVisibility(View.VISIBLE);
        });

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimerUI();
                uiHandler.postDelayed(this, 100);
            }
        };
        uiHandler.post(tickRunnable);
    }

    private void updateTimerUI() {
        if (logic == null) return;
        double elapsed = logic.getElapsedSeconds();
        tvTimer.setText(String.format("Time Survived: %.2fs", elapsed));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (logic == null) return super.onKeyDown(keyCode, event);

        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (logic.isRunning()) {
                logic.pause();
                pauseMenu.setVisibility(View.VISIBLE);
            } else {
                logic.resume();
                pauseMenu.setVisibility(View.GONE);
            }
            return true;
        }

        if (!logic.isRunning()) return true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A: logic.jumpLeft(); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D: logic.jumpRight(); return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onPause();
        if (logic.isRunning()) logic.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResume();
        if (!logic.isRunning() && !mainMenu.isShown() && !pauseMenu.isShown()) logic.resume();
    }
}

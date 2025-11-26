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

    private LinearLayout mainMenu, pauseMenu, winMenu;
    private Button btnStartGame, btnResume, btnRestartPause, btnReturnMenu, btnRestartWin, btnReturnMenuWin;
    private GameLogic logic;

    private boolean winMenuShown = false;
    private boolean isActivityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);
        tvTimer = findViewById(R.id.tvTimer);

        mainMenu = findViewById(R.id.mainMenu);
        pauseMenu = findViewById(R.id.pauseMenu);
        winMenu = findViewById(R.id.winMenu);

        btnStartGame = findViewById(R.id.btnStartGame);
        btnResume = findViewById(R.id.btnResume);
        btnRestartPause = findViewById(R.id.btnRestartPause);
        btnReturnMenu = findViewById(R.id.btnReturnMenu);
        btnRestartWin = findViewById(R.id.btnRestartWin);
        btnReturnMenuWin = findViewById(R.id.btnReturnMenuWin);

        logic = gameView.getRenderer().getLogic();

        if (logic == null) {
            return; // Emergency exit if logic failed to initialize
        }

        btnStartGame.setOnClickListener(v -> {
            if (logic == null || isActivityDestroyed) return;
            mainMenu.setVisibility(View.GONE);
            winMenuShown = false;
            logic.startGame();
        });

        btnResume.setOnClickListener(v -> {
            if (logic == null || isActivityDestroyed) return;
            logic.resumeGame();
            pauseMenu.setVisibility(View.GONE);
        });

        btnRestartPause.setOnClickListener(v -> {
            if (logic == null || isActivityDestroyed) return;
            pauseMenu.setVisibility(View.GONE);
            winMenuShown = false;
            logic.restartGame();
        });

        btnReturnMenu.setOnClickListener(v -> {
            if (logic == null || isActivityDestroyed) return;
            logic.returnToMenu();
            pauseMenu.setVisibility(View.GONE);
            mainMenu.setVisibility(View.VISIBLE);
            winMenuShown = false;
        });

        btnRestartWin.setOnClickListener(v -> {
            if (logic == null || isActivityDestroyed) return;
            winMenu.setVisibility(View.GONE);
            winMenuShown = false;
            logic.restartGame();
        });

        btnReturnMenuWin.setOnClickListener(v -> {
            if (logic == null || isActivityDestroyed) return;
            winMenu.setVisibility(View.GONE);
            winMenuShown = false;
            logic.returnToMenu();
            mainMenu.setVisibility(View.VISIBLE);
        });

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isActivityDestroyed) {
                    updateTimerUI();
                    uiHandler.postDelayed(this, 100);
                }
            }
        };
        uiHandler.post(tickRunnable);
    }

    private void updateTimerUI() {
        if (logic == null || isActivityDestroyed) return;

        try {
            double elapsed = logic.getElapsedSeconds();
            tvTimer.setText(String.format("Time: %.2fs", elapsed));

            // Show win menu only once
            if (logic.isGameWon() && !winMenuShown) {
                winMenuShown = true;
                winMenu.setVisibility(View.VISIBLE);
                double bestTime = logic.getBestTime();
                TextView tvWinTime = findViewById(R.id.tvWinTime);
                TextView tvBestTimeWin = findViewById(R.id.tvBestTimeWin);
                if (tvWinTime != null && tvBestTimeWin != null) {
                    tvWinTime.setText(String.format("You Won!\nTime: %.2fs", elapsed));
                    tvBestTimeWin.setText(String.format("Best Time: %.2fs", bestTime));
                }
            }

            // Hide win menu if player returns to menu
            if (!logic.isGameWon() && winMenuShown) {
                winMenuShown = false;
                winMenu.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            // Prevent crashes from UI updates
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (logic == null || isActivityDestroyed) return super.onKeyDown(keyCode, event);

        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (logic.isPlaying()) {
                logic.pauseGame();
                pauseMenu.setVisibility(View.VISIBLE);
                return true;
            } else if (logic.getGameState() == GameLogic.GameState.PAUSED) {
                logic.resumeGame();
                pauseMenu.setVisibility(View.GONE);
                return true;
            }
            // Don't intercept escape in menu or win states - let system handle it
            return false;
        }

        if (!logic.isPlaying()) return true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                logic.jumpLeft();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                logic.jumpRight();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.onPause();
        }
        if (logic != null && logic.isPlaying()) {
            logic.pauseGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityDestroyed = false;
        if (gameView != null) {
            gameView.onResume();
        }
        // Don't auto-resume - let player click resume button
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityDestroyed = true;
        if (uiHandler != null && tickRunnable != null) {
            uiHandler.removeCallbacks(tickRunnable);
        }
        if (gameView != null) {
            gameView.onPause();
        }
    }
}
package com.example.testapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final long UI_UPDATE_INTERVAL_MS = 50; // Faster updates for smoother timer

    private GameSurfaceView gameView;
    private Handler uiHandler;
    private TextView tvTimer, tvBestTime;
    private Runnable tickRunnable;

    private LinearLayout mainMenu, pauseMenu, winMenu;
    private Button btnStartGame, btnResume, btnRestartPause, btnReturnMenu,
            btnRestartWin, btnReturnMenuWin;
    private GameLogic logic;

    private boolean winMenuShown = false;
    private boolean isActivityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeGameLogic();
        setupButtonListeners();
        startUIUpdateLoop();
    }

    private void initializeViews() {
        gameView = findViewById(R.id.gameView);
        tvTimer = findViewById(R.id.tvTimer);
        tvBestTime = findViewById(R.id.tvBestTime);

        mainMenu = findViewById(R.id.mainMenu);
        pauseMenu = findViewById(R.id.pauseMenu);
        winMenu = findViewById(R.id.winMenu);

        btnStartGame = findViewById(R.id.btnStartGame);
        btnResume = findViewById(R.id.btnResume);
        btnRestartPause = findViewById(R.id.btnRestartPause);
        btnReturnMenu = findViewById(R.id.btnReturnMenu);
        btnRestartWin = findViewById(R.id.btnRestartWin);
        btnReturnMenuWin = findViewById(R.id.btnReturnMenuWin);

        uiHandler = new Handler(Looper.getMainLooper());
    }

    private void initializeGameLogic() {
        if (gameView == null || gameView.getRenderer() == null) {
            throw new IllegalStateException("GameView or Renderer failed to initialize");
        }

        logic = gameView.getRenderer().getLogic();

        if (logic == null) {
            throw new IllegalStateException("GameLogic failed to initialize");
        }
    }

    private void setupButtonListeners() {
        btnStartGame.setOnClickListener(v -> {
            if (isSafeToExecute()) {
                mainMenu.setVisibility(View.GONE);
                winMenuShown = false;
                logic.startGame();
            }
        });

        btnResume.setOnClickListener(v -> {
            if (isSafeToExecute()) {
                logic.resumeGame();
                pauseMenu.setVisibility(View.GONE);
            }
        });

        btnRestartPause.setOnClickListener(v -> {
            if (isSafeToExecute()) {
                pauseMenu.setVisibility(View.GONE);
                winMenuShown = false;
                logic.restartGame();
            }
        });

        btnReturnMenu.setOnClickListener(v -> {
            if (isSafeToExecute()) {
                logic.returnToMenu();
                pauseMenu.setVisibility(View.GONE);
                mainMenu.setVisibility(View.VISIBLE);
                winMenuShown = false;
            }
        });

        btnRestartWin.setOnClickListener(v -> {
            if (isSafeToExecute()) {
                winMenu.setVisibility(View.GONE);
                winMenuShown = false;
                logic.restartGame();
            }
        });

        btnReturnMenuWin.setOnClickListener(v -> {
            if (isSafeToExecute()) {
                winMenu.setVisibility(View.GONE);
                winMenuShown = false;
                logic.returnToMenu();
                mainMenu.setVisibility(View.VISIBLE);
            }
        });
    }

    private void startUIUpdateLoop() {
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isActivityDestroyed) {
                    updateTimerUI();
                    uiHandler.postDelayed(this, UI_UPDATE_INTERVAL_MS);
                }
            }
        };
        uiHandler.post(tickRunnable);
    }

    private boolean isSafeToExecute() {
        return logic != null && !isActivityDestroyed;
    }

    private void updateTimerUI() {
        if (!isSafeToExecute()) return;

        try {
            double elapsed = logic.getElapsedSeconds();
            tvTimer.setText(String.format("Time: %.2fs", elapsed));

            // Update best time display
            double bestTime = logic.getBestTime();
            if (bestTime > 0) {
                tvBestTime.setVisibility(View.VISIBLE);
                tvBestTime.setText(String.format("Best: %.2fs", bestTime));
            }

            // Show win menu only once
            if (logic.isGameWon() && !winMenuShown) {
                winMenuShown = true;
                winMenu.setVisibility(View.VISIBLE);
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
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isSafeToExecute()) return super.onKeyDown(keyCode, event);

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
            return false;
        }

        if (!logic.isPlaying()) return true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                logic.jumpRight(); // Left key jumps RIGHT (flipped for user perspective)
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                logic.jumpLeft(); // Right key jumps LEFT (flipped for user perspective)
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

        if (logic != null) {
            logic.cleanup();
        }

        if (gameView != null && gameView.getRenderer() != null) {
            gameView.getRenderer().release();
        }
    }
}
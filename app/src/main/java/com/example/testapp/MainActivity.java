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
    private TextView tvLevel;
    private TextView tvMemoryPhase;
    private Runnable tickRunnable;

    private LinearLayout mainMenu, pauseMenu, winMenu;
    private Button btnStartGame, btnResume, btnRestartPause, btnReturnMenu, btnNextLevel, btnReturnMenuWin;
    private GameLogic logic;

    private boolean winMenuShown = false;
    private boolean isActivityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);
        tvLevel = findViewById(R.id.tvLevel);
        tvMemoryPhase = findViewById(R.id.tvMemoryPhase);

        mainMenu = findViewById(R.id.mainMenu);
        pauseMenu = findViewById(R.id.pauseMenu);
        winMenu = findViewById(R.id.winMenu);

        btnStartGame = findViewById(R.id.btnStartGame);
        btnResume = findViewById(R.id.btnResume);
        btnRestartPause = findViewById(R.id.btnRestartPause);
        btnReturnMenu = findViewById(R.id.btnReturnMenu);
        btnNextLevel = findViewById(R.id.btnNextLevel);
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
            logic.restartCurrentLevel();
        });

        btnReturnMenu.setOnClickListener(v -> {
            if (logic == null || isActivityDestroyed) return;
            logic.returnToMenu();
            pauseMenu.setVisibility(View.GONE);
            mainMenu.setVisibility(View.VISIBLE);
            winMenuShown = false;
        });

        btnNextLevel.setOnClickListener(v -> {
            if (logic == null || isActivityDestroyed) return;
            winMenu.setVisibility(View.GONE);
            winMenuShown = false;
            logic.restartCurrentLevel();
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
                    updateUI();
                    uiHandler.postDelayed(this, 100);
                }
            }
        };
        uiHandler.post(tickRunnable);
    }

    private void updateUI() {
        if (logic == null || isActivityDestroyed) return;

        try {
            // Handle memory phase display
            if (logic.isInMemoryPhase()) {
                tvLevel.setVisibility(View.GONE);
                tvMemoryPhase.setVisibility(View.VISIBLE);

                long remainingMs = logic.getMemoryPhaseRemainingMs();
                double remainingSec = remainingMs / 1000.0;

                if (remainingMs > 0) {
                    tvMemoryPhase.setText(String.format("Level %d\nMemorize the path!\n%.1fs",
                            logic.getCurrentLevel(), remainingSec));
                } else {
                    tvMemoryPhase.setText("Get ready...");
                }
            } else {
                tvMemoryPhase.setVisibility(View.GONE);
                tvLevel.setVisibility(View.VISIBLE);

                tvLevel.setText(String.format("Level %d", logic.getCurrentLevel()));
            }

            // Show win menu only once
            if (logic.isGameWon() && !winMenuShown) {
                winMenuShown = true;
                winMenu.setVisibility(View.VISIBLE);

                int currentLevel = logic.getCurrentLevel();
                int highestLevel = logic.getHighestLevelReached();

                TextView tvLevelComplete = findViewById(R.id.tvLevelComplete);
                TextView tvHighestLevel = findViewById(R.id.tvHighestLevel);
                TextView tvLevelStats = findViewById(R.id.tvLevelStats);

                if (tvLevelComplete != null && tvHighestLevel != null && tvLevelStats != null) {
                    tvLevelComplete.setText(String.format("Level %d Complete!", currentLevel - 1));
                    tvHighestLevel.setText(String.format("Highest Level: %d", highestLevel));

                    // Show level stats
                    int platforms = logic.getCurrentPlatformCount();
                    float memoryTime = logic.getCurrentMemoryTimeSeconds();
                    tvLevelStats.setText(String.format("Next: %d platforms, %.1fs memory",
                            platforms, memoryTime));
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
        // Release OpenGL resources
        if (gameView != null && gameView.getRenderer() != null) {
            gameView.getRenderer().release();
        }
    }
}
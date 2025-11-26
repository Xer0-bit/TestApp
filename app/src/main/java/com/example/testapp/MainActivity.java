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

        btnStartGame.setOnClickListener(v -> {
            logic.startGame();
            mainMenu.setVisibility(View.GONE);
        });

        btnResume.setOnClickListener(v -> {
            logic.resumeGame();
            pauseMenu.setVisibility(View.GONE);
        });

        btnRestartPause.setOnClickListener(v -> {
            logic.startGame();
            pauseMenu.setVisibility(View.GONE);
        });

        btnReturnMenu.setOnClickListener(v -> {
            logic.returnToMenu();
            pauseMenu.setVisibility(View.GONE);
            mainMenu.setVisibility(View.VISIBLE);
        });

        btnRestartWin.setOnClickListener(v -> {
            logic.startGame();
            winMenu.setVisibility(View.GONE);
            tvTimer.setText("Time: 0.00s");
        });

        btnReturnMenuWin.setOnClickListener(v -> {
            logic.returnToMenu();
            winMenu.setVisibility(View.GONE);
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
        tvTimer.setText(String.format("Time: %.2fs", elapsed));

        // Check if game was won and win menu is not already showing
        if (logic.isGameWon() && winMenu.getVisibility() != View.VISIBLE) {
            winMenu.setVisibility(View.VISIBLE);
            double currentTime = logic.getElapsedSeconds();
            double bestTime = logic.getBestTime();
            TextView tvWinTime = findViewById(R.id.tvWinTime);
            TextView tvBestTimeWin = findViewById(R.id.tvBestTimeWin);
            tvWinTime.setText(String.format("You Won!\nTime: %.2fs", currentTime));
            tvBestTimeWin.setText(String.format("Best Time: %.2fs", bestTime));
        }

        // Hide win menu if game is no longer won (restarted)
        if (!logic.isGameWon() && winMenu.getVisibility() == View.VISIBLE) {
            winMenu.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (logic == null) return super.onKeyDown(keyCode, event);

        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (logic.isPlaying()) {
                logic.pauseGame();
                pauseMenu.setVisibility(View.VISIBLE);
            } else if (logic.getGameState() == GameLogic.GameState.PAUSED) {
                logic.resumeGame();
                pauseMenu.setVisibility(View.GONE);
            }
            return true;
        }

        if (!logic.isPlaying()) return true;

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
        if (logic.isPlaying()) logic.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResume();
    }
}
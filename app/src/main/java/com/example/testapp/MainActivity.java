package com.example.testapp;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GameSurfaceView gameView;
    private Handler uiHandler = new Handler();
    private TextView tvTimer, tvBest;
    private Runnable tickRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnPause = findViewById(R.id.btnPause);
        Button btnRestart = findViewById(R.id.btnRestart);
        tvTimer = findViewById(R.id.tvTimer);
        tvBest = findViewById(R.id.tvBest);

        btnStart.setOnClickListener(v -> {
            gameView.getRenderer().logic.start();
        });

        btnPause.setOnClickListener(v -> {
            if (gameView.getRenderer().logic.isRunning()) gameView.getRenderer().logic.pause();
            else gameView.getRenderer().logic.resume();
        });

        btnRestart.setOnClickListener(v -> {
            gameView.getRenderer().logic.restart();
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
        GameLogic logic = gameView.getRenderer().logic;
        if (logic == null) return;

        double elapsed = logic.getElapsedSeconds();
        if (logic.isRunning()) {
            tvTimer.setText(String.format("Time: %.3fs", elapsed));
        } else {
            tvTimer.setText(String.format("Time: %.3fs", elapsed));
        }

        long bestMs = logic.getBestTimeMs();
        if (bestMs == Long.MAX_VALUE) tvBest.setText("Best: --");
        else tvBest.setText(String.format("Best: %.3fs", bestMs / 1000.0));
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onPause();
        gameView.getRenderer().logic.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResume();
    }
}

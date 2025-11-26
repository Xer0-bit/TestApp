package com.example.testapp;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GameSurfaceView gameView;
    private Handler uiHandler = new Handler();
    private TextView tvTimer;
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

        btnStart.setOnClickListener(v -> gameView.getRenderer().getLogic().start());
        btnPause.setOnClickListener(v -> {
            GameLogic logic = gameView.getRenderer().getLogic();
            if (logic.isRunning()) logic.pause();
            else logic.resume();
        });
        btnRestart.setOnClickListener(v -> gameView.getRenderer().getLogic().restart());

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
        GameLogic logic = gameView.getRenderer().getLogic();
        if (logic == null) return;

        double elapsed = logic.getElapsedSeconds();
        tvTimer.setText(String.format("Time Survived: %.2fs", elapsed));
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.onPause();
        gameView.getRenderer().getLogic().pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onResume();
        gameView.getRenderer().getLogic().resume();
    }
}

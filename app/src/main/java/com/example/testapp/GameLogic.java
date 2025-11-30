package com.example.testapp;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Random;

public class GameLogic {

    // Constants
    private static final float PLATFORM_Y = 1.0f;
    private static final float PLATFORM_Z_SPACING = 5f;
    private static final long INPUT_DELAY_MS = 150;
    private static final float JUMP_LAND_SHAKE = 0.15f;
    private static final float GLASS_BREAK_SHAKE = 0.25f;
    private static final float SHAKE_DECAY_RATE = 3.0f;
    private static final long RESPAWN_DELAY_MS = 400;
    private static final float MAX_DELTA_TIME = 0.05f;

    // Level system constants
    private static final int STARTING_PLATFORMS = 3;
    private static final int MAX_PLATFORMS = 10;
    private static final long BASE_MEMORY_TIME_MS = 3000;
    private static final long MIN_MEMORY_TIME_MS = 500;
    private static final long MEMORY_FADE_DURATION_MS = 500;

    // Platform tiers - adds platform every 10 levels after level 50
    private static final int PLATFORMS_AT_LEVEL_1_50 = 5;
    private static final int LEVELS_PER_PLATFORM_INCREASE = 10;

    private boolean hasStartedTimer = false;

    public enum GameState {
        MENU, MEMORY_PHASE, PLAYING, PAUSED, WON, LEVEL_TRANSITION
    }

    // Level configuration
    private static class LevelConfig {
        int totalPlatforms;
        long memoryDisplayDuration;

        LevelConfig(int platforms, long memoryTime) {
            this.totalPlatforms = platforms;
            this.memoryDisplayDuration = memoryTime;
        }
    }

    public PlatformGlass[] platforms;
    public Player player;

    private int nextPlatform = 0;
    private Random random = new Random();

    private GameState state = GameState.MENU;
    private long gameStartTime = 0;
    private long pauseStartTime = 0;
    private long totalPausedTime = 0;

    // Level system
    private int currentLevel = 1;
    private int highestLevelReached = 1;
    private LevelConfig currentConfig;

    // Memory phase tracking
    private long memoryPhaseStartTime = 0;
    private boolean memoryPhaseComplete = false;

    private float shakeAmount = 0f;
    private long lastJumpTime = 0;
    private long lastFrameTime = 0;
    private boolean isRespawning = false;
    private volatile boolean isActive = true;

    // Scheduled events system
    private static class ScheduledEvent {
        long executeAt;
        Runnable action;
        ScheduledEvent(long executeAt, Runnable action) {
            this.executeAt = executeAt;
            this.action = action;
        }
    }

    private final ArrayList<ScheduledEvent> scheduledEvents = new ArrayList<>();

    public GameLogic() {
        currentConfig = getLevelConfig(currentLevel);
        initializeGame();
    }

    private LevelConfig getLevelConfig(int level) {
        int platforms;
        long memoryTime;

        // How many PLAYABLE platforms you want
        int playable;

        if (level < 20) {
            playable = 3;
        } else if (level < 40) {
            playable = 4;
        } else {
            // Level 40 gives 5 playable, +1 every 20 levels
            playable = 5 + ((level - 40) / 20);
        }

        // Convert playable count → total platform count
        platforms = playable + 2;

        // Safety clamp
        platforms = Math.min(platforms, MAX_PLATFORMS);

        // Memory time calculation - aggressive early game, scales with platforms later
        if (level <= 50) {
            // Levels 1-50: Aggressive decrease - 80ms per level
            // This creates immediate challenge and keeps early game engaging
            long baseDecrease = (level - 1) * 80L;
            memoryTime = BASE_MEMORY_TIME_MS - baseDecrease;
        } else {
            // Levels 51+: Slower decrease - 40ms per level
            // Starts from where level 50 left off
            long level50Time = BASE_MEMORY_TIME_MS - (49 * 80L);
            long additionalDecrease = (level - 50) * 40L;
            memoryTime = level50Time - additionalDecrease;
        }

        // When a new platform is added, give bonus time to compensate
        // Check if we just crossed a platform threshold
        if (level > 50) {
            int currentPlatformTier = (level - 50) / LEVELS_PER_PLATFORM_INCREASE;
            int previousLevel = level - 1;
            int previousPlatformTier = previousLevel > 50 ? (previousLevel - 50) / LEVELS_PER_PLATFORM_INCREASE : 0;

            // If we just gained a new platform, add 1000ms bonus time
            if (currentPlatformTier > previousPlatformTier) {
                memoryTime += 1000L;
            }
        }

        // Clamp to minimum
        memoryTime = Math.max(MIN_MEMORY_TIME_MS, memoryTime);

        return new LevelConfig(platforms, memoryTime);
    }

    private void initializeGame() {
        currentConfig = getLevelConfig(currentLevel);

        if (platforms == null || platforms.length != currentConfig.totalPlatforms) {
            platforms = new PlatformGlass[currentConfig.totalPlatforms];
        }

        float startZ = 0f;

        // First platform - starting platform (black, centered, full width)
        platforms[0] = new PlatformGlass(0, true, PLATFORM_Y, startZ);
        platforms[0].setIsStart(true);

        // Middle platforms - regular glass bridge sections with weighted randomization
        // This prevents long streaks of same side being correct
        int consecutiveCount = 0;
        boolean lastLeftIsCorrect = random.nextBoolean(); // First platform is pure random

        for (int i = 1; i < currentConfig.totalPlatforms - 1; i++) {
            boolean leftIsCorrect;

            if (i == 1) {
                // First glass platform is pure random
                leftIsCorrect = lastLeftIsCorrect;
            } else {
                // Calculate probability based on consecutive count
                // Much more aggressive penalty to prevent long streaks
                float baseProbability = 0.5f;
                float penaltyPerConsecutive = 0.25f; // Increased from 0.15f
                float probability = baseProbability - (consecutiveCount * penaltyPerConsecutive);

                // Hard cap at 4 consecutive - force switch after 4
                if (consecutiveCount >= 4) {
                    leftIsCorrect = !lastLeftIsCorrect; // Force switch
                    consecutiveCount = 0;
                } else {
                    // Clamp between 5% and 95% to keep some randomness
                    probability = Math.max(0.05f, Math.min(0.95f, probability));

                    // Decide if we should keep the same side correct
                    if (random.nextFloat() < probability) {
                        leftIsCorrect = lastLeftIsCorrect; // Same as last
                        consecutiveCount++;
                    } else {
                        leftIsCorrect = !lastLeftIsCorrect; // Switch sides
                        consecutiveCount = 0; // Reset counter
                    }
                }
            }

            platforms[i] = new PlatformGlass(i, leftIsCorrect, PLATFORM_Y, startZ + i * PLATFORM_Z_SPACING);
            lastLeftIsCorrect = leftIsCorrect;
        }

        // Last platform - finish platform (black, centered, full width)
        platforms[currentConfig.totalPlatforms - 1] = new PlatformGlass(
                currentConfig.totalPlatforms - 1,
                true,
                PLATFORM_Y,
                startZ + (currentConfig.totalPlatforms - 1) * PLATFORM_Z_SPACING
        );
        platforms[currentConfig.totalPlatforms - 1].setIsFinish(true);

        if (player == null) {
            player = new Player(0f, PLATFORM_Y, startZ);
        } else {
            player.respawnToStart(startZ);
        }

        nextPlatform = 1; // Start at platform 1 (first glass bridge platform)
        shakeAmount = 0f;
        lastJumpTime = 0;
        lastFrameTime = SystemClock.uptimeMillis();
        isRespawning = false;
        hasStartedTimer = false;
        memoryPhaseComplete = false;

        scheduledEvents.clear();
    }

    public void startGame() {
        currentLevel = 1;
        initializeGame();
        state = GameState.MEMORY_PHASE;
        memoryPhaseStartTime = SystemClock.uptimeMillis();
        totalPausedTime = 0;
        lastFrameTime = SystemClock.uptimeMillis();
        lastJumpTime = SystemClock.uptimeMillis() - INPUT_DELAY_MS;

        // Show hints on all platforms
        for (int i = 1; i < currentConfig.totalPlatforms - 1; i++) {
            platforms[i].showMemoryHint(true);
        }
    }

    public void restartCurrentLevel() {
        initializeGame();
        state = GameState.MEMORY_PHASE;
        memoryPhaseStartTime = SystemClock.uptimeMillis();
        gameStartTime = 0;
        totalPausedTime = 0;
        lastFrameTime = SystemClock.uptimeMillis();
        lastJumpTime = SystemClock.uptimeMillis() - INPUT_DELAY_MS;
        hasStartedTimer = false;

        // Show hints on all platforms
        for (int i = 1; i < currentConfig.totalPlatforms - 1; i++) {
            platforms[i].showMemoryHint(true);
        }
    }

    public void pauseGame() {
        if (state != GameState.PLAYING) return;
        pauseStartTime = SystemClock.uptimeMillis();
        state = GameState.PAUSED;
    }

    public void resumeGame() {
        if (state != GameState.PAUSED) return;
        long pauseDuration = SystemClock.uptimeMillis() - pauseStartTime;
        totalPausedTime += pauseDuration;
        state = GameState.PLAYING;
        lastFrameTime = SystemClock.uptimeMillis();
    }

    public void returnToMenu() {
        currentLevel = 1;
        state = GameState.MENU;
        initializeGame();
        totalPausedTime = 0;
    }

    private void scheduleOnGLThread(Runnable action, long delayMs) {
        long runAt = SystemClock.uptimeMillis() + Math.max(0, delayMs);
        synchronized (scheduledEvents) {
            scheduledEvents.add(new ScheduledEvent(runAt, action));
        }
    }

    public void jumpLeft() {
        handleJump(true);
    }

    public void jumpRight() {
        handleJump(false);
    }

    private void handleJump(boolean left) {
        if (!isActive || platforms == null || player == null || nextPlatform >= currentConfig.totalPlatforms) {
            return;
        }

        // Can't jump during memory phase
        if (state == GameState.MEMORY_PHASE) {
            return;
        }

        if (isRespawning) {
            return;
        }

        long currentTime = SystemClock.uptimeMillis();
        if (currentTime - lastJumpTime < INPUT_DELAY_MS) {
            return;
        }
        lastJumpTime = currentTime;

        // Start timer on first jump
        if (!hasStartedTimer) {
            hasStartedTimer = true;
            gameStartTime = currentTime;
        }

        PlatformGlass p = platforms[nextPlatform];

        // Always jump to the platform first
        player.jumpTo(p.getX(left), PLATFORM_Y, p.getZ());
        nextPlatform++;

        if (p.isCorrect(left) || p.isFinish()) {
            // Correct platform - just land with shake
            shakeAmount = JUMP_LAND_SHAKE;

            if (nextPlatform >= currentConfig.totalPlatforms) {
                scheduleOnGLThread(this::winLevel, 0);
            }
        } else {
            // Wrong platform - wait for player to land, then break it
            scheduleOnGLThread(() -> {
                if (isActive && player != null && platforms != null && state == GameState.PLAYING) {
                    // Break the platform
                    p.breakSide(left);
                    shakeAmount = GLASS_BREAK_SHAKE;

                    // Make player fall
                    player.fall();
                    isRespawning = true;

                    // Schedule level decrease after falling
                    scheduleOnGLThread(() -> {
                        if (isActive) {
                            failLevel();
                        }
                    }, RESPAWN_DELAY_MS);
                }
            }, 300); // Wait 300ms for player to land before breaking
        }
    }

    private void winLevel() {
        if (state != GameState.PLAYING) return;

        // Advance to next level
        currentLevel++;
        if (currentLevel > highestLevelReached) {
            highestLevelReached = currentLevel;
        }

        state = GameState.WON;
    }

    private void failLevel() {
        if (!isActive) return;

        // Go back one level (minimum level 1)
        currentLevel = Math.max(1, currentLevel - 1);

        // Reset game state
        state = GameState.PLAYING;
        isRespawning = false;

        // Reinitialize the game with new level
        initializeGame();

        // Start memory phase for the new level
        state = GameState.MEMORY_PHASE;
        memoryPhaseStartTime = SystemClock.uptimeMillis();
        gameStartTime = 0;
        totalPausedTime = 0;
        lastFrameTime = SystemClock.uptimeMillis();
        lastJumpTime = SystemClock.uptimeMillis() - INPUT_DELAY_MS;
        hasStartedTimer = false;

        // Show hints on all platforms
        if (platforms != null) {
            for (int i = 1; i < currentConfig.totalPlatforms - 1; i++) {
                if (platforms[i] != null) {
                    platforms[i].showMemoryHint(true);
                }
            }
        }
    }

    public void update() {
        if (!isActive) return;
        if (player == null || platforms == null) return;

        long currentTime = SystemClock.uptimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000.0f;
        lastFrameTime = currentTime;
        deltaTime = Math.min(deltaTime, MAX_DELTA_TIME);

        // Handle memory phase
        if (state == GameState.MEMORY_PHASE) {
            long elapsed = currentTime - memoryPhaseStartTime;
            long totalMemoryDuration = currentConfig.memoryDisplayDuration + MEMORY_FADE_DURATION_MS;

            if (elapsed >= totalMemoryDuration) {
                // Memory phase complete - transition to playing
                state = GameState.PLAYING;
                memoryPhaseComplete = true;

                // Hide all hints
                for (int i = 1; i < currentConfig.totalPlatforms - 1; i++) {
                    platforms[i].showMemoryHint(false);
                }
            } else if (elapsed >= currentConfig.memoryDisplayDuration) {
                // Fade out phase
                float fadeProgress = (elapsed - currentConfig.memoryDisplayDuration) / (float) MEMORY_FADE_DURATION_MS;
                for (int i = 1; i < currentConfig.totalPlatforms - 1; i++) {
                    platforms[i].setMemoryHintAlpha(1.0f - fadeProgress);
                }
            }
        }

        if (state == GameState.PLAYING) {
            // Execute scheduled events
            ArrayList<ScheduledEvent> toExecute = new ArrayList<>();
            synchronized (scheduledEvents) {
                for (int i = scheduledEvents.size() - 1; i >= 0; i--) {
                    ScheduledEvent ev = scheduledEvents.get(i);
                    if (ev.executeAt <= currentTime) {
                        toExecute.add(ev);
                    }
                }
                // Remove executed events
                for (ScheduledEvent ev : toExecute) {
                    scheduledEvents.remove(ev);
                }
            }

            // Execute outside the synchronized block to avoid deadlocks
            for (ScheduledEvent ev : toExecute) {
                try {
                    ev.action.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            if (player != null) {
                player.update();
            }
        }

        for (PlatformGlass p : platforms) {
            if (p != null) {
                p.update();
            }
        }

        if (shakeAmount > 0) {
            shakeAmount = Math.max(0f, shakeAmount - SHAKE_DECAY_RATE * deltaTime);
        }
    }

    public void draw(float[] vpMatrix) {
        if (vpMatrix == null || platforms == null || player == null) {
            return;
        }

        for (PlatformGlass p : platforms) {
            if (p != null) {
                p.draw(vpMatrix);
            }
        }
        player.draw(vpMatrix);
    }

    public float getShakeAmount() {
        return Math.max(0, shakeAmount);
    }

    public float getMemoryPhaseProgress() {
        if (state != GameState.MEMORY_PHASE) return 1.0f;
        long elapsed = SystemClock.uptimeMillis() - memoryPhaseStartTime;
        long totalDuration = currentConfig.memoryDisplayDuration + MEMORY_FADE_DURATION_MS;
        return Math.min(1.0f, elapsed / (float) totalDuration);
    }

    public long getMemoryPhaseRemainingMs() {
        if (state != GameState.MEMORY_PHASE) return 0;
        long elapsed = SystemClock.uptimeMillis() - memoryPhaseStartTime;
        long remaining = currentConfig.memoryDisplayDuration - elapsed;
        return Math.max(0, remaining);
    }

    public GameState getGameState() { return state; }
    public boolean isPlaying() { return state == GameState.PLAYING; }
    public boolean isInMemoryPhase() { return state == GameState.MEMORY_PHASE; }
    public boolean isGameWon() { return state == GameState.WON; }

    public int getCurrentLevel() { return currentLevel; }
    public int getHighestLevelReached() { return highestLevelReached; }
    public int getCurrentPlatformCount() { return currentConfig.totalPlatforms; }
    public float getCurrentMemoryTimeSeconds() { return currentConfig.memoryDisplayDuration / 1000f; }

    public void cleanup() {
        isActive = false;
        synchronized (scheduledEvents) {
            scheduledEvents.clear();
        }
    }
}
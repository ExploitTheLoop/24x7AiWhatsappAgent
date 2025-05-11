package com.animetone.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class dashboard extends AppCompatActivity {

    private final Handler sessionStatusHandler = new Handler();
    private Runnable sessionStatusRunnable;
    private Runnable messageFetchRunnable;
    private boolean isConnectedGlobally = false; // Globally declared variable

    private TextView activeBotsCountTextView;
    private TextView totalUsersCountTextView;
    private TextView messagesHandledCountTextView;
    private TextView badge;

    private int currentActiveBots = 137;
    private int currentTotalUsers = 5400;
    private int currentMessagesHandled = 51600;

    private final Random random = new Random();
    private static final long ANIMATION_DURATION = 1500; // Animation duration in milliseconds
    private Switch monitorSwitch,botSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        setupSystemBars();

        final String sessionId;

        String tempSessionId = getIntent().getStringExtra("sessionId");

        if (tempSessionId == null) {
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            tempSessionId = sharedPreferences.getString("sessionId", null);
        }

        sessionId = tempSessionId;

        LinearLayout botToggleLayout = findViewById(R.id.botToggleLayout);
        ImageView botToggleIcon = findViewById(R.id.botToggleIcon);
        TextView botToggleText = findViewById(R.id.botToggleText);
        ImageView logoutButton = findViewById(R.id.logoutButton);
        ProgressBar logoutSpinner = findViewById(R.id.logoutSpinner);
        activeBotsCountTextView = findViewById(R.id.textViewActiveBotsCount); // Replace with your actual IDs
        totalUsersCountTextView = findViewById(R.id.textViewTotalUsersCount);
        messagesHandledCountTextView = findViewById(R.id.textViewMessagesHandledCount);
        badge = findViewById(R.id.notificationBadge);
        ImageView bellbutton = findViewById(R.id.notificationBell);
        monitorSwitch = findViewById(R.id.switch_notify);
        botSwitch = findViewById(R.id.switch_activity);

        startContinuousAnimations();


        //fetchBotStatus(sessionId, botToggleText, botToggleIcon);

        botToggleLayout.setOnClickListener(v -> {
            String currentText = botToggleText.getText().toString();
            if (currentText.equalsIgnoreCase("Pause bot")) {
                toggleBotStatus(sessionId, false, botToggleText, botToggleIcon);
            } else {
                toggleBotStatus(sessionId, true, botToggleText, botToggleIcon);
            }
        });

        logoutButton.setOnClickListener(v -> {
            if (isConnectedGlobally) { // Check the global isConnected status
                logoutButton.setEnabled(false);
                logoutButton.setVisibility(View.INVISIBLE);
                logoutSpinner.setVisibility(View.VISIBLE);
                disconnectSessionAndLogout(sessionId, logoutButton, logoutSpinner);
            } else {

                Intent intent = new Intent(dashboard.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();

               // Toast.makeText(this, "Cannot logout while disconnected.", Toast.LENGTH_SHORT).show();
            }
        });

        sessionStatusRunnable = new Runnable() {
            @Override
            public void run() {
                checkSessionAndUpdateStatus(sessionId);
                fetchBotStatus(sessionId, botToggleText, botToggleIcon);
              //  fetchMessageDataAndUpdateBadge(); //need to set different timer 1 min
                sessionStatusHandler.postDelayed(this, 5000); // repeat every 2 seconds
            }
        };

        // Separate runnable to fetch messages every 1 minute
        messageFetchRunnable = new Runnable() {
            @Override
            public void run() {
                fetchMessageDataAndUpdateBadge(); // Runs only once every 60 seconds
                sessionStatusHandler.postDelayed(this, 60000); // repeat every 1 minute
            }
        };

        sessionStatusHandler.post(sessionStatusRunnable); // start polling
        sessionStatusHandler.postDelayed(messageFetchRunnable, 0); // start immediately or after a delay

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        int notificationcount = prefs.getInt("notificationcount",0);
        setBadgeCount(notificationcount);

        bellbutton.setOnClickListener(v -> {
            Intent intent = new Intent(dashboard.this, NotificationActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        boolean isMonitoring = prefs.getBoolean("monitoring_enabled", false);
        monitorSwitch.setChecked(isMonitoring);
        monitorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("monitoring_enabled", isChecked);
            editor.apply();


            if (isChecked) {
                startMonitoring();
            } else {
                stopMonitoring();
            }
        });

        boolean isBotEnabled = prefs.getBoolean("bot_control_enabled", false);
        botSwitch.setChecked(isBotEnabled);

        botSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("bot_control_enabled", isChecked);
            editor.apply();

            if (isChecked) {
                startBotWatchdog();
            } else {
                stopBotControl();
            }
        });


        // Auto start previously enabled services
        if (isMonitoring) startMonitoring();
        if (isBotEnabled) startBotWatchdog();
    }

    private void startMonitoring() {
        Intent serviceIntent = new Intent(this, MonitoringService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        startWatchdogWorker(); // Single shared watchdog
    }

    private void stopMonitoring() {
        stopService(new Intent(this, MonitoringService.class));
        // Note: don't cancel watchdog here—it may still be needed for BotControl
    }

    private void startBotWatchdog() {
        Intent botServiceIntent = new Intent(this, BotControlService.class);
        ContextCompat.startForegroundService(this, botServiceIntent);

        startWatchdogWorker(); // Single shared watchdog
    }

    private void stopBotControl() {
        stopService(new Intent(this, BotControlService.class));
        // Note: don't cancel watchdog here—it may still be needed for MonitoringService
    }

    // Only one watchdog work manager to manage both services
    private void startWatchdogWorker() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ServiceWatchdogWorker.class, 15, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "watchdog_work",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }


    public String getSheetApiUrl() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String sheetId = prefs.getString("googleSheetsId", null);

        // Get table name from SharedPreferences, default to "Sheet1"
        String tableName = prefs.getString("tableName", PromptConstants.DEFAULT_TABLE_NAME);

        if (sheetId != null && !sheetId.isEmpty() && !tableName.isEmpty()) {
            return PromptConstants.SHEET_BASE_URL + sheetId + "/" + tableName;
        }
        return null;
    }

    public void setBadgeCount(int count) {
        if (count > 0) {
            badge.setVisibility(View.VISIBLE);
            if (count > 99) {
                badge.setText("99+");
            } else {
                badge.setText(String.valueOf(count));
            }
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void fetchMessageDataAndUpdateBadge() {

        new Thread(() -> {
            try {
                String apiUrl = getSheetApiUrl();
                if (apiUrl == null) {
                   // runOnUiThread(() ->
                   //         Toast.makeText(this, "Sheet ID or table name not set", Toast.LENGTH_SHORT).show()
                   // );
                    return;
                }

                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();
                    connection.disconnect();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    int currentCount = jsonArray.length();


                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    boolean hasPrev = prefs.contains("messageCount");
                    int previousCount = prefs.getInt("messageCount", 0);

                    int newMessages = hasPrev ? currentCount - previousCount : currentCount;
                    if (newMessages < 0) newMessages = 0;

                    final int finalNewMessages = newMessages; // Create a final copy for the lambda

                    runOnUiThread(() -> {
                        if (finalNewMessages > 0) {
                            setBadgeCount(finalNewMessages); // Only update badge if new messages
                            prefs.edit().putInt("notificationcount", finalNewMessages).apply();
                        }
                        prefs.edit().putInt("messageCount", currentCount).apply();

                    });
                } else {
                   //runOnUiThread(() ->
                   //        Toast.makeText(this, "Failed to fetch data: HTTP " + responseCode, Toast.LENGTH_SHORT).show()
                   //);
                }

            } catch (Exception e) {
                e.printStackTrace();
               // runOnUiThread(() ->
               //         Toast.makeText(this, "Error fetching messages", Toast.LENGTH_SHORT).show()
               // );
            }
        }).start();
    }

    private void setupSystemBars() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.background));
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.background));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() & ~(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN));
        }
    }

    private void fetchBotStatus(String sessionId, TextView textView, ImageView iconView) {
        new Thread(() -> {
            try {
                URL url = new URL(PromptConstants.BASE_URL+"/api/bot-status/" + sessionId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                JSONObject response = new JSONObject(result.toString());
                boolean botActive = response.optBoolean("botActive");

                runOnUiThread(() -> {
                    if (botActive) {
                        textView.setText("Pause bot");
                        iconView.setImageResource(R.drawable.ic_pause);
                    } else {
                        textView.setText("Start bot");
                        iconView.setImageResource(R.drawable.ic_play); // Use a play icon if available
                    }
                });

            } catch (Exception e) {
               // runOnUiThread(() -> Toast.makeText(this, "Failed to fetch bot status", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void toggleBotStatus(String sessionId, boolean turnOn, TextView textView, ImageView iconView) {
        new Thread(() -> {
            try {
                String endpoint = turnOn ? "bot-on" : "bot-off";
                URL url = new URL(PromptConstants.BASE_URL+"/api/" + endpoint + "/" + sessionId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                runOnUiThread(() -> {
                    if (turnOn) {
                        textView.setText("Pause bot");
                        iconView.setImageResource(R.drawable.ic_pause);
                        Toast.makeText(this, "Bot activated", Toast.LENGTH_SHORT).show();
                    } else {
                        textView.setText("Start bot");
                        iconView.setImageResource(R.drawable.ic_play);
                        Toast.makeText(this, "Bot paused", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
              //  runOnUiThread(() -> Toast.makeText(this, "Failed to update bot", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void disconnectSessionAndLogout(String sessionId, ImageView logoutButton, ProgressBar spinner) {
        new Thread(() -> {
            try {
                URL url = new URL(PromptConstants.BASE_URL + "/api/disconnect/" + sessionId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.connect();

                int responseCode = conn.getResponseCode();
                conn.disconnect();

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit().remove("sessionId").apply();
                        Intent intent = new Intent(dashboard.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    } else {
                      //  Toast.makeText(this, "Logout failed", Toast.LENGTH_SHORT).show();
                        logoutButton.setEnabled(true);
                        logoutButton.setVisibility(View.VISIBLE);
                        spinner.setVisibility(View.GONE);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                  //  Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    logoutButton.setEnabled(true);
                    logoutButton.setVisibility(View.VISIBLE);
                    spinner.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void checkSessionAndUpdateStatus(String sessionId) {
        new Thread(() -> {
            try {
                URL checkUrl = new URL(PromptConstants.BASE_URL + "/api/check-session/" + sessionId);
                HttpURLConnection conn = (HttpURLConnection) checkUrl.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                conn.disconnect();

                JSONObject response = new JSONObject(result.toString());
                isConnectedGlobally = response.optBoolean("isConnected", false); // Update the global variable

                runOnUiThread(() -> updateStatusUI(isConnectedGlobally));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    isConnectedGlobally = false; // Set to false in case of an error
                    updateStatusUI(false);
                });
            }
        }).start();
    }

    private void updateStatusUI(boolean isConnected) {
        LinearLayout statusContainer = findViewById(R.id.statusContainer);
        View statusDot = findViewById(R.id.statusDot);
        TextView statusTitle = findViewById(R.id.statusTitle);
        TextView statusMessage = findViewById(R.id.statusMessage);
        ImageView statusIcon = findViewById(R.id.statusIcon);
        TextView statusText = findViewById(R.id.statusText);
        LottieAnimationView robotLottie = findViewById(R.id.robotLottie);
        TextView assistantStatusText = findViewById(R.id.assistantStatusText);
        TextView assistantOnlineIndicator = findViewById(R.id.assistantOnlineIndicator);

        if (isConnected) {
            statusContainer.setBackgroundResource(R.drawable.connected_bg);
            statusDot.setBackgroundResource(R.drawable.circle_greenbig);
            statusTitle.setText("Connected");
            statusMessage.setText("WhatsApp is running normally");
            statusIcon.setImageResource(R.drawable.ic_check_circle);
            statusIcon.setColorFilter(Color.parseColor("#81C784"));
            statusText.setText("Active");
            statusText.setTextColor(Color.parseColor("#81C784"));

            // Lottie animation and status message for active session
            robotLottie.setVisibility(View.VISIBLE);
            robotLottie.playAnimation();
            assistantStatusText.setText("Assistant is active and monitoring your WhatsApp");
            assistantOnlineIndicator.setText("● Online");
            assistantOnlineIndicator.setTextColor(Color.parseColor("#00FF00"));

        } else {
            statusContainer.setBackgroundResource(R.drawable.disconnected_bg);
            statusDot.setBackgroundResource(R.drawable.circle_red);
            statusTitle.setText("Disconnected");
            statusMessage.setText("WhatsApp is not connected");
            statusIcon.setImageResource(R.drawable.ic_error);
            statusIcon.setColorFilter(Color.parseColor("#FF5252"));
            statusText.setText("Inactive");
            statusText.setTextColor(Color.parseColor("#FF5252"));

            // Lottie animation and status message for inactive session
            robotLottie.setVisibility(View.VISIBLE); // or View.GONE if you want to hide it
            robotLottie.pauseAnimation();
            assistantStatusText.setText("Assistant is not running. Please reconnect.");
            assistantOnlineIndicator.setText("● Offline");
            assistantOnlineIndicator.setTextColor(Color.parseColor("#FF5252"));

        }
    }

    private void startContinuousAnimations() {
        // Initial display
        updateActiveBotsCount(currentActiveBots);
        updateTotalUsersCount(currentTotalUsers);
        updateMessagesHandledCount(currentMessagesHandled);

        // Start animations with a delay
        activeBotsCountTextView.postDelayed(this::animateActiveBots, 1000);
        totalUsersCountTextView.postDelayed(this::animateTotalUsers, 1500);
        messagesHandledCountTextView.postDelayed(this::animateMessagesHandled, 2000);
    }

    private void animateActiveBots() {
        int change = random.nextInt(21) - 10; // Random change between -10 and +10
        int newCount = Math.max(0, currentActiveBots + change); // Ensure it doesn't go below 0

        ValueAnimator animator = ValueAnimator.ofInt(currentActiveBots, newCount);
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(animation -> {
            updateActiveBotsCount((int) animation.getAnimatedValue());
        });
        animator.start();

        currentActiveBots = newCount;
        activeBotsCountTextView.postDelayed(this::animateActiveBots, 2000 + random.nextInt(1000)); // Animate again after a random delay
    }

    private void animateTotalUsers() {
        int increase = random.nextInt(50) + 50; // Increase by 50 to 100
        int newCount = currentTotalUsers + increase;

        ValueAnimator animator = ValueAnimator.ofInt(currentTotalUsers, newCount);
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(animation -> {
            updateTotalUsersCount((int) animation.getAnimatedValue());
        });
        animator.start();

        currentTotalUsers = newCount;
        totalUsersCountTextView.postDelayed(this::animateTotalUsers, 2500 + random.nextInt(1000)); // Animate again after a random delay
    }

    private void animateMessagesHandled() {
        int increase = random.nextInt(200) + 100; // Increase by 100 to 300
        int newCount = currentMessagesHandled + increase;

        ValueAnimator animator = ValueAnimator.ofInt(currentMessagesHandled, newCount);
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(animation -> {
            updateMessagesHandledCount((int) animation.getAnimatedValue());
        });
        animator.start();

        currentMessagesHandled = newCount;
        messagesHandledCountTextView.postDelayed(this::animateMessagesHandled, 3000 + random.nextInt(1000)); // Animate again after a random delay
    }

    private void updateActiveBotsCount(int count) {
        activeBotsCountTextView.setText(String.valueOf(count));
    }

    private void updateTotalUsersCount(int count) {
        String formattedCount = String.format("%.1fK", (float) count / 1000);
        totalUsersCountTextView.setText(formattedCount);
    }

    private void updateMessagesHandledCount(int count) {
        String formattedCount = String.format("%.1fK", (float) count / 1000);
        messagesHandledCountTextView.setText(formattedCount);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove callbacks to stop the polling
        sessionStatusHandler.removeCallbacks(sessionStatusRunnable);
        sessionStatusHandler.removeCallbacks(messageFetchRunnable);

    }
}
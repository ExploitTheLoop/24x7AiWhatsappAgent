package com.animetone.myapplication;

import android.app.Service;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BotControlService extends Service {

    private Handler handler;
    private Runnable checkRunnable;
    private String sessionId;
    private boolean isBotRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "bot_channel_id",
                    "Bot Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Start as a foreground service with initial "starting" status
        updateNotification("Starting...");
        startForeground(1, buildNotification("Starting..."));

        // Load session ID
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        sessionId = sharedPreferences.getString("sessionId", null);

        handler = new Handler();
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (sessionId != null) {
                    checkSessionAndUpdateStatus(sessionId);
                }
                handler.postDelayed(this, 5000); // every 5 seconds
            }
        };
        handler.post(checkRunnable);
    }

    private void checkSessionAndUpdateStatus(String sessionId) {
        new Thread(() -> {
            try {
                URL checkUrl = new URL(PromptConstants.BASE_URL + "/api/check-session/" + sessionId);
                HttpURLConnection conn = (HttpURLConnection) checkUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                conn.disconnect();

                JSONObject response = new JSONObject(result.toString());
                boolean isConnected = response.optBoolean("isConnected", false);

                if (!isConnected) return;

                boolean isWhatsAppRunning = isAppRunning("com.whatsapp");

                if (isWhatsAppRunning || isScreenOn()) {
                    stopBot();
                } else {
                    startBot();
                }

            } catch (Exception e) {
                e.printStackTrace();
                stopBot(); // fallback
                updateNotification("Error");
            }
        }).start();
    }

    private boolean isAppRunning(String packageName) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
            if (processInfo.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isScreenOn() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        boolean screenOn = powerManager.isInteractive();
        boolean unlocked = !keyguardManager.isKeyguardLocked();

        return screenOn && unlocked;
    }

    private void startBot() {
        if (!isBotRunning) {
            isBotRunning = true;
            toggleBotStatus(true);
        }
    }

    private void stopBot() {
        if (isBotRunning) {
            isBotRunning = false;
            toggleBotStatus(false);
        }
    }

    private void toggleBotStatus(boolean turnOn) {
        new Thread(() -> {
            try {
                String endpoint = turnOn ? "bot-on" : "bot-off";
                URL url = new URL(PromptConstants.BASE_URL + "/api/" + endpoint + "/" + sessionId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.connect();

                conn.getInputStream().close(); // consume response

                runOnUiThread(() -> {
                    String msg = turnOn ? "Bot started" : "Bot stopped";
                    Toast.makeText(BotControlService.this, msg, Toast.LENGTH_SHORT).show();
                    updateNotification(turnOn ? "Running" : "Paused");
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(BotControlService.this, "Failed to toggle bot", Toast.LENGTH_SHORT).show();
                    updateNotification("Error");
                });
            }
        }).start();
    }

    private void updateNotification(String statusText) {
        Notification notification = buildNotification(statusText);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notification); // Same ID as startForeground
    }

    private Notification buildNotification(String statusText) {
        return new NotificationCompat.Builder(this, "bot_channel_id")
                .setContentTitle("Bot Control Active")
                .setContentText("Status: " + statusText)
                .setSmallIcon(R.drawable.ic_robot) // Replace with your actual icon
                .setOngoing(true)
                .build();
    }

    private void runOnUiThread(Runnable runnable) {
        new Handler(getMainLooper()).post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkRunnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

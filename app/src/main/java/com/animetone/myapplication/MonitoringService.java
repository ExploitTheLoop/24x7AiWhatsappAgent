package com.animetone.myapplication;

import static com.animetone.myapplication.PromptConstants.SHEET_BASE_URL;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MonitoringService extends Service {

    private Handler handler = new Handler();
    private Runnable runnable;
   // private static final String SHEET_URL = "https://opensheet.elk.sh/1nHJvedNr-626HknpNydqIPZfCq998dyVUFSHCNBSXWQ/Sheet1";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(2, getNotification());

        runnable = new Runnable() {
            @Override
            public void run() {
                fetchDataAndNotify();
                handler.postDelayed(this, 60 * 1000); // 1 min
            }
        };

        handler.post(runnable);
        return START_STICKY;
    }

    private Notification getNotification() {
        String channelId = "notify_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Notification Channel", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Monitoring Google Sheet")
                .setContentText("Running...")
                .setSmallIcon(R.drawable.ic_bell)
                .build();
    }

    public String getSheetApiUrl() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String sheetId = prefs.getString("googleSheetsId", null);
        String tableName = prefs.getString("tableName", "Sheet1");

        if (sheetId != null && !sheetId.isEmpty() && tableName != null && !tableName.isEmpty()) {
            return SHEET_BASE_URL + sheetId + "/" + tableName;
        }
        return null;
    }


    private void fetchDataAndNotify() {
        String apiUrl = getSheetApiUrl();
        if (apiUrl == null) return;

        SharedPreferences prefsMain = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String currentSheetId = prefsMain.getString("googleSheetsId", null);

        SharedPreferences prefsSeen = getSharedPreferences("SeenData", MODE_PRIVATE);
        String lastSheetId = prefsSeen.getString("last_sheet_id", null);
        String seenSet;

        // 🔁 Reset seen set if sheet ID has changed
        if (lastSheetId == null || !lastSheetId.equals(currentSheetId)) {
            seenSet = "";
            prefsSeen.edit().putString("seen_set", "").putString("last_sheet_id", currentSheetId).apply();
        } else {
            seenSet = prefsSeen.getString("seen_set", "");
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(apiUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(body);
                        if (jsonArray.length() == 0) return;

                        StringBuilder newSeenSet = new StringBuilder(seenSet);
                        boolean hasNew = false;

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            String key = obj.toString().hashCode() + "";

                            if (!seenSet.contains(key)) {
                                hasNew = true;
                                showNotification(obj.getString("Sender"), obj.getString("Message"));
                                newSeenSet.append(",").append(key);
                            }
                        }

                        if (hasNew) {
                            prefsSeen.edit().putString("seen_set", newSeenSet.toString()).apply();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }




    private void showNotification(String number, String message) {
        String channelId = "new_data_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "New Data Alert", NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("New Message Received")
                .setContentText("From: " + number) // Shown when collapsed
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("From: " + number + "\n\n" + message)); // Shown when expanded

        // ✅ Permission check for Android 13+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }
}


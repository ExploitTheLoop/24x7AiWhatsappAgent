package com.animetone.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class qrActivity extends AppCompatActivity {

    private String sessionId;
    private TextView statusText, countertext;
    private ImageView qrImageView;
    private LinearLayout disconnectButton;

    private final Handler handler = new Handler();
    private Runnable pollingRunnable;
    private final int POLLING_INTERVAL = 5000; // 5 seconds
    private boolean isReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        setupSystemBars();

        statusText = findViewById(R.id.statusText);
        qrImageView = findViewById(R.id.qrImageView);
        countertext = findViewById(R.id.countertxt);

        sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null) {
            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
            sessionId = sharedPreferences.getString("sessionId", null);
        }

        if (sessionId != null) {
            startStatusPolling(sessionId);
        } else {
            statusText.setText("No active session found.");
        }
    }

    private void startStatusPolling(String sessionId) {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                checkStatus(sessionId);
                if (isReady) {
                    // Wait for a short delay before navigating to the dashboard
                    handler.postDelayed(() -> goDashBoardActivity(sessionId), 5000); // 2 seconds delay
                    handler.removeCallbacks(this); // Stop further polling
                } else {
                    handler.postDelayed(this, POLLING_INTERVAL);
                }
            }
        };
        handler.post(pollingRunnable);
    }

    private void startCounter() {
        final int[] secondsRemaining = {POLLING_INTERVAL / 1000}; // 5 seconds
        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isReady) {
                    if (secondsRemaining[0] > 0) {
                        countertext.setText("QR code will refresh in " + secondsRemaining[0] + "s");
                        secondsRemaining[0]--;
                        handler.postDelayed(this, 1000);
                    } else {
                        countertext.setText("Checking...");
                    }
                }
            }
        };
        handler.post(countdownRunnable);
    }

    private void checkStatus(String sessionId) {
        new Thread(() -> {
            try {
                URL url = new URL(PromptConstants.BASE_URL+"/api/status/" + sessionId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                conn.disconnect();

                JSONObject response = new JSONObject(result.toString());
                String status = response.optString("status");
                String qrCodeUrl = response.optString("qrCode");

                runOnUiThread(() -> {
                    switch (status) {
                        case "ready":
                            statusText.setText("🟢 WhatsApp Connected Successfully!");
                            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
                            isReady = true;
                            break;
                        case "connecting":
                            statusText.setText("🕓 Generating QR Code...");
                            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light));
                            isReady = false;
                            countertext.setVisibility(View.GONE);
                            break;
                        case "waiting":
                            statusText.setText("📱 Waiting for QR Scan on WhatsApp...");
                            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light));
                            isReady = false;
                            countertext.setVisibility(View.VISIBLE);
                            startCounter();
                            break;
                        default:
                            statusText.setText("🔴 Error: " + status);
                            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
                            isReady = false;
                            countertext.setVisibility(View.GONE);
                            break;
                    }

                    if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
                        loadQrImage(qrCodeUrl);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> statusText.setText("Error: " + e.getMessage()));
                // Do not immediately retry navigation on error, just continue polling
            }
        }).start();
    }

    private void loadQrImage(String imageUrl) {
        new Thread(() -> {
            try {
                Bitmap bitmap;
                if (imageUrl.startsWith("data:image")) {
                    String base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1);
                    byte[] decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                } else {
                    InputStream in = new URL(imageUrl).openStream();
                    bitmap = BitmapFactory.decodeStream(in);
                }

                Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> qrImageView.setImageBitmap(finalBitmap));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> statusText.setText("Failed to load QR image."));
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pollingRunnable); // Stop polling
        handler.removeCallbacksAndMessages(null); // Remove any pending callbacks
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

    private void goDashBoardActivity(String sessionId) {
        runOnUiThread(() -> {
            Intent dashboardIntent = new Intent(qrActivity.this, dashboard.class);
            dashboardIntent.putExtra("sessionId", sessionId);
            startActivity(dashboardIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish(); // Close the QR activity so the user doesn't go back to it easily
        });
    }
}
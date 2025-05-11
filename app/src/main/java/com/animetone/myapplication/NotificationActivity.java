package com.animetone.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout noNotificationsView;
    private TextView notificationbadeg;
    private ImageView backbutton;

    private NotificationAdapter adapter;
    private final Handler handler = new Handler();
    private final Runnable fetchRunnable = this::fetchNotifications;
    ImageView editbutton;
    LinearLayout editlayout,notifylayout,LoadButton;
    EditText tableinput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        setupSystemBars();

        //need  to set edittext toset table name
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        noNotificationsView = findViewById(R.id.noNotificationsView);
        notificationbadeg = findViewById(R.id.notificationBadge);
        backbutton = findViewById(R.id.backButton);
        editbutton = findViewById(R.id.editButton);
        editlayout = findViewById(R.id.editlayout);
        notifylayout = findViewById(R.id.notifytextlayout);
        tableinput = findViewById(R.id.tableNameInput);
        LoadButton = findViewById(R.id.loadDataButton);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new NotificationAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        int notificationCount = prefs.getInt("notificationcount", 0); //do it on resume
        String tableName = prefs.getString("tableName", PromptConstants.DEFAULT_TABLE_NAME);
        tableinput.setText(tableName);

        if (notificationCount > 0) {
            notificationbadeg.setText(String.valueOf(notificationCount));
            notificationbadeg.setVisibility(View.VISIBLE);
        } else {
            notificationbadeg.setVisibility(View.INVISIBLE);
        }

        backbutton.setOnClickListener(v -> {
            Intent dashboard = new Intent(NotificationActivity.this, dashboard.class);
            startActivity(dashboard);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        editbutton.setOnClickListener(v -> {
            if (editlayout.getVisibility() == View.VISIBLE) {
                editlayout.setVisibility(View.GONE);
                notifylayout.setVisibility(View.VISIBLE);
                editbutton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_edit));

            } else {
                editlayout.setVisibility(View.VISIBLE);
                notifylayout.setVisibility(View.GONE);
                editbutton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_close));

            }
        });

        LoadButton.setOnClickListener(v -> {
            String name = tableinput.getText().toString();
            prefs.edit().putString("tableName", name).apply();
            tableinput.setText(prefs.getString("tableName", PromptConstants.DEFAULT_TABLE_NAME));
        });

        prefs.edit().putInt("notificationcount", 0).apply();
        fetchNotifications();
    }

    @Override
    public void onBackPressed() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        prefs.edit().putInt("notificationcount", 0).apply();
        super.onBackPressed();
    }


    @Override
    protected void onResume() {
        super.onResume();
        startRepeatingFetch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeatingFetch();
    }

    private void startRepeatingFetch() {
        handler.postDelayed(fetchRunnable, 60000);
    }

    private void stopRepeatingFetch() {
        handler.removeCallbacks(fetchRunnable);
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

    public String getSheetApiUrl() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String sheetId = prefs.getString("googleSheetsId", null);
        String tableName = prefs.getString("tableName", PromptConstants.DEFAULT_TABLE_NAME);

        if (sheetId != null && !sheetId.isEmpty() && !tableName.isEmpty()) {
            return PromptConstants.SHEET_BASE_URL + sheetId + "/" + tableName;
        }
        return null;
    }

    private void fetchNotifications() {
        String url = getSheetApiUrl();

        if (url == null || url.isEmpty()) {
           // Toast.makeText(this, "Invalid Google Sheet URL", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<NotificationItem> itemList = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            itemList.add(new NotificationItem(
                                    obj.getString("Sender"),
                                    obj.getString("Message"),
                                    obj.getString("Timestamp")
                            ));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if (itemList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        noNotificationsView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        noNotificationsView.setVisibility(View.GONE);
                        adapter.updateData(itemList);
                    }

                    // Schedule next fetch
                    handler.postDelayed(fetchRunnable, 60000);
                },
                error -> {
                    error.printStackTrace();
                   // Toast.makeText(this, "Failed to fetch notifications", Toast.LENGTH_SHORT).show();
                    handler.postDelayed(fetchRunnable, 60000); // retry
                });

        queue.add(request);
    }
}

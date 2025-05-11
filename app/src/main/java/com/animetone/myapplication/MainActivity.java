package com.animetone.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.graphics.drawable.Drawable;
import android.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import android.widget.ArrayAdapter;

import com.thecode.aestheticdialogs.AestheticDialog;
import com.thecode.aestheticdialogs.DialogStyle;
import com.thecode.aestheticdialogs.DialogType;

import org.json.JSONObject;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private EditText geminiApiKey, deepgramApiKey, elevenLabsApiKey, elevenLabsVoiceId, googleSheetsId, promptInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set status bar color and make navigation bar transparent
        setupSystemBars();

        // Initialize EditText fields
        geminiApiKey = findViewById(R.id.geminiApiKey);
        deepgramApiKey = findViewById(R.id.deepgramApiKey);
        elevenLabsApiKey = findViewById(R.id.elevenApiKey);
        elevenLabsVoiceId = findViewById(R.id.elevenVoiceId);
        googleSheetsId = findViewById(R.id.sheetsId);
        promptInput = findViewById(R.id.promptEditText);

        // In your onCreate() method

        ImageView geminiInfoIcon = findViewById(R.id.geminiInfoIcon);
        ImageView deepgramInfoIcon = findViewById(R.id.deepgramInfoIcon);
        ImageView elevenInfoIcon = findViewById(R.id.elevenInfoIcon);
        ImageView voiceInfoIcon = findViewById(R.id.voiceInfoIcon);
        ImageView sheetsInfoIcon = findViewById(R.id.sheetsInfoIcon);

        geminiInfoIcon.setOnClickListener(v -> showGeminiInfoDialog());
        deepgramInfoIcon.setOnClickListener(v -> showDeepgramInfoDialog());
        elevenInfoIcon.setOnClickListener(v -> showElevenLabsApiKeyInfoDialog());
        voiceInfoIcon.setOnClickListener(v -> showElevenLabsVoiceIdInfoDialog());
        sheetsInfoIcon.setOnClickListener(v -> showGoogleSheetsIdInfoDialog());

        RelativeLayout continueLayout = findViewById(R.id.continueLayout);

        continueLayout.setOnClickListener(v -> {
            if (!validateFields()) return;

            showLoading();

            saveData();

            new Handler().postDelayed(() -> checkOrStartBotSession(), 2000);
        });

        Spinner promptDropdown = findViewById(R.id.promptDropdown);

        // Sample data for the Spinner (replace with your actual data)
        List<String> promptOptions = new ArrayList<>();
        promptOptions.add("Custom Prompt");
       // promptOptions.add("Tech Support");
        promptOptions.add("Professional");


        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item, // Layout for the selected item (what's shown when closed)
                promptOptions
        );
        adapter.setDropDownViewResource(R.layout.my_spinner_dropdown_item); // Layout for the dropdown items (what you see when opened)
        promptDropdown.setAdapter(adapter);

        promptDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected item
                String selectedPrompt = (String) parent.getItemAtPosition(position);
                if(position == 0){
                   // Toast.makeText(MainActivity.this, "Selected (Position 0): " + selectedPrompt, Toast.LENGTH_SHORT).show();
                }else if (position == 1){
                    promptInput.setText(PromptConstants.PROFESSIONAL_ASSISTANT_PROMPT);
                    //Toast.makeText(MainActivity.this, "Selected (Position 1): " + selectedPrompt, Toast.LENGTH_SHORT).show();
                }else{
                   // promptInput.setText(PromptConstants.TECH_SUPPORT_PROMPT);
                    //Toast.makeText(MainActivity.this, "Selected (Position 2): " + selectedPrompt, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                promptInput.setText("");
            }
        });

        // Allow internal scrolling inside EditText
        promptInput.setMovementMethod(new ScrollingMovementMethod());

        promptInput.setOnTouchListener((v, event) -> {
            if (v.hasFocus()) {
                v.getParent().requestDisallowInterceptTouchEvent(true);

                if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
            return false;
        });

        loadData();


    }

    private boolean validateFields() {
        boolean isValid = true;

        if (geminiApiKey.getText().toString().trim().isEmpty()) {
            geminiApiKey.setBackgroundResource(R.drawable.red_border);
          //  Toast.makeText(this, "Please enter your Gemini API Key", Toast.LENGTH_SHORT).show();
            geminiApiKey.requestFocus();
            isValid = false;
        } else {
            geminiApiKey.setBackgroundResource(R.drawable.edit_text_border); // Reset to the original background
        }

        // Check Deepgram API Key
        if (deepgramApiKey.getText().toString().trim().isEmpty()) {

            deepgramApiKey.setBackgroundResource(R.drawable.red_border);
           // Toast.makeText(this, "Please enter your Deepgram API Key", Toast.LENGTH_SHORT).show();
            deepgramApiKey.requestFocus();
            isValid = false;
        } else {
            deepgramApiKey.setBackgroundResource(R.drawable.edit_text_border); // Reset to the original background
        }

        // Check ElevenLabs API Key
        if (elevenLabsApiKey.getText().toString().trim().isEmpty()) {

            elevenLabsApiKey.setBackgroundResource(R.drawable.red_border);
          //  Toast.makeText(this, "Please enter your ElevenLabs API Key", Toast.LENGTH_SHORT).show();
            elevenLabsApiKey.requestFocus();
            isValid = false;
        } else {
            elevenLabsApiKey.setBackgroundResource(R.drawable.edit_text_border);
        }

        // Check ElevenLabs Voice ID
        if (elevenLabsVoiceId.getText().toString().trim().isEmpty()) {
            elevenLabsVoiceId.setBackgroundResource(R.drawable.red_border);
           // Toast.makeText(this, "Please enter your ElevenLabs Voice ID", Toast.LENGTH_SHORT).show();
            elevenLabsVoiceId.requestFocus();
            isValid = false;
        } else {
            elevenLabsVoiceId.setBackgroundResource(R.drawable.edit_text_border);
        }

        // Check Google Sheets ID
        if (googleSheetsId.getText().toString().trim().isEmpty()) {

            googleSheetsId.setBackgroundResource(R.drawable.red_border);
         //   Toast.makeText(this, "Please enter your Google Sheets ID", Toast.LENGTH_SHORT).show();
            googleSheetsId.requestFocus();
            isValid = false;
        } else {
            googleSheetsId.setBackgroundResource(R.drawable.edit_text_border);
        }

        // Check Prompt
        if (promptInput.getText().toString().trim().isEmpty()) {
            promptInput.setBackgroundResource(R.drawable.red_border);
        //    Toast.makeText(this, "Please enter your Prompt", Toast.LENGTH_SHORT).show();
            promptInput.requestFocus();
            isValid = false;
        } else {
            promptInput.setBackgroundResource(R.drawable.edit_text_border);
        }

        return isValid;
    }

    private void setupSystemBars() {
        Window window = getWindow();

        // Clear the flag that makes the content appear behind the system bars
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // Add the flag to draw the system bar backgrounds
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // Set the status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.background));
        }

        // Set the navigation bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.background));
        }

        // For dark background, we don't need light status bar text, so we ensure the flag is cleared
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(window.getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Remove the flags related to making the layout fullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true); // Set to true to respect window insets
        } else {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() & ~(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            );
        }

    }

    private void showGeminiInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null);
        builder.setView(customView);
        AlertDialog dialog = builder.create();

        TextView titleTextView = customView.findViewById(R.id.dialogTitle);
        TextView messageTextView = customView.findViewById(R.id.dialogMessage);
        ImageView closeButton = customView.findViewById(R.id.dialogCloseButton);
        LinearLayout defaultApiKeyLayout = customView.findViewById(R.id.defaultApiKeyLayout);
        TextView defaultApiKeyTextView = customView.findViewById(R.id.defaultApiKeyText);

        titleTextView.setText("Get Gemini API Key");
        String message = "• Go to Google AI Studio (<a href='https://makersuite.google.com/app/apikey'>https://makersuite.google.com/app/apikey</a>)<br>" // Changed "Visit" to "Go to" for clarity
                + "• Sign in with your Google account<br>"
                + "• Click the 'Create API key' button<br>" // More specific button name
                + "• Copy the generated API key";
        messageTextView.setText(Html.fromHtml(message));

        defaultApiKeyTextView.setText("AIzaSyAM7LcuHrVDe8_7NB4YE8wJkfr0Szg3VjQ");

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }


    private void showDeepgramInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null);
        builder.setView(customView);
        AlertDialog dialog = builder.create();

        TextView titleTextView = customView.findViewById(R.id.dialogTitle);
        TextView messageTextView = customView.findViewById(R.id.dialogMessage);
        ImageView closeButton = customView.findViewById(R.id.dialogCloseButton);
        LinearLayout defaultApiKeyLayout = customView.findViewById(R.id.defaultApiKeyLayout);
        TextView defaultApiKeyTextView = customView.findViewById(R.id.defaultApiKeyText);

        titleTextView.setText("Get Deepgram API Key");
        String message = "• Visit the Deepgram Console (<a href='https://console.deepgram.com/dev-settings'>https://console.deepgram.com/dev-settings</a>)<br>"
                + "• Sign up or log in<br>"
                + "• Go to 'API Keys' in the navigation menu<br>" // More specific navigation
                + "• Click 'Create New API Key'<br>"
                + "• Copy the generated API key"; // Clarified "generated"
        messageTextView.setText(Html.fromHtml(message));
        defaultApiKeyTextView.setText("4124c6e774717a15544e6c66c21a3f875c558b9c");

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void showElevenLabsApiKeyInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null);
        builder.setView(customView);
        AlertDialog dialog = builder.create();

        TextView titleTextView = customView.findViewById(R.id.dialogTitle);
        TextView messageTextView = customView.findViewById(R.id.dialogMessage);
        ImageView closeButton = customView.findViewById(R.id.dialogCloseButton);
        LinearLayout defaultApiKeyLayout = customView.findViewById(R.id.defaultApiKeyLayout);
        TextView defaultApiKeyTextView = customView.findViewById(R.id.defaultApiKeyText);


        titleTextView.setText("Get ElevenLabs API Key"); // Corrected title
        String message = "• Visit ElevenLabs (<a href='https://elevenlabs.io'>https://elevenlabs.io</a>)<br>"
                + "• Create an account or log in<br>"
                + "• Go to 'Profile' (or 'Settings')<br>"  // More general
                + "• Find and copy your API Key."; // More concise
        messageTextView.setText(Html.fromHtml(message));

        defaultApiKeyTextView.setText("sk_2edc8eb92636e99f711344d51329e9342c6874b17557eb82");

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void showElevenLabsVoiceIdInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null);
        builder.setView(customView);
        AlertDialog dialog = builder.create();

        TextView titleTextView = customView.findViewById(R.id.dialogTitle);
        TextView messageTextView = customView.findViewById(R.id.dialogMessage);
        ImageView closeButton = customView.findViewById(R.id.dialogCloseButton);
        LinearLayout defaultApiKeyLayout = customView.findViewById(R.id.defaultApiKeyLayout);
        TextView defaultApiKeyTextView = customView.findViewById(R.id.defaultApiKeyText);
        TextView defaultApiKeyTextHeadView = customView.findViewById(R.id.defapikeyhead);

        titleTextView.setText("Get ElevenLabs Voice Id"); // Changed title for clarity
        String message = "<b>Get Voice ID:</b><br>"
                + "• Visit ElevenLabs (<a href='https://elevenlabs.io'>https://elevenlabs.io</a>)<br>"
                + "• Go to the 'Voice Library' or create a voice in 'VoiceLab'<br>"
                + "• Select the desired voice<br>"
                + "• The Voice ID is in the URL (e.g., clv... in elevenlabs.io/voice/clv...) or voice settings.";
        messageTextView.setText(Html.fromHtml(message));
        defaultApiKeyTextHeadView.setVisibility(View.INVISIBLE);
        defaultApiKeyTextView.setText("TRnaQb7q41oL7sV0w6Bu");

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void showGoogleSheetsIdInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_info, null);
        builder.setView(customView);
        AlertDialog dialog = builder.create();

        TextView titleTextView = customView.findViewById(R.id.dialogTitle);
        TextView messageTextView = customView.findViewById(R.id.dialogMessage);
        ImageView closeButton = customView.findViewById(R.id.dialogCloseButton);
        LinearLayout defaultApiKeyLayout = customView.findViewById(R.id.defaultApiKeyLayout);
        TextView defaultApiKeyTextView = customView.findViewById(R.id.defaultApiKeyText);
        TextView defaultApiKeyTextHeadView = customView.findViewById(R.id.defapikeyhead);

        titleTextView.setText("Get Google Sheets ID");
        String message = "•  Create a new Google Sheet.<br>"
                + "•  Share the sheet with: <font color='#80CBC4'>serviceman11@divine-apogee-456813-e0.iam.gserviceaccount.com</font> (Editor access).<br>"
                + "•  Make sure the sheet is public or accessible to anyone with the link.<br>"
                + "•  The Sheet ID is found in the URL:<br>"
                + "   <font color='#AAAAAA'>https://docs.google.com/spreadsheets/d/</font><font color='#80CBC4'><b>{YOUR_SHEET_ID}</b></font><font color='#AAAAAA'>/edit...</font><br>"
                + "•  Copy the highlighted section.<br><br>"
                + "<b>Tip for Logging:</b><br>"
                + "To help the AI log important messages to this Google Sheet, consider adding specific tags or phrases at the end of your assistant's response in the prompt. For example:<br>"
                + "<font color='#AAAAAA'>Urgent/Important {\"isImportant\": true, \"why\": \"Reason for urgency\"}</font><br>"
                + "<font color='#AAAAAA'>Follow-up Required {\"checkLogs\": true}</font>";
        messageTextView.setText(Html.fromHtml(message));
        defaultApiKeyTextHeadView.setVisibility(View.INVISIBLE);
        defaultApiKeyTextView.setText("serviceman11@divine-apogee-456813-e0.iam.gserviceaccount.com");
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("geminiApiKey", geminiApiKey.getText().toString());
        editor.putString("deepgramApiKey", deepgramApiKey.getText().toString());
        editor.putString("elevenLabsApiKey", elevenLabsApiKey.getText().toString());
        editor.putString("elevenLabsVoiceId", elevenLabsVoiceId.getText().toString());
        editor.putString("googleSheetsId", googleSheetsId.getText().toString());
        editor.putString("promptInput", promptInput.getText().toString());

        editor.apply(); // save the data asynchronously
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        geminiApiKey.setText(sharedPreferences.getString("geminiApiKey", ""));
        deepgramApiKey.setText(sharedPreferences.getString("deepgramApiKey", ""));
        elevenLabsApiKey.setText(sharedPreferences.getString("elevenLabsApiKey", ""));
        elevenLabsVoiceId.setText(sharedPreferences.getString("elevenLabsVoiceId", ""));
        googleSheetsId.setText(sharedPreferences.getString("googleSheetsId", ""));
        promptInput.setText(sharedPreferences.getString("promptInput", ""));
    }

    private void checkOrStartBotSession() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String existingSessionId = sharedPreferences.getString("sessionId", "");

        if (!existingSessionId.isEmpty()) {
            new Thread(() -> {
                try {
                    URL checkUrl = new URL(PromptConstants.BASE_URL+"/api/check-session/" + existingSessionId);
                    HttpURLConnection conn = (HttpURLConnection) checkUrl.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);

                    JSONObject response = new JSONObject(result.toString());
                    boolean isConnected = response.optBoolean("isConnected", false);

                    conn.disconnect();

                    if (isConnected) {
                        goDashBoardActivity(existingSessionId);
                        return;
                    }
                } catch (Exception e) {
                    // ignore and start new session
                }

                startNewBotSession();

            }).start();
        } else {
            startNewBotSession();
        }
    }

    private void startNewBotSession() {
        new Thread(() -> {
            try {
                JSONObject postData = new JSONObject();
                postData.put("geminiApiKey", geminiApiKey.getText().toString());
                postData.put("deepgramApiKey", deepgramApiKey.getText().toString());
                postData.put("elevenLabsApiKey", elevenLabsApiKey.getText().toString());
                postData.put("elevenLabsVoiceId", elevenLabsVoiceId.getText().toString());
                postData.put("googleSheetsId", googleSheetsId.getText().toString());
                postData.put("prompt", promptInput.getText().toString());

                URL url = new URL(PromptConstants.BASE_URL+"/api/start-bot");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(postData.toString().getBytes("UTF-8"));
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(conn.getInputStream())));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                JSONObject response = new JSONObject(result.toString());
                String sessionId = response.optString("sessionId");

              //  runOnUiThread(() -> Toast.makeText(MainActivity.this, "response: " + result.toString(), Toast.LENGTH_SHORT).show());

                if (!sessionId.isEmpty()) {
                    SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    sharedPreferences.edit().putString("sessionId", sessionId).apply();

                    goToQrActivity(sessionId);
                } else {
                  //  runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to start bot", Toast.LENGTH_SHORT).show());

                    runOnUiThread(() ->
                            new AestheticDialog.Builder(MainActivity.this, DialogStyle.TOASTER, DialogType.ERROR)
                                    .setTitle("Error")
                                    .setMessage("Failed to start bot")
                                    .show()
                    );

                }

                conn.disconnect();

            } catch (Exception e) {
                runOnUiThread(() ->
                        new AestheticDialog.Builder(MainActivity.this, DialogStyle.TOASTER, DialogType.ERROR)
                                .setTitle("Error")
                                .setMessage("Failed to start bot: " + e.getMessage())
                                .show()
                );
            } finally {
                hideLoading();
            }
        }).start();
    }

    private void goDashBoardActivity(String sessionId) {
        runOnUiThread(() -> {
            Intent qrIntent = new Intent(MainActivity.this, dashboard.class);
            qrIntent.putExtra("sessionId", sessionId);
            startActivity(qrIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void goToQrActivity(String sessionId) {
        runOnUiThread(() -> {
            Intent qrIntent = new Intent(MainActivity.this, qrActivity.class);
            qrIntent.putExtra("sessionId", sessionId);
            startActivity(qrIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void showLoading() {
        runOnUiThread(() -> {
            findViewById(R.id.continueContent).setVisibility(View.GONE);
            findViewById(R.id.loadingLayout).setVisibility(View.VISIBLE);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            findViewById(R.id.loadingLayout).setVisibility(View.GONE);
            findViewById(R.id.continueContent).setVisibility(View.VISIBLE);
        });
    }



}
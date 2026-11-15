package com.max.goldai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int REQ_PERMISSIONS = 101;
    private static final int REQ_SPEECH = 102;
    private static final int REQ_CAMERA = 103;
    private static final int REQ_GALLERY = 104;

    private TextView chatHistoryText;
    private ScrollView chatScroll;
    private EditText inputMessage;
    private TextToSpeech kannadaTTS;
    private SharedPreferences memoryPrefs;
    private String pendingImageBase64 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatHistoryText = findViewById(R.id.chatHistoryText);
        chatScroll = findViewById(R.id.chatScroll);
        inputMessage = findViewById(R.id.inputMessage);

        memoryPrefs = getSharedPreferences("MaxGoldAIMemory", MODE_PRIVATE);
        loadMemoryHistory();

        kannadaTTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                kannadaTTS.setLanguage(new Locale("kn", "IN"));
                kannadaTTS.setSpeechRate(0.98f);
            }
        });

        findViewById(R.id.btnPermissions).setOnClickListener(v -> requestAllAppPermissions());
        findViewById(R.id.btnAccessibility).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.btnMic).setOnClickListener(v -> startVoiceRecognition());
        findViewById(R.id.btnCamera).setOnClickListener(v -> {
            Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(camIntent, REQ_CAMERA);
        });
        findViewById(R.id.btnPhoto).setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickIntent, REQ_GALLERY);
        });
        findViewById(R.id.btnClearMemory).setOnClickListener(v -> {
            memoryPrefs.edit().clear().apply();
            chatHistoryText.setText("🧠 ಜ್ಞಾಪಕ ಶಕ್ತಿ (Memory History) ಕ್ಲಿಯರ್ ಆಗಿದೆ.\n");
        });

        findViewById(R.id.btnSend).setOnClickListener(v -> {
            String msg = inputMessage.getText().toString().trim();
            if (!msg.isEmpty() || pendingImageBase64 != null) {
                inputMessage.setText("");
                sendToAIWithMemory(msg.isEmpty() ? "ಈ ಫೋಟೋದಲ್ಲಿ ಏನಿದೆ ವಿಶ್ಲೇಷಿಸಿ ಹೇಳಿ" : msg, pendingImageBase64);
                pendingImageBase64 = null;
            }
        });
    }

    private void requestAllAppPermissions() {
        String[] perms = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_MEDIA_IMAGES
        };
        requestPermissions(perms, REQ_PERMISSIONS);

        if (!Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivity(overlayIntent);
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kn-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "ಕನ್ನಡದಲ್ಲಿ ಮಾತನಾಡಿ...");
        try {
            startActivityForResult(intent, REQ_SPEECH);
        } catch (Exception e) {
            Toast.makeText(this, "ವಾಯ್ಸ್ ರೆಕಾರ್ಡಿಂಗ್ ಲಭ್ಯವಿಲ್ಲ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQ_SPEECH) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                sendToAIWithMemory(results.get(0), pendingImageBase64);
                pendingImageBase64 = null;
            }
        } else if (requestCode == REQ_CAMERA) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            if (photo != null) {
                pendingImageBase64 = bitmapToBase64(photo);
                appendAndSaveMemory("📷 [ಕ್ಯಾಮೆರಾ ದೃಶ್ಯ ಸೆರೆಹಿಡಿಯಲಾಗಿದೆ]");
                sendToAIWithMemory("ಕ್ಯಾಮೆರಾದಲ್ಲಿ ಕಾಣಿಸುತ್ತಿರುವ ದೃಶ್ಯವನ್ನು ನೋಡಿ ಕನ್ನಡದಲ್ಲಿ ವಿವರಿಸಿ.", pendingImageBase64);
                pendingImageBase64 = null;
            }
        } else if (requestCode == REQ_GALLERY) {
            try {
                Uri imageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                pendingImageBase64 = bitmapToBase64(bitmap);
                appendAndSaveMemory("🖼️ [ಗ್ಯಾಲರಿ ಫೋಟೋ ಆಯ್ಕೆಮಾಡಲಾಗಿದೆ]");
                Toast.makeText(this, "ಫೋಟೋ ಲೋಡ್ ಆಗಿದೆ, ಈಗ ಪ್ರಶ್ನೆ ಕೇಳಿ ಅಥವಾ ಕಳುಹಿಸಿ ಒತ್ತಿ", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getCurrentLocationText() {
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    return "Lat:" + loc.getLatitude() + ", Lon:" + loc.getLongitude();
                }
            }
        } catch (Exception ignored) {}
        return "Location unavailable";
    }

    private void sendToAIWithMemory(String userMessage, String imageBase64) {
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        appendAndSaveMemory("👤 ನೀವು (" + time + "): " + userMessage);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String savedHistory = memoryPrefs.getString("full_history", "");
                String recentMemory = savedHistory.length() > 1500
                    ? savedHistory.substring(savedHistory.length() - 1500) : savedHistory;

                JSONObject payload = new JSONObject();
                payload.put("message", userMessage);
                payload.put("memoryContext", recentMemory);
                payload.put("location", getCurrentLocationText());
                if (imageBase64 != null) {
                    payload.put("imageBase64", imageBase64);
                    payload.put("mimeType", "image/jpeg");
                }

                URL url = new URL("https://ais-dev-nwpboumkkayvdoq6syy3wr-606584189706.asia-northeast1.run.app/api/analyze-chart");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                InputStream is = (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
                Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";

                JSONObject json = new JSONObject(response);
                String reply = json.optString("kannada_msg",
                    json.optString("reply", "ನಾನು ನಿಮ್ಮ ಮಾತನ್ನು ಮೆಮೊರಿಯಲ್ಲಿ ಸೇವ್ ಮಾಡಿಕೊಂಡಿದ್ದೇನೆ ಬಾಸ್."));

                runOnUiThread(() -> {
                    appendAndSaveMemory("🤖 Max AI: " + reply + "\n");
                    if (kannadaTTS != null) {
                        kannadaTTS.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> appendAndSaveMemory("⚠️ ಸಂಪರ್ಕ ದೋಷ: ಇಂಟರ್ನೆಟ್ ಅಥವಾ ಸರ್ವರ್ ಪರಿಶೀಲಿಸಿ.\n"));
            }
        });
    }

    private void appendAndSaveMemory(String line) {
        String existing = memoryPrefs.getString("full_history", "");
        String updated = existing + "\n" + line;
        memoryPrefs.edit().putString("full_history", updated).apply();
        chatHistoryText.setText(updated);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void loadMemoryHistory() {
        String saved = memoryPrefs.getString("full_history",
            "🤖 ನಮಸ್ಕಾರ ಬಾಸ್! ನಾನು Max AI. ನಿಮ್ಮ ಮಾತು, ಫೋಟೋ, ಲೊಕೇಶನ್ ಮತ್ತು MT5 ಚಾರ್ಟ್ ಎಲ್ಲವನ್ನೂ ನೆನಪಿಟ್ಟುಕೊಳ್ಳುತ್ತೇನೆ.\n");
        chatHistoryText.setText(saved);
    }

    private String bitmapToBase64(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    @Override
    protected void onDestroy() {
        if (kannadaTTS != null) {
            kannadaTTS.stop();
            kannadaTTS.shutdown();
        }
        super.onDestroy();
    }
                    }

package com.max.goldai;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private static final int REQ_CAMERA = 103;
    private static final int REQ_GALLERY = 104;

    private TextView chatHistoryText, fullMemoryLogText, deviceStatusText;
    private ScrollView chatScroll;
    private EditText inputMessage, inputApiKey, searchFeatures;
    private Button btnMicManual, btnLangToggle;
    private View tabHome, tabHistory, tabFeatures, tabSettings;
    private LinearLayout featuresListContainer;
    private Button navHome, navHistory, navFeatures, navSettings;

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private SharedPreferences prefs;

    private boolean isMicManualOn = false;
    private boolean isFlashlightOn = false;
    private String currentLangCode = "kn-IN";
    private String currentLangName = "ಕನ್ನಡ";
    private String pendingImageBase64 = null;

    private final String[][] ALL_FEATURES = {
        {"ಧ್ವನಿ ಸಹಾಯಕ (Continuous Voice)", "CORE", "VOICE"},
        {"ವೈಯಕ್ತಿಕ ಸ್ಮರಣೆ (Personal Memory)", "CORE", "MEMORY"},
        {"ಬಹುಭಾಷಾ ಬದಲಾವಣೆ (Kannada / English / Hindi)", "CORE", "LANG"},
        {"ಫ್ಲ್ಯಾಶ್‌ಲೈಟ್ ನಿಯಂತ್ರಣ (Flashlight ON/OFF)", "UTILITY", "FLASH"},
        {"ಕ್ಯಾಮೆರಾ ಮತ್ತು ಸೆಲ್ಫಿ (Camera Vision)", "MEDIA", "CAMERA"},
        {"ದೃಶ್ಯ ವಿಶ್ಲೇಷಣೆ (Gallery Photo AI)", "VISION", "PHOTO"},
        {"ಪರದೆಯ ವಿಶ್ಲೇಷಣೆ (Live Screen & MT5 Gold)", "VISION", "SCREEN"},
        {"ಸ್ಮಾರ್ಟ್ ಸ್ಕ್ರೀನ್ ಕ್ಲಿಕ್‌ಗಳು (Auto-Tap Engine)", "NAVIGATION", "SCREEN"},
        {"ಸ್ಮಾರ್ಟ್ ಡ್ರೈವಿಂಗ್ ಮೋಡ್ (Driving Hands-Free)", "CALLS", "DRIVE"},
        {"ಒಳಬರುವ ಕರೆ ನಿಯಂತ್ರಣ (Dialer & Call)", "CALLS", "CALL"},
        {"ಫೋನ್ ಕರೆಗಳು ಮತ್ತು SMS", "CALLS", "SMS"},
        {"WhatsApp ಧ್ವನಿ ಮತ್ತು ಕರೆಗಳು", "SOCIAL", "WHATSAPP"},
        {"ಆಂಟಿ ಥೆಫ್ಟ್ ಗಾರ್ಡ್ (Anti-Theft Alert)", "SECURITY", "SECURITY"},
        {"ಲೈವ್ ಸಾಧನದ ಸ್ಥಿತಿ (Battery & Internet)", "UTILITY", "STATUS"},
        {"Spotify ಸಂಗೀತ ಎಂಜಿನ್", "MEDIA", "SPOTIFY"},
        {"YouTube ವೀಡಿಯೊ ಪ್ಲೇಯರ್", "MEDIA", "YOUTUBE"},
        {"Google ವೆಬ್ ಹುಡುಕಾಟ (Live Internet)", "UTILITY", "SEARCH"},
        {"ಸಾಮಾಜಿಕ ಮತ್ತು ರೀಲ್ ಸ್ಕ್ರಾಲ್", "NAVIGATION", "REELS"},
        {"ಜ್ಞಾಪನೆಗಳು ಮತ್ತು ಎಚ್ಚರಿಕೆಗಳು (Alarms)", "UTILITY", "ALARM"},
        {"ಸಿಸ್ಟಮ್ ನಿಯಂತ್ರಣಗಳು (Device Settings)", "UTILITY", "SYS"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("MaxGoldAIMemory", MODE_PRIVATE);
        currentLangCode = prefs.getString("lang_code", "kn-IN");
        currentLangName = prefs.getString("lang_name", "ಕನ್ನಡ");

        initViews();
        initTTS();
        initContinuousSpeechRecognizer();
        loadMemoryHistory();
        renderFeaturesList("ALL", "");
        updateDeviceStatus();
    }

    private void initViews() {
        chatHistoryText = findViewById(R.id.chatHistoryText);
        fullMemoryLogText = findViewById(R.id.fullMemoryLogText);
        deviceStatusText = findViewById(R.id.deviceStatusText);
        chatScroll = findViewById(R.id.chatScroll);
        inputMessage = findViewById(R.id.inputMessage);
        inputApiKey = findViewById(R.id.inputApiKey);
        searchFeatures = findViewById(R.id.searchFeatures);
        btnMicManual = findViewById(R.id.btnMicManual);
        btnLangToggle = findViewById(R.id.btnLangToggle);

        tabHome = findViewById(R.id.tabHome);
        tabHistory = findViewById(R.id.tabHistory);
        tabFeatures = findViewById(R.id.tabFeatures);
        tabSettings = findViewById(R.id.tabSettings);
        featuresListContainer = findViewById(R.id.featuresListContainer);

        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navFeatures = findViewById(R.id.navFeatures);
        navSettings = findViewById(R.id.navSettings);

        inputApiKey.setText(prefs.getString("gemini_api_key", ""));
        btnLangToggle.setText("🌐 " + currentLangName);

        navHome.setOnClickListener(v -> switchTab(0));
        navHistory.setOnClickListener(v -> { loadMemoryHistory(); switchTab(1); });
        navFeatures.setOnClickListener(v -> switchTab(2));
        navSettings.setOnClickListener(v -> { updateDeviceStatus(); switchTab(3); });

        btnLangToggle.setOnClickListener(v -> cycleLanguage());

        btnMicManual.setOnClickListener(v -> {
            if (!isMicManualOn) {
                isMicManualOn = true;
                btnMicManual.setText("🔴 Mic: ON");
                btnMicManual.setBackgroundColor(Color.parseColor("#DC2626"));
                startListeningLoop();
            } else {
                isMicManualOn = false;
                btnMicManual.setText("🎤 Mic: OFF");
                btnMicManual.setBackgroundColor(Color.parseColor("#334155"));
                try {
                    if (speechRecognizer != null) speechRecognizer.stopListening();
                } catch (Exception ignored) {}
            }
        });

        findViewById(R.id.btnCamera).setOnClickListener(v ->
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQ_CAMERA));

        findViewById(R.id.btnPhoto).setOnClickListener(v ->
            startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQ_GALLERY));

        findViewById(R.id.btnClearMemory).setOnClickListener(v -> {
            prefs.edit().remove("full_history").apply();
            loadMemoryHistory();
            Toast.makeText(this, "ಮೆಮೊರಿ ಕ್ಲಿಯರ್ ಆಗಿದೆ", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSaveApiKey).setOnClickListener(v -> {
            String key = inputApiKey.getText().toString().trim();
            prefs.edit().putString("gemini_api_key", key).apply();
            Toast.makeText(this, "Gemini API Key ಸೇವ್ ಆಗಿದೆ!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnPermissions).setOnClickListener(v -> requestAllAppPermissions());
        findViewById(R.id.btnAccessibility).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.btnSend).setOnClickListener(v -> {
            String msg = inputMessage.getText().toString().trim();
            if (!msg.isEmpty() || pendingImageBase64 != null) {
                inputMessage.setText("");
                sendToGeminiLive(msg.isEmpty() ? "ಈ ಫೋಟೋದಲ್ಲಿ ಏನಿದೆ ವಿವರಿಸಿ" : msg, pendingImageBase64);
                pendingImageBase64 = null;
            }
        });

        findViewById(R.id.catAll).setOnClickListener(v -> renderFeaturesList("ALL", searchFeatures.getText().toString()));
        findViewById(R.id.catCore).setOnClickListener(v -> renderFeaturesList("CORE", searchFeatures.getText().toString()));
        findViewById(R.id.catMedia).setOnClickListener(v -> renderFeaturesList("MEDIA", searchFeatures.getText().toString()));
        findViewById(R.id.catVision).setOnClickListener(v -> renderFeaturesList("VISION", searchFeatures.getText().toString()));
        findViewById(R.id.catUtility).setOnClickListener(v -> renderFeaturesList("UTILITY", searchFeatures.getText().toString()));
        findViewById(R.id.catCalls).setOnClickListener(v -> renderFeaturesList("CALLS", searchFeatures.getText().toString()));
        findViewById(R.id.catSecurity).setOnClickListener(v -> renderFeaturesList("SECURITY", searchFeatures.getText().toString()));
        findViewById(R.id.catNav).setOnClickListener(v -> renderFeaturesList("NAVIGATION", searchFeatures.getText().toString()));

        searchFeatures.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderFeaturesList("ALL", s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void switchTab(int index) {
        tabHome.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tabHistory.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tabFeatures.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tabSettings.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
    }

    private void cycleLanguage() {
        if ("kn-IN".equals(currentLangCode)) {
            currentLangCode = "en-IN";
            currentLangName = "English";
        } else if ("en-IN".equals(currentLangCode)) {
            currentLangCode = "hi-IN";
            currentLangName = "हिन्दी";
        } else {
            currentLangCode = "kn-IN";
            currentLangName = "ಕನ್ನಡ";
        }
        prefs.edit().putString("lang_code", currentLangCode).putString("lang_name", currentLangName).apply();
        btnLangToggle.setText("🌐 " + currentLangName);
        if (tts != null) tts.setLanguage(Locale.forLanguageTag(currentLangCode));
        Toast.makeText(this, "Language: " + currentLangName, Toast.LENGTH_SHORT).show();
    }

    private void initTTS() {
        try {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.forLanguageTag(currentLangCode));
                    tts.setSpeechRate(0.98f);
                }
            });
        } catch (Exception ignored) {}
    }

    private void initContinuousSpeechRecognizer() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    if (isMicManualOn) startListeningLoop();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        sendToGeminiLive(matches.get(0), pendingImageBase64);
                        pendingImageBase64 = null;
                    }
                    if (isMicManualOn) startListeningLoop();
                }

                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        } catch (Exception ignored) {}
    }

    private void startListeningLoop() {
        if (!isMicManualOn || speechRecognizer == null) return;
        runOnUiThread(() -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLangCode);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L);
            try {
                speechRecognizer.startListening(intent);
            } catch (Exception ignored) {}
        });
    }

    private void renderFeaturesList(String category, String query) {
        featuresListContainer.removeAllViews();
        String q = query.toLowerCase(Locale.ROOT);
        for (String[] feat : ALL_FEATURES) {
            if (!"ALL".equals(category) && !feat[1].equals(category)) continue;
            if (!q.isEmpty() && !feat[0].toLowerCase(Locale.ROOT).contains(q)) continue;

            Button card = new Button(this);
            card.setText(feat[0] + "   [" + feat[1] + "]");
            card.setAllCaps(false);
            card.setTextColor(Color.WHITE);
            card.setBackgroundColor(Color.parseColor("#1E293B"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 10);
            card.setLayoutParams(lp);
            card.setOnClickListener(v -> executeFeatureAction(feat[2]));
            featuresListContainer.addView(card);
        }
    }

    private void executeFeatureAction(String actionCode) {
        switch (actionCode) {
            case "VOICE": switchTab(0); btnMicManual.performClick(); break;
            case "MEMORY": loadMemoryHistory(); switchTab(1); break;
            case "LANG": cycleLanguage(); break;
            case "FLASH": toggleFlashlight(); break;
            case "CAMERA": startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQ_CAMERA); break;
            case "PHOTO": startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), REQ_GALLERY); break;
            case "SCREEN": startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); break;
            case "SPOTIFY": openAppOrWeb("com.spotify.music", "https://open.spotify.com"); break;
            case "YOUTUBE": openAppOrWeb("com.google.android.youtube", "https://m.youtube.com"); break;
            case "WHATSAPP": openAppOrWeb("com.whatsapp", "https://wa.me"); break;
            case "CALL": startActivity(new Intent(Intent.ACTION_DIAL)); break;
            case "SMS": startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))); break;
            case "SEARCH": startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))); break;
            case "ALARM": startActivity(new Intent("android.intent.action.SHOW_ALARMS")); break;
            case "SYS": startActivity(new Intent(Settings.ACTION_SETTINGS)); break;
            case "STATUS": updateDeviceStatus(); switchTab(3); break;
            default:
                Toast.makeText(this, "ಸಕ್ರಿಯವಾಗಿದೆ: " + actionCode, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void toggleFlashlight() {
        try {
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String camId = cm.getCameraIdList()[0];
            isFlashlightOn = !isFlashlightOn;
            cm.setTorchMode(camId, isFlashlightOn);
            Toast.makeText(this, isFlashlightOn ? "🔦 ಫ್ಲ್ಯಾಶ್‌ಲೈಟ್ ON" : "🔦 ಫ್ಲ್ಯಾಶ್‌ಲೈಟ್ OFF", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "ಫ್ಲ್ಯಾಶ್‌ಲೈಟ್ ದೋಷ", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppOrWeb(String pkg, String fallbackUrl) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch != null) {
            startActivity(launch);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
        }
    }

    private void updateDeviceStatus() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : 0;
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo net = cm != null ? cm.getActiveNetworkInfo() : null;
            boolean connected = net != null && net.isConnected();

            deviceStatusText.setText(
                "📡 Internet Status: " + (connected ? "Connected (Live Online)" : "Offline") + "\n" +
                "🔋 Battery Level: " + level + "%\n" +
                "📍 Location: " + getCurrentLocationText() + "\n" +
                "🌐 Active Language: " + currentLangName + " (" + currentLangCode + ")"
            );
        } catch (Exception ignored) {}
    }

    private void requestAllAppPermissions() {
        String[] perms = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.CALL_PHONE
        };
        requestPermissions(perms, REQ_PERMISSIONS);
        if (!Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQ_CAMERA && data.getExtras() != null) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            if (photo != null) {
                pendingImageBase64 = bitmapToBase64(photo);
                appendAndSaveMemory("📷 [ಕ್ಯಾಮೆರಾ ದೃಶ್ಯ]");
                sendToGeminiLive("ಕ್ಯಾಮೆರಾದಲ್ಲಿ ಕಾಣಿಸುತ್ತಿರುವುದನ್ನು ವಿವರಿಸಿ.", pendingImageBase64);
                pendingImageBase64 = null;
            }
        } else if (requestCode == REQ_GALLERY) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                pendingImageBase64 = bitmapToBase64(bitmap);
                appendAndSaveMemory("🖼️ [ಗ್ಯಾಲರಿ ಫೋಟೋ ಆಯ್ಕೆಯಾಗಿದೆ]");
                Toast.makeText(this, "ಫೋಟೋ ಸಿದ್ಧವಾಗಿದೆ, ಈಗ ಪ್ರಶ್ನೆ ಕೇಳಿ", Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
        }
    }

    private String getCurrentLocationText() {
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) return loc.getLatitude() + ", " + loc.getLongitude();
            }
        } catch (Exception ignored) {}
        return "Bengaluru, India";
    }

    private void sendToGeminiLive(String userMessage, String imageBase64) {
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        appendAndSaveMemory("👤 ನೀವು (" + time + "): " + userMessage);

        String apiKey = prefs.getString("gemini_api_key", "").trim();
        if (apiKey.isEmpty()) {
            appendAndSaveMemory("⚠️ ದಯವಿಟ್ಟು ಕೆಳಗಿನ Settings ಟ್ಯಾಬ್‌ನಲ್ಲಿ ನಿಮ್ಮ Google Gemini API Key ಸೇವ್ ಮಾಡಿ.\n");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String history = prefs.getString("full_history", "");
                String contextMem = history.length() > 1500 ? history.substring(history.length() - 1500) : history;

                String systemPrompt = "You are MAX Gold AI, a warm, intelligent human-like personal assistant and XAUUSD ICT trading expert. " +
                    "Respond naturally in " + currentLangName + " (" + currentLangCode + "). " +
                    "User Location: " + getCurrentLocationText() + ". Memory History:\n" + contextMem +
                    "\nUser says: " + userMessage;

                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", systemPrompt));
                if (imageBase64 != null) {
                    JSONObject inlineData = new JSONObject();
                    inlineData.put("mime_type", "image/jpeg");
                    inlineData.put("data", imageBase64);
                    parts.put(new JSONObject().put("inline_data", inlineData));
                }

                JSONObject contentObj = new JSONObject().put("parts", parts);
                JSONArray contents = new JSONArray().put(contentObj);

                JSONObject body = new JSONObject();
                body.put("contents", contents);

                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=" + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("x-goog-api-key", apiKey);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(25000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
                String respStr = s.hasNext() ? s.next() : "";

                JSONObject respJson = new JSONObject(respStr);
                if (code >= 200 && code < 300) {
                    String reply = respJson.getJSONArray("candidates")
                        .getJSONObject(0).getJSONObject("content")
                        .getJSONArray("parts").getJSONObject(0).getString("text");

                    runOnUiThread(() -> {
                        appendAndSaveMemory("🤖 MAX AI: " + reply + "\n");
                        if (tts != null) tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null);
                    });
                } else {
                    String errMsg = respJson.has("error")
                        ? respJson.getJSONObject("error").optString("message", respStr)
                        : respStr;
                    runOnUiThread(() -> appendAndSaveMemory("⚠️ API ದೋಷ (" + code + "): " + errMsg + "\n"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> appendAndSaveMemory("⚠️ ಸಂಪರ್ಕ ದೋಷ: " + e.getMessage() + "\n"));
            }
        });
    }

    private void appendAndSaveMemory(String line) {
        String updated = prefs.getString("full_history", "") + "\n" + line;
        prefs.edit().putString("full_history", updated).apply();
        chatHistoryText.setText(updated);
        fullMemoryLogText.setText(updated);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void loadMemoryHistory() {
        String saved = prefs.getString("full_history",
            "🤖 ನಮಸ್ಕಾರ ಬಾಸ್! ನಾನು MAX AI v3.0. ಮೈಕ್ ON ಮಾಡಿ ಎಷ್ಟು ಹೊತ್ತು ಬೇಕಾದರೂ ಮಾತನಾಡಿ, ಅಥವಾ Settings ನಲ್ಲಿ API Key ಹಾಕಿ ಬಳಸಿ.\n");
        chatHistoryText.setText(saved);
        fullMemoryLogText.setText(saved);
    }

    private String bitmapToBase64(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    @Override
    protected void onDestroy() {
        isMicManualOn = false;
        try {
            if (speechRecognizer != null) speechRecognizer.destroy();
            if (tts != null) { tts.stop(); tts.shutdown(); }
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}

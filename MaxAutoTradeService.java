package com.max.goldai;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.NonNull;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class MaxAutoTradeService extends AccessibilityService {

    private TextToSpeech kannadaTTS;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private boolean isScanning = true;

    // MT5 BUY/SELL coordinates calibrated for 1080x2460
    private final float BUY_BUTTON_X = 820f;
    private final float BUY_BUTTON_Y = 2250f;
    private final float SELL_BUTTON_X = 260f;
    private final float SELL_BUTTON_Y = 2250f;
    private static final int CONFIDENCE_GATE = 80;
    private static final long SCAN_INTERVAL_MS = 15000L;
    private static final long STARTUP_DELAY_MS = 5000L;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        kannadaTTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                kannadaTTS.setLanguage(new Locale("kn", "IN"));
                kannadaTTS.setSpeechRate(0.95f);
                kannadaTTS.setPitch(1.00f);
                speakKannada("ಮ್ಯಾಕ್ಸ್ ಗೋಲ್ಡ್ ಐಸಿಟಿ ಆಟೋ ಟ್ರೇಡಿಂಗ್ ಸಕ್ರಿಯವಾಗಿದೆ ಬಾಸ್.");
            }
        });
        startLiveChartLoop();
    }

    private void startLiveChartLoop() {
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    captureScreenAndAnalyze();
                }
                scanHandler.postDelayed(this, SCAN_INTERVAL_MS);
            }
        }, STARTUP_DELAY_MS);
    }

    private void captureScreenAndAnalyze() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(Display.DEFAULT_DISPLAY, Executors.newSingleThreadExecutor(),
                new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(@NonNull ScreenshotResult screenshot) {
                        Bitmap bitmap = Bitmap.wrapHardwareBuffer(
                            screenshot.getHardwareBuffer(), screenshot.getColorSpace());
                        if (bitmap != null) {
                            Bitmap softBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                            bitmap.recycle();
                            sendChartToGemini(softBitmap);
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) {}
                });
        }
    }

    private void sendChartToGemini(Bitmap chartBitmap) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                chartBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                chartBitmap.recycle();
                String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                JSONObject requestBody = new JSONObject();
                requestBody.put("imageBase64", base64Image);
                requestBody.put("mimeType", "image/jpeg");
                requestBody.put("confidenceGate", CONFIDENCE_GATE);

                URL url = new URL("https://ais-dev-nwpboumkkayvdoq6syy3wr-606584189706.asia-northeast1.run.app/api/analyze-chart");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
                Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                String response = scanner.hasNext() ? scanner.next() : "";

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject decision = new JSONObject(response);
                    String action = decision.optString("action", "WAIT");
                    int confidence = decision.optInt("confidence", 0);
                    String kannadaMsg = decision.optString("kannada_msg", "");

                    if (!kannadaMsg.isEmpty()) {
                        speakKannada(kannadaMsg);
                    }

                    if (confidence >= CONFIDENCE_GATE) {
                        if ("BUY".equalsIgnoreCase(action)) {
                            performAutoTap(BUY_BUTTON_X, BUY_BUTTON_Y);
                        } else if ("SELL".equalsIgnoreCase(action)) {
                            performAutoTap(SELL_BUTTON_X, SELL_BUTTON_Y);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void performAutoTap(float x, float y) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Path clickPath = new Path();
            clickPath.moveTo(x, y);
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(clickPath, 0, 100);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);
            dispatchGesture(builder.build(), null, null);
        });
    }

    private void speakKannada(String text) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (kannadaTTS != null) {
                kannadaTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    @Override
    public void onDestroy() {
        isScanning = false;
        scanHandler.removeCallbacksAndMessages(null);
        if (kannadaTTS != null) {
            kannadaTTS.stop();
            kannadaTTS.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}

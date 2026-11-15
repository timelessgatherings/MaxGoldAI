package com.max.goldai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusView = findViewById(R.id.statusText);
        statusView.setText(
            "Package: com.max.goldai\n" +
            "Resolution: 1080x2460\n" +
            "BUY: (820, 2250) | SELL: (260, 2250)\n" +
            "Confidence Gate: 80% | Interval: 15s\n" +
            "Relay: https://ais-dev-nwpboumkkayvdoq6syy3wr-606584189706.asia-northeast1.run.app/api/analyze-chart"
        );

        Button btnAccessibility = findViewById(R.id.btnAccessibility);
        btnAccessibility.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        Button btnOverlay = findViewById(R.id.btnOverlay);
        btnOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        });
    }
}
# Max Gold AI — Android Accessibility Auto-Trade APK Project

## Option A: Build APK in Android Studio
1. Extract this ZIP and open the folder in **Android Studio**.
2. Wait for Gradle sync to finish.
3. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Install `app/build/outputs/apk/debug/app-debug.apk` on your Android 11+ (API 30+) phone.

## Option B: Build APK via GitHub Actions (No Local Android Studio Required)
1. Push this extracted folder to a GitHub repository.
2. Open the **Actions** tab and run **Build Android Debug APK**.
3. Download `max-gold-ai-debug-apk` (`app-debug.apk`) directly to your phone.

## On-Device Setup
1. Open **Max Gold AI** on your Android phone.
2. Tap **1. Enable Accessibility Service** and toggle **Max Gold AI** ON.
3. Tap **2. Grant Display Over Other Apps** if prompted.
4. Switch to MetaTrader 5 (XAUUSD) — the service captures the chart every 15s and dispatches taps when confidence >= 80%.
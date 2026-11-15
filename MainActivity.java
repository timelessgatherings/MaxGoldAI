<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="#0F172A">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Max Gold AI — ಜ್ಞಾಪಕ ಶಕ್ತಿ &amp; ಲೈವ್ ಅಸಿಸ್ಟೆಂಟ್"
        android:textColor="#38BDF8"
        android:textSize="18sp"
        android:textStyle="bold" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="8dp">

        <Button
            android:id="@+id/btnPermissions"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:textSize="11sp"
            android:text="ಎಲ್ಲಾ ಪರ್ಮಿಷನ್ ನೀಡಿ" />

        <Button
            android:id="@+id/btnAccessibility"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:layout_marginStart="6dp"
            android:textSize="11sp"
            android:text="ಲೈವ್ ಸ್ಕ್ರೀನ್ ON" />
    </LinearLayout>

    <ScrollView
        android:id="@+id/chatScroll"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginTop="10dp"
        android:background="#1E293B"
        android:padding="12dp">

        <TextView
            android:id="@+id/chatHistoryText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="#F1F5F9"
            android:textSize="14sp"
            android:lineSpacingExtra="4dp" />
    </ScrollView>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="8dp">

        <Button
            android:id="@+id/btnCamera"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:textSize="11sp"
            android:text="📷 ಕ್ಯಾಮೆರಾ" />

        <Button
            android:id="@+id/btnPhoto"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:layout_marginStart="4dp"
            android:textSize="11sp"
            android:text="🖼️ ಫೋಟೋ" />

        <Button
            android:id="@+id/btnClearMemory"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:layout_marginStart="4dp"
            android:textSize="11sp"
            android:text="🧠 ಮೆಮೊರಿ ಕ್ಲಿಯರ್" />
    </LinearLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="8dp">

        <EditText
            android:id="@+id/inputMessage"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:hint="ಕನ್ನಡದಲ್ಲಿ ಕೇಳಿ ಅಥವಾ ಬರೆಯಿರಿ..."
            android:textColor="#FFFFFF"
            android:textColorHint="#94A3B8"
            android:background="#334155"
            android:padding="10dp" />

        <Button
            android:id="@+id/btnMic"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="6dp"
            android:text="🎤" />

        <Button
            android:id="@+id/btnSend"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="6dp"
            android:text="ಕಳುಹಿಸಿ" />
    </LinearLayout>
</LinearLayout>

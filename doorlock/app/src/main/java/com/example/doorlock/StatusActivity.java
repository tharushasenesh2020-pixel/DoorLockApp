package com.example.doorlock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class StatusActivity extends AppCompatActivity {

    private TextView statusBox;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        statusBox = findViewById(R.id.statusBox);
        Button refreshBtn = findViewById(R.id.refreshBtn);
        prefs = getSharedPreferences("LockSettings", MODE_PRIVATE);

        refreshBtn.setOnClickListener(v -> updateStatus());
        updateStatus();
    }

    private void updateStatus() {
        String status = prefs.getString("lock_status", "available"); // Default: locked
        if (status.equals("unavailable")) {
            statusBox.setText("🚫 Unavailable");
            statusBox.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            statusBox.setText("✅ Available");
            statusBox.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }
}

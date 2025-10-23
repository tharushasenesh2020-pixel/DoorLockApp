package com.example.doorlock;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class settings extends AppCompatActivity {

    private LinearLayout layoutAutoLockDelay, layoutChangePin;
    private TextView textAutoLockValue;
    private Switch switchOneTouch, switchKeypressBeep;
    private Button btnDeleteLock, btnUnlockTime;
    private SharedPreferences prefs;
    private final String[] delayOptions = {"2 minutes", "5 minutes", "10 minutes", "30 minutes", "1 hour"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Views
        prefs = getSharedPreferences("DoorLockPrefs", MODE_PRIVATE);

        layoutAutoLockDelay = findViewById(R.id.layout_auto_lock_delay);
        layoutChangePin = findViewById(R.id.layout_change_pin);
        textAutoLockValue = findViewById(R.id.text_auto_lock_value);
        switchOneTouch = findViewById(R.id.switch_one_touch);
        btnUnlockTime = findViewById(R.id.btn_unlock_time);
        switchKeypressBeep = findViewById(R.id.switch_keypress_beep);
        btnDeleteLock = findViewById(R.id.btn_delete_lock);

        textAutoLockValue.setText(prefs.getString("auto_lock_delay", "2 minutes"));
        switchOneTouch.setChecked(prefs.getBoolean("one_touch_locking", false));
        switchKeypressBeep.setChecked(prefs.getBoolean("keypress_beep", false));

        layoutAutoLockDelay.setOnClickListener(v -> showDelayDialog());
        layoutChangePin.setOnClickListener(v -> startActivity(new Intent(this, ChangePinActivity.class)));

        btnUnlockTime.setOnClickListener(v -> {
            startActivity(new Intent(settings.this, UnlockTimeActivity.class));
        });
        // Switch - One Touch Locking
        switchOneTouch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "One-Touch Locking Enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "One-Touch Locking Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Switch - Keypress Beep
        switchKeypressBeep.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Keypress Beep Enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Keypress Beep Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteLock.setOnClickListener(v -> {
            Toast.makeText(this, "Lock Deleted!", Toast.LENGTH_SHORT).show();
            // here you can add code to actually remove lock from database if needed
        });

    }

        private void showDelayDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Auto Lock Delay");
            builder.setItems(delayOptions, (DialogInterface dialog, int which) -> {
                textAutoLockValue.setText(delayOptions[which]);
                prefs.edit().putString("auto_lock_delay", delayOptions[which]).apply();
            });
            builder.show();
    }
}

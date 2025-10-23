package com.example.doorlock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class ChangePinActivity extends AppCompatActivity {

    private EditText etNewPin, etConfirmPin;
    private Button btnSavePin;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pin);

        etNewPin = findViewById(R.id.et_new_pin);
        etConfirmPin = findViewById(R.id.et_confirm_pin);
        btnSavePin = findViewById(R.id.btn_save_pin);

        prefs = getSharedPreferences("LockSettings", MODE_PRIVATE);

        btnSavePin.setOnClickListener(v -> {
            String newPin = etNewPin.getText().toString().trim();
            String confirm = etConfirmPin.getText().toString().trim();

            if (TextUtils.isEmpty(newPin) || TextUtils.isEmpty(confirm)) {
                Toast.makeText(this, "Please fill both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPin.equals(confirm)) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPin.length() < 4 || newPin.length() > 8) {
                Toast.makeText(this, "PIN must be 4–8 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            String pinLog = prefs.getString("pin_change_log", "");
            prefs.edit().putString("pin_change_log", pinLog + "PIN changed at " + java.time.LocalDateTime.now() + ";").apply();


            // All good — save PIN
            prefs.edit().putString("saved_pin", newPin).apply();
            Toast.makeText(this, "PIN changed successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}

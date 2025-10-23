package com.example.doorlock;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class PinActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BT_PERMISSIONS = 2;

    private TextView pinDisplay;
    private StringBuilder enteredPin = new StringBuilder();

    // Bluetooth
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private OutputStream outputStream;
    private boolean isConnected = false;

    // 🔧 Replace this with your HC-06 MAC address
    private static final String HC06_ADDRESS = "00:00:00:00:00:00";
    private static final UUID HC06_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Shared Preferences
    private SharedPreferences prefs;
    private String correctPin;

    // Buttons
    private Button btnLock;

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pin);

        pinDisplay = findViewById(R.id.pinDisplay);
        btnLock = findViewById(R.id.btnLock);

        prefs = getSharedPreferences("LockSettings", MODE_PRIVATE);
        correctPin = prefs.getString("saved_pin", "1234"); // Default PIN

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Request Bluetooth permissions (for Android 12+)
        checkAndRequestPermissions();

        btnLock.setOnClickListener(v -> {
            if (isConnected) {
                sendCommand("L"); // Lock command
            } else {
                Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
            }
        });

        setupNumberButtons();
        setupClearAndEnterButtons();
        updateDisplay();
    }

    private void setupNumberButtons() {
        int[] ids = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : ids) {
            Button btn = findViewById(id);
            btn.setOnClickListener(v -> {
                if (enteredPin.length() < 8) {
                    enteredPin.append(btn.getText().toString());
                    updateDisplay();
                }
            });
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    private void setupClearAndEnterButtons() {
        Button btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> {
            if (enteredPin.length() > 0) {
                enteredPin.deleteCharAt(enteredPin.length() - 1);
                updateDisplay();
            }
        });

        Button btnEnter = findViewById(R.id.btnEnter);
        btnEnter.setOnClickListener(v -> {
            if (enteredPin.toString().equals(correctPin)) {
                Toast.makeText(this, "✅ Correct PIN! Unlocking...", Toast.LENGTH_SHORT).show();
                connectAndUnlock();
            } else {
                Toast.makeText(this, "❌ Incorrect PIN", Toast.LENGTH_SHORT).show();
            }
            enteredPin.setLength(0);
            updateDisplay();
        });
    }

    private void updateDisplay() {
        if (enteredPin.length() == 0) {
            pinDisplay.setText("----");
        } else {
            StringBuilder hidden = new StringBuilder();
            for (int i = 0; i < enteredPin.length(); i++) {
                hidden.append("*");
            }
            pinDisplay.setText(hidden.toString());
        }
    }

    private void checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    },
                    REQUEST_BT_PERMISSIONS);
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    private void connectAndUnlock() {
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        if (isConnected) {
            sendCommand("U");
            return;
        }

        ProgressDialog dialog = ProgressDialog.show(this, "Connecting", "Please wait...", true);

        new Thread(() -> {
            try {
                BluetoothDevice device = btAdapter.getRemoteDevice(HC06_ADDRESS);
                btSocket = device.createRfcommSocketToServiceRecord(HC06_UUID);
                btAdapter.cancelDiscovery();
                btSocket.connect();

                outputStream = btSocket.getOutputStream();
                isConnected = true;

                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(PinActivity.this, "Connected to HC-06", Toast.LENGTH_SHORT).show();
                    sendCommand("U"); // Unlock command after connect
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(PinActivity.this, "Connection failed!", Toast.LENGTH_SHORT).show();
                });
                isConnected = false;
                e.printStackTrace();
            }
        }).start();
    }

    private void sendCommand(String cmd) {
        if (isConnected && outputStream != null) {
            try {
                outputStream.write(cmd.getBytes());
                if (cmd.equals("U")) {
                    Toast.makeText(this, "🔓 Door Unlocked!", Toast.LENGTH_SHORT).show();
                } else if (cmd.equals("L")) {
                    Toast.makeText(this, "🔒 Door Locked!", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Failed to send command", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (btSocket != null) {
                btSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handle Bluetooth permission results
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSIONS) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (!granted) {
                Toast.makeText(this, "Bluetooth permissions are required!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}

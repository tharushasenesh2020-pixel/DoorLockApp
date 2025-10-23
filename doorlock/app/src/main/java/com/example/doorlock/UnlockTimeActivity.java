package com.example.doorlock;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class UnlockTimeActivity extends AppCompatActivity {

    private static final String HC06_ADDRESS = "00:00:00:00:00:00"; // 🔧 Replace with your HC-06 MAC
    private static final UUID HC06_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private OutputStream outputStream;
    private boolean isConnected = false;

    private Handler handler = new Handler();

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock_time);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Button btn1 = findViewById(R.id.btn1Hour);
        Button btn2 = findViewById(R.id.btn2Hour);
        Button btn3 = findViewById(R.id.btn3Hour);
        Button btn5 = findViewById(R.id.btn5Hour);
        Button btnLockNow = findViewById(R.id.btnLockNow);

        btn1.setOnClickListener(v -> unlockForDuration(1));
        btn2.setOnClickListener(v -> unlockForDuration(2));
        btn3.setOnClickListener(v -> unlockForDuration(3));
        btn5.setOnClickListener(v -> unlockForDuration(5));

        btnLockNow.setOnClickListener(v -> sendCommand("L")); // Lock instantly
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    private void unlockForDuration(int hours) {
        long millis = hours * 60L * 60L * 1000L;
        Toast.makeText(this, "Unlocking for " + hours + " hour(s)...", Toast.LENGTH_SHORT).show();
        connectAndUnlock();

        handler.postDelayed(() -> {
            sendCommand("L");
            Toast.makeText(this, "⏰ Time expired — Door Locked!", Toast.LENGTH_LONG).show();
        }, millis);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    private void connectAndUnlock() {
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!btAdapter.isEnabled()) {
            btAdapter.enable();
            Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Connected to Door Lock", Toast.LENGTH_SHORT).show();
                    sendCommand("U");
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Connection failed!", Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void sendCommand(String cmd) {
        if (outputStream != null) {
            try {
                outputStream.write(cmd.getBytes());
                if (cmd.equals("U")) {
                    Toast.makeText(this, "🔓 Door Unlocked!", Toast.LENGTH_SHORT).show();
                } else if (cmd.equals("L")) {
                    Toast.makeText(this, "🔒 Door Locked!", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (btSocket != null) btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.example.doorlock;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private volatile boolean isConnected = false;
    private volatile boolean rxLoopRunning = false;

    private ExecutorService btExecutor = Executors.newSingleThreadExecutor();

    private static final String HC05_ADDRESS = "78:84:32:14:DE:21";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int BT_PERMISSION_REQUEST = 100;
    private static final int BT_ENABLE_REQUEST = 101;
    private static final int PAIRING_REQUEST = 102;

    TextView textView, textBtStatus;
    Button btnLogout, btnPutPin, btnCheckStatus, btnManageUsers, settings;
    Button btnBtConnect, btnBtDisconnect;

    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Firebase
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        // UI
        textView = findViewById(R.id.user_details);
        btnLogout = findViewById(R.id.logOut);
        btnPutPin = findViewById(R.id.btn_put_pin);
        btnCheckStatus = findViewById(R.id.btn_check_status);
        btnManageUsers = findViewById(R.id.btn_manage_users);
        settings = findViewById(R.id.settings);
        textBtStatus = findViewById(R.id.text_bt_status);
        btnBtConnect = findViewById(R.id.btn_bt_connect);
        btnBtDisconnect = findViewById(R.id.btn_bt_disconnect);

        if (user == null) {
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
            return;
        }

        // Admin check
        textView.setText("Logged in as: " + user.getEmail());
        isAdmin = user.getEmail() != null && user.getEmail().equalsIgnoreCase("admin@gmail.com");

        if (!isAdmin) {
            btnManageUsers.setVisibility(Button.GONE);
            settings.setVisibility(Button.GONE);
        }

        // Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
        });

        // Open screens
        btnPutPin.setOnClickListener(v -> openScreen(PinActivity.class, "Unlock Time screen"));
        btnCheckStatus.setOnClickListener(v -> openScreen(StatusActivity.class, "Status screen"));
        btnManageUsers.setOnClickListener(v -> openScreen(ManageUsersActivity.class, "Manage Users screen"));
        settings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, settings.class)));

        // Bluetooth setup
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported!", Toast.LENGTH_LONG).show();
        }

        checkAndRequestBluetoothPermissions();

        btnBtConnect.setOnClickListener(v -> connectToBluetooth());
        btnBtDisconnect.setOnClickListener(v -> disconnectBluetooth());

        updateBtButtons();
    }

    private void openScreen(Class<?> targetActivity, String screenName) {
        startActivity(new Intent(MainActivity.this, targetActivity));
        SharedPreferences prefs = getSharedPreferences("LockSettings", MODE_PRIVATE);
        String usageLog = prefs.getString("usage_log", "");
        prefs.edit().putString("usage_log",
                usageLog + "Accessed " + screenName + " at " + LocalDateTime.now() + ";").apply();
    }

    private void checkAndRequestBluetoothPermissions() {
        ArrayList<String> req = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            req.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            req.add(Manifest.permission.BLUETOOTH_SCAN);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            req.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (!req.isEmpty())
            ActivityCompat.requestPermissions(this, req.toArray(new String[0]), BT_PERMISSION_REQUEST);
    }

    private void connectToBluetooth() {
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestBluetoothPermissions();
            return;
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, BT_ENABLE_REQUEST);
            return;
        }

        // Disable button to prevent spam
        runOnUiThread(() -> btnBtConnect.setEnabled(false));

        final ProgressBarDialog dlg = ProgressBarDialog.show(this, "Connecting to HC-05");

        btExecutor.execute(() -> {
            BluetoothDevice device = null;
            boolean connected = false;
            try {
                device = btAdapter.getRemoteDevice(HC05_ADDRESS);

                // Check bond state
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    runOnUiThread(() -> {
                        dlg.dismiss();
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Pair HC-05?")
                                .setMessage("Device not paired. Please pair manually in Settings > Bluetooth, then retry.")
                                .setPositiveButton("OK", (d, which) -> {
                                    // No action needed, just dismiss
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        btnBtConnect.setEnabled(true);
                    });
                    return;
                }

                btAdapter.cancelDiscovery();
                Thread.sleep(500); // Brief delay for cancel to propagate

                // Try secure connect
                btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                btSocket.connect();
                connected = true;
            } catch (IOException | InterruptedException e) {
                Log.w("BT", "Secure connect failed, trying insecure", e);
                safeClose(btSocket);

                try {
                    // Try insecure connect
                    btSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                    btSocket.connect();
                    connected = true;
                } catch (IOException insecureE) {
                    Log.w("BT", "Insecure connect failed, trying UUID fallback", insecureE);
                    safeClose(btSocket);

                    try {
                        // Fallback: Use device's first UUID
                        ParcelUuid[] parcelUuids = device.getUuids();
                        if (parcelUuids != null && parcelUuids.length > 0) {
                            Log.i("BT", "Trying device UUID: " + parcelUuids[0].getUuid());
                            btSocket = device.createInsecureRfcommSocketToServiceRecord(parcelUuids[0].getUuid());
                            btSocket.connect();
                            connected = true;
                        }
                    } catch (Exception fallbackE) {
                        Log.e("BT", "All connect attempts failed", fallbackE);
                    }
                }
            }

            if (connected && btSocket != null) {
                try {
                    outputStream = btSocket.getOutputStream();
                    inputStream = btSocket.getInputStream();
                    isConnected = true;

                    startRxLoop();

                    BluetoothDevice finalDevice = device;
                    runOnUiThread(() -> {
                        dlg.dismiss();
                        try {
                            String name = finalDevice.getName() != null ? finalDevice.getName() : "HC-05";
                            textBtStatus.setText("Connected to " + name);
                            Toast.makeText(MainActivity.this, "Connected to HC-05", Toast.LENGTH_SHORT).show();
                        } catch (Exception nameE) {
                            textBtStatus.setText("Connected (name unavailable)");
                        }
                        updateBtButtons();
                    });
                } catch (IOException ioE) {
                    Log.e("BT", "Failed to get streams", ioE);
                    connected = false;
                }
            }

            if (!connected) {
                isConnected = false;
                safeClose(outputStream);
                safeClose(inputStream);
                safeClose(btSocket);
                outputStream = null;
                inputStream = null;
                btSocket = null;

                runOnUiThread(() -> {
                    dlg.dismiss();
                    textBtStatus.setText("Bluetooth: Not connected");
                    Toast.makeText(MainActivity.this, "Connection failed. Check pairing/power.", Toast.LENGTH_LONG).show();
                    btnBtConnect.setEnabled(true);
                    updateBtButtons();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BT_ENABLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                connectToBluetooth();
            } else {
                Toast.makeText(this, "BT enable canceled", Toast.LENGTH_SHORT).show();
                btnBtConnect.setEnabled(true);
            }
        } else if (requestCode == PAIRING_REQUEST) {
            connectToBluetooth(); // Retry after manual pair
        }
    }

    private void updateBtButtons() {
        runOnUiThread(() -> {
            btnBtConnect.setEnabled(!isConnected);
            btnBtDisconnect.setEnabled(isConnected);
        });
    }

    private void startRxLoop() {
        if (inputStream == null || rxLoopRunning) return;
        rxLoopRunning = true;
        btExecutor.execute(() -> {
            byte[] buf = new byte[256];
            StringBuilder line = new StringBuilder();
            try {
                while (rxLoopRunning) {
                    int len = inputStream.read(buf);
                    if (len == -1) break;
                    for (int i = 0; i < len; i++) {
                        char c = (char) buf[i];
                        if (c == '\n') {
                            final String msg = line.toString().trim();
                            line.setLength(0);
                            runOnUiThread(() ->
                                    Toast.makeText(MainActivity.this, "RX: " + msg, Toast.LENGTH_SHORT).show());
                        } else {
                            line.append(c);
                        }
                    }
                }
            } catch (IOException e) {
                Log.w("BT", "RX loop ended", e);
            } finally {
                rxLoopRunning = false;
            }
        });
    }

    private void disconnectBluetooth() {
        rxLoopRunning = false;
        isConnected = false;
        safeClose(outputStream);
        safeClose(inputStream);
        safeClose(btSocket);
        outputStream = null;
        inputStream = null;
        btSocket = null;
        runOnUiThread(() -> {
            textBtStatus.setText("Bluetooth: Disconnected");
            updateBtButtons();
        });
    }

    public void sendCommand(String cmd) {
        if (!isConnected || outputStream == null) {
            Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        btExecutor.execute(() -> {
            try {
                outputStream.write((cmd + "\n").getBytes());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Command sent: " + cmd, Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e("BT", "Send failed", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnectBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();
        btExecutor.shutdownNow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BT_PERMISSION_REQUEST) {
            boolean allGranted = true;
            for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) allGranted = false;
            Toast.makeText(this, allGranted ? "Bluetooth permissions granted" : "Bluetooth permissions denied",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void safeClose(Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) {}
    }

    private void safeClose(BluetoothSocket s) {
        if (s != null) try { s.close(); } catch (IOException ignored) {}
    }

    public static final class ProgressBarDialog extends Dialog {
        public static ProgressBarDialog show(Context ctx, String title) {
            ProgressBarDialog d = new ProgressBarDialog(ctx);
            d.setTitle(title);
            d.setCancelable(false);
            d.show();
            return d;
        }
        private ProgressBarDialog(Context ctx) { super(ctx); setContentView(new ProgressBar(ctx)); }
    }
}
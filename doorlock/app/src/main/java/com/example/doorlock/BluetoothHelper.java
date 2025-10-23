package com.example.doorlock;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;
import android.content.Context;

import androidx.annotation.RequiresPermission;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothHelper {
    private static BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothSocket btSocket;
    private static OutputStream outputStream;
    private static boolean isConnected = false;

    private static final String HC06_ADDRESS = "00:00:00:00:00:00"; // Replace with your HC-06 MAC address
    private static final UUID HC06_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    public static boolean connect(Context context) {
        if (btAdapter == null) {
            Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!btAdapter.isEnabled()) {
            Toast.makeText(context, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            BluetoothDevice device = btAdapter.getRemoteDevice(HC06_ADDRESS);
            btSocket = device.createRfcommSocketToServiceRecord(HC06_UUID);
            btAdapter.cancelDiscovery();
            btSocket.connect();
            outputStream = btSocket.getOutputStream();
            isConnected = true;
            Toast.makeText(context, "Connected to HC-06", Toast.LENGTH_SHORT).show();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Connection failed", Toast.LENGTH_SHORT).show();
            isConnected = false;
            return false;
        }
    }

    public static void disconnect(Context context) {
        try {
            if (btSocket != null) {
                btSocket.close();
                isConnected = false;
                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error while disconnecting", Toast.LENGTH_SHORT).show();
        }
    }

    public static void sendCommand(Context context, String cmd) {
        if (isConnected && outputStream != null) {
            try {
                outputStream.write(cmd.getBytes());
                Toast.makeText(context, "Command sent: " + cmd, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "Failed to send command", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isConnected() {
        return isConnected;
    }
}


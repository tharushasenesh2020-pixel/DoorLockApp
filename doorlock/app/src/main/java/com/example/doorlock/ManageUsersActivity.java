package com.example.doorlock;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;

public class ManageUsersActivity extends AppCompatActivity {

    private ListView listUsers, listAccess, listPinChanges, listUsage;
    private SharedPreferences prefs;
    private ArrayList<String> usersList;
    private ArrayAdapter<String> usersAdapter;
    private boolean isAdmin = false; // flag to check admin

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        listUsers = findViewById(R.id.list_users);
        listAccess = findViewById(R.id.list_access);
        listPinChanges = findViewById(R.id.list_pin_changes);
        listUsage = findViewById(R.id.list_usage);

        prefs = getSharedPreferences("LockSettings", MODE_PRIVATE);

        // === Check if current user is admin ===
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            // You can change this to your real admin email
            if (currentUser.getEmail().equalsIgnoreCase("admin@gmail.com")) {
                isAdmin = true;
            }
        }

        // Load and show user list
        loadUsers();

        // Load and show logs
        showHistory("access_log", listAccess);
        showHistory("pin_change_log", listPinChanges);
        showHistory("usage_log", listUsage);

        // Handle user deletion (only admin can do this)
        listUsers.setOnItemLongClickListener((parent, view, position, id) -> {
            String user = usersList.get(position);
            if (user.equals("No users yet")) return true;

            if (!isAdmin) {
                Toast.makeText(this, "Only admin can delete users", Toast.LENGTH_SHORT).show();
                return true;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete User")
                    .setMessage("Are you sure you want to delete '" + user + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteUser(user))
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });
    }

    private void loadUsers() {
        String data = prefs.getString("user_list", "");
        if (data.isEmpty()) {
            usersList = new ArrayList<>();
            usersList.add("No users yet");
        } else {
            usersList = new ArrayList<>(Arrays.asList(data.split(";")));
        }

        usersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, usersList);
        listUsers.setAdapter(usersAdapter);
    }

    private void deleteUser(String username) {
        usersList.remove(username);

        SharedPreferences.Editor editor = prefs.edit();
        if (usersList.isEmpty()) {
            editor.remove("user_list");
            usersList.add("No users yet");
        } else {
            editor.putString("user_list", String.join(";", usersList));
        }
        editor.apply();

        usersAdapter.notifyDataSetChanged();
        Toast.makeText(this, "User '" + username + "' deleted", Toast.LENGTH_SHORT).show();
    }

    private void showHistory(String key, ListView listView) {
        String data = prefs.getString(key, "");
        ArrayList<String> list;

        if (data.isEmpty()) {
            list = new ArrayList<>();
            list.add("No records yet");
        } else {
            list = new ArrayList<>(Arrays.asList(data.split(";")));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
    }
}

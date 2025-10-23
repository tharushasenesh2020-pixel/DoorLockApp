package com.example.doorlock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    private static final String ADMIN_EMAIL = "admin@gmail.com";

    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private ProgressBar progressBar;
    private TextView textRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar);
        textRegister = findViewById(R.id.registerNow);

        textRegister.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, Register.class));
            finish();
        });

        buttonLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Enter Email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Enter Password");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            handleLoginSuccess(user);
                        }
                    } else {
                        Toast.makeText(Login.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleLoginSuccess(FirebaseUser user) {
        String email = user.getEmail();
        boolean isAdmin = email != null && email.equalsIgnoreCase(ADMIN_EMAIL);

        // Save current user and role in SharedPreferences
        SharedPreferences prefs = getSharedPreferences("LockSettings", MODE_PRIVATE);
        prefs.edit()
                .putString("current_user", email)
                .putBoolean("is_admin", isAdmin)
                .apply();

        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

        // Start MainActivity
        Intent intent = new Intent(Login.this, MainActivity.class);
        intent.putExtra("isAdmin", isAdmin);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-login if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            handleLoginSuccess(currentUser);
        }
    }
}

package com.example.mesroptest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mesroptest.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "login_pref";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Check if user is already logged in and remembered
        if (sharedPreferences.getBoolean(KEY_REMEMBER_ME, false) && auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Restore saved email if exists
        String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
        if (!savedEmail.isEmpty()) {
            binding.emailEditText.setText(savedEmail);
            binding.rememberMeCheckbox.setChecked(true);
        }

        binding.loginButton.setOnClickListener(v -> loginUser());
        binding.registerButton.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loginButton.setEnabled(false);

        Log.d("DEBUG", "Email: " + email);
        Log.d("DEBUG", "Password: " + password);


        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.loginButton.setEnabled(true);
                    try {
                        if (task.isSuccessful()) {
                            // Успешный вход
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(KEY_REMEMBER_ME, binding.rememberMeCheckbox.isChecked());
                            if (binding.rememberMeCheckbox.isChecked()) {
                                editor.putString(KEY_EMAIL, email);
                            } else {
                                editor.remove(KEY_EMAIL);
                            }
                            editor.apply();

                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Ошибка входа: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка приложения: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
//                    if (task.isSuccessful()) {
//                        // Save preferences if "Remember Me" is checked
//                        SharedPreferences.Editor editor = sharedPreferences.edit();
//                        editor.putBoolean(KEY_REMEMBER_ME, binding.rememberMeCheckbox.isChecked());
//                        if (binding.rememberMeCheckbox.isChecked()) {
//                            editor.putString(KEY_EMAIL, email);
//                        } else {
//                            editor.remove(KEY_EMAIL);
//                        }
//                        editor.apply();
//
//                        startActivity(new Intent(this, MainActivity.class));
//                        finish();
//                    } else {
//                        Toast.makeText(this, "Authentication failed: " +
//                               task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                    }
                });
    }
}

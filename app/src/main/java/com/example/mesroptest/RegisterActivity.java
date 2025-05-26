package com.example.mesroptest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mesroptest.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.registerButton.setOnClickListener(v -> registerUser());
        binding.loginButton.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = binding.nameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String repeatPassword = binding.repeatPasswordEditText.getText().toString().trim();

        // Validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.setError("Please enter a valid email");
            return;
        }

        if (password.length() < 6) {
            binding.passwordEditText.setError("Password should be at least 6 characters");
            return;
        }

        if (!password.equals(repeatPassword)) {
            binding.repeatPasswordEditText.setError("Passwords do not match");
            return;
        }

        binding.registerButton.setEnabled(false);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Update user profile with name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();

                        auth.getCurrentUser().updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    binding.registerButton.setEnabled(true);
                                    if (profileTask.isSuccessful()) {
                                        // Send verification email after registration
                                        sendVerificationEmail();  // Added this line

                                        startActivity(new Intent(this, MainActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Failed to update profile",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        binding.registerButton.setEnabled(true);
                        Toast.makeText(this, "Registration failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to send verification email to the user after successful registration
    private void sendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}


//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Patterns;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.mesroptest.databinding.ActivityRegisterBinding;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.auth.UserProfileChangeRequest;
//public class RegisterActivity extends AppCompatActivity {
//    private ActivityRegisterBinding binding;
//    private FirebaseAuth auth;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
//
//        auth = FirebaseAuth.getInstance();
//
//        binding.registerButton.setOnClickListener(v -> registerUser());
//        binding.loginButton.setOnClickListener(v -> finish());
//    }
//
//    private void sendVerificationEmail() {
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        if (user != null) {
//            user.sendEmailVerification()
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful()) {
//                            Toast.makeText(RegisterActivity.this, "Verification email sent!", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(RegisterActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//        }
//    }
//
//    private void registerUser() {
//        String name = binding.nameEditText.getText().toString().trim();
//        String email = binding.emailEditText.getText().toString().trim();
//        String password = binding.passwordEditText.getText().toString().trim();
//        String repeatPassword = binding.repeatPasswordEditText.getText().toString().trim();
//
//        // Validation
//        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
//            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//            binding.emailEditText.setError("Please enter a valid email");
//            return;
//        }
//
//        if (password.length() < 6) {
//            binding.passwordEditText.setError("Password should be at least 6 characters");
//            return;
//        }
//
//        if (!password.equals(repeatPassword)) {
//            binding.repeatPasswordEditText.setError("Passwords do not match");
//            return;
//        }
//
//        binding.registerButton.setEnabled(false);
//
//        auth.createUserWithEmailAndPassword(email, password)
//                .addOnCompleteListener(this, task -> {
//                    if (task.isSuccessful()) {
//                        // Update user profile with name
//                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
//                                .setDisplayName(name)
//                                .build();
//
//                        auth.getCurrentUser().updateProfile(profileUpdates)
//                                .addOnCompleteListener(profileTask -> {
//                                    binding.registerButton.setEnabled(true);
//                                    if (profileTask.isSuccessful()) {
//
//                                        startActivity(new Intent(this, MainActivity.class));
//                                        finish();
//                                    } else {
//                                        Toast.makeText(this, "Failed to update profile",
//                                                Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                    } else {
//                        binding.registerButton.setEnabled(true);
//                        Toast.makeText(this, "Registration failed: " +
//                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
//}

package com.example.mesroptest.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mesroptest.LoginActivity;
import com.example.mesroptest.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();

        setupUserInfo();
        setupLogoutButton();

        return binding.getRoot();
    }

    private void setupUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            binding.nameTextView.setText(user.getDisplayName());
            binding.emailTextView.setText(user.getEmail());
        }
    }

    private void setupLogoutButton() {
        binding.logoutButton.setOnClickListener(v -> {
            auth.signOut();
            // Clear shared preferences
            requireActivity().getSharedPreferences("login_pref", 0)
                    .edit()
                    .clear()
                    .apply();
            // Navigate to login screen
            startActivity(new Intent(requireActivity(), LoginActivity.class));
            requireActivity().finish();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

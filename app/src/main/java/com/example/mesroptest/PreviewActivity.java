package com.example.mesroptest;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mesroptest.databinding.ActivityPreviewBinding;
public class PreviewActivity extends AppCompatActivity {
    private ActivityPreviewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            binding.imageView.setImageURI(imageUri);
        } else {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            finish();
        }

        binding.backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

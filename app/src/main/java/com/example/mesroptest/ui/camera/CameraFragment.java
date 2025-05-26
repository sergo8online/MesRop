package com.example.mesroptest.ui.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mesroptest.R;
import com.example.mesroptest.data.Note;
import com.example.mesroptest.databinding.FragmentCameraBinding;
import com.example.mesroptest.ui.notes.NotesViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    private FragmentCameraBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCameraBinding.inflate(inflater, container, false);

        // Запускаем камеру, если есть разрешения
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Кнопка сделать фото
        binding.captureButton.setOnClickListener(v -> takePhoto());

        // Кнопка открыть галерею
        binding.galleryButton.setOnClickListener(v -> openGallery());

        return binding.getRoot();
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Не удалось запустить камеру", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(requireContext().getExternalCacheDir(),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".png");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                Uri savedUri = Uri.fromFile(photoFile);
                requireActivity().runOnUiThread(() -> processImageForPrediction(savedUri));
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Ошибка при съёмке фото", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Новый ActivityResultLauncher для выбора изображения из галереи
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            File imageFile = createFileFromUri(selectedImageUri);
                            processImageForPrediction(Uri.fromFile(imageFile));
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(requireContext(), "Ошибка обработки выбранного изображения", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private File createFileFromUri(Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
        File tempFile = new File(requireContext().getCacheDir(), "selected_image.png");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        return tempFile;
    }

    private void processImageForPrediction(Uri imageUri) {
        try {
            File imageFile = new File(imageUri.getPath());
            uploadImageToServer(imageFile);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToServer(File imageFile) {
        new Thread(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                File highQualityFile = new File(requireContext().getCacheDir(), "high_quality_image.png");
                try (FileOutputStream out = new FileOutputStream(highQualityFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }

                Log.d("ImageUpload", "Изображение сохранено: " + highQualityFile.getAbsolutePath());
                Log.d("ImageUpload", "Размер файла: " + highQualityFile.length() + " байт");

                String boundary = "Boundary-" + System.currentTimeMillis();
                URL url = new URL("https://mesrop-server-eng.onrender.com/predict");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream outputStream = connection.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"high_quality_image.png\"\r\n");
                writer.append("Content-Type: image/png\r\n\r\n");
                writer.flush();

                try (FileInputStream inputStream = new FileInputStream(highQualityFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }

                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--").append("\r\n");
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();
                Log.d("ImageUpload", "Ответ сервера: код " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    String responseText = response.toString();
                    Log.d("ImageUpload", "Ответ: " + responseText);
                    String prediction = parsePredictionFromJson(responseText);

                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getActivity(), "Распознано: " + prediction, Toast.LENGTH_LONG).show();
                            savePredictionToNotes(prediction);
                        });
                    } else {
                        Log.w("ImageUpload", "Фрагмент больше не активен — не обновляем UI.");
                    }
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    Log.e("ImageUpload", "Ошибка сервера: " + errorResponse.toString());

                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show());
                    }
                }

            } catch (Exception e) {
                Log.e("ImageUpload", "Ошибка при отправке изображения", e);

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Ошибка отправки: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }


//    private void uploadImageToServer(File imageFile) {
//        new Thread(() -> {
//            try {
//                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
//
//                File highQualityFile = new File(requireContext().getCacheDir(), "high_quality_image.png");
//                try (FileOutputStream out = new FileOutputStream(highQualityFile)) {
//                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//                }
//
//                Log.d("ImageUpload", "Изображение сохранено: " + highQualityFile.getAbsolutePath());
//                Log.d("ImageUpload", "Размер файла: " + highQualityFile.length() + " байт");
//
//                String boundary = "Boundary-" + System.currentTimeMillis();
//                URL url = new URL("https://185b-35-221-204-58.ngrok-free.app/predict");
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.setRequestMethod("POST");
//                connection.setDoOutput(true);
//                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
//
//                OutputStream outputStream = connection.getOutputStream();
//                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
//
//                writer.append("--").append(boundary).append("\r\n");
//                writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"high_quality_image.png\"\r\n");
//                writer.append("Content-Type: image/png\r\n\r\n");
//                writer.flush();
//
//                try (FileInputStream inputStream = new FileInputStream(highQualityFile)) {
//                    byte[] buffer = new byte[4096];
//                    int bytesRead;
//                    while ((bytesRead = inputStream.read(buffer)) != -1) {
//                        outputStream.write(buffer, 0, bytesRead);
//                    }
//                    outputStream.flush();
//                }
//
//                writer.append("\r\n").flush();
//                writer.append("--").append(boundary).append("--").append("\r\n");
//                writer.close();
//
//                int responseCode = connection.getResponseCode();
//                Log.d("ImageUpload", "Ответ сервера: код " + responseCode);
//
//                if (responseCode == HttpURLConnection.HTTP_OK) {
//                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    StringBuilder response = new StringBuilder();
//                    String line;
//                    while ((line = in.readLine()) != null) {
//                        response.append(line);
//                    }
//                    in.close();
//
//                    String responseText = response.toString();
//                    Log.d("ImageUpload", "Ответ: " + responseText);
//                    String prediction = parsePredictionFromJson(responseText);
//
//                    requireActivity().runOnUiThread(() -> {
//                        Toast.makeText(requireContext(), "Распознано: " + prediction, Toast.LENGTH_LONG).show();
//                        savePredictionToNotes(prediction);
//                    });
//                } else {
//                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    StringBuilder errorResponse = new StringBuilder();
//                    String errorLine;
//                    while ((errorLine = errorReader.readLine()) != null) {
//                        errorResponse.append(errorLine);
//                    }
//                    errorReader.close();
//
//                    Log.e("ImageUpload", "Ошибка сервера: " + errorResponse.toString());
//
//                    requireActivity().runOnUiThread(() ->
//                            Toast.makeText(requireContext(), "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show());
//                }
//
//            } catch (Exception e) {
//                Log.e("ImageUpload", "Ошибка при отправке изображения", e);
//                requireActivity().runOnUiThread(() ->
//                        Toast.makeText(requireContext(), "Ошибка отправки: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }

    private String parsePredictionFromJson(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.getString("result"); // или "prediction" в зависимости от API
        } catch (JSONException e) {
            e.printStackTrace();
            return "Ошибка";
        }
    }

    private void savePredictionToNotes(String letter) {
        NotesViewModel viewModel = new ViewModelProvider(requireActivity()).get(NotesViewModel.class);
        Note newNote = new Note("Prediction", letter);
        viewModel.insert(newNote);

        Toast.makeText(requireContext(), "Буква сохранена: " + letter, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(requireContext(), "Нет разрешений на камеру", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        binding = null;
    }
}

//
//import static androidx.core.content.ContentProviderCompat.requireContext;
//
//import android.Manifest;
//import android.app.Activity;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.content.res.AssetFileDescriptor;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Color;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.annotation.NonNull;
//import androidx.camera.core.CameraSelector;
//import androidx.camera.core.ImageCapture;
//import androidx.camera.core.ImageCaptureException;
//import androidx.camera.core.Preview;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.fragment.app.Fragment;
//import androidx.lifecycle.ViewModelProvider;
//
//import com.example.mesroptest.R;
//import com.example.mesroptest.TFLiteModel;
//import com.example.mesroptest.data.Note;
//import com.example.mesroptest.databinding.FragmentCameraBinding;
//import com.example.mesroptest.ui.notes.NotesViewModel;
//import com.google.common.util.concurrent.ListenableFuture;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.tensorflow.lite.Interpreter;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
//import java.text.SimpleDateFormat;
//import java.util.Locale;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//
//public class CameraFragment extends Fragment {
//    private TFLiteModel tfliteModel;
//
//    private FragmentCameraBinding binding;
//    private ImageCapture imageCapture;
//    private ExecutorService cameraExecutor;
//    private static final int REQUEST_CODE_PERMISSIONS = 10;
//    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
//    private static final String MODEL_PATH = "MesRop_model.tflite";
//    private Interpreter tfliteInterpreter;
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        tfliteModel = new TFLiteModel(requireContext().getAssets());
//
//        binding.galleryButton.setOnClickListener(v -> openGallery());
//
//
//        binding = FragmentCameraBinding.inflate(inflater, container, false);
//
//        if (allPermissionsGranted()) {
//            startCamera();
//        } else {
//            ActivityCompat.requestPermissions(requireActivity(),
//                    REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
//        }
//
//        binding.captureButton.setOnClickListener(v -> takePhoto());
//        cameraExecutor = Executors.newSingleThreadExecutor();
//
//        return binding.getRoot();
//    }
//
//
//    private MappedByteBuffer loadModelFile() throws IOException {
//        AssetFileDescriptor fileDescriptor = requireActivity().getAssets().openFd("MesRop_model.tflite");
//        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = fileDescriptor.getStartOffset();
//        long declaredLength = fileDescriptor.getDeclaredLength();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//    }
//
//    private void startCamera() {
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
//                ProcessCameraProvider.getInstance(requireContext());
//
//        cameraProviderFuture.addListener(() -> {
//            try {
//                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//
//                Preview preview = new Preview.Builder().build();
//                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
//
//                imageCapture = new ImageCapture.Builder()
//                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                        .build();
//
//                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
//
//                cameraProvider.unbindAll();
//                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
//
//            } catch (Exception e) {
//                Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show();
//            }
//        }, ContextCompat.getMainExecutor(requireContext()));
//    }
//
//    private void takePhoto() {
//        if (imageCapture == null) return;
//
//        File photoFile = new File(requireContext().getExternalCacheDir(),
//                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
//                        .format(System.currentTimeMillis()) + ".png");
//
//        ImageCapture.OutputFileOptions outputOptions =
//                new ImageCapture.OutputFileOptions.Builder(photoFile).build();
//
//        imageCapture.takePicture(outputOptions, cameraExecutor,
//                new ImageCapture.OnImageSavedCallback() {
//                    @Override
//                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
//                        Uri savedUri = Uri.fromFile(photoFile);
//                        requireActivity().runOnUiThread(() -> {
//                            processImageForPrediction(savedUri);
//                        });
//                    }
//
//                    @Override
//                    public void onError(@NonNull ImageCaptureException exception) {
//                        requireActivity().runOnUiThread(() ->
//                                Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show());
//                    }
//                });
//    }
//
//    private void savePredictionToNotes(String letter) {
//        NotesViewModel viewModel = new ViewModelProvider(requireActivity()).get(NotesViewModel.class);
//        Note newNote = new Note("Prediction", letter);
//        viewModel.insert(newNote);
//
//        Toast.makeText(requireContext(), "Letter saved: " + letter, Toast.LENGTH_SHORT).show();
//    }
//
//
//    private String parsePredictionFromJson(String json) {
//        try {
//            JSONObject jsonObject = new JSONObject(json);
//            return jsonObject.getString("result"); // замените на "prediction", если надо
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return "Ошибка";
//        }
//    }
//
//
//    private void uploadImageToServer(File imageFile) {
//        new Thread(() -> {
//            try {
//                // Просто загружаем и сохраняем исходное изображение с более высоким качеством
//                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
//
//                // Сохраняем изображение в формате PNG без сжатия
//                File highQualityFile = new File(requireContext().getCacheDir(), "high_quality_image.png");
//                FileOutputStream out = new FileOutputStream(highQualityFile);
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//                out.close();
//
//                // Логируем для отладки
//                Log.d("ImageUpload", "Изображение сохранено: " + highQualityFile.getAbsolutePath());
//                Log.d("ImageUpload", "Размер файла: " + highQualityFile.length() + " байт");
//
//                // Создаем и настраиваем HTTP-соединение
//                String boundary = "Boundary-" + System.currentTimeMillis();
//                URL url = new URL("https://9d15-35-221-204-58.ngrok-free.app/predict");
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.setRequestMethod("POST");
//                connection.setDoOutput(true);
//                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
//
//                // Открываем поток для отправки данных
//                OutputStream outputStream = connection.getOutputStream();
//                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
//
//                // Формируем заголовки multipart запроса
//                writer.append("--").append(boundary).append("\r\n");
//                writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"high_quality_image.png\"\r\n");
//                writer.append("Content-Type: image/png\r\n\r\n");
//                writer.flush();
//
//                // Отправляем содержимое изображения
//                FileInputStream inputStream = new FileInputStream(highQualityFile);
//                byte[] buffer = new byte[4096];
//                int bytesRead;
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    outputStream.write(buffer, 0, bytesRead);
//                }
//                outputStream.flush();
//                inputStream.close();
//
//                // Закрываем multipart запрос
//                writer.append("\r\n").flush();
//                writer.append("--").append(boundary).append("--").append("\r\n");
//                writer.close();
//
//                // Обрабатываем ответ сервера
//                int responseCode = connection.getResponseCode();
//                Log.d("ImageUpload", "Ответ сервера: код " + responseCode);
//
//                if (responseCode == HttpURLConnection.HTTP_OK) {
//                    // Читаем ответ от сервера
//                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    StringBuilder response = new StringBuilder();
//                    String line;
//                    while ((line = in.readLine()) != null) {
//                        response.append(line);
//                    }
//                    in.close();
//
//                    // Парсим результат из JSON
//                    String responseText = response.toString();
//                    Log.d("ImageUpload", "Ответ: " + responseText);
//                    String prediction = parsePredictionFromJson(responseText);
//
//                    // Обновляем UI в основном потоке
//                    requireActivity().runOnUiThread(() -> {
//                        Toast.makeText(requireContext(), "Распознано: " + prediction, Toast.LENGTH_LONG).show();
//                        savePredictionToNotes(prediction);
//                    });
//                } else {
//                    // Обрабатываем ошибку
//                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                    StringBuilder errorResponse = new StringBuilder();
//                    String errorLine;
//                    while ((errorLine = errorReader.readLine()) != null) {
//                        errorResponse.append(errorLine);
//                    }
//                    errorReader.close();
//
//                    Log.e("ImageUpload", "Ошибка сервера: " + errorResponse.toString());
//
//                    requireActivity().runOnUiThread(() ->
//                            Toast.makeText(requireContext(), "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show());
//                }
//
//            } catch (Exception e) {
//                Log.e("ImageUpload", "Ошибка при отправке изображения", e);
//                requireActivity().runOnUiThread(() ->
//                        Toast.makeText(requireContext(), "Ошибка отправки: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }
//
//
//
//    private void processImageForPrediction(Uri imageUri) {
//        try {
//            File imageFile = new File(imageUri.getPath());
//            uploadImageToServer(imageFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(requireContext(), "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private static final int PICK_IMAGE_REQUEST = 1;
//    private void openGallery() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        intent.setType("image/*");
//        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
//    }
//
//    private File createFileFromUri(Uri uri) throws IOException {
//        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
//        File tempFile = new File(requireContext().getCacheDir(), "selected_image.png");
//        FileOutputStream out = new FileOutputStream(tempFile);
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//        out.close();
//        return tempFile;
//    }
//
//    private ActivityResultLauncher<Intent> galleryLauncher;
//
//
//
//
//
//
//
//
//
//
//    private boolean allPermissionsGranted() {
//        for (String permission : REQUIRED_PERMISSIONS) {
//            if (ContextCompat.checkSelfPermission(requireContext(), permission)
//                    != PackageManager.PERMISSION_GRANTED) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == CameraFragment.REQUEST_CODE_PERMISSIONS) {
//            if (allPermissionsGranted()) {
//                startCamera();
//            } else {
//                Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        cameraExecutor.shutdown();
//        binding = null;
//    }
//}
package com.example.mesroptest;


import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import org.tensorflow.lite.Interpreter;
import java.util.Arrays;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFLiteModel {

    //private Interpreter interpreter;
    private Interpreter tfliteInterpreter;

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public TFLiteModel(AssetManager assetManager) {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(assetManager, "MesRop_model.tflite");
            tfliteInterpreter = new Interpreter(modelBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            tfliteInterpreter = null;
        }
    }


//    public TFLiteModel(AssetManager assetManager) {
//        try {
//            AssetFileDescriptor fileDescriptor = assetManager.openFd("MesRop_model.tflite");
//            FileInputStream inputStream = fileDescriptor.createInputStream();
//            FileChannel fileChannel = inputStream.getChannel();
//            MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
//
//            interpreter = new Interpreter(modelBuffer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public float[] predict(float[][][][] input) {
        if (tfliteInterpreter == null){
            throw new IllegalStateException("TFLite Interpreter is not initialized!");
        }
        float[][] output = new float[1][78];  // 78 классов
        tfliteInterpreter.run(input, output);
        System.out.println("Predictions: " + Arrays.toString(output[0]));
        return output[0];
    }


//    public float[] predict(float[] input) {
//        float[][] output = new float[1][78];
//        interpreter.run(input, output);
//        return output[0];
//    }

    public void close() {
        tfliteInterpreter.close();
    }
}

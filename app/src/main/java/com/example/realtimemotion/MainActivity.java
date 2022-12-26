package com.example.realtimemotion;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.realtimemotion.databinding.ActivityMainBinding;
import com.example.realtimemotion.ml.BasicModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private BasicModel model;
    private MotionBuffer motionBuffer = new MotionBuffer(400);
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float PROBABILITY_THRESHOLD = 0.90f;
    private ActivityMainBinding binding;

    private long consecutiveStaticSamples = 0;
    private boolean isStatic = false;
    private long IS_STATIC_THRESHOLD = 100;
    private long STATIC_SAMPLE_THRESHOLD = 5;
    private long MAX_SAMPLES_THRESHOLD = 200;
    private long detectedSamples = 0;
    private boolean isDetecting = false;

    private int COLOR_STATIC = Color.YELLOW;
    private int COLOR_CHAOTIC = Color.GREEN;
    private int COLOR_DETECTING = Color.RED;

    ArrayList<String> labels = new ArrayList<>(Arrays.asList(
            "nothing",
            "x_negative",
            "x_positive",
            "y_negative",
            "y_positive",
            "z_negative",
            "z_positive"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        try {
            model = BasicModel.newInstance(this);
        }
        catch (Exception e) {
            Toast.makeText(this, "Model creation error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        motionBuffer.addToMemory(new float[]{x, y, z});

        if (x <= STATIC_SAMPLE_THRESHOLD && y <= STATIC_SAMPLE_THRESHOLD && z <= STATIC_SAMPLE_THRESHOLD) {
            consecutiveStaticSamples++;
        }
        else if (isStatic) {
            binding.mainLayout.setBackgroundColor(COLOR_DETECTING);
            consecutiveStaticSamples = 0;
            isDetecting = true;
            isStatic = false;
        }
        else if (!isDetecting){
                binding.mainLayout.setBackgroundColor(COLOR_CHAOTIC);
                consecutiveStaticSamples = 0;
        }

        if (consecutiveStaticSamples >= IS_STATIC_THRESHOLD) {
            binding.mainLayout.setBackgroundColor(COLOR_STATIC);
            isStatic = true;
            if (isDetecting) {
                detectedSamples = 0;
                isDetecting = false;
                predict();
                motionBuffer.clear();
            }
        }

        if (isDetecting) {
            detectedSamples++;
        }

        if (detectedSamples > MAX_SAMPLES_THRESHOLD) {
            // ignore what was recorded
            detectedSamples = 0;
            isDetecting = false;
            motionBuffer.clear();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void predict() {
        Motion motion = motionBuffer.getMotion();
        motion.crop(motion.getGlobalExtremumPosition(), 120/2);

        float[] flattenedInput = new float[1 * 1 * 120 * 3];

        float[][] motionArray = motion.getRecordedSamples();
        int flattenedIndex = 0;
        for (int i = 0; i < 120; i++) {
            flattenedInput[flattenedIndex++] = motionArray[i][0];
            flattenedInput[flattenedIndex++] = motionArray[i][1];
            flattenedInput[flattenedIndex++] = motionArray[i][2];
        }

        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 1, 120, 3}, DataType.FLOAT32);
        inputFeature0.loadArray(flattenedInput);
        BasicModel.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        float[] outputArray = outputFeature0.getFloatArray();

        int mostLikelyIndex = 0;
        float highestProbability = 0;
        for (int i = 0; i < labels.size(); i++) {
            if (outputArray[i] > highestProbability) {
                mostLikelyIndex = i;
                highestProbability = outputArray[i];
            }
        }

        if (labels.get(mostLikelyIndex).equals("nothing")) {
            return;
        }

        if (highestProbability < PROBABILITY_THRESHOLD) {
            return;
        }

        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("Gesture detected")
                .setMessage(labels.get(mostLikelyIndex))
                .setPositiveButton("Cancel", null)
                .show();
    }

}
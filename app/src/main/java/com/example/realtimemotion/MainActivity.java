package com.example.realtimemotion;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.realtimemotion.ml.BasicModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private BasicModel model;
    private long recordingPeriodMs = 700;
    private MotionBuffer motionBuffer = new MotionBuffer(1);
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float probabilityThreshold = 0.85f;
    ArrayList<String> labels = new ArrayList<>(Arrays.asList(
            "nothing",
            "x_negative",
            "x_positive",
            "y_negative",
            "y_positive",
            "z_negative",
            "z_positive"
    ));
    private Timer carouselTimer = new Timer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        try {
            model = BasicModel.newInstance(this);
        }
        catch (Exception e) {
            Toast.makeText(this, "Model creation error", Toast.LENGTH_SHORT).show();
        }
        carouselTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    predict();
                    motionBuffer.clear();
                });
            }
        }, 0, (long) recordingPeriodMs);
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

        motionBuffer.recordSample(new float[]{x, y, z});
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

        if (highestProbability < probabilityThreshold) {
            return;
        }

        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("Gesture detected")
                .setMessage(labels.get(mostLikelyIndex))
                .setPositiveButton("Cancel", null)
                .show();
    }

}
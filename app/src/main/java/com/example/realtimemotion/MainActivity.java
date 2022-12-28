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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.realtimemotion.databinding.ActivityMainBinding;
import com.example.realtimemotion.ml.BasicModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private BasicModel model;
    private MotionBuffer motionBuffer = new MotionBuffer(400);
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ActivityMainBinding binding;

    private MotionDetector motionDetector;

    private final HashMap<MotionDetector.DetectorState, Integer> stateColorMapping = new HashMap<MotionDetector.DetectorState, Integer>(){{
     put(MotionDetector.DetectorState.READY, Color.YELLOW);
     put(MotionDetector.DetectorState.NOT_READY, Color.RED);
     put(MotionDetector.DetectorState.TRIGGERED, Color.GREEN);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);

        motionDetector = new MotionDetector(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        motionDetector.unregister();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        motionDetector.registerSensorListener();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMotionDetectedEvent(MotionDetector.MotionDetectedEvent event) {
        AlertDialog dialog = new AlertDialog.Builder(this).setMessage(event.detectedMotion.toString()).setPositiveButton("OK", null).create();
        dialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMotionDetectorStateEvent(MotionDetector.MotionDetectorStateEvent event) {
        binding.mainLayout.setBackgroundColor(stateColorMapping.get(event.newState));
    }
}
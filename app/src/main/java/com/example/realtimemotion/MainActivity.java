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

public class MainActivity extends AppCompatActivity {
    private BasicModel model;
    private MotionBuffer motionBuffer = new MotionBuffer(400);
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ActivityMainBinding binding;

    private MotionDetector motionDetector;

    private int COLOR_READY = Color.YELLOW;
    private int COLOR_NOT_READY = Color.RED;
    private int COLOR_TRIGGERED = Color.GREEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);

        motionDetector = new MotionDetector(this);
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        EventBus.getDefault().unregister(this);
//        motionDetector.unregister();
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        EventBus.getDefault().register(this);
//        motionDetector.register();
//    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        motionDetector.unregister();
        Log.e("ACTIVITY", "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        motionDetector.register();
        Log.e("ACTIVITY", "onResume");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMotionDetectedEvent(MotionDetector.MotionDetectedEvent event) {
        AlertDialog dialog = new AlertDialog.Builder(this).setMessage(event.detectedMotion.toString()).setPositiveButton("OK", null).create();
        dialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMotionDetectorStateEvent(MotionDetector.MotionDetectorStateEvent event) {
        int color = COLOR_NOT_READY;
        switch (event.newState) {
            case READY:
                color = COLOR_READY;
                break;
            case TRIGGERED:
                color = COLOR_TRIGGERED;
                break;
            case NOT_READY:
                color = COLOR_NOT_READY;
                break;
        }
        binding.mainLayout.setBackgroundColor(color);
    }
}
package com.example.realtimemotion;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.realtimemotion.ml.BasicModel;

import org.greenrobot.eventbus.EventBus;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.util.ArrayList;
import java.util.EnumSet;

public class MotionDetector implements SensorEventListener {
    public enum DetectorState {READY, TRIGGERED, NOT_READY}
    public enum MotionClass {NOTHING, XNEG, XPOS, YNEG, YPOS, ZNEG, ZPOS};
    public ArrayList<MotionClass> classes = new ArrayList<>(EnumSet.allOf(MotionClass.class));
    public static class MotionDetectedEvent {

        public final MotionDetector.MotionClass detectedMotion;

        public MotionDetectedEvent(MotionDetector.MotionClass detectedMotion) {
            this.detectedMotion = detectedMotion;
        }
    }

    public static class MotionDetectorStateEvent {

        public final MotionDetector.DetectorState newState;

        public MotionDetectorStateEvent(MotionDetector.DetectorState newState) {
            this.newState = newState;
        }
    }

    private Context context;
    private BasicModel model;
    private MotionBuffer motionBuffer = new MotionBuffer(400);
    private SensorManager sensorManager;

    private float PROBABILITY_THRESHOLD = 0.70f;
    private long activeSamples = 0;
    private long consecutiveStaticSamples = 0;
    private long IS_STATIC_THRESHOLD = 50;
    private long STATIC_SAMPLE_THRESHOLD = 5;
    private long MAX_SAMPLES_THRESHOLD = 150;

    public DetectorState currentState = DetectorState.NOT_READY;

    public MotionDetector(Context context) {
        this.context = context;
        sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);

        try {
            model = BasicModel.newInstance(this.context);
        }
        catch (Exception e) {
            Toast.makeText(this.context, "Model creation error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        motionBuffer.addToMemory(new float[]{x, y, z});

        boolean aboveThreshold = false;

        if (areSamplesUnderStaticThreshold(x, y, z)) {
            consecutiveStaticSamples++;
        }
        else {
            aboveThreshold = true;
            consecutiveStaticSamples = 0;
        }

        switch (currentState) {
            case READY:
                if (aboveThreshold) {
                    changeState(DetectorState.TRIGGERED);
                    EventBus.getDefault().post(new MotionDetectorStateEvent(currentState));
                }
                break;
            case TRIGGERED:
                activeSamples++;
                if (areSamplesStaticLongEnough()) {
                    predict();
                    activeSamples = 0;
                    motionBuffer.clear();
                    changeState(DetectorState.READY);
                    EventBus.getDefault().post(new MotionDetectorStateEvent(currentState));
                }
                else if (isDetectorActiveLongEnough()) {
                    activeSamples = 0;
                    motionBuffer.clear();
                    changeState(DetectorState.NOT_READY);
                    EventBus.getDefault().post(new MotionDetectorStateEvent(currentState));
                }
                break;
            case NOT_READY:
                if (areSamplesStaticLongEnough()) {
                    motionBuffer.clear();
                    changeState(DetectorState.READY);
                    EventBus.getDefault().post(new MotionDetectorStateEvent(currentState));
                }
                break;
        }
    }


    private boolean areSamplesUnderStaticThreshold(float x, float y, float z) {
        return (Math.abs(x) <= STATIC_SAMPLE_THRESHOLD && Math.abs(y) <= STATIC_SAMPLE_THRESHOLD && Math.abs(z) <= STATIC_SAMPLE_THRESHOLD);
    }

    private boolean areSamplesStaticLongEnough() {
        return (consecutiveStaticSamples >= IS_STATIC_THRESHOLD);
    }

    private boolean isDetectorActiveLongEnough() {
        return (activeSamples >= MAX_SAMPLES_THRESHOLD);
    }

    private void changeState(DetectorState newState) {
        consecutiveStaticSamples = 0;
        currentState = newState;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    private void predict() {
        Motion motion = motionBuffer.getMotionFromMemory();
        motion.crop(motion.getGlobalExtremumPosition(), 120/2);

        float[] flattenedInput = motion.getFlattenedSamples();
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 1, 120, 3}, DataType.FLOAT32);
        inputFeature0.loadArray(flattenedInput);
        BasicModel.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
        float[] outputArray = outputFeature0.getFloatArray();

        int mostLikelyIndex = 0;
        float highestProbability = 0;
        for (int i = 0; i < classes.size(); i++) {
            if (outputArray[i] > highestProbability) {
                mostLikelyIndex = i;
                highestProbability = outputArray[i];
            }
        }

        if (classes.get(mostLikelyIndex).equals(MotionClass.NOTHING)) {
            return;
        }

        if (highestProbability < PROBABILITY_THRESHOLD) {
            return;
        }

        EventBus.getDefault().post(new MotionDetectedEvent(classes.get(mostLikelyIndex)));
    }

    public void registerSensorListener() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
        currentState = DetectorState.NOT_READY;
    }

    public void unregister() {
        sensorManager.unregisterListener(this);
    }
}



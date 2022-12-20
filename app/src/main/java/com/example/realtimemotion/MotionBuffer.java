package com.example.realtimemotion;

import java.util.ArrayList;

public class MotionBuffer {
    private int memorySize;
    private EvictingQueue<float[]> memory;
    private ArrayList<float[]> recording;

    public MotionBuffer(int memorySize) {
        memory = new EvictingQueue<>(memorySize);
        recording = new ArrayList<>();
        this.memorySize = memorySize;

        for (int i = 0; i < memorySize; i++) {
            memory.add(new float[]{0.f, 0.f, 0.f});
        }
    }

    public void addToMemory(float[] sample) {
        memory.add(sample);
    }

    public void recordSample(float[] sample) {
        recording.add(sample);
    }

    public void clear() {
        memory.clear();
        for (int i = 0; i < memorySize; i++) {
            memory.add(new float[]{0.f, 0.f, 0.f});
        }
        recording.clear();
    }

    public Motion getMotion() {
        ArrayList<float[]> combined = new ArrayList<>(memory);
        combined.addAll(recording);
        return new Motion(combined.toArray(new float[combined.size()][3]), combined.size());
    }

    public boolean isEmpty() {
        return (memory.isEmpty() && recording.isEmpty());
    }

    public EvictingQueue<float[]> getMemory() {
        return memory;
    }

    public ArrayList<float[]> getRecording() {
        return recording;
    }

}

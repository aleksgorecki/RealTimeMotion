package com.example.realtimemotion;

import java.util.ArrayList;

public class Motion {
    private float[][] recordedSamples;
    private int numSamples;

    public Motion(float[][] recordedSamples, int numSamples) {
        this.recordedSamples = recordedSamples;
        this.numSamples = numSamples;
    }

    public Motion(ArrayList<double[]> recordedSamples) {
        this.recordedSamples = recordedSamples.toArray(new float[recordedSamples.size()][3]);
        this.numSamples = recordedSamples.size();
    }

    public int getGlobalExtremumPosition() {
        int extremumPosition = 0;
        float extremumValue = 0;
        for (int i = 0; i < numSamples; i++) {
            float x = recordedSamples[i][0];
            float y = recordedSamples[i][1];
            float z = recordedSamples[i][2];
            if (x > extremumValue) {
                extremumPosition = i;
                extremumValue = x;
            }
            if (y > extremumValue) {
                extremumPosition = i;
                extremumValue = y;
            }
            if (z > extremumValue) {
                extremumPosition = i;
                extremumValue = z;
            }
        }
        return extremumPosition;
    }

    public float[][][] getThreeChannelTimeseries() {
        float[][][] multichannelArray = new float[1][numSamples][3];
        multichannelArray[0] = recordedSamples;
        return multichannelArray;
    }

    public void crop(int centerPosition, int halfSpan) {
        int lowerBound = centerPosition - halfSpan;
        int lowerPadding = 0;
        if (lowerBound < 0) {
            lowerPadding = Math.abs(lowerBound);
            lowerBound = 0;
        }
        int upperBound = centerPosition + halfSpan;
        int fullAxisLength = numSamples;
        int upperPadding = 0;
        if (upperBound >= fullAxisLength) {
            upperPadding = upperBound - fullAxisLength;
            upperBound = fullAxisLength - 1;
        }

        float[][] cropped = new float[2 * halfSpan][3];

        for (int i = 0; i < lowerPadding; i++) {
            cropped[i] = new float[] {0.0f, 0.0f, 0.0f};
        }
        for (int i = lowerBound; i < upperBound; i++) {
            for (int j = 0; j < 3; j++) {
                cropped[i - lowerBound][j] = recordedSamples[i][j];
            }
        }
        for (int i = upperBound; i < upperPadding; i++) {
            cropped[i] = new float[] {0.0f, 0.0f, 0.0f};
        }

        numSamples = 2 * halfSpan;
        recordedSamples = cropped;
    }

    public int getNumSamples() {
        return numSamples;
    }

    public ArrayList<Float>[] getSeparatedAxes() {
        ArrayList<Float> x = new ArrayList<>();
        ArrayList<Float> y = new ArrayList<>();
        ArrayList<Float> z = new ArrayList<>();
        for (int i = 0; i < numSamples; i++) {
            x.add(recordedSamples[i][0]);
            y.add(recordedSamples[i][1]);
            z.add(recordedSamples[i][2]);
        }
        return new ArrayList[]{x, y, z};
    }

    public float[][] getRecordedSamples() {
        return recordedSamples;
    }

}

package com.cellules.cordova.audiofrequency;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jtransforms.fft.DoubleFFT_1D;

public class ToneReceiver extends Thread {

    private int sampleRateInHz = 44100;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;

    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

    private AudioRecord recorder;

    private Handler handler;

    private Message message;

    private Bundle messageBundle = new Bundle();

    public ToneReceiver() {
        // use the mic with Auto Gain Control turned off
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRateInHz, channelConfig, audioFormat, bufferSize);
    }

    public ToneReceiver(int bufferSizeInBytes) {
        if (bufferSizeInBytes > bufferSize) {
            bufferSize = bufferSizeInBytes;
        }

        // use the mic with Auto Gain Control turned off
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRateInHz, channelConfig, audioFormat, bufferSize);
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        int numReadBytes = 0;
        short audioBuffer[] = new short[bufferSize];
        DoubleFFT_1D fft = new DoubleFFT_1D(bufferSize);

        synchronized(this)
        {
            recorder.startRecording();

            while (!isInterrupted()) {
                numReadBytes = recorder.read(audioBuffer, 0, bufferSize);

                if (numReadBytes > 0) {
                    // Convert samples to double
                    double[] samples = new double[bufferSize];
                    for (int i = 0; i < bufferSize; i++) {
                        samples[i] = (double) audioBuffer[i];
                    }

                    // window the samples
                    samples = hammingWindow(samples);

                    // copying audio data to the fft data buffer, imaginary part is 0
                    double[] fftData = new double[bufferSize * 2];
                    for (int i = 0; i < bufferSize; i++) {
                        fftData[2*i] = samples[i];
                        fftData[2*i+1] = 0;
                    }

                    // FFT compute
                    fft.complexForward(fftData);

                    // magnitudes
                    double[] magnitude = magnitude(fftData);

                    // Get the largest magnitude peak
                    //int peakIndex = peakIndex(magnitude);

                    // gets frequency value for peak index
                    //double frequency = calculateFrequency(peakIndex);
					//ArrayList<Integer> frequencies = new ArrayList<Integer>(Arrays.asList(calculateFrequencies(magnitude)));
					ArrayList<Integer> magnitudes = new ArrayList<Integer>(Arrays.asList(calculateMagnitudes(magnitude)));
					
                    // send frequency to handler
                    message = handler.obtainMessage();
                    //messageBundle.putIntegerArrayList("frequencies", frequencies);
					messageBundle.putIntegerArrayList("magnitudes", magnitudes);
					
					//messageBundle.putLong("frequencies", Math.round(frequency));
					message.setData(messageBundle);
                    handler.sendMessage(message);
                }
            }

            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
            }

            recorder.release();
            recorder = null;
        }
    }

    // Hann(ing) window
    // private double[] hannWindow(double[] samples) {
    //     for (int index = 0; index < samples.length; index++) {
    //         samples[index] *= 0.5 * (1 - (double) Math.cos(2 * Math.PI * index / (samples.length - 1)));
    //     }
    //     return samples;
    // }

    // Hamming window
    private double[] hammingWindow(double[] samples) {
        for (int index = 0; index < samples.length; index++) {
            samples[index] *= 0.54 - 0.46 * (double) Math.cos(2 * Math.PI * index / (samples.length - 1));
        }
        return samples;
    }

    // Blackman window
    // private double[] blackmanWindow(double[] samples) {
    //     for (int index = 0; index < samples.length; index++) {
    //         samples[index] *= 0.42 - 0.5 * (double) Math.cos(2 * Math.PI * index / (samples.length - 1)) + 0.08 * (double) Math.cos(4 * Math.PI * index / (samples.length - 1));
    //     }
    //     return samples;
    // }

    private double[] magnitude(double[] realData) {
         double[] magnitude = new double[bufferSize / 2];
         for (int i = 0; i < magnitude.length; i++) {
             double R = realData[2*i];
             double I = realData[2*i+1];
             // complex numbers -> vectors
             magnitude[i] = Math.sqrt(I*I + R*R);
         }
         return magnitude;
    }

    private int peakIndex(double[] data) {
        int peakIndex = 0;
        double peak = data[0];
        for(int i = 0; i < data.length; i++){
            if(peak < data[i]) {
                peak = data[i];
                peakIndex = i;
            }
        }
        return peakIndex;
    }
	
	private Integer[] calculateFrequencies(double[] data) {
		int ii = 0;
		Integer[] result = new Integer[bufferSize / 16];
		for(int i = 0; i < data.length; i++){
			if(i%8 == 0) {
				result[ii] = (int) Math.round(sampleRateInHz * i / bufferSize);
				ii++;
			}
		}
		return result;
    }
	
	private Integer[] calculateMagnitudes(double[] data) {
		int ii = 0;
		Integer[] result = new Integer[bufferSize / 4];
		for(int i = 0; i < data.length; i++){
			if(i%4 == 0) {
				result[2*ii] = (int) Math.round(data[i]);
				result[2*ii+1] = (int) Math.round(sampleRateInHz * i / bufferSize);
				ii++;
			}
			
		}
		return result;
    }

    private double calculateFrequency(double index) {
        return sampleRateInHz * index / bufferSize;
    }
}

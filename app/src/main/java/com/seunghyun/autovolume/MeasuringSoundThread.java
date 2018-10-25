package com.seunghyun.autovolume;

import android.media.MediaRecorder;
import android.util.Log;

public class MeasuringSoundThread extends Thread {
    private static final double EMA_FILTER = 0.6;
    private static final double amp = 10 * Math.exp(-2);
    private static final double mEMA = 0.0;
    static boolean isRunning = false;
    static int decibel = 0;

    @Override
    public void run() {
        super.run();
        //mediaRecorder 세팅
        isRunning = true;
        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (java.io.IOException ioe) {
            Log.e("[Error]", "IOException: " + android.util.Log.getStackTraceString(ioe));
        } catch (IllegalStateException ISe) {
            Log.e("[Error]", "IllegalStateException: " + android.util.Log.getStackTraceString(ISe));
        }

        while (isRunning) {
            int amplitude = mediaRecorder.getMaxAmplitude();
            decibel = (int) (long) Math.round(20 * Math.log10(EMA_FILTER * amplitude + (1.0 - EMA_FILTER) * mEMA / amp));

            //딜레이
            try {
                sleep(200);
            } catch (InterruptedException e) {
                Log.e("[Error]", "InterruptedException");
            }
        }
        mediaRecorder.release();
//        mediaRecorder = null;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        isRunning = false;
    }
}

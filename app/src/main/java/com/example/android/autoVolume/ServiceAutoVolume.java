package com.example.android.autoVolume;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Objects;

public class ServiceAutoVolume extends Service {
    private static final double EMA_FILTER = 0.6;
    private static final double ampl = 10 * Math.exp(-2);
    private MediaRecorder mediaRecorder;
    private AudioManager audioManager;
    private Notification.Builder builder;
    private Boolean isToastShowing;
    private Boolean isServiceRunning;

    private int micLevel;
    private int micSensitivity;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 서비스가 최초 시작될때 호출
     */
    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this); //EventBus register

        setNotification();
        startForeground(1, builder.build());

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        SharedPreferences sharedPreferences = getSharedPreferences("autoVolume", MODE_PRIVATE);
        micLevel = sharedPreferences.getInt("mic_level", 100);
        micSensitivity = sharedPreferences.getInt("mic_sensitivity", 50);

        isToastShowing = false;
        isServiceRunning = true;
        startRecord(); //녹음 시작
        new MeasuringThread().start();
    }

    /**
     * EventMIcLevel 를 받음
     */
    @Subscribe
    public void changeMIcLevelEvent(EventMIcLevel event) {
        micLevel = event.micLevel;
    }

    /**
     * EventMicSensitivity 를 받음
     */
    @Subscribe
    public void changeMicSensitivityEvent(EventMicSensitivity event) {
        micSensitivity = event.value;
    }

    /**
     * EventSwitchState 를 받음
     */
    @Subscribe
    public void changeSwitchStateEvent(EventSwitchState event) {
        isServiceRunning = event.isChecked;
    }

    /**
     * startService 로 실행될 때마다 호출
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * stopService 로 중지될 떄마다 호출
     */
    @Override
    public void onDestroy() {
        isServiceRunning = false;
        stopRecord();
        EventBus.getDefault().unregister(this); //EventBus unregister
    }

    /**
     * start record
     */
    private void startRecord() {
        if (mediaRecorder == null) {
            //set media recorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile("/dev/null");
        }
        //start record
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (java.io.IOException ioe) {
            Log.e("[Error]", "IOException: " + android.util.Log.getStackTraceString(ioe));
        } catch (IllegalStateException ISe) {
            Log.e("[Error]", "IllegalStateException: " + android.util.Log.getStackTraceString(ISe));
        }
    }

    /**
     * stop record
     */
    private void stopRecord() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    /**
     * 데시벨 계산
     */
    private int getDecibel() {
        if (mediaRecorder != null && isServiceRunning) {
            int decibel;
            int amplitude = mediaRecorder.getMaxAmplitude();
            double mEMA = 0.0;

            decibel = (int) (long) Math.round(20 * Math.log10(EMA_FILTER * amplitude + (1.0 - EMA_FILTER) * mEMA / ampl));

            return decibel;
        } else {
            return 0;
        }
    }

    /**
     * 데시벨에따라 볼륨 변경
     */
    private void setVolume(int decibel) {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int progressMax = 130 - micSensitivity;
        float ratio = (float) decibel / progressMax * maxVolume;
        int volume = Math.round(ratio);
        if (volume < 1) volume = 1; //볼륨이 진동모드로 바뀌는 것을 방지

        //오류 발생을 줄이기 위해 한번더 확인
        if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
    }

    /**
     * 알림 설정
     */
    public void setNotification() {
        //SDK 가 26이상이면 channel 설정, 아니면 일반 설정
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(ServiceAutoVolume.this, "notification");

            NotificationChannel notificationChannel = new NotificationChannel("notification", getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setShowBadge(false);
            Objects.requireNonNull(notificationManager).createNotificationChannel(notificationChannel);
        } else {
            builder = new Notification.Builder(ServiceAutoVolume.this);
        }

        //알림 클릭시 나올 액티비티
        PendingIntent pendingIntent = PendingIntent.getActivity(ServiceAutoVolume.this,
                0,
                new Intent(getApplicationContext(), ActivityMain.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        //builder 설정
        builder.setSmallIcon(R.drawable.ic_baseline_feedback_24px)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_content))
                .setContentIntent(pendingIntent);
    }

    /**
     * 데시벨 측정 스레드
     */
    private class MeasuringThread extends Thread {
        @Override
        public void run() {
            while (mediaRecorder != null && isServiceRunning) {
                //볼륨이 음소거 되있을때 실행되는것 방지
                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                    if (!isToastShowing) {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ServiceAutoVolume.this, getString(R.string.app_name) + ": " + getString(R.string.turn_off_mute), Toast.LENGTH_LONG).show();
                            }
                        });
                        isToastShowing = true;
                    }
                } else {
                    isToastShowing = false;
                    //데시벨 구하기
                    int decibel = getDecibel() + (micLevel - 100);
                    if (ActivityAutoVolume.isRunning) {
                        EventBus.getDefault().post(new EventProgress(decibel));
                    }
                    setVolume(decibel);

                    //딜레이
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        Log.e("[Error]", "InterruptedException");
                    }
                }
            }
        }
    }
}
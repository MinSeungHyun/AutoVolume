package com.seunghyun.autovolume;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

public class AutoVolumeService extends Service {
    static boolean isRunning;
    private AudioManager audioManager;
    private Notification.Builder builder;
    private Boolean isToastShowing;
    private int ringtoneMin, ringtoneMax, mediaMin, mediaMax, notificationsMin, notificationsMax, alarmMin, alarmMax;

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

        //초기값 설정
        SharedPreferences autoVolumePreferences = getSharedPreferences(SaveValues.Keys.autoVolumePreference, MODE_PRIVATE);
        SaveValues.StateValues.micLevel = autoVolumePreferences.getInt(SaveValues.Keys.micLevel, SaveValues.DefValues.micLevel);
        SaveValues.StateValues.micSensitivity = autoVolumePreferences.getInt(SaveValues.Keys.micSensitivity, SaveValues.DefValues.micSensitivity);
        SaveValues.StateValues.changeInterval = autoVolumePreferences.getInt(SaveValues.Keys.interval, SaveValues.DefValues.changeInterval) * 5;
        if (SaveValues.StateValues.changeInterval < 1) SaveValues.StateValues.changeInterval = 1;

        SharedPreferences rangePreference = getSharedPreferences(SaveValues.Keys.rangePreference, MODE_PRIVATE);
        ringtoneMin = rangePreference.getInt(SaveValues.Keys.ringtoneMin, 0);
        ringtoneMax = rangePreference.getInt(SaveValues.Keys.ringtoneMax, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        mediaMin = rangePreference.getInt(SaveValues.Keys.mediaMin, 0);
        mediaMax = rangePreference.getInt(SaveValues.Keys.mediaMax, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        notificationsMin = rangePreference.getInt(SaveValues.Keys.notificationsMin, 0);
        notificationsMax = rangePreference.getInt(SaveValues.Keys.notificationsMax, audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));
        alarmMin = rangePreference.getInt(SaveValues.Keys.alarmMin, 0);
        alarmMax = rangePreference.getInt(SaveValues.Keys.alarmMax, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));

        isToastShowing = false;
        isRunning = true;
        if (!MeasuringSoundThread.isRunning) new MeasuringSoundThread().start();
        new CalculatingThread().start();
    }

    /**
     * MinMaxValueEvent 를 받아서 볼륨 범위를 변경 from RangePopupActivity
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeMinMax(MinMaxValueEvent event) {
        switch (event.keyName) {
            case SaveValues.Keys.ringtone:
                ringtoneMin = event.minValue;
                ringtoneMax = event.maxValue;
                break;
            case SaveValues.Keys.media:
                mediaMin = event.minValue;
                mediaMax = event.maxValue;
                break;
            case SaveValues.Keys.notifications:
                notificationsMin = event.minValue;
                notificationsMax = event.maxValue;
                break;
            case SaveValues.Keys.alarm:
                alarmMin = event.minValue;
                alarmMax = event.maxValue;
                break;
        }
    }

    /**
     * stopService 로 중지될 떄마다 호출
     */
    @Override
    public void onDestroy() {
        isRunning = false;
        EventBus.getDefault().unregister(this); //EventBus unregister
        if (!AutoVolumeActivity.isRunning) new MeasuringSoundThread().interrupt();
    }

    /**
     * 볼륨이 변경되기전에 볼륨을 구함
     */
    private int getVolume(int value) {
        //마이크 감도에따라 값 조절
        int progressMax = SaveValues.DefValues.noiseProgressBarMax - SaveValues.StateValues.micSensitivity;
        float ratio = (float) value / progressMax;

        //볼륨 범위에 따라 값 조절
        int minVolume = ringtoneMin;
        int maxVolume = ringtoneMax;
        int range = maxVolume - minVolume;
        int volume = Math.round(range * ratio) + minVolume;
        if (volume > maxVolume) volume = maxVolume;
        if (volume < minVolume) volume = minVolume;
        return volume;
    }

    /**
     * 데시벨에따라 볼륨 변경
     */
    private void setVolume(int volume) {
        if (volume < 1) volume = 1; //볼륨이 진동모드로 바뀌는 것을 방지
        if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
    }

    /**
     * 알림 설정
     */
    private void setNotification() {
        //SDK 가 26이상이면 channel 설정, 아니면 일반 설정
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(AutoVolumeService.this, "notification");

            NotificationChannel notificationChannel = new NotificationChannel("notification", getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setShowBadge(false);
            Objects.requireNonNull(notificationManager).createNotificationChannel(notificationChannel);
        } else {
            builder = new Notification.Builder(AutoVolumeService.this);
        }

        //알림 클릭시 나올 액티비티
        PendingIntent pendingIntent = PendingIntent.getActivity(AutoVolumeService.this,
                0,
                new Intent(getApplicationContext(), MainActivity.class),
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
    private class CalculatingThread extends Thread {
        @Override
        public void run() {
            int time = 1;
            int sum = 0;
            while (SaveValues.StateValues.isAutoVolumeOn) {
                //볼륨이 음소거 되있을때 실행되는것 방지
                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                    if (!isToastShowing) {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AutoVolumeService.this, getString(R.string.app_name) + ": " + getString(R.string.turn_off_mute), Toast.LENGTH_LONG).show();
                            }
                        });
                        isToastShowing = true;
                    }
                } else {
                    isToastShowing = false;
                    int decibel = MeasuringSoundThread.decibel;
                    decibel += (SaveValues.StateValues.micLevel - 100); //마이크 수준에따라 값 조절

                    //변경 간격동안 볼륨 평균 계산
                    time++;
                    if (time < SaveValues.StateValues.changeInterval) {
                        sum += getVolume(decibel);
                    } else {
                        sum += getVolume(decibel);
                        int volume = sum / time;
                        setVolume(volume);
                        time = 0;
                        sum = 0;
                    }
                    //딜레이
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e("[Error]", "InterruptedException");
                    }
                }
            }
        }
    }
}
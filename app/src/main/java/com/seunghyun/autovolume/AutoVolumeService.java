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
    private int[] sum = new int[4];
    private int[] volume = new int[4];

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, builder.build());

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 서비스가 최초 시작될때 호출
     */
    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this); //EventBus register

        setNotification();

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
    private int getVolume(int value, String key) {
        //마이크 감도에따라 값 조절
        int progressMax = SaveValues.DefValues.noiseProgressBarMax - SaveValues.StateValues.micSensitivity;
        float ratio = (float) value / progressMax;

        //볼륨 범위에 따라 값 조절
        int minVolume = 0;
        int maxVolume = 0;

        switch (key) {
            case SaveValues.Keys.ringtone:
                minVolume = ringtoneMin;
                maxVolume = ringtoneMax;
                break;
            case SaveValues.Keys.media:
                minVolume = mediaMin;
                maxVolume = mediaMax;
                break;
            case SaveValues.Keys.notifications:
                minVolume = notificationsMin;
                maxVolume = notificationsMax;
                break;
            case SaveValues.Keys.alarm:
                minVolume = alarmMin;
                maxVolume = alarmMax;
                break;
        }

        int range = maxVolume - minVolume;
        int volume = Math.round(range * ratio) + minVolume;
        if (volume > maxVolume) volume = maxVolume;
        if (volume < minVolume) volume = minVolume;
        return volume;
    }

    /**
     * 데시벨에따라 볼륨 변경
     */
    private void setVolume() {
        if (SaveValues.StateValues.isRingtoneOn) {
            if (volume[0] < 1) volume[0] = 1;
            if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, volume[0], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
        }
        if (SaveValues.StateValues.isMediaOn) {
            if (volume[1] < 1) volume[1] = 1;
            if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume[1], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
        }
        if (SaveValues.StateValues.isNotificationsOn) {
            if (volume[2] < 1) volume[2] = 1;
            if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume[2], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
        }
        if (SaveValues.StateValues.isAlarmOn) {
            if (volume[3] < 1) volume[3] = 1;
            if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume[3], AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
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

        //알림에서 버튼 클릭시 나올 액티비티
        PendingIntent buttonIntent = PendingIntent.getActivity(AutoVolumeService.this,
                1,
                new Intent(getApplicationContext(), TurnOffActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action action = new Notification.Action(R.drawable.notification_icon, getString(R.string.turn_off), buttonIntent);
        builder.setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_content))
                .setContentIntent(pendingIntent)
                .addAction(action);
    }

    /**
     * 데시벨 측정 스레드
     */
    private class CalculatingThread extends Thread {
        @Override
        public void run() {
            int time = 1;
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
                        if (SaveValues.StateValues.isRingtoneOn)
                            sum[0] += getVolume(decibel, SaveValues.Keys.ringtone);
                        if (SaveValues.StateValues.isMediaOn)
                            sum[1] += getVolume(decibel, SaveValues.Keys.media);
                        if (SaveValues.StateValues.isNotificationsOn)
                            sum[2] += getVolume(decibel, SaveValues.Keys.notifications);
                        if (SaveValues.StateValues.isAlarmOn)
                            sum[3] += getVolume(decibel, SaveValues.Keys.alarm);

                    } else {
                        if (SaveValues.StateValues.isRingtoneOn) {
                            sum[0] += getVolume(decibel, SaveValues.Keys.ringtone);
                            volume[0] = sum[0] / time;
                        }
                        if (SaveValues.StateValues.isMediaOn) {
                            sum[1] += getVolume(decibel, SaveValues.Keys.media);
                            volume[1] = sum[1] / time;
                        }
                        if (SaveValues.StateValues.isNotificationsOn) {
                            sum[2] += getVolume(decibel, SaveValues.Keys.notifications);
                            volume[2] = sum[2] / time;
                        }
                        if (SaveValues.StateValues.isAlarmOn) {
                            sum[3] += getVolume(decibel, SaveValues.Keys.alarm);
                            volume[3] = sum[3] / time;
                        }
                        setVolume();
                        time = 0;
                        sum[0] = 0;
                        sum[1] = 0;
                        sum[2] = 0;
                        sum[3] = 0;
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
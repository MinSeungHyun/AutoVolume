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

public class ServiceAutoVolume extends Service {
    private static final double EMA_FILTER = 0.6;
    private static final double amp = 10 * Math.exp(-2);
    static Boolean isRunning;
    private AudioManager audioManager;
    private Notification.Builder builder;
    private Boolean isToastShowing;
    private int micLevel;
    private int micSensitivity;
    private int changeInterval;
    private int ringtoneMin, ringtoneMax, mediaMin, mediaMax, notificationsMin, notificationsMax, alarmMin, alarmMax;
    private boolean isRingtoneOn, isMediaOn, isNotificationsOn, isAlarmOn;

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
        SharedPreferences autoVolumePreferences = getSharedPreferences(SaveKey.autoVolumePreferenceKey, MODE_PRIVATE);
        micLevel = autoVolumePreferences.getInt(SaveKey.micLevelKey, 100);
        micSensitivity = autoVolumePreferences.getInt(SaveKey.micSensitivityKey, 50);
        changeInterval = autoVolumePreferences.getInt(SaveKey.intervalKey, 6) * 5;
        if (changeInterval < 1) changeInterval = 1;
        SharedPreferences rangePreference = getSharedPreferences(SaveKey.rangePreferenceKey, MODE_PRIVATE);
        ringtoneMin = rangePreference.getInt(SaveKey.ringtoneMinKey, 0);
        ringtoneMax = rangePreference.getInt(SaveKey.ringtoneMaxKey, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        mediaMin = rangePreference.getInt(SaveKey.mediaMinKey, 0);
        mediaMax = rangePreference.getInt(SaveKey.mediaMaxKey, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        notificationsMin = rangePreference.getInt(SaveKey.notificationsMinKey, 0);
        notificationsMax = rangePreference.getInt(SaveKey.notificationsMaxKey, audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));
        alarmMin = rangePreference.getInt(SaveKey.alarmMinKey, 0);
        alarmMax = rangePreference.getInt(SaveKey.alarmMaxKey, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));
        SharedPreferences isOnPreference = getSharedPreferences(SaveKey.switchPreferenceKey, MODE_PRIVATE);
        isRingtoneOn = isOnPreference.getBoolean(SaveKey.ringtoneStateKey, false);
        isMediaOn = isOnPreference.getBoolean(SaveKey.mediaStateKey, false);
        isNotificationsOn = isOnPreference.getBoolean(SaveKey.notificationsStateKey, false);
        isAlarmOn = isOnPreference.getBoolean(SaveKey.alarmStateKey, false);

        isToastShowing = false;
        isRunning = true;
        if (!ThreadMeasuringSound.isRunning) new ThreadMeasuringSound().start();
        new CalculatingThread().start();
    }

    /**
     * EventMIcLevel 를 받음 from ActivityAutoVolume
     */
    @Subscribe
    public void changeMIcLevel(EventMIcLevel event) {
        micLevel = event.micLevel;
    }

    /**
     * EventMicSensitivity 를 받음 from ActivityAutoVolume
     */
    @Subscribe
    public void changeMicSensitivity(EventMicSensitivity event) {
        micSensitivity = event.value;
    }

    /**
     * EventChangeInterval 를 받음 from ActivityAutoVolume
     */
    @Subscribe
    public void changeInterval(EventChangeInterval event) {
        changeInterval = event.interval;
    }

    /**
     * EventMainSwitchState 를 받음 from ActivityMain
     */
    @Subscribe
    public void changeSwitchState(EventMainSwitchState event) {
        isRunning = event.isChecked;
    }

    /**
     * EventIsOn 를 받음 from ActivityMain
     */
    @Subscribe
    public void changeIsOnState(EventIsOn event) {
        isRingtoneOn = event.ringtone;
        isMediaOn = event.media;
        isNotificationsOn = event.notifications;
        isAlarmOn = event.alarm;
    }

    /**
     * EventMinMaxValue 를 받아서 볼륨 범위를 변경 from ActivityRangePopup
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeMinMax(EventMinMaxValue event) {
        switch (event.keyName) {
            case SaveKey.ringtone:
                ringtoneMin = event.minValue;
                ringtoneMax = event.maxValue;
                break;
            case SaveKey.media:
                mediaMin = event.minValue;
                mediaMax = event.maxValue;
                break;
            case SaveKey.notifications:
                notificationsMin = event.minValue;
                notificationsMax = event.maxValue;
                break;
            case SaveKey.alarm:
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
        if (!ActivityAutoVolume.isRunning) new ThreadMeasuringSound().interrupt();
    }

    /**
     * 볼륨이 변경되기전에 볼륨을 구함
     */
    private int getVolume(int value) {
        //마이크 감도에따라 값 조절
        int progressMax = 130 - micSensitivity;
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
        //오류 발생을 줄이기 위해 한번더 무음인지 확인
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
    private class CalculatingThread extends Thread {
        @Override
        public void run() {
            int time = 1;
            int sum = 0;
            while (isRunning) {
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
                    int decibel = ThreadMeasuringSound.decibel;
                    decibel += (micLevel - 100); //마이크 수준에따라 값 조절

                    time++;
                    if (time < changeInterval) {
                        sum += getVolume(decibel);
                    } else {
                        sum += getVolume(decibel);
                        int volume = sum / time;
                        setVolume(volume);
                        time = 0;
                        sum = 0;
                    }
                    Log.d("Notice", "Service Thread Running");
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
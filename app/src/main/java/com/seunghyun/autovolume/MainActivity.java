package com.seunghyun.autovolume;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    static boolean isRunning = false;
    //etc
    long time = 0;
    //view
    private TextView textView_1, textView_2, textView_3, textView_4,
            minRingtone, maxRingtone, minMedia, maxMedia, minNotifications, maxNotifications, minAlarm, maxAlarm;
    private Switch mainSwitch;
    private View highlightSwitch;
    private LinearLayout topLinearLayout, linearLayout_1, linearLayout_2, linearLayout_3, linearLayout_4,
            rangeLinearLayout_1, rangeLinearLayout_2, rangeLinearLayout_3, rangeLinearLayout_4;
    private ImageView imageView_1, imageView_2, imageView_3, imageView_4;
    //sharedPreference
    private SharedPreferences switchPreference, volumeRangePreference;
    private SharedPreferences.Editor switchPreferenceEditor;
    //audioManager
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide(); //액션바 숨기기
        setContentView(R.layout.activity_main);

        isRunning = true;
        //from TurnOffActivity
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals("finish_activity")) finish();
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("finish_activity"));

        //권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        EventBus.getDefault().register(this); //EventBus register

        findViews();
        getReferences();

        reloadSwitchState();
        reloadTextState();

        setListeners();

        //서비스 실행상태에 따라 변경
        if (isServiceRunning()) mainSwitch.setChecked(true);
        else mainSwitch.setChecked(false);

        //mainSwitch 값에 따라 항목 변경
        if (mainSwitch.isChecked()) setEnableByMainSwitch();
        else setDisableByMainSwitch();

    }

    /**
     * 앱이 종료될때 이벤트버스 unregister
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        EventBus.getDefault().unregister(this); //EventBus unregister
    }

    /**
     * MinMaxValueEvent 를 받아서 볼륨 범위 텍스트를 변경 from RangePopupActivity
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeTV(MinMaxValueEvent event) {
        switch (event.keyName) {
            case SaveValues.Keys.ringtone:
                minRingtone.setText(String.format(getString(R.string.min_volume), event.minValue));
                maxRingtone.setText(String.format(getString(R.string.max_volume), event.maxValue));
                break;
            case SaveValues.Keys.media:
                minMedia.setText(String.format(getString(R.string.min_volume), event.minValue));
                maxMedia.setText(String.format(getString(R.string.max_volume), event.maxValue));
                break;
            case SaveValues.Keys.notifications:
                minNotifications.setText(String.format(getString(R.string.min_volume), event.minValue));
                maxNotifications.setText(String.format(getString(R.string.max_volume), event.maxValue));
                break;
            case SaveValues.Keys.alarm:
                minAlarm.setText(String.format(getString(R.string.min_volume), event.minValue));
                maxAlarm.setText(String.format(getString(R.string.max_volume), event.maxValue));
                break;
        }
    }

    /**
     * 모든 뷰들의 참조
     */
    private void findViews() {
        textView_1 = findViewById(R.id.textView_1);
        textView_2 = findViewById(R.id.textView_2);
        textView_3 = findViewById(R.id.textView_3);
        textView_4 = findViewById(R.id.textView_4);
        minRingtone = findViewById(R.id.min_ringtone);
        minMedia = findViewById(R.id.min_media);
        minNotifications = findViewById(R.id.min_notifications);
        minAlarm = findViewById(R.id.min_alarm);
        maxRingtone = findViewById(R.id.max_ringtone);
        maxMedia = findViewById(R.id.max_media);
        maxNotifications = findViewById(R.id.max_notifications);
        maxAlarm = findViewById(R.id.max_alarm);

        mainSwitch = findViewById(R.id.main_switch);

        topLinearLayout = findViewById(R.id.top_linearLayout);
        linearLayout_1 = findViewById(R.id.linearLayout_1);
        linearLayout_2 = findViewById(R.id.linearLayout_2);
        linearLayout_3 = findViewById(R.id.linearLayout_3);
        linearLayout_4 = findViewById(R.id.linearLayout_4);
        rangeLinearLayout_1 = findViewById(R.id.range_linearLayout_1);
        rangeLinearLayout_2 = findViewById(R.id.range_linearLayout_2);
        rangeLinearLayout_3 = findViewById(R.id.range_linearLayout_3);
        rangeLinearLayout_4 = findViewById(R.id.range_linearLayout_4);

        imageView_1 = findViewById(R.id.ringtoneIcon);
        imageView_2 = findViewById(R.id.mediaIcon);
        imageView_3 = findViewById(R.id.notificationsIcon);
        imageView_4 = findViewById(R.id.alarmIcon);

        highlightSwitch = findViewById(R.id.highlight_switch);
    }

    /**
     * 다른 참조들
     */
    @SuppressLint("CommitPrefEdits")
    private void getReferences() {
        //switchPreference 참조 생성
        switchPreference = getSharedPreferences(SaveValues.Keys.switchPreference, MODE_PRIVATE);
        volumeRangePreference = getSharedPreferences(SaveValues.Keys.rangePreference, MODE_PRIVATE);
        switchPreferenceEditor = switchPreference.edit();

        //audioManager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    /**
     * 저장되었던 스위치 상태를 가져오는 메소드
     */
    private void reloadSwitchState() {
        Boolean isChecked = switchPreference.getBoolean(SaveValues.Keys.autoVolumeState, SaveValues.DefValues.autoVolume);
        Boolean isChecked_1 = switchPreference.getBoolean(SaveValues.Keys.ringtoneState, SaveValues.DefValues.ringtone);
        Boolean isChecked_2 = switchPreference.getBoolean(SaveValues.Keys.mediaState, SaveValues.DefValues.media);
        Boolean isChecked_3 = switchPreference.getBoolean(SaveValues.Keys.notificationsState, SaveValues.DefValues.notifications);
        Boolean isChecked_4 = switchPreference.getBoolean(SaveValues.Keys.alarmState, SaveValues.DefValues.alarm);

        mainSwitch.setChecked(isChecked);
        setIconTV(imageView_1, textView_1, isChecked_1);
        setIconTV(imageView_2, textView_2, isChecked_2);
        setIconTV(imageView_3, textView_3, isChecked_3);
        setIconTV(imageView_4, textView_4, isChecked_4);
        SaveValues.StateValues.isRingtoneOn = isChecked_1;
        SaveValues.StateValues.isMediaOn = isChecked_2;
        SaveValues.StateValues.isNotificationsOn = isChecked_3;
        SaveValues.StateValues.isAlarmOn = isChecked_4;

    }

    /**
     * 저장되었던 최소/최대 볼륨을 가져오는 메소드
     */
    private void reloadTextState() {
        int sMinRingtone = volumeRangePreference.getInt(SaveValues.Keys.ringtoneMin, 0);
        int sMaxRingtone = volumeRangePreference.getInt(SaveValues.Keys.ringtoneMax, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        int sMinMedia = volumeRangePreference.getInt(SaveValues.Keys.mediaMin, 0);
        int sMaxMedia = volumeRangePreference.getInt(SaveValues.Keys.mediaMax, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int sMinNotifications = volumeRangePreference.getInt(SaveValues.Keys.notificationsMin, 0);
        int sMaxNotifications = volumeRangePreference.getInt(SaveValues.Keys.notificationsMax, audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));
        int sMinAlarm = volumeRangePreference.getInt(SaveValues.Keys.alarmMin, 0);
        int sMaxAlarm = volumeRangePreference.getInt(SaveValues.Keys.alarmMax, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));

        minRingtone.setText(String.format(getString(R.string.min_volume), sMinRingtone));
        maxRingtone.setText(String.format(getString(R.string.max_volume), sMaxRingtone));
        minMedia.setText(String.format(getString(R.string.min_volume), sMinMedia));
        maxMedia.setText(String.format(getString(R.string.max_volume), sMaxMedia));
        minNotifications.setText(String.format(getString(R.string.min_volume), sMinNotifications));
        maxNotifications.setText(String.format(getString(R.string.max_volume), sMaxNotifications));
        minAlarm.setText(String.format(getString(R.string.min_volume), sMinAlarm));
        maxAlarm.setText(String.format(getString(R.string.max_volume), sMaxAlarm));
    }

    /**
     * 리스너 설정
     */
    private void setListeners() {
        mainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switchPreferenceEditor.putBoolean(SaveValues.Keys.autoVolumeState, isChecked);
                switchPreferenceEditor.commit();
                if (isChecked) {
                    //권한 허용 여부 확인하고 거부되었으면 팝업 띄움
                    Boolean permissionGranted = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                    if (!permissionGranted) {
                        //권한 거부됨
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.notice))
                                .setMessage(getString(R.string.permission_denied_message))
                                .setNeutralButton(getString(R.string.setting), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(Uri.parse("package:" + getPackageName()));
                                        startActivity(intent);
                                    }
                                })
                                .setPositiveButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                })
                                .setCancelable(false)
                                .create()
                                .show();
                        //스위치 체크된것 취소
                        mainSwitch.setChecked(false);
                        switchPreferenceEditor.putBoolean(SaveValues.Keys.autoVolumeState, false);
                        switchPreferenceEditor.commit();
                    } else {
                        //권한 허용됨, 활성화
                        setEnableByMainSwitch();
                        //서비스 시작
                        Intent service = new Intent(MainActivity.this, AutoVolumeService.class);
                        startService(service);
                        SaveValues.StateValues.isAutoVolumeOn = true;
                    }
                } else {
                    //비활성화
                    setDisableByMainSwitch();
                    //서비스 종료
                    SaveValues.StateValues.isAutoVolumeOn = false;
                    Intent service = new Intent(MainActivity.this, AutoVolumeService.class);
                    stopService(service);
                }
            }
        });

        topLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AutoVolumeActivity.class);
                startActivity(intent);
            }
        });

        LinearLayout.OnClickListener itemLinearLayoutClickListener = new LinearLayout.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainSwitch.isChecked()) {
                    switch (view.getId()) {
                        case R.id.linearLayout_1:
                            if (SaveValues.StateValues.isRingtoneOn) {
                                switchPreferenceEditor.putBoolean(SaveValues.Keys.ringtoneState, false);
                                switchPreferenceEditor.apply();
                                SaveValues.StateValues.isRingtoneOn = false;
                                setIconTV(imageView_1, textView_1, false);
                            } else {
                                switchPreferenceEditor.putBoolean(SaveValues.Keys.ringtoneState, true);
                                switchPreferenceEditor.apply();
                                SaveValues.StateValues.isRingtoneOn = true;
                                setIconTV(imageView_1, textView_1, true);
                            }
                            break;
                        case R.id.linearLayout_2:
                            if (SaveValues.StateValues.isMediaOn) {
                                switchPreferenceEditor.putBoolean(SaveValues.Keys.mediaState, false);
                                switchPreferenceEditor.apply();
                                SaveValues.StateValues.isMediaOn = false;
                                setIconTV(imageView_2, textView_2, false);
                            } else {
                                switchPreferenceEditor.putBoolean(SaveValues.Keys.mediaState, true);
                                switchPreferenceEditor.apply();
                                SaveValues.StateValues.isMediaOn = true;
                                setIconTV(imageView_2, textView_2, true);
                            }
                            break;
                        case R.id.linearLayout_3:
                            if (SaveValues.StateValues.isNotificationsOn) {
                                switchPreferenceEditor.putBoolean(SaveValues.Keys.notificationsState, false);
                                switchPreferenceEditor.apply();
                                SaveValues.StateValues.isNotificationsOn = false;
                                setIconTV(imageView_3, textView_3, false);
                            } else {
                                switchPreferenceEditor.putBoolean(SaveValues.Keys.notificationsState, true);
                                switchPreferenceEditor.apply();
                                SaveValues.StateValues.isNotificationsOn = true;
                                setIconTV(imageView_3, textView_3, true);
                            }
                            break;
                        case R.id.linearLayout_4:
                            if (SaveValues.StateValues.isAlarmOn) {
                                switchPreferenceEditor.putBoolean(SaveValues.Keys.alarmState, false);
                                switchPreferenceEditor.apply();
                                SaveValues.StateValues.isAlarmOn = false;
                                setIconTV(imageView_4, textView_4, false);
                            } else {
                                switchPreferenceEditor.putBoolean(SaveValues.Keys.alarmState, true);
                                switchPreferenceEditor.apply();
                                SaveValues.StateValues.isAlarmOn = true;
                                setIconTV(imageView_4, textView_4, true);
                            }
                            break;
                    }
                } else {
                    //강조
                    ValueAnimator animation = ValueAnimator.ofFloat(1f, 0f);
                    animation.setDuration(1000);
                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            highlightSwitch.setAlpha((float) valueAnimator.getAnimatedValue());
                        }
                    });
                    animation.start();
                }
            }
        };
        linearLayout_1.setOnClickListener(itemLinearLayoutClickListener);
        linearLayout_2.setOnClickListener(itemLinearLayoutClickListener);
        linearLayout_3.setOnClickListener(itemLinearLayoutClickListener);
        linearLayout_4.setOnClickListener(itemLinearLayoutClickListener);

        LinearLayout.OnClickListener rangeLinearLayoutClickListener = new LinearLayout.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RangePopupActivity.class);
                switch (view.getId()) {
                    case R.id.range_linearLayout_1:
                        intent.putExtra(SaveValues.Keys.viewName, SaveValues.Keys.ringtone);
                        break;
                    case R.id.range_linearLayout_2:
                        intent.putExtra(SaveValues.Keys.viewName, SaveValues.Keys.media);
                        break;
                    case R.id.range_linearLayout_3:
                        intent.putExtra(SaveValues.Keys.viewName, SaveValues.Keys.notifications);
                        break;
                    case R.id.range_linearLayout_4:
                        intent.putExtra(SaveValues.Keys.viewName, SaveValues.Keys.alarm);
                        break;
                }
                startActivity(intent);
            }
        };
        rangeLinearLayout_1.setOnClickListener(rangeLinearLayoutClickListener);
        rangeLinearLayout_2.setOnClickListener(rangeLinearLayoutClickListener);
        rangeLinearLayout_3.setOnClickListener(rangeLinearLayoutClickListener);
        rangeLinearLayout_4.setOnClickListener(rangeLinearLayoutClickListener);
    }

    /**
     * 아이콘과 글씨를 활성화 또는 비활성화
     */
    private void setIconTV(ImageView imageView, TextView textView, Boolean isEnable) {
        if (isEnable) {
            imageView.setColorFilter(getColor(R.color.colorPrimary));
            textView.setTextColor(getColor(R.color.colorPrimary));
        } else {
            imageView.setColorFilter(null);
            textView.setTextColor(Color.parseColor("#000000"));
        }
    }

    /**
     * mainSwitch 의 값에따라 항목들 활성화
     */
    private void setEnableByMainSwitch() {
        imageView_1.setAlpha(1f);
        imageView_2.setAlpha(1f);
        imageView_3.setAlpha(1f);
        imageView_4.setAlpha(1f);
        textView_1.setAlpha(1f);
        textView_2.setAlpha(1f);
        textView_3.setAlpha(1f);
        textView_4.setAlpha(1f);
    }

    /**
     * mainSwitch 의 값에따라 항목들 비활성화
     */
    private void setDisableByMainSwitch() {
        imageView_1.setAlpha(0.5f);
        imageView_2.setAlpha(0.5f);
        imageView_3.setAlpha(0.5f);
        imageView_4.setAlpha(0.5f);
        textView_1.setAlpha(0.5f);
        textView_2.setAlpha(0.5f);
        textView_3.setAlpha(0.5f);
        textView_4.setAlpha(0.5f);
    }

    /**
     * Check if service is running
     */
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AutoVolumeService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 뒤로가기 두번눌러 종료
     */
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - time >= 2000) {
            time = System.currentTimeMillis();
            Toast.makeText(getApplicationContext(), R.string.backButton_twice, Toast.LENGTH_SHORT).show();
        } else if (System.currentTimeMillis() - time < 2000) {
            finish();
        }
    }

    /**
     * 권한 결과 가져오기
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted.
            Log.d("Permission", "Granted");
        } else {
            // permission denied.
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {

                Toast.makeText(getApplicationContext(), getString(R.string.permission_denied_toast), Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //여기에 딜레이 후 시작할 작업들을 입력
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                    }
                }, 2000);
            }
        }
    }
}
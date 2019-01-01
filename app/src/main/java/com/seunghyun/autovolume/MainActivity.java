package com.seunghyun.autovolume;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;

public class MainActivity extends AppCompatActivity {
    static boolean isRunning = false;
    //etc
    long time = 0;
    BroadcastReceiver broadcastReceiver;
    //view
    private TextView textView_1, textView_2, textView_3, textView_4,
            minRingtone, maxRingtone, minMedia, maxMedia, minNotifications, maxNotifications, minAlarm, maxAlarm;
    private LinearLayout topLinearLayout, linearLayout_1, linearLayout_2, linearLayout_3, linearLayout_4,
            rangeLinearLayout_1, rangeLinearLayout_2, rangeLinearLayout_3, rangeLinearLayout_4;
    private ImageView imageView_1, imageView_2, imageView_3, imageView_4;
    //sharedPreference
    private SharedPreferences switchPreference, volumeRangePreference, isGuideShownPreference;
    private SharedPreferences.Editor switchPreferenceEditor, isGuideShownEditor;
    //audioManager
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide(); //액션바 숨기기
        setContentView(R.layout.activity_main);

        isRunning = true;
        //from TurnOffActivity
        broadcastReceiver = new BroadcastReceiver() {
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            makeGuides();
        }
    }

    /**
     * 액티비티가 전면에 보일 때 호출
     */
    protected void onResume() {
        super.onResume();
        makeSecondGuides();
    }

    /**
     * 앱이 종료될때 이벤트버스 unregister
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        EventBus.getDefault().unregister(this);
        unregisterReceiver(broadcastReceiver);
    }

    /**
     * MinMaxValueEvent 를 받아서 볼륨 범위 텍스트를 변경 from RangePopupActivity
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeTV(MinMaxValueEvent event) {
        switch (event.keyName) {
            case SaveValues.Keys.ringtone:
                minRingtone.setText(String.valueOf(event.minValue));
                maxRingtone.setText(String.valueOf(event.maxValue));
                break;
            case SaveValues.Keys.media:
                minMedia.setText(String.valueOf(event.minValue));
                maxMedia.setText(String.valueOf(event.maxValue));
                break;
            case SaveValues.Keys.notifications:
                minNotifications.setText(String.valueOf(event.minValue));
                maxNotifications.setText(String.valueOf(event.maxValue));
                break;
            case SaveValues.Keys.alarm:
                minAlarm.setText(String.valueOf(event.minValue));
                maxAlarm.setText(String.valueOf(event.maxValue));
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
    }

    /**
     * 다른 참조들
     */
    @SuppressLint("CommitPrefEdits")
    private void getReferences() {
        //switchPreference 참조 생성
        switchPreference = getSharedPreferences(SaveValues.Keys.switchPreference, MODE_PRIVATE);
        volumeRangePreference = getSharedPreferences(SaveValues.Keys.rangePreference, MODE_PRIVATE);
        isGuideShownPreference = getSharedPreferences(SaveValues.isGuideShownPreference.preferenceName, MODE_PRIVATE);
        switchPreferenceEditor = switchPreference.edit();
        isGuideShownEditor = isGuideShownPreference.edit();

        //audioManager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    /**
     * 저장되었던 스위치 상태를 가져오는 메소드
     */
    private void reloadSwitchState() {
        if (isServiceRunning()) {
            Boolean isChecked_1 = switchPreference.getBoolean(SaveValues.Keys.ringtoneState, SaveValues.DefValues.ringtone);
            Boolean isChecked_2 = switchPreference.getBoolean(SaveValues.Keys.mediaState, SaveValues.DefValues.media);
            Boolean isChecked_3 = switchPreference.getBoolean(SaveValues.Keys.notificationsState, SaveValues.DefValues.notifications);
            Boolean isChecked_4 = switchPreference.getBoolean(SaveValues.Keys.alarmState, SaveValues.DefValues.alarm);

            setIconTV(imageView_1, textView_1, isChecked_1);
            setIconTV(imageView_2, textView_2, isChecked_2);
            setIconTV(imageView_3, textView_3, isChecked_3);
            setIconTV(imageView_4, textView_4, isChecked_4);
            SaveValues.StateValues.isRingtoneOn = isChecked_1;
            SaveValues.StateValues.isMediaOn = isChecked_2;
            SaveValues.StateValues.isNotificationsOn = isChecked_3;
            SaveValues.StateValues.isAlarmOn = isChecked_4;
        } else {
            setIconTV(imageView_1, textView_1, false);
            setIconTV(imageView_2, textView_2, false);
            setIconTV(imageView_3, textView_3, false);
            setIconTV(imageView_4, textView_4, false);
            SaveValues.StateValues.isRingtoneOn = false;
            SaveValues.StateValues.isMediaOn = false;
            SaveValues.StateValues.isNotificationsOn = false;
            SaveValues.StateValues.isAlarmOn = false;
        }
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

        minRingtone.setText(String.valueOf(sMinRingtone));
        maxRingtone.setText(String.valueOf(sMaxRingtone));
        minMedia.setText(String.valueOf(sMinMedia));
        maxMedia.setText(String.valueOf(sMaxMedia));
        minNotifications.setText(String.valueOf(sMinNotifications));
        maxNotifications.setText(String.valueOf(sMaxNotifications));
        minAlarm.setText(String.valueOf(sMinAlarm));
        maxAlarm.setText(String.valueOf(sMaxAlarm));
    }

    /**
     * 리스너 설정
     */
    private void setListeners() {
        LinearLayout.OnClickListener itemLinearLayoutClickListener = new LinearLayout.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean permissionGranted = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                if (!permissionGranted) {
                    //권한 거부됨
                    showPermissionPopup();
                } else {
                    //권한 혀용됨
                    saveSwitchState(view);
                    if (!SaveValues.StateValues.isRingtoneOn && !SaveValues.StateValues.isMediaOn && !SaveValues.StateValues.isNotificationsOn && !SaveValues.StateValues.isAlarmOn) {
                        //모든 항목이 꺼졌으므로 서비스 종료
                        SaveValues.StateValues.isAutoVolumeOn = false;
                        Intent service = new Intent(MainActivity.this, AutoVolumeService.class);
                        stopService(service);
                    } else {
                        if (!isServiceRunning()) {
                            //서비스 시작
                            Intent service = new Intent(MainActivity.this, AutoVolumeService.class);
                            startService(service);
                            SaveValues.StateValues.isAutoVolumeOn = true;
                        }
                    }
                }
            }
        };
        linearLayout_1.setOnClickListener(itemLinearLayoutClickListener);
        linearLayout_2.setOnClickListener(itemLinearLayoutClickListener);
        linearLayout_3.setOnClickListener(itemLinearLayoutClickListener);
        linearLayout_4.setOnClickListener(itemLinearLayoutClickListener);

        topLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean permissionGranted = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                if (!permissionGranted) {
                    //권한 거부됨
                    showPermissionPopup();
                } else {
                    //권한 혀용됨
                    Intent intent = new Intent(MainActivity.this, AutoVolumeActivity.class);
                    startActivity(intent);
                }
            }
        });

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
     * 가이드뷰 생성
     */
    private void makeGuides() {
        if (!isGuideShownPreference.getBoolean(SaveValues.isGuideShownPreference.detailSetting, false)) {
            new GuideView.Builder(this)
                    .setTargetView(topLinearLayout)
                    .setTitle(getString(R.string.detail_setting))
                    .setContentSpan(SaveValues.GuideViewValues.contentSpan(getString(R.string.detail_setting_description)))
                    .setTitleTypeFace(Typeface.defaultFromStyle(Typeface.BOLD))
                    .setTitleTextSize(SaveValues.GuideViewValues.titleTextSize)
                    .setContentTextSize(SaveValues.GuideViewValues.contentTextSize)
                    .setGravity(GuideView.Gravity.center)
                    .setDismissType(GuideView.DismissType.outside)
                    .setGuideListener(new GuideView.GuideListener() {
                        @Override
                        public void onDismiss(View view) {
                            startActivity(new Intent(MainActivity.this, AutoVolumeActivity.class));
                            isGuideShownEditor.putBoolean(SaveValues.isGuideShownPreference.detailSetting, true);
                            isGuideShownEditor.apply();
                        }
                    })
                    .build()
                    .show();
        }
    }

    private void makeSecondGuides() {
        boolean isDetailSettingShown = isGuideShownPreference.getBoolean(SaveValues.isGuideShownPreference.detailSetting, false);
        boolean isToggleShown = !isGuideShownPreference.getBoolean(SaveValues.isGuideShownPreference.mainActivity, false);
        if (isDetailSettingShown && isToggleShown) {
            new GuideView.Builder(this)
                    .setTargetView(linearLayout_1)
                    .setTitle(getString(R.string.switch_title))
                    .setContentSpan(SaveValues.GuideViewValues.contentSpan(getString(R.string.switch_description)))
                    .setTitleTypeFace(Typeface.defaultFromStyle(Typeface.BOLD))
                    .setTitleTextSize(SaveValues.GuideViewValues.titleTextSize)
                    .setContentTextSize(SaveValues.GuideViewValues.contentTextSize)
                    .setGravity(GuideView.Gravity.center)
                    .setDismissType(GuideView.DismissType.outside)
                    .setGuideListener(new GuideView.GuideListener() {
                        @Override
                        public void onDismiss(View view) {
                            new GuideView.Builder(MainActivity.this)
                                    .setTargetView(rangeLinearLayout_1)
                                    .setTitle(getString(R.string.volume_range_title))
                                    .setContentSpan(SaveValues.GuideViewValues.contentSpan(getString(R.string.volume_range_description)))
                                    .setTitleTypeFace(Typeface.defaultFromStyle(Typeface.BOLD))
                                    .setTitleTextSize(SaveValues.GuideViewValues.titleTextSize)
                                    .setContentTextSize(SaveValues.GuideViewValues.contentTextSize)
                                    .setGravity(GuideView.Gravity.center)
                                    .setDismissType(GuideView.DismissType.outside)
                                    .setGuideListener(new GuideView.GuideListener() {
                                        @Override
                                        public void onDismiss(View view) {
                                            Intent intent = new Intent(MainActivity.this, RangePopupActivity.class);
                                            intent.putExtra(SaveValues.Keys.viewName, SaveValues.Keys.ringtone);
                                            startActivity(intent);

                                            isGuideShownEditor.putBoolean(SaveValues.isGuideShownPreference.mainActivity, true);
                                            isGuideShownEditor.apply();
                                        }
                                    })
                                    .build()
                                    .show();
                        }
                    })
                    .build()
                    .show();
        }
    }

    /**
     * 항목의 상태 저장
     */
    private void saveSwitchState(View view) {
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
    }

    /**
     * 권한 거부됬을 시 팝업으로 알려줌
     */
    private void showPermissionPopup() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(getString(R.string.notice))
                .setMessage(getString(R.string.permission_denied_message))
                .setNeutralButton(getString(R.string.setting_capital), new DialogInterface.OnClickListener() {
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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //여기에 딜레이 후 시작할 작업들을 입력
                makeGuides();
            }
        }, 2000);
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
            makeGuides();
        } else {
            // permission denied.
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {

                Toast.makeText(getApplicationContext(), getString(R.string.permission_denied_toast), Toast.LENGTH_LONG).show();
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //여기에 딜레이 후 시작할 작업들을 입력
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                }, 2000);
            }
        }
    }
}
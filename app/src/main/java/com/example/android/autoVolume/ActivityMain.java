package com.example.android.autoVolume;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.util.TypedValue;
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

public class ActivityMain extends AppCompatActivity {
    //view
    TextView mainTextView, textView_1, textView_2, textView_3, textView_4,
            minRingtone, maxRingtone, minMedia, maxMedia, minNotifications, maxNotifications, minAlarm, maxAlarm;
    Switch mainSwitch;
    View highlightSwitch;
    LinearLayout topLinearLayout, linearLayout_1, linearLayout_2, linearLayout_3, linearLayout_4,
            rangeLinearLayout_1, rangeLinearLayout_2, rangeLinearLayout_3, rangeLinearLayout_4;
    ImageView imageView_1, imageView_2, imageView_3, imageView_4;
    //sharedPreference
    SharedPreferences switchPreference, volumeRangePreference;
    SharedPreferences.Editor switchPreferenceEditor;
    //audioManager
    AudioManager audioManager;
    //etc
    long time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide(); //액션바 숨기기
        setContentView(R.layout.activity_main);

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
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this); //EventBus unregister
    }

    /**
     * EventMinMaxValue 를 받아서 볼륨 범위 텍스트를 변경한다
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeTV(EventMinMaxValue event) {
        switch (event.keyName) {
            case SaveKey.ringtone:
                minRingtone.setText(String.format(getString(R.string.min_volume), event.minValue));
                maxRingtone.setText(String.format(getString(R.string.max_volume), event.maxValue));
                break;
            case SaveKey.media:
                minMedia.setText(String.format(getString(R.string.min_volume), event.minValue));
                maxMedia.setText(String.format(getString(R.string.max_volume), event.maxValue));
                break;
            case SaveKey.notifications:
                minNotifications.setText(String.format(getString(R.string.min_volume), event.minValue));
                maxNotifications.setText(String.format(getString(R.string.max_volume), event.maxValue));
                break;
            case SaveKey.alarm:
                minAlarm.setText(String.format(getString(R.string.min_volume), event.minValue));
                maxAlarm.setText(String.format(getString(R.string.max_volume), event.maxValue));
                break;
        }
    }

    /**
     * 모든 뷰들의 참조
     */
    public void findViews() {
        mainTextView = findViewById(R.id.main_text);
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
        switchPreference = getSharedPreferences(SaveKey.switchPreferenceKey, MODE_PRIVATE);
        volumeRangePreference = getSharedPreferences(SaveKey.rangePreferenceKey, MODE_PRIVATE);
        switchPreferenceEditor = switchPreference.edit();

        //audioManager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    /**
     * 저장되었던 스위치 상태를 가져오는 메소드
     */
    private void reloadSwitchState() {
        Boolean isChecked = switchPreference.getBoolean(String.valueOf(mainSwitch.getTag()), false);
        Boolean isChecked_1 = switchPreference.getBoolean(SaveKey.ringtoneStateKey, false);
        Boolean isChecked_2 = switchPreference.getBoolean(SaveKey.mediaStateKey, false);
        Boolean isChecked_3 = switchPreference.getBoolean(SaveKey.notificationsStateKey, false);
        Boolean isChecked_4 = switchPreference.getBoolean(SaveKey.alarmStateKey, false);

        mainSwitch.setChecked(isChecked);
        setIconTV(imageView_1, textView_1, isChecked_1);
        setIconTV(imageView_2, textView_2, isChecked_2);
        setIconTV(imageView_3, textView_3, isChecked_3);
        setIconTV(imageView_4, textView_4, isChecked_4);

    }

    /**
     * 저장되었던 최소/최대 볼륨을 가져오는 메소드
     */
    private void reloadTextState() {
        int sMinRingtone = volumeRangePreference.getInt(SaveKey.ringtoneMinKey, 0);
        int sMaxRingtone = volumeRangePreference.getInt(SaveKey.ringtoneMaxKey, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        int sMinMedia = volumeRangePreference.getInt(SaveKey.mediaMinKey, 0);
        int sMaxMedia = volumeRangePreference.getInt(SaveKey.mediaMaxKey, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int sMinNotifications = volumeRangePreference.getInt(SaveKey.notificationsMinKey, 0);
        int sMaxNotifications = volumeRangePreference.getInt(SaveKey.notificationsMaxKey, audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));
        int sMinAlarm = volumeRangePreference.getInt(SaveKey.alarmMinKey, 0);
        int sMaxAlarm = volumeRangePreference.getInt(SaveKey.alarmMaxKey, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));

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
                String switchTag = String.valueOf(mainSwitch.getTag());
                switchPreferenceEditor.putBoolean(switchTag, isChecked);
                switchPreferenceEditor.commit();
                if (isChecked) {
                    //권한 허용 여부 확인하고 거부되었으면 팝업 띄움
                    Boolean permissionGranted = ContextCompat.checkSelfPermission(ActivityMain.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                    if (!permissionGranted) {
                        //권한 거부됨
                        new AlertDialog.Builder(ActivityMain.this)
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
                        switchPreferenceEditor.putBoolean(String.valueOf(mainSwitch.getTag()), false);
                        switchPreferenceEditor.commit();
                    } else {
                        //권한 허용됨, 활성화
                        setEnableByMainSwitch();
                        //서비스 시작
                        Intent service = new Intent(ActivityMain.this, ServiceAutoVolume.class);
                        startService(service);
                        EventBus.getDefault().post(new EventMainSwitchState(true));
                    }
                } else {
                    //비활성화
                    setDisableByMainSwitch();
                    //서비스 종료
                    EventBus.getDefault().post(new EventMainSwitchState(false));
                    Intent service = new Intent(ActivityMain.this, ServiceAutoVolume.class);
                    stopService(service);
                }
            }
        });

        topLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainSwitch.isChecked()) {
                    Intent intent = new Intent(ActivityMain.this, ActivityAutoVolume.class);
                    startActivity(intent);
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
        });

        LinearLayout.OnClickListener itemLinearLayoutClickListener = new LinearLayout.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainSwitch.isChecked()) {
                    switch (view.getId()) {
                        case R.id.linearLayout_1:
                            if (switchPreference.getBoolean(SaveKey.ringtoneStateKey, false)) {
                                switchPreferenceEditor.putBoolean(SaveKey.ringtoneStateKey, false);
                                switchPreferenceEditor.apply();
                                setIconTV(imageView_1, textView_1, false);
                            } else {
                                switchPreferenceEditor.putBoolean(SaveKey.ringtoneStateKey, true);
                                switchPreferenceEditor.apply();
                                setIconTV(imageView_1, textView_1, true);
                            }
                            break;
                        case R.id.linearLayout_2:
                            if (switchPreference.getBoolean(SaveKey.mediaStateKey, false)) {
                                switchPreferenceEditor.putBoolean(SaveKey.mediaStateKey, false);
                                switchPreferenceEditor.apply();
                                setIconTV(imageView_2, textView_2, false);
                            } else {
                                switchPreferenceEditor.putBoolean(SaveKey.mediaStateKey, true);
                                switchPreferenceEditor.apply();
                                setIconTV(imageView_2, textView_2, true);
                            }
                            break;
                        case R.id.linearLayout_3:
                            if (switchPreference.getBoolean(SaveKey.notificationsStateKey, false)) {
                                switchPreferenceEditor.putBoolean(SaveKey.notificationsStateKey, false);
                                switchPreferenceEditor.apply();
                                setIconTV(imageView_3, textView_3, false);
                            } else {
                                switchPreferenceEditor.putBoolean(SaveKey.notificationsStateKey, true);
                                switchPreferenceEditor.apply();
                                setIconTV(imageView_3, textView_3, true);
                            }
                            break;
                        case R.id.linearLayout_4:
                            if (switchPreference.getBoolean(SaveKey.alarmStateKey, false)) {
                                switchPreferenceEditor.putBoolean(SaveKey.alarmStateKey, false);
                                switchPreferenceEditor.apply();
                                setIconTV(imageView_4, textView_4, false);
                            } else {
                                switchPreferenceEditor.putBoolean(SaveKey.alarmStateKey, true);
                                switchPreferenceEditor.apply();
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
                Intent intent = new Intent(ActivityMain.this, ActivityRangePopup.class);
                switch (view.getId()) {
                    case R.id.range_linearLayout_1:
                        intent.putExtra(SaveKey.viewName, SaveKey.ringtone);
                        break;
                    case R.id.range_linearLayout_2:
                        intent.putExtra(SaveKey.viewName, SaveKey.media);
                        break;
                    case R.id.range_linearLayout_3:
                        intent.putExtra(SaveKey.viewName, SaveKey.notifications);
                        break;
                    case R.id.range_linearLayout_4:
                        intent.putExtra(SaveKey.viewName, SaveKey.alarm);
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
    public void setEnableByMainSwitch() {
        mainTextView.setAlpha(1f);
        imageView_1.setAlpha(1f);
        imageView_2.setAlpha(1f);
        imageView_3.setAlpha(1f);
        imageView_4.setAlpha(1f);
        textView_1.setAlpha(1f);
        textView_2.setAlpha(1f);
        textView_3.setAlpha(1f);
        textView_4.setAlpha(1f);
        final TypedValue outValue = new TypedValue();
        getApplicationContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        topLinearLayout.setBackgroundResource(outValue.resourceId);
    }

    /**
     * mainSwitch 의 값에따라 항목들 비활성화
     */
    public void setDisableByMainSwitch() {
        mainTextView.setAlpha(0.8f);
        imageView_1.setAlpha(0.5f);
        imageView_2.setAlpha(0.5f);
        imageView_3.setAlpha(0.5f);
        imageView_4.setAlpha(0.5f);
        textView_1.setAlpha(0.5f);
        textView_2.setAlpha(0.5f);
        textView_3.setAlpha(0.5f);
        textView_4.setAlpha(0.5f);
        topLinearLayout.setBackground(null);
    }

    /**
     * Check if service is running
     */
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ServiceAutoVolume.class.getName().equals(service.service.getClassName())) {
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
                        ActivityCompat.requestPermissions(ActivityMain.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                    }
                }, 2000);
            }
        }
    }
}
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

public class MainActivity extends AppCompatActivity {
    //view
    TextView mainTextView, textView_1, textView_2, textView_3, textView_4,
            minRingtone, maxRingtone, minMedia, maxMedia, minNotifications, maxNotifications, minAlarm, maxAlarm;
    Switch mainSwitch, switch_1, switch_2, switch_3, switch_4;
    View highlightSwitch;
    LinearLayout topLinearLayout, linearLayout_1, linearLayout_2, linearLayout_3, linearLayout_4;
    ImageView imageView_1, imageView_2, imageView_3, imageView_4;
    //sharedPreference
    SharedPreferences switchPreference;
    SharedPreferences.Editor switchPreferenceEditor;
    SharedPreferences ringtonePreference, mediaPreference, notificationsPreference, alarmPreference;
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
        if (mainSwitch.isChecked()) setEnabled();
        else setDisabled();

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
     * ChangeMinMaxTVEvent 를 받아서 볼륨 범위 텍스트를 변경한다
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeTV(ChangeMinMaxTVEvent event) {
        if (event.isMinValue) {
            switch (event.keyName) {
                case "ringtone":
                    minRingtone.setText(String.format(getString(R.string.min_volume), event.value));
                    break;
                case "media":
                    minMedia.setText(String.format(getString(R.string.min_volume), event.value));
                    break;
                case "notifications":
                    minNotifications.setText(String.format(getString(R.string.min_volume), event.value));
                    break;
                case "alarm":
                    minAlarm.setText(String.format(getString(R.string.min_volume), event.value));
                    break;
            }
        } else {
            switch (event.keyName) {
                case "ringtone":
                    maxRingtone.setText(String.format(getString(R.string.max_volume), event.value));
                    break;
                case "media":
                    maxMedia.setText(String.format(getString(R.string.max_volume), event.value));
                    break;
                case "notifications":
                    maxNotifications.setText(String.format(getString(R.string.max_volume), event.value));
                    break;
                case "alarm":
                    maxAlarm.setText(String.format(getString(R.string.max_volume), event.value));
                    break;
            }
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
        switch_1 = findViewById(R.id.switch_1);
        switch_2 = findViewById(R.id.switch_2);
        switch_3 = findViewById(R.id.switch_3);
        switch_4 = findViewById(R.id.switch_4);

        topLinearLayout = findViewById(R.id.top_linearLayout);
        linearLayout_1 = findViewById(R.id.linearLayout_1);
        linearLayout_2 = findViewById(R.id.linearLayout_2);
        linearLayout_3 = findViewById(R.id.linearLayout_3);
        linearLayout_4 = findViewById(R.id.linearLayout_4);

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
        switchPreference = getSharedPreferences("switchState", MODE_PRIVATE);
        switchPreferenceEditor = switchPreference.edit();
        ringtonePreference = getSharedPreferences("ringtone", MODE_PRIVATE);
        mediaPreference = getSharedPreferences("media", MODE_PRIVATE);
        notificationsPreference = getSharedPreferences("notifications", MODE_PRIVATE);
        alarmPreference = getSharedPreferences("alarm", MODE_PRIVATE);

        //audioManager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    /**
     * 저장되었던 스위치 상태를 가져오는 메소드
     */
    public void reloadSwitchState() {
        Boolean isChecked = switchPreference.getBoolean(String.valueOf(mainSwitch.getTag()), false);
        Boolean isChecked1 = switchPreference.getBoolean(String.valueOf(switch_1.getTag()), false);
        Boolean isChecked2 = switchPreference.getBoolean(String.valueOf(switch_2.getTag()), false);
        Boolean isChecked3 = switchPreference.getBoolean(String.valueOf(switch_3.getTag()), false);
        Boolean isChecked4 = switchPreference.getBoolean(String.valueOf(switch_4.getTag()), false);
        mainSwitch.setChecked(isChecked);
        switch_1.setChecked(isChecked1);
        switch_2.setChecked(isChecked2);
        switch_3.setChecked(isChecked3);
        switch_4.setChecked(isChecked4);
    }

    /**
     * 저장되었던 최소/최대 볼륨을 가져오는 메소드
     */
    public void reloadTextState() {
        int sMinRingtone = ringtonePreference.getInt("minVolume", 0);
        int sMaxRingtone = ringtonePreference.getInt("maxVolume", audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        int sMinMedia = mediaPreference.getInt("minVolume", 0);
        int sMaxMedia = mediaPreference.getInt("maxVolume", audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int sMinNotifications = notificationsPreference.getInt("minVolume", 0);
        int sMaxNotifications = notificationsPreference.getInt("maxVolume", audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));
        int sMinAlarm = alarmPreference.getInt("minVolume", 0);
        int sMaxAlarm = alarmPreference.getInt("maxVolume", audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));

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
        //mainSwitch 의 리스너
        mainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //권한 허용됨, 상태저장
                String switchTag = String.valueOf(mainSwitch.getTag());
                switchPreferenceEditor.putBoolean(switchTag, isChecked);
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
                        switchPreferenceEditor.putBoolean(String.valueOf(mainSwitch.getTag()), false);
                        switchPreferenceEditor.commit();
                    } else { //권한 허용됨
                        //활성화
                        setEnabled();
                        //서비스 시작
                        Intent service = new Intent(MainActivity.this, AutoVolumeService.class);
                        startService(service);
                        EventBus.getDefault().post(new ChangeSwitchStateEvent(true));
                    }
                } else {
                    //비활성화
                    setDisabled();
                    //서비스 종료
                    EventBus.getDefault().post(new ChangeSwitchStateEvent(false));
                    Intent service = new Intent(MainActivity.this, AutoVolumeService.class);
                    stopService(service);
                }
            }
        });

        //바꿘 스위치의 참조로 항목의 상태를 저장하는 리스너
        Switch.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String switchTag = String.valueOf(buttonView.getTag());
                switchPreferenceEditor.putBoolean(switchTag, isChecked);
                switchPreferenceEditor.apply();
            }
        };

        switch_1.setOnCheckedChangeListener(onCheckedChangeListener);
        switch_2.setOnCheckedChangeListener(onCheckedChangeListener);
        switch_3.setOnCheckedChangeListener(onCheckedChangeListener);
        switch_4.setOnCheckedChangeListener(onCheckedChangeListener);

        //항목과 mainView 의 클릭을 받아 액티비티를 여는 리스너
        LinearLayout.OnClickListener linearLayoutOnClickListener = new LinearLayout.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainSwitch.isChecked()) {
                    //체크 되있으면 원래대로 실행
                    Intent intent = new Intent(MainActivity.this, RangePopupActivity.class);
                    switch (view.getId()) {
                        case R.id.top_linearLayout:
                            intent = new Intent(MainActivity.this, AutoVolumeActivity.class);
                            break;
                        case R.id.linearLayout_1:
                            intent.putExtra("viewName", "view_1");
                            break;
                        case R.id.linearLayout_2:
                            intent.putExtra("viewName", "view_2");
                            break;
                        case R.id.linearLayout_3:
                            intent.putExtra("viewName", "view_3");
                            break;
                        case R.id.linearLayout_4:
                            intent.putExtra("viewName", "view_4");
                            break;
                    }
                    startActivity(intent);
                } else {
                    //안되있으면 스위치 강조
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

        topLinearLayout.setOnClickListener(linearLayoutOnClickListener);
        linearLayout_1.setOnClickListener(linearLayoutOnClickListener);
        linearLayout_2.setOnClickListener(linearLayoutOnClickListener);
        linearLayout_3.setOnClickListener(linearLayoutOnClickListener);
        linearLayout_4.setOnClickListener(linearLayoutOnClickListener);
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
     * mainSwitch 의 값에따라 항목들 활성화
     */
    public void setEnabled() {
        mainTextView.setAlpha(1f);
        textView_1.setAlpha(1f);
        textView_2.setAlpha(1f);
        textView_3.setAlpha(1f);
        textView_4.setAlpha(1f);
        switch_1.setEnabled(true);
        switch_2.setEnabled(true);
        switch_3.setEnabled(true);
        switch_4.setEnabled(true);
        final TypedValue outValue = new TypedValue();
        getApplicationContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        topLinearLayout.setBackgroundResource(outValue.resourceId);
        linearLayout_1.setBackgroundResource(outValue.resourceId);
        linearLayout_2.setBackgroundResource(outValue.resourceId);
        linearLayout_3.setBackgroundResource(outValue.resourceId);
        linearLayout_4.setBackgroundResource(outValue.resourceId);
        imageView_1.setAlpha(1f);
        imageView_2.setAlpha(1f);
        imageView_3.setAlpha(1f);
        imageView_4.setAlpha(1f);

    }

    /**
     * mainSwitch 의 값에따라 항목들 비활성화
     */
    public void setDisabled() {
        mainTextView.setAlpha(0.8f);
        textView_1.setAlpha(0.5f);
        textView_2.setAlpha(0.5f);
        textView_3.setAlpha(0.5f);
        textView_4.setAlpha(0.5f);
        switch_1.setEnabled(false);
        switch_2.setEnabled(false);
        switch_3.setEnabled(false);
        switch_4.setEnabled(false);
        topLinearLayout.setBackground(null);
        linearLayout_1.setBackground(null);
        linearLayout_2.setBackground(null);
        linearLayout_3.setBackground(null);
        linearLayout_4.setBackground(null);
        imageView_1.setAlpha(0.5f);
        imageView_2.setAlpha(0.5f);
        imageView_3.setAlpha(0.5f);
        imageView_4.setAlpha(0.5f);

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
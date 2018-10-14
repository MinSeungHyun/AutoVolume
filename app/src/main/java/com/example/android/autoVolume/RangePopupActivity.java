package com.example.android.autoVolume;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.Window;
import android.widget.TextView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import org.greenrobot.eventbus.EventBus;

public class RangePopupActivity extends Activity {
    //views
    CrystalRangeSeekbar rangeSeekBar;
    TextView topTextView, leftText, rightText;
    //sharedPreferences
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    //audioManager
    AudioManager audioManager;
    //오디오 타입 식별을 위한 변수들
    int audioType;
    String audioName;
    String keyName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.range_popup);

        //디스플레이 해상도를 가져와서 크기를 조절
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Window window = this.getWindow();
        int x = (int) (size.x * 0.9f);
        window.setLayout(x, ActionBar.LayoutParams.WRAP_CONTENT);


        //참조
        topTextView = findViewById(R.id.topTextView);
        leftText = findViewById(R.id.leftText);
        rightText = findViewById(R.id.rightText);
        rangeSeekBar = findViewById(R.id.seekBar);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        //클릭된 뷰를 받아와서 각종 변수 설정
        Intent intent = getIntent();
        String viewName = intent.getStringExtra("viewName");

        switch (viewName) {
            case "view_1":
                audioType = AudioManager.STREAM_RING;
                audioName = getString(R.string.ringtone);
                keyName = "ringtone";
                break;
            case "view_2":
                audioType = AudioManager.STREAM_MUSIC;
                audioName = getString(R.string.media);
                keyName = "media";
                break;
            case "view_3":
                audioType = AudioManager.STREAM_NOTIFICATION;
                audioName = getString(R.string.notifications);
                keyName = "notifications";
                break;
            case "view_4":
                audioType = AudioManager.STREAM_ALARM;
                audioName = getString(R.string.alarm);
                keyName = "alarm";
                break;
        }

        //sharedPreferences 설정
        sharedPreferences = getSharedPreferences(keyName, 0);
        editor = sharedPreferences.edit();

        //저장된 값 설정, 초기 설정
        rangeSeekBar.setMinStartValue(sharedPreferences.getInt("minVolume", 0))
                .setMaxStartValue(sharedPreferences.getInt("maxVolume", audioManager.getStreamMaxVolume(audioType)))
                .setGap(1)
                .setMaxValue(audioManager.getStreamMaxVolume(audioType))
                .apply();

        String titleText = audioName + " " + getString(R.string.volume_range);
        topTextView.setText(titleText);

        //seekBar 리스너
        rangeSeekBar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener() {
            @Override
            public void valueChanged(Number minValue, Number maxValue) {
                editor.putInt("minVolume", minValue.intValue());
                editor.putInt("maxVolume", maxValue.intValue());
                editor.apply();


                leftText.setText(String.valueOf(minValue));
                rightText.setText(String.valueOf(maxValue));

                //이벤트버스로 메인액티비티의 텍스트뷰 변경
                EventBus.getDefault().post(new ChangeMinMaxTVEvent(true, keyName, minValue.intValue()));
                EventBus.getDefault().post(new ChangeMinMaxTVEvent(false, keyName, maxValue.intValue()));
            }
        });
    }
}

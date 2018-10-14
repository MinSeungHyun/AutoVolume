package com.example.android.autoVolume;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AutoVolumeActivity extends AppCompatActivity {
    static final String micLevelKey = "mic_level";
    static final String micSensitivityKey = "mic_sensitivity";
    static final String intervalKey = "interval";
    static public Boolean isRunning = false;
    ProgressBar noiseProgressBar;
    SeekBar micLevelSeekBar, micSensitivitySeekBar, intervalSeekBar;
    TextView intervalTextView;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.auto_volume);
        setContentView(R.layout.auto_volume);
        EventBus.getDefault().register(this); //EventBus register
        isRunning = true;

        getReferences();
        reloadStates();
        setListener();

        //초기화
        EventBus.getDefault().post(new ChangeMIcLevelEvent(micSensitivitySeekBar.getProgress()));
        EventBus.getDefault().post(new ChangeMIcLevelEvent(micLevelSeekBar.getProgress()));
        noiseProgressBar.setMax(130 - sharedPreferences.getInt(micSensitivityKey, 50));
        //
    }

    /**
     * 앱이 종료될때 이벤트버스 unregister
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        EventBus.getDefault().unregister(this); //EventBus unregister
    }

    /**
     * ChangeProgressEvent 를 받는 메소드를 작성한다
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeProgressEvent(ChangeProgressEvent event) {
        noiseProgressBar.setProgress(event.decibel);
    }

    /**
     * 참조들
     */
    @SuppressLint("CommitPrefEdits")
    private void getReferences() {
        //뷰 참조
        noiseProgressBar = findViewById(R.id.pb);
        micLevelSeekBar = findViewById(R.id.levelSb);
        micSensitivitySeekBar = findViewById(R.id.sensitivitySb);
        intervalSeekBar = findViewById(R.id.intervalSb);
        intervalTextView = findViewById(R.id.intervalTV);

        //sharedPreferences 참조
        sharedPreferences = getSharedPreferences("autoVolume", MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * reload states
     */
    private void reloadStates() {
        //저장했던 값 불러오기
        micLevelSeekBar.setProgress(sharedPreferences.getInt(micLevelKey, 30));
        micSensitivitySeekBar.setProgress(sharedPreferences.getInt(micSensitivityKey, 50));

        int progressValue = sharedPreferences.getInt(intervalKey, 6);
        intervalSeekBar.setProgress(progressValue);
        progressValue *= 5;

        if (progressValue < 10) progressValue = 10;
        long minute = TimeUnit.SECONDS.toMinutes(progressValue);
        long second = progressValue - TimeUnit.SECONDS.toMinutes(progressValue) * 60;

        if (Locale.getDefault().getDisplayLanguage().equals("English")) {
            if (minute > 1) {
                String text = minute + getString(R.string.minute) + "s " + second + getString(R.string.second);
                intervalTextView.setText(text);
            }
            if (second > 1) {
                String text = minute + getString(R.string.minute) + " " + second + getString(R.string.second) + "s";
                intervalTextView.setText(text);
            }
            if (second > 1 && minute > 1) {
                String text = minute + getString(R.string.minute) + "s " + second + getString(R.string.second) + "s";
                intervalTextView.setText(text);
            }
        } else {
            String text = minute + getString(R.string.minute) + " " + second + getString(R.string.second);
            intervalTextView.setText(text);
        }
    }

    /**
     * 리스너 설정
     */
    private void setListener() {
        //micLevelSeekBar 값 저장 리스너
        micLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editor.putInt(micLevelKey, progress);
                editor.apply();
                EventBus.getDefault().post(new ChangeMIcLevelEvent(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //micSensitivitySeekBar 값 저장 리스너
        micSensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editor.putInt(micSensitivityKey, progress);
                editor.apply();
                noiseProgressBar.setMax(130 - progress);
                EventBus.getDefault().post(new ChangeMicSensitivityEvent(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //intervalSeekBar 값 저장 리스너
        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editor.putInt(intervalKey, progress);
                editor.apply();
                progress *= 5;
                if (progress < 10) progress = 10;

                long minute = TimeUnit.SECONDS.toMinutes(progress);
                long second = progress - TimeUnit.SECONDS.toMinutes(progress) * 60;
                if (Locale.getDefault().getDisplayLanguage().equals("English")) {
                    if (second > 1 && minute > 1) {
                        String text = minute + getString(R.string.minute) + "s " + second + getString(R.string.second) + "s";
                        intervalTextView.setText(text);
                    } else if (minute > 1) {
                        String text = minute + getString(R.string.minute) + "s " + second + getString(R.string.second);
                        intervalTextView.setText(text);
                    } else if (second > 1) {
                        String text = minute + getString(R.string.minute) + " " + second + getString(R.string.second) + "s";
                        intervalTextView.setText(text);
                    } else {
                        String text = minute + getString(R.string.minute) + " " + second + getString(R.string.second);
                        intervalTextView.setText(text);
                    }
                } else {
                    String text = minute + getString(R.string.minute) + " " + second + getString(R.string.second);
                    intervalTextView.setText(text);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    /**
     * 액션바의 뒤로가기버튼 누르는것을 휴대폰의 뒤로가기로 매핑
     */
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

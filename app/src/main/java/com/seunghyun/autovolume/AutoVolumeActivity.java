package com.seunghyun.autovolume;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AutoVolumeActivity extends AppCompatActivity {
    static boolean isRunning = false;
    private ProgressBar noiseProgressBar;
    private SeekBar micLevelSeekBar, micSensitivitySeekBar, intervalSeekBar;
    private TextView intervalTextView;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.detail_setting);
        setContentView(R.layout.activity_auto_volume);
        isRunning = true;

        getReferences();
        reloadStates();
        setListener();

        //초기화
        SaveValues.StateValues.micSensitivity = micSensitivitySeekBar.getProgress();
        SaveValues.StateValues.micLevel = micLevelSeekBar.getProgress();
        noiseProgressBar.setMax(SaveValues.DefValues.noiseProgressBarMax - sharedPreferences.getInt(SaveValues.Keys.micSensitivity, SaveValues.DefValues.micSensitivity));

        if (!MeasuringSoundThread.isRunning)
            new MeasuringSoundThread().start();
        new ChangeProgressBarThread().start();
    }

    /**
     * 앱이 종료될때 이벤트버스 unregister
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (!AutoVolumeService.isRunning) new MeasuringSoundThread().interrupt();
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
        sharedPreferences = getSharedPreferences(SaveValues.Keys.autoVolumePreference, MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * reload states
     */
    private void reloadStates() {
        //저장했던 값 불러오기
        micLevelSeekBar.setProgress(sharedPreferences.getInt(SaveValues.Keys.micLevel, SaveValues.DefValues.micLevel));
        micSensitivitySeekBar.setProgress(sharedPreferences.getInt(SaveValues.Keys.micSensitivity, SaveValues.DefValues.micSensitivity));

        int progressValue = sharedPreferences.getInt(SaveValues.Keys.interval, SaveValues.DefValues.changeInterval);
        intervalSeekBar.setProgress(progressValue);
        progressValue *= 5;

        if (progressValue < 1) progressValue = 1;
        long minute = TimeUnit.SECONDS.toMinutes(progressValue);
        long second = progressValue - TimeUnit.SECONDS.toMinutes(progressValue) * 60;

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

    /**
     * 리스너 설정
     */
    private void setListener() {
        //micLevelSeekBar 값 저장 리스너
        micLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editor.putInt(SaveValues.Keys.micLevel, progress);
                editor.apply();
                SaveValues.StateValues.micLevel = progress;
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
                editor.putInt(SaveValues.Keys.micSensitivity, progress);
                editor.apply();
                noiseProgressBar.setMax(SaveValues.DefValues.noiseProgressBarMax - progress);
                SaveValues.StateValues.micSensitivity = micSensitivitySeekBar.getProgress();
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
                editor.putInt(SaveValues.Keys.interval, progress);
                editor.apply();
                progress *= 5;
                if (progress < 1) progress = 1;
                SaveValues.StateValues.changeInterval = progress;

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

    private class ChangeProgressBarThread extends Thread {
        @Override
        public void run() {
            while (isRunning) {
                int decibel = MeasuringSoundThread.decibel;
                decibel += (micLevelSeekBar.getProgress() - 100);
                noiseProgressBar.setProgress(decibel);

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

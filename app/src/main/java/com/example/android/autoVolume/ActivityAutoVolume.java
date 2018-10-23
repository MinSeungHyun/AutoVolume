package com.example.android.autoVolume;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ActivityAutoVolume extends AppCompatActivity {
    static boolean isRunning = false;
    private ProgressBar noiseProgressBar;
    private SeekBar micLevelSeekBar, micSensitivitySeekBar, intervalSeekBar;
    private TextView intervalTextView;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.auto_volume);
        setContentView(R.layout.auto_volume);
        isRunning = true;

        getReferences();
        reloadStates();
        setListener();

        //초기화
        EventBus.getDefault().post(new EventMIcLevel(micSensitivitySeekBar.getProgress()));
        EventBus.getDefault().post(new EventMIcLevel(micLevelSeekBar.getProgress()));
        noiseProgressBar.setMax(130 - sharedPreferences.getInt(SaveKey.micSensitivityKey, 50));

        if (!ThreadMeasuringSound.isRunning)
            new ThreadMeasuringSound().start();
        new ChangeProgressBarThread().start();
    }

    /**
     * 앱이 종료될때 이벤트버스 unregister
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (!ServiceAutoVolume.isRunning) new ThreadMeasuringSound().interrupt();
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
        sharedPreferences = getSharedPreferences(SaveKey.autoVolumePreferenceKey, MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * reload states
     */
    private void reloadStates() {
        //저장했던 값 불러오기
        micLevelSeekBar.setProgress(sharedPreferences.getInt(SaveKey.micLevelKey, 70));
        micSensitivitySeekBar.setProgress(sharedPreferences.getInt(SaveKey.micSensitivityKey, 50));

        int progressValue = sharedPreferences.getInt(SaveKey.intervalKey, 6);
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
                editor.putInt(SaveKey.micLevelKey, progress);
                editor.apply();
                EventBus.getDefault().post(new EventMIcLevel(progress)); //To ServiceAutoVolume
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
                editor.putInt(SaveKey.micSensitivityKey, progress);
                editor.apply();
                noiseProgressBar.setMax(130 - progress);
                EventBus.getDefault().post(new EventMicSensitivity(progress)); //To ServiceAutoVolume
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
                editor.putInt(SaveKey.intervalKey, progress);
                editor.apply();
                progress *= 5;
                if (progress < 1) progress = 1;
                EventBus.getDefault().post(new EventChangeInterval((progress))); //To ServiceAutoVolume

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
                int decibel = ThreadMeasuringSound.decibel;
                decibel += (micLevelSeekBar.getProgress() - 100);
                noiseProgressBar.setProgress(decibel);

                Log.d("Notice", "AutoVolume Thread Running");
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

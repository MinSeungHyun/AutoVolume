package com.seunghyun.autovolume;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;

public class AutoVolumeActivity extends AppCompatActivity {
    static boolean isRunning = false;
    private ProgressBar noiseProgressBar;
    private SeekBar micLevelSeekBar, micSensitivitySeekBar, intervalSeekBar;
    private TextView intervalTextView;
    private LinearLayout guideView_1, guideView_2, guideView_3, guideView_4;
    private SharedPreferences autoVolumePreference, isGuideShownPreference;
    private SharedPreferences.Editor autoVolumeEditor, isGuideShownEditor;

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
        noiseProgressBar.setMax(SaveValues.DefValues.noiseProgressBarMax - autoVolumePreference.getInt(SaveValues.Keys.micSensitivity, SaveValues.DefValues.micSensitivity));

        if (!MeasuringSoundThread.isRunning)
            new MeasuringSoundThread().start();
        new ChangeProgressBarThread().start();

        makeGuides();
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
        guideView_1 = findViewById(R.id.guide_view_1);
        guideView_2 = findViewById(R.id.guide_view_2);
        guideView_3 = findViewById(R.id.guide_view_3);
        guideView_4 = findViewById(R.id.guide_view_4);

        //autoVolumePreference 참조
        autoVolumePreference = getSharedPreferences(SaveValues.Keys.autoVolumePreference, MODE_PRIVATE);
        isGuideShownPreference = getSharedPreferences(SaveValues.isGuideShownPreference.preferenceName, MODE_PRIVATE);
        autoVolumeEditor = autoVolumePreference.edit();
        isGuideShownEditor = isGuideShownPreference.edit();
    }

    /**
     * reload states
     */
    private void reloadStates() {
        //저장했던 값 불러오기
        micLevelSeekBar.setProgress(autoVolumePreference.getInt(SaveValues.Keys.micLevel, SaveValues.DefValues.micLevel));
        micSensitivitySeekBar.setProgress(autoVolumePreference.getInt(SaveValues.Keys.micSensitivity, SaveValues.DefValues.micSensitivity));

        int progressValue = autoVolumePreference.getInt(SaveValues.Keys.interval, SaveValues.DefValues.changeInterval);
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
                autoVolumeEditor.putInt(SaveValues.Keys.micLevel, progress);
                autoVolumeEditor.apply();
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
                autoVolumeEditor.putInt(SaveValues.Keys.micSensitivity, progress);
                autoVolumeEditor.apply();
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
                autoVolumeEditor.putInt(SaveValues.Keys.interval, progress);
                autoVolumeEditor.apply();
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
     * 가이드뷰 생성
     */
    private void makeGuides() {
        if (!isGuideShownPreference.getBoolean(SaveValues.isGuideShownPreference.autoVolumeActivity, false)) {
            new GuideView.Builder(this)
                    .setTargetView(guideView_1)
                    .setTitle(getString(R.string.mic_input))
                    .setContentSpan(SaveValues.GuideViewValues.contentSpan(getString(R.string.mic_input_description)))
                    .setTitleTypeFace(Typeface.defaultFromStyle(Typeface.BOLD))
                    .setTitleTextSize(SaveValues.GuideViewValues.titleTextSize)
                    .setContentTextSize(SaveValues.GuideViewValues.contentTextSize)
                    .setGravity(GuideView.Gravity.center)
                    .setDismissType(GuideView.DismissType.outside)
                    .setGuideListener(new GuideView.GuideListener() {
                        @Override
                        public void onDismiss(View view) {
                            makeGuide_2();
                        }
                    })
                    .build()
                    .show();

        }
    }

    private void makeGuide_2() {
        new GuideView.Builder(this)
                .setTargetView(guideView_2)
                .setTitle(getString(R.string.mic_volume))
                .setContentSpan(SaveValues.GuideViewValues.contentSpan(getString(R.string.mic_volume_description)))
                .setTitleTypeFace(Typeface.defaultFromStyle(Typeface.BOLD))
                .setTitleTextSize(SaveValues.GuideViewValues.titleTextSize)
                .setContentTextSize(SaveValues.GuideViewValues.contentTextSize)
                .setGravity(GuideView.Gravity.center)
                .setDismissType(GuideView.DismissType.outside)
                .setGuideListener(new GuideView.GuideListener() {
                    @Override
                    public void onDismiss(View view) {
                        makeGuide_3();
                    }
                })
                .build()
                .show();
    }

    private void makeGuide_3() {
        new GuideView.Builder(this)
                .setTargetView(guideView_3)
                .setTitle(getString(R.string.mic_sensitive))
                .setContentSpan(SaveValues.GuideViewValues.contentSpan(getString(R.string.mic_sensitive_description)))
                .setTitleTypeFace(Typeface.defaultFromStyle(Typeface.BOLD))
                .setTitleTextSize(SaveValues.GuideViewValues.titleTextSize)
                .setContentTextSize(SaveValues.GuideViewValues.contentTextSize)
                .setGravity(GuideView.Gravity.center)
                .setDismissType(GuideView.DismissType.outside)
                .setGuideListener(new GuideView.GuideListener() {
                    @Override
                    public void onDismiss(View view) {
                        makeGuide_4();
                    }
                })
                .build()
                .show();
    }

    private void makeGuide_4() {
        new GuideView.Builder(this)
                .setTargetView(guideView_4)
                .setTitle(getString(R.string.volume_interval))
                .setContentSpan(SaveValues.GuideViewValues.contentSpan(getString(R.string.volume_interval_description)))
                .setTitleTypeFace(Typeface.defaultFromStyle(Typeface.BOLD))
                .setTitleTextSize(SaveValues.GuideViewValues.titleTextSize)
                .setContentTextSize(SaveValues.GuideViewValues.contentTextSize)
                .setGravity(GuideView.Gravity.center)
                .setDismissType(GuideView.DismissType.outside)
                .setGuideListener(new GuideView.GuideListener() {
                    @Override
                    public void onDismiss(View view) {
                        isGuideShownEditor.putBoolean(SaveValues.isGuideShownPreference.autoVolumeActivity, true);
                        isGuideShownEditor.apply();
                    }
                })
                .build()
                .show();
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

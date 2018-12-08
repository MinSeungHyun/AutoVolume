package com.seunghyun.autovolume;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class TurnOffActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences(SaveValues.Keys.switchPreference, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //MainActivity 실행중일경우 알림 생성
        if (MainActivity.isRunning) {
            Toast.makeText(this, getString(R.string.turn_off_in_app), Toast.LENGTH_LONG).show();
        } else {
            //서비스 종료
            SaveValues.StateValues.isAutoVolumeOn = false;
            Intent service = new Intent(TurnOffActivity.this, AutoVolumeService.class);
            stopService(service);
            //모두 비활성화
            editor.putBoolean(SaveValues.Keys.ringtoneState, false);
            editor.putBoolean(SaveValues.Keys.mediaState, false);
            editor.putBoolean(SaveValues.Keys.notificationsState, false);
            editor.putBoolean(SaveValues.Keys.alarmState, false);
            editor.apply();
        }
        finish();
    }
}

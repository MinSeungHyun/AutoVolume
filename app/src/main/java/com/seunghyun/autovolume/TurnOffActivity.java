package com.seunghyun.autovolume;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class TurnOffActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //서비스 종료
        SaveValues.StateValues.isAutoVolumeOn = false;
        Intent service = new Intent(TurnOffActivity.this, AutoVolumeService.class);
        stopService(service);
        //MainActivity 실행중일경우 재시작
        if (MainActivity.isRunning) {
            sendBroadcast(new Intent("finish_activity"));
            startActivity(new Intent(TurnOffActivity.this, MainActivity.class));
        }
        finish();
    }
}

package com.example.android.autoVolume;

class EventMinMaxTV {
    boolean isMinValue;
    String keyName;
    int value;

    EventMinMaxTV(boolean isMinValue, String keyName, int value) {
        this.isMinValue = isMinValue;
        this.keyName = keyName;
        this.value = value;
    }
}

package com.example.android.autoVolume;

class EventMinMaxValue {
    String keyName;
    int minValue;
    int maxValue;

    EventMinMaxValue(String keyName, int minValue, int maxValue) {
        this.keyName = keyName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
}

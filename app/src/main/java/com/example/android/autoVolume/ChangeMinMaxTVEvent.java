package com.example.android.autoVolume;

class ChangeMinMaxTVEvent {
    boolean isMinValue;
    String keyName;
    int value;

    ChangeMinMaxTVEvent(boolean isMinValue, String keyName, int value) {
        this.isMinValue = isMinValue;
        this.keyName = keyName;
        this.value = value;
    }
}

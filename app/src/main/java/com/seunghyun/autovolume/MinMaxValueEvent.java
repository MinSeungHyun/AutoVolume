package com.seunghyun.autovolume;

class MinMaxValueEvent {
    String keyName;
    int minValue;
    int maxValue;

    MinMaxValueEvent(String keyName, int minValue, int maxValue) {
        this.keyName = keyName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
}

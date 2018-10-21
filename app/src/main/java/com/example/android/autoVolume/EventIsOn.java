package com.example.android.autoVolume;

class EventIsOn {
    boolean ringtone, media, notifications, alarm;

    EventIsOn(boolean ringtone, boolean media, boolean notifications, boolean alarm) {
        this.ringtone = ringtone;
        this.media = media;
        this.notifications = notifications;
        this.alarm = alarm;
    }
}

package com.example.android.autoVolume;

class SaveKey {
    //ActivityRangePopup
    static final String rangePreferenceKey = "volume_range";
    static final String ringtoneMinKey = "ringtone_min";
    static final String ringtoneMaxKey = "ringtone_max";
    static final String mediaMinKey = "media_min";
    static final String mediaMaxKey = "media_max";
    static final String notificationsMinKey = "notifications_min";
    static final String notificationsMaxKey = "notifications_max";
    static final String alarmMinKey = "alarm_min";
    static final String alarmMaxKey = "alarm_max";

    //ActivityMain
    static final String switchPreferenceKey = "switch_state";
    static final String ringtoneStateKey = "ringtone_state";
    static final String mediaStateKey = "media_state";
    static final String notificationsStateKey = "notifications_state";
    static final String alarmStateKey = "alarm_state";

    //ActivityMain to ActivityRangePopup Intent value
    static final String viewName= "view_name";
    static final String ringtone= "ringtone";
    static final String media= "media";
    static final String notifications= "notifications";
    static final String alarm= "alarm";

    //ActivityAutoVolume
    static final String autoVolumePreferenceKey = "auto_volume";
    static final String micLevelKey = "mic_level";
    static final String micSensitivityKey = "mic_sensitivity";
    static final String intervalKey = "interval";
}

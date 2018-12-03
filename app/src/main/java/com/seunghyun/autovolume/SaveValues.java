package com.seunghyun.autovolume;

import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;

class SaveValues {
    static final class Keys {
        //RangePopupActivity
        static final String rangePreference = "volume_range";
        static final String ringtoneMin = "ringtone_min";
        static final String ringtoneMax = "ringtone_max";
        static final String mediaMin = "media_min";
        static final String mediaMax = "media_max";
        static final String notificationsMin = "notifications_min";
        static final String notificationsMax = "notifications_max";
        static final String alarmMin = "alarm_min";
        static final String alarmMax = "alarm_max";

        //MainActivity
        static final String switchPreference = "switch_state";
        static final String ringtoneState = "ringtone_state";
        static final String mediaState = "media_state";
        static final String notificationsState = "notifications_state";
        static final String alarmState = "alarm_state";

        //MainActivity to RangePopupActivity Intent value
        static final String viewName = "view_name";
        static final String ringtone = "ringtone";
        static final String media = "media";
        static final String notifications = "notifications";
        static final String alarm = "alarm";

        //AutoVolumeActivity
        static final String autoVolumePreference = "activity_auto_volume";
        static final String micLevel = "mic_level";
        static final String micSensitivity = "mic_sensitivity";
        static final String interval = "interval";
    }

    static final class isGuideShownPreference {
        static final String preferenceName = "is_guide_shown";
        static final String detailSetting = "detail_setting";
        static final String autoVolumeActivity = "auto_volume_activity";
    }

    static final class GuideViewValues {
        static final int titleTextSize = 20;
        static final int contentTextSize = 16;
        static Spannable contentSpan(String text) {
            return (Spannable) Html.fromHtml(String.format("<font color='#43a047'>%s</p>", text));
        }
    }

    static class StateValues {
        static int micLevel;
        static int micSensitivity;
        static int changeInterval;

        static boolean isAutoVolumeOn;
        static boolean isRingtoneOn;
        static boolean isMediaOn;
        static boolean isNotificationsOn;
        static boolean isAlarmOn;
    }

    static final class DefValues {
        static final boolean ringtone = false;
        static final boolean media = false;
        static final boolean notifications = false;
        static final boolean alarm = false;

        static final int micLevel = 100;
        static final int micSensitivity = 50;
        static final int changeInterval = 6;
        static final int noiseProgressBarMax = 130;
    }
}

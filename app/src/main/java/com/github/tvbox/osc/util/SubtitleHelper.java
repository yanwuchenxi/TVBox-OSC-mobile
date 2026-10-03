package com.github.tvbox.osc.util;

import android.app.Activity;


public class SubtitleHelper {

    public static int getSubtitleTextAutoSize(Activity activity) {
        double screenSqrt = ScreenUtils.getSqrt(activity);
        int subtitleTextSize = 16;
        if (screenSqrt > 7.0 && screenSqrt <= 13.0) {
            subtitleTextSize = 24;
        } else if (screenSqrt > 13.0 && screenSqrt <= 50.0) {
            subtitleTextSize = 36;
        } else if (screenSqrt > 50.0) {
            subtitleTextSize = 46;
        }
        return subtitleTextSize;
    }

    public static int getTextSize(Activity activity) {
        int autoSize = getSubtitleTextAutoSize(activity);
        int subtitleConfigSize = ConfigStore.getInt(HawkConfig.SUBTITLE_TEXT_SIZE, autoSize);
        return subtitleConfigSize;
    }

    public static void setTextSize(int size) {
        ConfigStore.putInt(HawkConfig.SUBTITLE_TEXT_SIZE, size);
    }

    public static int getTimeDelay() {
        int subtitleConfigTimeDelay = ConfigStore.getInt(HawkConfig.SUBTITLE_TIME_DELAY, 0);
        return subtitleConfigTimeDelay;
    }

    public static void setTimeDelay(int delay) {
        ConfigStore.putInt(HawkConfig.SUBTITLE_TIME_DELAY, delay);
    }

}

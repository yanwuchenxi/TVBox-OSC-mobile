package com.github.tvbox.osc.player;

import com.github.tvbox.osc.util.ConfigStore;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.M3u8AdFilter;

/**
 * 播放管线：m3u8 净化档位读取与过滤，从 PlayFragment 拆出。
 */
public final class M3u8PurifyHelper {
    private M3u8PurifyHelper() {
    }

    public static int currentLevel() {
        return ConfigStore.getInt(HawkConfig.VIDEO_PURIFY_LEVEL,
                ConfigStore.getBool(HawkConfig.VIDEO_PURIFY, true) ? 1 : 0);
    }

    public static boolean isEnabled() {
        return currentLevel() > 0;
    }

    public static String filter(String tsUrlPre, String m3u8Content) {
        try {
            return M3u8AdFilter.filter(tsUrlPre, m3u8Content, currentLevel());
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}

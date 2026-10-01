package com.github.tvbox.osc.player;

import com.github.tvbox.osc.bean.VodInfo;

/**
 * 字幕缓存 key 生成，从 PlayFragment 拆出。
 */
public final class SubtitleCacheKey {
    private SubtitleCacheKey() {
    }

    public static String of(VodInfo info, String seriesName) {
        if (info == null) return "";
        return info.sourceKey + "-" + info.id + "-" + info.playFlag + "-" + info.playIndex
                + "-" + (seriesName != null ? seriesName : "") + "-subt";
    }
}

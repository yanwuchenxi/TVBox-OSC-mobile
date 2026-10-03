package com.github.tvbox.osc.util;

import com.orhanobut.hawk.Hawk;

/**
 * 健康检测结果缓存，避免每次进首页都打满请求。
 */
public class HealthCheckCache {
    /** 默认缓存 6 小时 */
    public static final long TTL_MS = 6L * 60 * 60 * 1000;

    public static boolean isFresh(String tsKey) {
        long ts = ConfigStore.getLong(tsKey, 0L);
        return ts > 0 && (System.currentTimeMillis() - ts) < TTL_MS;
    }

    public static void touch(String tsKey) {
        ConfigStore.putLong(tsKey, System.currentTimeMillis());
    }

    public static void invalidate(String tsKey) {
        Hawk.delete(tsKey);
    }
}

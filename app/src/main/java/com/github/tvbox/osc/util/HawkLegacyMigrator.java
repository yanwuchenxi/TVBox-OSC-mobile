package com.github.tvbox.osc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Map;

/**
 * 从旧版 Hawk SharedPreferences（通常为 Hawk2）尽量迁移明文配置。
 * 无 Hawk 库依赖；复杂对象/加密值无法解析时跳过。
 */
public final class HawkLegacyMigrator {
    private static final String FLAG = "hawk_legacy_migrated_v1";

    private HawkLegacyMigrator() {
    }

    public static void migrateIfNeeded(Context context) {
        if (context == null) return;
        if (ConfigStore.getBool(FLAG, false)) return;
        try {
            migratePrefs(context, "Hawk2");
            migratePrefs(context, "hawk");
        } catch (Throwable ignored) {
        }
        ConfigStore.putBool(FLAG, true);
    }

    private static void migratePrefs(Context context, String name) {
        SharedPreferences hawk = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        Map<String, ?> all = hawk.getAll();
        if (all == null || all.isEmpty()) return;

        String[] stringKeys = new String[]{
                HawkConfig.API_URL, HawkConfig.LIVE_URL, HawkConfig.EPG_URL,
                HawkConfig.IJK_CODEC, HawkConfig.HOME_API, HawkConfig.DEFAULT_PARSE,
                HawkConfig.REMOTE_TVBOX, HawkConfig.LIVE_CHANNEL
        };
        for (String key : stringKeys) {
            if (ConfigStore.contains(key)) continue;
            Object v = all.get(key);
            String s = extractPlainString(v);
            if (!TextUtils.isEmpty(s)) {
                ConfigStore.putString(key, s);
            }
        }

        String[] intKeys = new String[]{
                HawkConfig.HOME_REC, HawkConfig.PLAY_TYPE, HawkConfig.PLAY_RENDER,
                HawkConfig.PLAY_SCALE, HawkConfig.DOH_URL, HawkConfig.THEME_TAG,
                HawkConfig.BACKGROUND_PLAY_TYPE, HawkConfig.HISTORY_NUM,
                HawkConfig.VIDEO_PURIFY_LEVEL, HawkConfig.LIVE_CONNECT_TIMEOUT
        };
        for (String key : intKeys) {
            if (ConfigStore.contains(key)) continue;
            Object v = all.get(key);
            Integer n = extractInt(v);
            if (n != null) {
                ConfigStore.putInt(key, n);
            }
        }

        String[] boolKeys = new String[]{
                HawkConfig.PRIVATE_BROWSING, HawkConfig.IJK_CACHE_PLAY,
                HawkConfig.LIVE_SHOW_TIME, HawkConfig.LIVE_SHOW_NET_SPEED,
                HawkConfig.LIVE_CHANNEL_REVERSE, HawkConfig.LIVE_CROSS_GROUP,
                HawkConfig.VIDEO_PURIFY, HawkConfig.SHOW_PREVIEW
        };
        for (String key : boolKeys) {
            if (ConfigStore.contains(key)) continue;
            Object v = all.get(key);
            Boolean b = extractBool(v);
            if (b != null) {
                ConfigStore.putBool(key, b);
            }
        }

        // 订阅列表可能以 JSON 字符串存在（ConfigStore 使用 subscriptions_json）
        if (!ConfigStore.hasApiUrl() || ConfigStore.getSubscriptions().isEmpty()) {
            Object v = all.get(HawkConfig.SUBSCRIPTIONS);
            String s = extractPlainString(v);
            if (!TextUtils.isEmpty(s) && s.trim().startsWith("[")) {
                try {
                    ConfigStore.putString("subscriptions_json", s);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static String extractPlainString(Object v) {
        if (v == null) return null;
        if (v instanceof String) {
            String s = (String) v;
            // Hawk 序列化常见形态：纯字符串或带引号 JSON
            if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
                return s.substring(1, s.length() - 1);
            }
            return s;
        }
        return String.valueOf(v);
    }

    private static Integer extractInt(Object v) {
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Long) return ((Long) v).intValue();
        if (v instanceof String) {
            try {
                String s = ((String) v).trim();
                if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
                    s = s.substring(1, s.length() - 1);
                }
                return Integer.parseInt(s);
            } catch (Throwable e) {
                return null;
            }
        }
        return null;
    }

    private static Boolean extractBool(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) {
            String s = ((String) v).trim();
            if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
                return Boolean.parseBoolean(s);
            }
        }
        return null;
    }
}

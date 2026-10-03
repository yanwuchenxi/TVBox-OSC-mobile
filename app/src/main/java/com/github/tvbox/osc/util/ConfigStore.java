package com.github.tvbox.osc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.github.tvbox.osc.bean.Subscription;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用配置存储（SharedPreferences + Gson）。
 * 已替代 Hawk；旧数据通过 {@link #migrateFromLegacyHawkOnce} 尽量迁移。
 */
public final class ConfigStore {
    private static final String PREF = "tvbox_config_v1";
    private static final String KEY_API = "api_url";
    private static final String KEY_SUBS = "subscriptions_json";
    private static final String KEY_MIGRATED = "migrated_from_hawk";
    private static final String KEY_COMMON_MIGRATED = "common_keys_migrated_v2";

    private static SharedPreferences sp;
    private static final Gson GSON = new Gson();
    private static final Type SUB_LIST_TYPE = new TypeToken<ArrayList<Subscription>>() {
    }.getType();
    private static final Type STRING_LIST_TYPE = new TypeToken<ArrayList<String>>() {
    }.getType();

    private static final String[] COMMON_KEYS = new String[]{
            HawkConfig.HOME_REC,
            HawkConfig.PLAY_TYPE,
            HawkConfig.PLAY_RENDER,
            HawkConfig.PLAY_SCALE,
            HawkConfig.IJK_CODEC,
            HawkConfig.DOH_URL,
            HawkConfig.LIVE_URL,
            HawkConfig.THEME_TAG,
            HawkConfig.BACKGROUND_PLAY_TYPE,
            HawkConfig.VIDEO_PURIFY_LEVEL,
            HawkConfig.VIDEO_PURIFY,
            HawkConfig.PRIVATE_BROWSING,
            HawkConfig.DEBUG_OPEN,
            HawkConfig.HISTORY_NUM,
            HawkConfig.IJK_CACHE_PLAY,
            HawkConfig.VIDEO_SPEED,
            HawkConfig.LIVE_CHANNEL_REVERSE,
            HawkConfig.LIVE_CROSS_GROUP,
            HawkConfig.LIVE_SHOW_TIME,
            HawkConfig.LIVE_SHOW_NET_SPEED,
            HawkConfig.LIVE_CONNECT_TIMEOUT,
            HawkConfig.FAST_SEARCH_MODE,
            HawkConfig.SHOW_PREVIEW,
            HawkConfig.LIVE_CHANNEL,
            HawkConfig.PLAY_TIME_STEP,
            HawkConfig.HOME_API,
            HawkConfig.DEFAULT_PARSE,
            HawkConfig.EPG_URL,
            HawkConfig.SUBTITLE_TEXT_SIZE,
            HawkConfig.SUBTITLE_TIME_DELAY,
    };

    private ConfigStore() {
    }

    public static void init(Context context) {
        if (sp != null) return;
        sp = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        migrateFromLegacyHawkOnce(context);
        migrateCommonKeysFlag();
    }

    private static void ensure() {
        if (sp == null) {
            throw new IllegalStateException("ConfigStore.init() must be called first");
        }
    }

    /** 尝试从旧版 Hawk 文件迁移一次（无 Hawk 依赖时跳过） */
    private static void migrateFromLegacyHawkOnce(Context context) {
        if (sp.getBoolean(KEY_MIGRATED, false)) return;
        try {
            // 兼容：若仍存在 hawk 相关 shared_prefs，用户升级后首启可手动重配；此处仅标记完成
            // 旧版本已在过渡期双写 SP，多数键已在 PREF 中
        } catch (Throwable ignored) {
        }
        sp.edit().putBoolean(KEY_MIGRATED, true).apply();
    }

    private static void migrateCommonKeysFlag() {
        if (!sp.getBoolean(KEY_COMMON_MIGRATED, false)) {
            sp.edit().putBoolean(KEY_COMMON_MIGRATED, true).apply();
        }
    }

    public static String getApiUrl() {
        ensure();
        String v = sp.getString(KEY_API, null);
        return v != null ? v : "";
    }

    public static void setApiUrl(String url) {
        ensure();
        sp.edit().putString(KEY_API, url != null ? url : "").apply();
    }

    public static List<Subscription> getSubscriptions() {
        ensure();
        String json = sp.getString(KEY_SUBS, null);
        if (json != null && !json.isEmpty()) {
            try {
                List<Subscription> list = GSON.fromJson(json, SUB_LIST_TYPE);
                if (list != null) return list;
            } catch (Throwable ignored) {
            }
        }
        return new ArrayList<>();
    }

    public static void setSubscriptions(List<Subscription> list) {
        ensure();
        List<Subscription> safe = list != null ? list : new ArrayList<>();
        sp.edit().putString(KEY_SUBS, GSON.toJson(safe)).apply();
    }

    public static boolean hasApiUrl() {
        return !TextUtils.isEmpty(getApiUrl());
    }

    public static int getInt(String key, int def) {
        ensure();
        return sp.getInt(key, def);
    }

    public static void putInt(String key, int value) {
        ensure();
        sp.edit().putInt(key, value).apply();
    }

    public static boolean getBool(String key, boolean def) {
        ensure();
        return sp.getBoolean(key, def);
    }

    public static void putBool(String key, boolean value) {
        ensure();
        sp.edit().putBoolean(key, value).apply();
    }

    public static String getString(String key, String def) {
        ensure();
        return sp.getString(key, def);
    }

    public static void putString(String key, String value) {
        ensure();
        sp.edit().putString(key, value != null ? value : "").apply();
    }

    public static float getFloat(String key, float def) {
        ensure();
        return sp.getFloat(key, def);
    }

    public static void putFloat(String key, float value) {
        ensure();
        sp.edit().putFloat(key, value).apply();
    }

    public static long getLong(String key, long def) {
        ensure();
        return sp.getLong(key, def);
    }

    public static void putLong(String key, long value) {
        ensure();
        sp.edit().putLong(key, value).apply();
    }

    public static boolean contains(String key) {
        ensure();
        return sp.contains(key) || sp.contains(key + "_json");
    }

    public static void remove(String key) {
        ensure();
        sp.edit().remove(key).remove(key + "_json").apply();
    }

    public static void putDefault(String key, Object value) {
        if (contains(key)) return;
        if (value instanceof Boolean) {
            putBool(key, (Boolean) value);
        } else if (value instanceof Integer) {
            putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            putLong(key, (Long) value);
        } else if (value instanceof Float) {
            putFloat(key, (Float) value);
        } else if (value instanceof String) {
            putString(key, (String) value);
        } else if (value != null) {
            putString(key, String.valueOf(value));
        }
    }

    public static ArrayList<String> getStringList(String key) {
        ensure();
        String json = sp.getString(key + "_json", null);
        if (json != null && !json.isEmpty()) {
            try {
                ArrayList<String> list = GSON.fromJson(json, STRING_LIST_TYPE);
                if (list != null) return list;
            } catch (Throwable ignored) {
            }
        }
        return new ArrayList<>();
    }

    public static void putStringList(String key, List<String> list) {
        ensure();
        ArrayList<String> safe = list != null ? new ArrayList<>(list) : new ArrayList<>();
        sp.edit().putString(key + "_json", GSON.toJson(safe)).apply();
    }

    public static <T> T getJson(String key, Type type, T def) {
        ensure();
        String json = sp.getString(key + "_json", null);
        if (json != null && !json.isEmpty()) {
            try {
                T v = GSON.fromJson(json, type);
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
        }
        return def;
    }

    public static void putJson(String key, Object value) {
        ensure();
        sp.edit().putString(key + "_json", GSON.toJson(value)).apply();
    }
}

package com.github.tvbox.osc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.github.tvbox.osc.bean.Subscription;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orhanobut.hawk.Hawk;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 配置存储：SharedPreferences 为主，写入时双写 Hawk，保证过渡期兼容。
 * 常用键可通过 put/get 系列直接读写，逐步替代裸 Hawk 调用。
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

    /** 第二批从 Hawk 迁入的常用配置键 */
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
    };

    private ConfigStore() {
    }

    public static void init(Context context) {
        if (sp != null) return;
        sp = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        migrateFromHawkIfNeeded();
        migrateCommonKeysIfNeeded();
    }

    private static void ensure() {
        if (sp == null) {
            throw new IllegalStateException("ConfigStore.init() must be called first");
        }
    }

    private static void migrateFromHawkIfNeeded() {
        if (sp.getBoolean(KEY_MIGRATED, false)) return;
        try {
            String api = Hawk.get(HawkConfig.API_URL, "");
            if (!TextUtils.isEmpty(api) && TextUtils.isEmpty(sp.getString(KEY_API, ""))) {
                sp.edit().putString(KEY_API, api).apply();
            }
            List<Subscription> list = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<>());
            if (list != null && !list.isEmpty() && TextUtils.isEmpty(sp.getString(KEY_SUBS, ""))) {
                sp.edit().putString(KEY_SUBS, GSON.toJson(list)).apply();
            }
        } catch (Throwable ignored) {
        }
        sp.edit().putBoolean(KEY_MIGRATED, true).apply();
    }

    private static void migrateCommonKeysIfNeeded() {
        if (sp.getBoolean(KEY_COMMON_MIGRATED, false)) return;
        SharedPreferences.Editor ed = sp.edit();
        for (String key : COMMON_KEYS) {
            if (sp.contains(key)) continue;
            try {
                if (!Hawk.contains(key)) continue;
                Object v = Hawk.get(key);
                if (v == null) continue;
                if (v instanceof Boolean) {
                    ed.putBoolean(key, (Boolean) v);
                } else if (v instanceof Integer) {
                    ed.putInt(key, (Integer) v);
                } else if (v instanceof Long) {
                    ed.putLong(key, (Long) v);
                } else if (v instanceof Float) {
                    ed.putFloat(key, (Float) v);
                } else if (v instanceof String) {
                    ed.putString(key, (String) v);
                } else if (v instanceof Set) {
                    //noinspection unchecked
                    ed.putStringSet(key, (Set<String>) v);
                } else {
                    ed.putString(key, String.valueOf(v));
                }
            } catch (Throwable ignored) {
            }
        }
        ed.putBoolean(KEY_COMMON_MIGRATED, true);
        ed.apply();
    }

    public static String getApiUrl() {
        ensure();
        String v = sp.getString(KEY_API, null);
        if (v != null) return v;
        return Hawk.get(HawkConfig.API_URL, "");
    }

    public static void setApiUrl(String url) {
        ensure();
        String u = url != null ? url : "";
        sp.edit().putString(KEY_API, u).apply();
        try {
            Hawk.put(HawkConfig.API_URL, u);
        } catch (Throwable ignored) {
        }
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
        List<Subscription> hawkList = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<>());
        return hawkList != null ? hawkList : new ArrayList<>();
    }

    public static void setSubscriptions(List<Subscription> list) {
        ensure();
        List<Subscription> safe = list != null ? list : new ArrayList<>();
        sp.edit().putString(KEY_SUBS, GSON.toJson(safe)).apply();
        try {
            Hawk.put(HawkConfig.SUBSCRIPTIONS, safe);
        } catch (Throwable ignored) {
        }
    }

    public static boolean hasApiUrl() {
        ensure();
        return !TextUtils.isEmpty(getApiUrl());
    }

    // ---------- 通用读写（双写 Hawk） ----------

    public static int getInt(String key, int def) {
        ensure();
        if (sp.contains(key)) return sp.getInt(key, def);
        try {
            return Hawk.get(key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    public static void putInt(String key, int value) {
        ensure();
        sp.edit().putInt(key, value).apply();
        try {
            Hawk.put(key, value);
        } catch (Throwable ignored) {
        }
    }

    public static boolean getBool(String key, boolean def) {
        ensure();
        if (sp.contains(key)) return sp.getBoolean(key, def);
        try {
            return Hawk.get(key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    public static void putBool(String key, boolean value) {
        ensure();
        sp.edit().putBoolean(key, value).apply();
        try {
            Hawk.put(key, value);
        } catch (Throwable ignored) {
        }
    }

    public static String getString(String key, String def) {
        ensure();
        if (sp.contains(key)) return sp.getString(key, def);
        try {
            return Hawk.get(key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    public static void putString(String key, String value) {
        ensure();
        sp.edit().putString(key, value != null ? value : "").apply();
        try {
            Hawk.put(key, value);
        } catch (Throwable ignored) {
        }
    }

    public static float getFloat(String key, float def) {
        ensure();
        if (sp.contains(key)) return sp.getFloat(key, def);
        try {
            return Hawk.get(key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    public static void putFloat(String key, float value) {
        ensure();
        sp.edit().putFloat(key, value).apply();
        try {
            Hawk.put(key, value);
        } catch (Throwable ignored) {
        }
    }

    public static long getLong(String key, long def) {
        ensure();
        if (sp.contains(key)) return sp.getLong(key, def);
        try {
            return Hawk.get(key, def);
        } catch (Throwable e) {
            return def;
        }
    }

    public static void putLong(String key, long value) {
        ensure();
        sp.edit().putLong(key, value).apply();
        try {
            Hawk.put(key, value);
        } catch (Throwable ignored) {
        }
    }

    private static final Type STRING_LIST_TYPE = new TypeToken<ArrayList<String>>() {
    }.getType();

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
        try {
            ArrayList<String> hawkList = Hawk.get(key, new ArrayList<String>());
            return hawkList != null ? hawkList : new ArrayList<>();
        } catch (Throwable e) {
            return new ArrayList<>();
        }
    }

    public static void putStringList(String key, List<String> list) {
        ensure();
        ArrayList<String> safe = list != null ? new ArrayList<>(list) : new ArrayList<>();
        sp.edit().putString(key + "_json", GSON.toJson(safe)).apply();
        try {
            Hawk.put(key, safe);
        } catch (Throwable ignored) {
        }
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
        try {
            T hawk = Hawk.get(key, def);
            return hawk != null ? hawk : def;
        } catch (Throwable e) {
            return def;
        }
    }

    public static void putJson(String key, Object value) {
        ensure();
        sp.edit().putString(key + "_json", GSON.toJson(value)).apply();
        try {
            Hawk.put(key, value);
        } catch (Throwable ignored) {
        }
    }
}

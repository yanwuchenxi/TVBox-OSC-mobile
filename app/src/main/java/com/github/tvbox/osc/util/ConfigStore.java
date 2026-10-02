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

/**
 * 配置存储试点：SharedPreferences + Gson，读写订阅列表与 API 地址。
 * 启动时从 Hawk 迁移一次；写入时双写 Hawk，保证旧代码可读。
 * 后续可平滑替换为 DataStore Preferences。
 */
public final class ConfigStore {
    private static final String PREF = "tvbox_config_v1";
    private static final String KEY_API = "api_url";
    private static final String KEY_SUBS = "subscriptions_json";
    private static final String KEY_MIGRATED = "migrated_from_hawk";

    private static SharedPreferences sp;
    private static final Gson GSON = new Gson();
    private static final Type SUB_LIST_TYPE = new TypeToken<ArrayList<Subscription>>() {}.getType();

    private ConfigStore() {
    }

    public static void init(Context context) {
        if (sp != null) return;
        sp = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        migrateFromHawkIfNeeded();
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
}

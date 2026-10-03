package com.github.tvbox.osc.util;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.activity.FastSearchActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SearchHelper {

    public static HashMap<String, String> getSourcesForSearch() {
        HashMap<String, String> mCheckSources;
        try {
            String api = ConfigStore.getApiUrl();
            if(api.isEmpty())return null;
            Type type = new TypeToken<HashMap<String, HashMap<String, String>>>(){}.getType();
            HashMap<String, HashMap<String, String>> mCheckSourcesForApi = ConfigStore.getJson(HawkConfig.SOURCES_FOR_SEARCH, type, new HashMap<>());
            mCheckSources = mCheckSourcesForApi.get(api);
        } catch (Exception e) {
            return null;
        }
        if (mCheckSources == null || mCheckSources.isEmpty()) mCheckSources = getSources();
        return mCheckSources;
    }

    public static void putCheckedSources(HashMap<String, String> mCheckSources,boolean isAll) {
        String api = ConfigStore.getApiUrl();
        if (api.isEmpty()) {
            return;
        }
        Type type = new TypeToken<HashMap<String, HashMap<String, String>>>(){}.getType();
        HashMap<String, HashMap<String, String>> mCheckSourcesForApi = ConfigStore.getJson(HawkConfig.SOURCES_FOR_SEARCH, type, null);

        if(isAll){
            if (mCheckSourcesForApi == null) return;
            if (mCheckSourcesForApi.containsKey(api)) mCheckSourcesForApi.remove(api);
        }else {
            if (mCheckSourcesForApi == null) mCheckSourcesForApi = new HashMap<>();
            mCheckSourcesForApi.put(api, mCheckSources);
        }
        FastSearchActivity.Companion.setCheckedSourcesForSearch(mCheckSources);
        ConfigStore.putJson(HawkConfig.SOURCES_FOR_SEARCH, mCheckSourcesForApi);
    }

    public static HashMap<String, String> getSources(){
        HashMap<String, String> mCheckSources = new HashMap<>();
        for (SourceBean bean : ApiConfig.get().getSourceBeanList()) {
            if (!bean.isSearchable()) {
                continue;
            }
            mCheckSources.put(bean.getKey(), "1");
        }
        return mCheckSources;
    }

    public static List<String> splitWords(String text) {
        List<String> result = new ArrayList<>();
        result.add(text);
        String[] parts = text.split("\\W+");
        if (parts.length > 1) {
            result.addAll(Arrays.asList(parts));
        }
        return result;
    }

}

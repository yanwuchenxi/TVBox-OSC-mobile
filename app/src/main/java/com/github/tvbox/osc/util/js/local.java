package com.github.tvbox.osc.util.js;

import androidx.annotation.Keep;

import com.github.tvbox.osc.util.ConfigStore;
import com.whl.quickjs.wrapper.Function;

public class local {
    @Keep
    @Function
    public void delete(String str, String str2) {
        try {
            ConfigStore.remove("jsRuntime_" + str + "_" + str2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Keep
    @Function
    public String get(String str, String str2) {
        try {
            return ConfigStore.getString("jsRuntime_" + str + "_" + str2, "");
        } catch (Exception e) {
            ConfigStore.remove(str);
            return str2;
        }
    }

    @Keep
    @Function
    public void set(String str, String str2, String str3) {
        try {
            ConfigStore.putString("jsRuntime_" + str + "_" + str2, str3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

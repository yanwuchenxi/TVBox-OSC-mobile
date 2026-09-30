package com.github.tvbox.osc.util;

import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AdBlocker {
    private static final List<String> AD_HOSTS = new ArrayList<>();
    private static final List<String> BUILTIN_HOSTS = Arrays.asList(
            "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com",
            "www.googletagmanager.com",
            "www.google-analytics.com",
            "adservice.google.com",
            "ad.qq.com",
            "ads.qq.com",
            "adsmind.gdtimg.com",
            "cpro.baidustatic.com",
            "hm.baidu.com",
            "pos.baidu.com",
            "cnzz.com",
            "umeng.com",
            "doubleclick.net",
            "googlesyndication.com"
    );
    private static final List<String> AD_PATH_KEYS = Arrays.asList(
            "/ad/", "/ads/", "/advert/", "ad_ts", "adsegment", "pre-roll", "preroll",
            "midroll", "sponsor", "commercial", "gpad/"
    );

    public static void clear() {
        AD_HOSTS.clear();
    }

    public static boolean isEmpty() {
        return AD_HOSTS.isEmpty();
    }

    public static void addAdHost(String host) {
        if (host != null && !host.isEmpty() && !AD_HOSTS.contains(host)) {
            AD_HOSTS.add(host.toLowerCase(Locale.US));
        }
    }

    public static boolean hasHost(String host) {
        return host != null && AD_HOSTS.contains(host.toLowerCase(Locale.US));
    }

    public static boolean isAd(String url) {
        if (url == null || url.isEmpty()) return false;
        String u = url.toLowerCase(Locale.US);
        for (String adHost : AD_HOSTS) {
            if (u.contains(adHost)) return true;
        }
        for (String h : BUILTIN_HOSTS) {
            if (u.contains(h)) return true;
        }
        for (String k : AD_PATH_KEYS) {
            if (u.contains(k)) return true;
        }
        return false;
    }

    public static WebResourceResponse createEmptyResource() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    }
}

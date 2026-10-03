package com.github.tvbox.osc.player.sniff;

import android.os.Build;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.github.tvbox.osc.util.ConfigStore;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

/**
 * 嗅探 WebView 通用设置，从 PlayFragment.configWebViewSys 拆出。
 */
public final class SniffWebViewConfig {
    private SniffWebViewConfig() {
    }

    public static ViewGroup.LayoutParams layoutParams() {
        boolean debug = ConfigStore.getBool(HawkConfig.DEBUG_OPEN, false);
        return debug
                ? new ViewGroup.LayoutParams(800, 400)
                : new ViewGroup.LayoutParams(1, 1);
    }

    public static void apply(WebView webView) {
        if (webView == null) return;
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.clearFocus();
        webView.setOverScrollMode(WebView.OVER_SCROLL_ALWAYS);
        WebSettings settings = webView.getSettings();
        settings.setNeedInitialFocus(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
        settings.setBlockNetworkImage(!ConfigStore.getBool(HawkConfig.DEBUG_OPEN, false));
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setSupportZoom(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDefaultTextEncodingName("utf-8");
        settings.setUserAgentString(webView.getSettings().getUserAgentString());
    }
}

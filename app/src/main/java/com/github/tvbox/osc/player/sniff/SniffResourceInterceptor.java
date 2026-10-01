package com.github.tvbox.osc.player.sniff;

import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;

import com.github.tvbox.osc.player.PlayRetryHelper;
import com.github.tvbox.osc.util.AdBlocker;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.VideoParseRuler;

import java.util.HashMap;
import java.util.Map;

/**
 * 嗅探资源拦截：广告过滤 + 视频 URL 发现，从 PlayFragment.SysWebClient 拆出。
 */
public class SniffResourceInterceptor {

    public interface Host {
        boolean isVideoUrl(String url);

        void onFirstVideoFound(String url, HashMap<String, String> headers);

        void cancelSniffTimeout();
    }

    private final PlayRetryHelper retryHelper;
    private final Map<String, Boolean> loadedUrls = new HashMap<>();
    private final Host host;
    private String pageUrl;

    public SniffResourceInterceptor(PlayRetryHelper retryHelper, Host host) {
        this.retryHelper = retryHelper;
        this.host = host;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public void reset() {
        loadedUrls.clear();
    }

    public WebResourceResponse intercept(String url, HashMap<String, String> headers) {
        if (url == null) return null;
        if (url.endsWith("/favicon.ico")) {
            if (url.startsWith("http://127.0.0.1")) {
                return new WebResourceResponse("image/x-icon", "UTF-8", null);
            }
            return null;
        }

        if (VideoParseRuler.isFilter(pageUrl, url)) {
            LOG.i("shouldInterceptLoadRequest filter:" + url);
            return null;
        }

        boolean ad;
        if (!loadedUrls.containsKey(url)) {
            ad = AdBlocker.isAd(url);
            loadedUrls.put(url, ad);
        } else {
            ad = Boolean.TRUE.equals(loadedUrls.get(url));
        }

        if (!ad && host != null && host.isVideoUrl(url)) {
            if (headers == null) headers = new HashMap<>();
            retryHelper.offerFound(url, headers);
            LOG.i("loadFoundVideoUrl:" + url);
            if (retryHelper.getLoadFoundCount().incrementAndGet() == 1) {
                String play = retryHelper.pollFoundUrl();
                if (host != null) host.cancelSniffTimeout();
                String cookie = CookieManager.getInstance().getCookie(play);
                if (!TextUtils.isEmpty(cookie)) {
                    headers.put("Cookie", " " + cookie);
                }
                if (host != null) host.onFirstVideoFound(play, headers);
            }
        }

        return ad || retryHelper.getLoadFoundCount().get() > 0
                ? AdBlocker.createEmptyResource()
                : null;
    }
}

package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.Subscription;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订阅可达性检测。UA 必须与 ApiConfig 一致（部分源按 UA 返回 JSON 或 HTML）。
 */
public class SubscriptionHealthChecker {

    /** 与 ApiConfig.userAgent 保持一致，避免摸鱼等源返回 HTML 落地页 */
    private static final String UA = "okhttp/3.15";

    public interface Callback {
        void onOneFinished(Subscription item, int index);

        void onAllFinished(int ok, int fail);
    }

    public static void checkAll(List<Subscription> list, Callback callback) {
        if (list == null || list.isEmpty()) {
            if (callback != null) callback.onAllFinished(0, 0);
            return;
        }
        Handler main = new Handler(Looper.getMainLooper());
        AtomicInteger left = new AtomicInteger(list.size());
        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            final Subscription item = list.get(i);
            item.setHealthStatus(Subscription.STATUS_CHECKING);
            item.setHealthMsg("检测中…");
            main.post(() -> {
                if (callback != null) callback.onOneFinished(item, index);
            });

            String url = item.getUrl();
            if (url == null || url.isEmpty()) {
                item.setHealthStatus(Subscription.STATUS_FAIL);
                item.setHealthMsg("地址为空");
                fail.incrementAndGet();
                doneOne(main, callback, item, index, left, ok, fail);
                continue;
            }
            if (url.startsWith("clan://localhost/")) {
                try {
                    String rel = url.substring("clan://localhost/".length());
                    try {
                        rel = java.net.URLDecoder.decode(rel, "UTF-8");
                    } catch (Throwable ignored) {
                    }
                    while (rel.startsWith("/")) rel = rel.substring(1);
                    java.io.File f = new java.io.File(android.os.Environment.getExternalStorageDirectory(), rel);
                    if (!f.exists()) f = new java.io.File("/storage/emulated/0/" + rel);
                    if (f.exists() && f.isFile() && f.length() > 0) {
                        item.setHealthStatus(Subscription.STATUS_OK);
                        item.setHealthMsg("本地文件有效");
                        ok.incrementAndGet();
                    } else {
                        item.setHealthStatus(Subscription.STATUS_FAIL);
                        item.setHealthMsg("本地文件不存在");
                        fail.incrementAndGet();
                    }
                } catch (Throwable e) {
                    item.setHealthStatus(Subscription.STATUS_FAIL);
                    item.setHealthMsg("本地路径无效");
                    fail.incrementAndGet();
                }
                doneOne(main, callback, item, index, left, ok, fail);
                continue;
            }
            if (url.startsWith("clan://")) {
                item.setHealthStatus(Subscription.STATUS_OK);
                item.setHealthMsg("局域网文件");
                ok.incrementAndGet();
                doneOne(main, callback, item, index, left, ok, fail);
                continue;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                item.setHealthStatus(Subscription.STATUS_FAIL);
                item.setHealthMsg("格式无效");
                fail.incrementAndGet();
                doneOne(main, callback, item, index, left, ok, fail);
                continue;
            }

            NetworkClient.getStringAsync(url, UA, new NetworkClient.StringCallback() {
                @Override
                public void onSuccess(int code, String body) {
                    String text = body != null ? body.trim() : "";
                    // 部分源对非 okhttp UA 返回 HTML
                    if (text.regionMatches(true, 0, "<!DOCTYPE", 0, 9)
                            || text.regionMatches(true, 0, "<html", 0, 5)) {
                        item.setHealthStatus(Subscription.STATUS_FAIL);
                        item.setHealthMsg("返回网页非配置(请检查UA)");
                        fail.incrementAndGet();
                        doneOne(main, callback, item, index, left, ok, fail);
                        return;
                    }
                    if (!ApiConfig.looksLikeJson(text) && text.length() > 10) {
                        try {
                            String dec = ApiConfig.FindResult(text, null);
                            if (dec != null && !dec.isEmpty()) text = dec.trim();
                        } catch (Throwable ignored) {
                        }
                    }
                    boolean hasSites = text.contains("\"sites\"") || text.contains("sites");
                    boolean isJson = ApiConfig.looksLikeJson(text);
                    if (isJson && hasSites) {
                        item.setHealthStatus(Subscription.STATUS_OK);
                        item.setHealthMsg("有效(含站点)");
                        ok.incrementAndGet();
                    } else if (isJson) {
                        item.setHealthStatus(Subscription.STATUS_OK);
                        item.setHealthMsg("有效(JSON)");
                        ok.incrementAndGet();
                    } else if (code >= 200 && code < 400 && text.length() > 20) {
                        item.setHealthStatus(Subscription.STATUS_FAIL);
                        item.setHealthMsg("可访问但无法解析站点");
                        fail.incrementAndGet();
                    } else {
                        item.setHealthStatus(Subscription.STATUS_FAIL);
                        item.setHealthMsg("内容异常");
                        fail.incrementAndGet();
                    }
                    doneOne(main, callback, item, index, left, ok, fail);
                }

                @Override
                public void onError(Throwable e) {
                    item.setHealthStatus(Subscription.STATUS_FAIL);
                    String msg = e != null ? e.getMessage() : "请求失败";
                    if (msg != null && msg.length() > 24) msg = msg.substring(0, 24) + "…";
                    item.setHealthMsg(msg == null ? "失败" : msg);
                    fail.incrementAndGet();
                    doneOne(main, callback, item, index, left, ok, fail);
                }
            });
        }
    }

    private static void doneOne(Handler main, Callback callback, Subscription item, int index,
                                AtomicInteger left, AtomicInteger ok, AtomicInteger fail) {
        main.post(() -> {
            if (callback != null) callback.onOneFinished(item, index);
            if (left.decrementAndGet() == 0 && callback != null) {
                callback.onAllFinished(ok.get(), fail.get());
            }
        });
    }

    public static void cancel() {
    }
}

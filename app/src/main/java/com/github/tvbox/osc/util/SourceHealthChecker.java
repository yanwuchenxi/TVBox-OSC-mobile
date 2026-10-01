package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.SourceBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 配置加载后对站点 API 做轻量可达性抽检，并按结果重排 source 列表。
 * 网络走 NetworkClient，不再依赖 OkGo。
 */
public class SourceHealthChecker {

    public interface Callback {
        void onFinished(int ok, int fail);
    }

    public static void checkAndSort(Callback cb) {
        checkAndSort(false, cb);
    }

    public static void checkAndSort(boolean force, Callback cb) {
        if (!force && HealthCheckCache.isFresh(HawkConfig.SOURCE_HEALTH_TS)) {
            if (cb != null) cb.onFinished(-1, -1);
            return;
        }
        List<SourceBean> sites = new ArrayList<>(ApiConfig.get().getSourceBeanList());
        if (sites.isEmpty()) {
            if (cb != null) cb.onFinished(0, 0);
            return;
        }
        Handler main = new Handler(Looper.getMainLooper());
        AtomicInteger left = new AtomicInteger(0);
        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        List<SourceBean> toCheck = new ArrayList<>();
        for (SourceBean s : sites) {
            if (s == null || s.getApi() == null) continue;
            String api = s.getApi();
            if (api.startsWith("http://") || api.startsWith("https://")) {
                toCheck.add(s);
            }
        }
        if (toCheck.size() > 20) {
            toCheck = toCheck.subList(0, 20);
        }
        if (toCheck.isEmpty()) {
            if (cb != null) main.post(() -> cb.onFinished(0, 0));
            return;
        }
        left.set(toCheck.size());
        final List<SourceBean> okList = new ArrayList<>();
        final List<SourceBean> failList = new ArrayList<>();
        final List<SourceBean> other = new ArrayList<>();
        for (SourceBean s : sites) {
            if (!toCheck.contains(s)) other.add(s);
        }

        for (SourceBean site : toCheck) {
            final SourceBean s = site;
            NetworkClient.getStringAsync(s.getApi(), "TVBoxMobile", new NetworkClient.StringCallback() {
                @Override
                public void onSuccess(int code, String body) {
                    boolean looksValid = false;
                    if (body != null && body.length() > 10) {
                        looksValid = body.contains("sites") || body.contains("class")
                                || body.contains("list") || body.contains("type")
                                || body.trim().startsWith("{") || body.trim().startsWith("[")
                                || body.contains("vod") || body.contains("data");
                    }
                    if (looksValid || (code >= 200 && code < 400 && body != null && body.length() > 0)) {
                        ok.incrementAndGet();
                        synchronized (okList) {
                            okList.add(s);
                        }
                    } else {
                        fail.incrementAndGet();
                        synchronized (failList) {
                            failList.add(s);
                        }
                    }
                    done();
                }

                @Override
                public void onError(Throwable e) {
                    fail.incrementAndGet();
                    synchronized (failList) {
                        failList.add(s);
                    }
                    done();
                }

                private void done() {
                    if (left.decrementAndGet() == 0) {
                        List<SourceBean> ordered = new ArrayList<>();
                        ordered.addAll(okList);
                        ordered.addAll(other);
                        ordered.addAll(failList);
                        try {
                            ApiConfig.get().reorderSourceBeans(ordered);
                            HealthCheckCache.touch(HawkConfig.SOURCE_HEALTH_TS);
                        } catch (Throwable ignored) {
                        }
                        main.post(() -> {
                            if (cb != null) cb.onFinished(ok.get(), fail.get());
                        });
                    }
                }
            });
        }
    }
}

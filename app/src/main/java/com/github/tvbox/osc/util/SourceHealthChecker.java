package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.SourceBean;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 配置加载后对站点 API 做轻量可达性抽检，并按结果重排 source 列表。
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
            if (cb != null) cb.onFinished(-1, -1); // -1 表示跳过（使用缓存）
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
        // 最多检测前 20 个，避免过多请求
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
            OkGo.<String>get(s.getApi())
                    .tag("source_health")
                    .headers("User-Agent", "TVBoxMobile")
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            ok.incrementAndGet();
                            synchronized (okList) {
                                okList.add(s);
                            }
                            done();
                        }

                        @Override
                        public void onError(Response<String> response) {
                            fail.incrementAndGet();
                            synchronized (failList) {
                                failList.add(s);
                            }
                            done();
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            return response.body() != null ? response.body().string() : "";
                        }

                        private void done() {
                            if (left.decrementAndGet() == 0) {
                                // 重排：有效在前
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

package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;

import com.github.tvbox.osc.bean.Subscription;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对订阅地址做轻量可达性检测（GET，超时较短）。
 */
public class SubscriptionHealthChecker {

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
            if (url.startsWith("clan://")) {
                item.setHealthStatus(Subscription.STATUS_OK);
                item.setHealthMsg("本地文件");
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

            OkGo.<String>get(url)
                    .tag("sub_health_" + index)
                    .headers("User-Agent", "TVBoxMobile")
                    .execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            String body = response.body();
                            if (body != null && body.length() > 20 && (body.contains("{") || body.contains("urls") || body.contains("sites"))) {
                                item.setHealthStatus(Subscription.STATUS_OK);
                                item.setHealthMsg("有效");
                                ok.incrementAndGet();
                            } else if (response.code() >= 200 && response.code() < 400) {
                                item.setHealthStatus(Subscription.STATUS_OK);
                                item.setHealthMsg("可访问");
                                ok.incrementAndGet();
                            } else {
                                item.setHealthStatus(Subscription.STATUS_FAIL);
                                item.setHealthMsg("内容异常");
                                fail.incrementAndGet();
                            }
                            doneOne(main, callback, item, index, left, ok, fail);
                        }

                        @Override
                        public void onError(Response<String> response) {
                            item.setHealthStatus(Subscription.STATUS_FAIL);
                            String msg = response.getException() != null ? response.getException().getMessage() : "请求失败";
                            if (msg != null && msg.length() > 24) msg = msg.substring(0, 24) + "…";
                            item.setHealthMsg(msg == null ? "失败" : msg);
                            fail.incrementAndGet();
                            doneOne(main, callback, item, index, left, ok, fail);
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            return response.body() != null ? response.body().string() : "";
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
        OkGo.getInstance().cancelTag("sub_health");
    }
}

package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;

import com.github.tvbox.osc.bean.Subscription;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对订阅地址做轻量可达性检测。网络走 NetworkClient。
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

            NetworkClient.getStringAsync(url, "TVBoxMobile", new NetworkClient.StringCallback() {
                @Override
                public void onSuccess(int code, String body) {
                    if (body != null && body.length() > 20
                            && (body.contains("{") || body.contains("urls") || body.contains("sites"))) {
                        item.setHealthStatus(Subscription.STATUS_OK);
                        item.setHealthMsg("有效");
                        ok.incrementAndGet();
                    } else if (code >= 200 && code < 400) {
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
        // NetworkClient 使用短生命周期请求，暂无全局 tag 取消
    }
}

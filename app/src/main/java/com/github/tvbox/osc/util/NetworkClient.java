package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 全局单一 OkHttpClient，逐步替代 OkGo 散落调用。
 */
public final class NetworkClient {
    private static volatile OkHttpClient client;
    private static final ExecutorService EXEC = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface StringCallback {
        void onSuccess(int code, String body);

        void onError(Throwable e);
    }

    private NetworkClient() {
    }

    public static OkHttpClient get() {
        if (client == null) {
            synchronized (NetworkClient.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(12, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .build();
                }
            }
        }
        return client;
    }

    public static void getStringAsync(String url, String userAgent, StringCallback cb) {
        EXEC.execute(() -> {
            try {
                Request.Builder b = new Request.Builder().url(url).get();
                if (userAgent != null) {
                    b.header("User-Agent", userAgent);
                }
                try (Response resp = get().newCall(b.build()).execute()) {
                    int code = resp.code();
                    String body = resp.body() != null ? resp.body().string() : "";
                    MAIN.post(() -> {
                        if (cb != null) cb.onSuccess(code, body);
                    });
                }
            } catch (Throwable e) {
                MAIN.post(() -> {
                    if (cb != null) cb.onError(e);
                });
            }
        });
    }

    public static String getStringSync(String url, String userAgent) throws IOException {
        Request.Builder b = new Request.Builder().url(url).get();
        if (userAgent != null) {
            b.header("User-Agent", userAgent);
        }
        try (Response resp = get().newCall(b.build()).execute()) {
            return resp.body() != null ? resp.body().string() : "";
        }
    }
}

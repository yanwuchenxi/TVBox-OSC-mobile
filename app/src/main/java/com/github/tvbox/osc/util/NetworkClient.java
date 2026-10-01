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
        getStringAsync(url, userAgent, null, cb);
    }

    public static void getStringAsync(String url, String userAgent, java.util.Map<String, String> extraHeaders, StringCallback cb) {
        EXEC.execute(() -> {
            try {
                Request.Builder b = new Request.Builder().url(url).get();
                if (userAgent != null) {
                    b.header("User-Agent", userAgent);
                }
                if (extraHeaders != null) {
                    for (java.util.Map.Entry<String, String> e : extraHeaders.entrySet()) {
                        if (e.getKey() != null && e.getValue() != null) {
                            b.header(e.getKey(), e.getValue());
                        }
                    }
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
        return getStringSync(url, userAgent, null);
    }

    public static String getStringSync(String url, String userAgent, java.util.Map<String, String> extraHeaders) throws IOException {
        Request.Builder b = new Request.Builder().url(url).get();
        if (userAgent != null) {
            b.header("User-Agent", userAgent);
        }
        if (extraHeaders != null) {
            for (java.util.Map.Entry<String, String> e : extraHeaders.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    b.header(e.getKey(), e.getValue());
                }
            }
        }
        try (Response resp = get().newCall(b.build()).execute()) {
            return resp.body() != null ? resp.body().string() : "";
        }
    }

    public static byte[] getBytesSync(String url, java.util.Map<String, String> headers) throws IOException {
        Request.Builder b = new Request.Builder().url(url).get();
        if (headers != null) {
            for (java.util.Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    b.header(e.getKey(), e.getValue());
                }
            }
        }
        try (Response resp = get().newCall(b.build()).execute()) {
            return resp.body() != null ? resp.body().bytes() : new byte[0];
        }
    }

    public static class HttpResult {
        public final int code;
        public final byte[] body;
        public final String contentDisposition;

        public HttpResult(int code, byte[] body, String contentDisposition) {
            this.code = code;
            this.body = body != null ? body : new byte[0];
            this.contentDisposition = contentDisposition;
        }
    }

    public static HttpResult getResultSync(String url, java.util.Map<String, String> headers) throws IOException {
        Request.Builder b = new Request.Builder().url(url).get();
        if (headers != null) {
            for (java.util.Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    b.header(e.getKey(), e.getValue());
                }
            }
        }
        try (Response resp = get().newCall(b.build()).execute()) {
            byte[] body = resp.body() != null ? resp.body().bytes() : new byte[0];
            return new HttpResult(resp.code(), body, resp.header("content-disposition", ""));
        }
    }
}

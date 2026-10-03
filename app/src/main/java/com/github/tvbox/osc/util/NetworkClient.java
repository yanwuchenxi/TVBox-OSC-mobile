package com.github.tvbox.osc.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 全局单一 OkHttpClient：磁盘缓存 + 连接池，逐步替代 OkGo 散落调用。
 */
public final class NetworkClient {
    private static volatile OkHttpClient client;
    private static File cacheDir;
    private static final ExecutorService EXEC = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long CACHE_SIZE = 50L * 1024 * 1024;

    public interface StringCallback {
        void onSuccess(int code, String body);

        void onError(Throwable e);
    }

    private NetworkClient() {
    }

    /** 建议在 Application 中调用，以便启用磁盘缓存 */
    public static void init(Context context) {
        if (context == null) return;
        cacheDir = new File(context.getApplicationContext().getCacheDir(), "okhttp_cache");
    }

    public static OkHttpClient get() {
        if (client == null) {
            synchronized (NetworkClient.class) {
                if (client == null) {
                    OkHttpClient.Builder b = new OkHttpClient.Builder()
                            .connectTimeout(12, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .connectionPool(new ConnectionPool(8, 5, TimeUnit.MINUTES))
                            .followRedirects(true)
                            .followSslRedirects(true);
                    if (cacheDir != null) {
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            cacheDir.mkdirs();
                            b.cache(new Cache(cacheDir, CACHE_SIZE));
                        } catch (Throwable ignored) {
                        }
                    }
                    client = b.build();
                }
            }
        }
        return client;
    }


    /** 将 query 参数拼到 URL（跳过 key 或 value 为 null 的项） */
    public static String buildUrl(String base, java.util.Map<String, String> params) {
        if (base == null || base.isEmpty()) return base;
        if (params == null || params.isEmpty()) return base;
        try {
            okhttp3.HttpUrl hu = okhttp3.HttpUrl.parse(base);
            if (hu == null) {
                StringBuilder sb = new StringBuilder(base);
                boolean first = !base.contains("?");
                for (java.util.Map.Entry<String, String> e : params.entrySet()) {
                    if (e.getKey() == null || e.getValue() == null) continue;
                    sb.append(first ? "?" : "&");
                    first = false;
                    sb.append(java.net.URLEncoder.encode(e.getKey(), "UTF-8"));
                    sb.append("=");
                    sb.append(java.net.URLEncoder.encode(e.getValue(), "UTF-8"));
                }
                return sb.toString();
            }
            okhttp3.HttpUrl.Builder b = hu.newBuilder();
            for (java.util.Map.Entry<String, String> e : params.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                b.addQueryParameter(e.getKey(), e.getValue());
            }
            return b.build().toString();
        } catch (Throwable e) {
            return base;
        }
    }

    public static void getStringAsync(String url, String userAgent,
                                      java.util.Map<String, String> extraHeaders,
                                      java.util.Map<String, String> queryParams,
                                      StringCallback cb) {
        String full = buildUrl(url, queryParams);
        getStringAsync(full, userAgent, extraHeaders, cb);
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

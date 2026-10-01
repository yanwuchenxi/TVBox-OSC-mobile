package com.github.tvbox.osc.util;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * 全局单一 OkHttpClient，后续网络调用逐步迁到此处，替代分散的 OkGo。
 */
public final class NetworkClient {
    private static volatile OkHttpClient client;

    private NetworkClient() {
    }

    public static OkHttpClient get() {
        if (client == null) {
            synchronized (NetworkClient.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .build();
                }
            }
        }
        return client;
    }
}

package com.github.catvod;

import com.github.tvbox.osc.server.RemoteServer;

/**
 * 本地代理 URL 辅助（Python/爬虫用）。
 */
public class Proxy {
    public static String getUrl(boolean local) {
        try {
            int port = RemoteServer.serverPort > 0 ? RemoteServer.serverPort : 9978;
            return "http://127.0.0.1:" + port + "/proxy";
        } catch (Throwable e) {
            return "http://127.0.0.1:9978/proxy";
        }
    }
}

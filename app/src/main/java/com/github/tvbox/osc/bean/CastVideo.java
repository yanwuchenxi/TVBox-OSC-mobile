package com.github.tvbox.osc.bean;

/**
 * 投屏数据（无 DLNA 依赖的轻量实现，便于构建）
 */
public class CastVideo {
    private final String name;
    private final String url;

    public CastVideo(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}

package com.github.catvod.crawler.python;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.tvbox.osc.util.LOG;

import java.util.Map;

public class PyLoaderStub implements IPyLoader {
    private static boolean logged;

    @Override public void clear() {}
    @Override public void setConfig(String jsonStr) {}
    @Override public void setRecentPyKey(String key) {}

    @Override
    public Spider getSpider(String key, String cls, String ext) {
        if (!logged) {
            logged = true;
            LOG.e("Python runtime not in this APK variant. key=" + key);
        }
        return new SpiderNull();
    }

    @Override
    public Object[] proxyInvoke(Map params) {
        return new Object[0];
    }

    @Override
    public Object[] proxyInvoke(Map params, String key) {
        return new Object[0];
    }
}

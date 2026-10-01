package com.github.tvbox.osc.api;

import com.github.catvod.crawler.JarLoader;
import com.github.catvod.crawler.JsLoader;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.crawler.python.IPyLoader;
import com.github.catvod.crawler.python.PyLoaderStub;
import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.util.LOG;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 爬虫实例工厂：jar / js / python 分流，从 ApiConfig 拆出以降低耦合。
 */
public class SpiderFactory {
    private final JarLoader jarLoader = new JarLoader();
    private final JsLoader jsLoader = new JsLoader();
    private final IPyLoader pyLoader;

    public SpiderFactory() {
        this.pyLoader = createPyLoader();
    }

    private static IPyLoader createPyLoader() {
        try {
            if (BuildConfig.HAS_PYTHON) {
                Class<?> clz = Class.forName("com.github.catvod.crawler.pyLoader");
                return (IPyLoader) clz.getDeclaredConstructor().newInstance();
            }
        } catch (Throwable e) {
            LOG.e("SpiderFactory pyLoader: " + e.getMessage());
        }
        return new PyLoaderStub();
    }

    public IPyLoader getPyLoader() {
        return pyLoader;
    }

    public JarLoader getJarLoader() {
        return jarLoader;
    }

    public JsLoader getJsLoader() {
        return jsLoader;
    }

    public Spider getCSP(SourceBean sourceBean) {
        if (sourceBean == null || sourceBean.getApi() == null) {
            return new SpiderNull();
        }
        String api = sourceBean.getApi();
        boolean js = api.endsWith(".js") || api.contains(".js?");
        boolean py = api.endsWith(".py") || api.contains(".py?") || api.contains("python");
        if (py) {
            return pyLoader.getSpider(sourceBean.getKey(), sourceBean.getApi(), sourceBean.getExt());
        }
        if (js) {
            return jsLoader.getSpider(sourceBean.getKey(), sourceBean.getApi(), sourceBean.getExt(), sourceBean.getJar());
        }
        return jarLoader.getSpider(sourceBean.getKey(), sourceBean.getApi(), sourceBean.getExt(), sourceBean.getJar());
    }

    public Object[] proxyLocal(Map param) {
        return jarLoader.proxyInvoke(param);
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) {
        return jarLoader.jsonExt(key, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name,
                                 LinkedHashMap<String, HashMap<String, String>> jxs, String url) {
        return jarLoader.jsonExtMix(flag, key, name, jxs, url);
    }

    public boolean loadJarFile(String path) {
        return jarLoader.load(path);
    }
}

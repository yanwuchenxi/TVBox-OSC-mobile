package com.github.tvbox.osc.player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 播放自动重试与嗅探到的候选地址队列，从 PlayFragment 拆出。
 */
public class PlayRetryHelper {
    private int autoRetryCount = 0;
    private LinkedList<String> loadFoundVideoUrls = new LinkedList<>();
    private HashMap<String, HashMap<String, String>> loadFoundVideoUrlsHeader = new HashMap<>();
    private final AtomicInteger loadFoundCount = new AtomicInteger(0);

    public void resetFound() {
        loadFoundCount.set(0);
        loadFoundVideoUrls = new LinkedList<>();
        loadFoundVideoUrlsHeader = new HashMap<>();
    }

    public AtomicInteger getLoadFoundCount() {
        return loadFoundCount;
    }

    public LinkedList<String> getLoadFoundVideoUrls() {
        return loadFoundVideoUrls;
    }

    public HashMap<String, HashMap<String, String>> getLoadFoundVideoUrlsHeader() {
        return loadFoundVideoUrlsHeader;
    }

    public void offerFound(String url, HashMap<String, String> headers) {
        if (url == null || url.isEmpty()) return;
        if (loadFoundVideoUrls.contains(url)) return;
        loadFoundVideoUrls.add(url);
        if (headers != null) {
            loadFoundVideoUrlsHeader.put(url, headers);
        }
    }

    /**
     * @return true 表示还有可重试路径（调用方继续播放）
     */
    public boolean tryConsumeRetry(Runnable replayOnce) {
        if (loadFoundVideoUrls != null && !loadFoundVideoUrls.isEmpty()) {
            return true;
        }
        if (autoRetryCount < 1) {
            autoRetryCount++;
            if (replayOnce != null) {
                replayOnce.run();
            }
            return true;
        }
        autoRetryCount = 0;
        return false;
    }

    public String pollFoundUrl() {
        if (loadFoundVideoUrls == null || loadFoundVideoUrls.isEmpty()) return null;
        return loadFoundVideoUrls.poll();
    }

    public HashMap<String, String> headerFor(String url) {
        return loadFoundVideoUrlsHeader.get(url);
    }

    public void resetRetryCount() {
        autoRetryCount = 0;
    }

    public int getAutoRetryCount() {
        return autoRetryCount;
    }
}


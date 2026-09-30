package com.github.tvbox.osc.util;

import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增强版 m3u8 切片广告过滤：
 * 1) 主路径前缀聚类（少数不同前缀视为广告）
 * 2) 过短切片（常见贴片广告）
 * 3) 域名黑名单（AdBlocker + 内置）
 * 4) 已知广告路径关键字
 */
public class M3u8AdFilter {

    private static final Pattern EXTINF = Pattern.compile("#EXTINF:([0-9.]+)");
    private static final Set<String> PATH_AD_KEYS = new HashSet<>();
    private static final Set<String> EXTRA_AD_HOSTS = new HashSet<>();

    static {
        String[] keys = {
                "/ad/", "/ads/", "/advert/", "/advertise/", "/gg/", "/gpad/",
                "adsegment", "ad_segment", "adts", "ad_ts", "adload",
                "commercial", "sponsor", "pre-roll", "preroll", "midroll"
        };
        for (String k : keys) PATH_AD_KEYS.add(k);
        String[] hosts = {
                "ad.qq.com", "ads.yahoo.com", "googleads.g.doubleclick.net",
                "pagead2.googlesyndication.com", "adsmind.gdtimg.com"
        };
        for (String h : hosts) EXTRA_AD_HOSTS.add(h);
    }

    public static String filter(String tsUrlPre, String m3u8content) {
        if (TextUtils.isEmpty(m3u8content) || !m3u8content.trim().startsWith("#EXTM3U")) {
            return null;
        }
        String linesplit = m3u8content.contains("\r\n") ? "\r\n" : "\n";
        String[] lines = m3u8content.split(linesplit);

        // 收集 ts 行索引与 EXTINF 时长
        List<Integer> tsIdx = new ArrayList<>();
        Map<Integer, Float> durationMap = new HashMap<>();
        float lastDur = -1f;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line == null) continue;
            Matcher m = EXTINF.matcher(line);
            if (m.find()) {
                try {
                    lastDur = Float.parseFloat(m.group(1));
                } catch (Exception e) {
                    lastDur = -1f;
                }
            }
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            tsIdx.add(i);
            if (lastDur >= 0) durationMap.put(i, lastDur);
            lastDur = -1f;
        }
        if (tsIdx.size() < 3) return null;

        // 主前缀
        Map<String, Integer> preUrlMap = new HashMap<>();
        for (int idx : tsIdx) {
            String line = lines[idx];
            int is = line.lastIndexOf('/');
            if (is <= 0) continue;
            String pre = line.substring(0, is + 1);
            Integer cnt = preUrlMap.get(pre);
            preUrlMap.put(pre, cnt == null ? 1 : cnt + 1);
        }
        if (preUrlMap.size() <= 1 && durationMap.isEmpty()) {
            // 仍可能有关键字/域名广告
        }
        String maxTimesPreUrl = "";
        int maxTimes = 0;
        for (Map.Entry<String, Integer> e : preUrlMap.entrySet()) {
            if (e.getValue() > maxTimes) {
                maxTimes = e.getValue();
                maxTimesPreUrl = e.getKey();
            }
        }

        boolean dealedExtXKey = false;
        int removed = 0;
        for (int i = 0; i < lines.length; ++i) {
            if (!dealedExtXKey && lines[i].startsWith("#EXT-X-KEY")) {
                String keyUrl = StringUtils.substringBetween(lines[i], "URI=\"", "\"");
                if (keyUrl != null && !keyUrl.startsWith("http://") && !keyUrl.startsWith("https://")) {
                    String newKeyUrl;
                    if (keyUrl.charAt(0) == '/') {
                        int ifirst = tsUrlPre.indexOf('/', 9);
                        newKeyUrl = (ifirst > 0 ? tsUrlPre.substring(0, ifirst) : tsUrlPre) + keyUrl;
                    } else {
                        newKeyUrl = tsUrlPre + keyUrl;
                    }
                    lines[i] = lines[i].replace("URI=\"" + keyUrl + "\"", "URI=\"" + newKeyUrl + "\"");
                }
                dealedExtXKey = true;
            }
            if (lines[i].isEmpty() || lines[i].charAt(0) == '#') continue;

            String abs = toAbsolute(tsUrlPre, lines[i]);
            boolean isAd = false;

            // 1) 少数前缀
            if (maxTimes > 0 && preUrlMap.size() > 1 && preUrlMap.size() <= 8) {
                if (!lines[i].startsWith(maxTimesPreUrl) && !abs.startsWith(maxTimesPreUrl)) {
                    // 相对路径需规范化后再比
                    String pre = abs.substring(0, abs.lastIndexOf('/') + 1);
                    if (!pre.equals(maxTimesPreUrl) && !maxTimesPreUrl.isEmpty()) {
                        Integer c = preUrlMap.get(lines[i].substring(0, Math.max(0, lines[i].lastIndexOf('/') + 1)));
                        if (c == null || c < maxTimes * 0.3f) {
                            isAd = true;
                        }
                    }
                }
            }

            // 2) 过短切片 < 1.2s 且不是全部都很短
            Float dur = durationMap.get(i);
            if (!isAd && dur != null && dur > 0 && dur < 1.2f) {
                // 若大部分切片都短，则不判广告
                int shortCnt = 0;
                for (Float d : durationMap.values()) {
                    if (d != null && d < 1.2f) shortCnt++;
                }
                if (shortCnt < durationMap.size() * 0.5f) {
                    isAd = true;
                }
            }

            // 3) 域名黑名单
            if (!isAd && isAdHost(abs)) {
                isAd = true;
            }

            // 4) 路径关键字
            if (!isAd) {
                String low = abs.toLowerCase(Locale.US);
                for (String k : PATH_AD_KEYS) {
                    if (low.contains(k)) {
                        isAd = true;
                        break;
                    }
                }
            }

            if (isAd) {
                if (i > 0 && !lines[i - 1].isEmpty() && lines[i - 1].charAt(0) == '#') {
                    lines[i - 1] = "";
                }
                lines[i] = "";
                removed++;
            } else {
                // 规范化相对 ts
                if (!lines[i].startsWith("http://") && !lines[i].startsWith("https://")) {
                    lines[i] = abs;
                }
            }
        }

        if (removed == 0 && preUrlMap.size() <= 1) {
            // 无改动可返回 null 让播放器直接用原地址
            return null;
        }
        // 若删光了则放弃
        int left = 0;
        for (String line : lines) {
            if (line != null && !line.isEmpty() && line.charAt(0) != '#') left++;
        }
        if (left < 2) return null;

        return StringUtils.join(lines, linesplit);
    }

    private static String toAbsolute(String tsUrlPre, String line) {
        if (line.startsWith("http://") || line.startsWith("https://")) return line;
        if (line.startsWith("/")) {
            int ifirst = tsUrlPre.indexOf('/', 9);
            if (ifirst > 0) return tsUrlPre.substring(0, ifirst) + line;
            return tsUrlPre + line;
        }
        return tsUrlPre + line;
    }

    private static boolean isAdHost(String url) {
        try {
            if (AdBlocker.isAd(url)) return true;
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.US);
            if (EXTRA_AD_HOSTS.contains(host)) return true;
            for (String h : EXTRA_AD_HOSTS) {
                if (host.endsWith("." + h) || host.equals(h)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}

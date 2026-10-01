package com.github.tvbox.osc.player.parse;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.util.VideoParseRuler;

/**
 * 判断嗅探到的 URL 是否为视频地址。
 */
public final class VideoFormatChecker {
    private VideoFormatChecker() {
    }

    public static boolean isVideo(SourceBean sourceBean, String pageUrl, String candidateUrl) {
        try {
            if (candidateUrl == null) return false;
            if (candidateUrl.contains("url=http") || candidateUrl.contains(".html")) {
                return false;
            }
            if (sourceBean != null && sourceBean.getType() == 3) {
                Spider sp = ApiConfig.get().getCSP(sourceBean);
                if (sp != null && sp.manualVideoCheck()) {
                    return sp.isVideoFormat(candidateUrl);
                }
            }
            return VideoParseRuler.checkIsVideoForParse(pageUrl, candidateUrl);
        } catch (Exception e) {
            return false;
        }
    }
}

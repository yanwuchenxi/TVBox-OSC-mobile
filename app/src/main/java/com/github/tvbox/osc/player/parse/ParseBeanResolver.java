package com.github.tvbox.osc.player.parse;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.ParseBean;

/**
 * 根据播放标记与 playUrl 协议前缀解析出 ParseBean。
 */
public final class ParseBeanResolver {
    private ParseBeanResolver() {
    }

    public static ParseBean resolve(boolean useParse, String playUrl) {
        if (useParse) {
            return ApiConfig.get().getDefaultParse();
        }
        ParseBean parseBean = null;
        if (playUrl != null) {
            if (playUrl.startsWith("json:")) {
                parseBean = new ParseBean();
                parseBean.setType(1);
                parseBean.setUrl(playUrl.substring(5));
            } else if (playUrl.startsWith("parse:")) {
                String parseRedirect = playUrl.substring(6);
                for (ParseBean pb : ApiConfig.get().getParseBeanList()) {
                    if (pb.getName().equals(parseRedirect)) {
                        parseBean = pb;
                        break;
                    }
                }
            }
            if (parseBean == null) {
                parseBean = new ParseBean();
                parseBean.setType(0);
                parseBean.setUrl(playUrl);
            }
        }
        return parseBean;
    }
}

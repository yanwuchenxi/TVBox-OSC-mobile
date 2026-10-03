package com.github.tvbox.osc.util;

import android.text.TextUtils;

import java.util.Locale;

/**
 * 按媒体类型 + 设备厂商选择较兼容的 DLNA protocolInfo。
 */
public final class DlnaProtocolInfo {

    private DlnaProtocolInfo() {
    }

    public static String resolve(String mediaUrl, String deviceDescriptionXml) {
        String mime = mimeFromUrl(mediaUrl);
        Brand brand = Brand.fromDescription(deviceDescriptionXml);
        switch (brand) {
            case SAMSUNG:
                return samsung(mime);
            case LG:
                return lg(mime);
            case SONY:
                return sony(mime);
            case XIAOMI:
            case HISENSE:
            case TCL:
                // 多数国产品牌对通配更友好
                return "http-get:*:" + mime + ":*";
            default:
                return "http-get:*:" + mime + ":*";
        }
    }

    private static String mimeFromUrl(String url) {
        if (url == null) return "*";
        String low = url.toLowerCase(Locale.US);
        if (low.contains(".m3u8") || low.contains("m3u8")) {
            return "application/vnd.apple.mpegurl";
        }
        if (low.contains(".mp4")) return "video/mp4";
        if (low.contains(".mkv")) return "video/x-matroska";
        if (low.contains(".ts") || low.contains(".m2ts")) return "video/mp2t";
        if (low.contains(".flv")) return "video/x-flv";
        if (low.contains(".webm")) return "video/webm";
        if (low.contains(".avi")) return "video/avi";
        return "*";
    }

    private static String samsung(String mime) {
        // Samsung 常要求 DLNA.ORG_* 附加参数
        String pn;
        if (mime.contains("mp4")) {
            pn = "DLNA.ORG_PN=AVC_MP4_MP_HD_720p_AAC;"
                    + "DLNA.ORG_OP=01;DLNA.ORG_CI=0;"
                    + "DLNA.ORG_FLAGS=01700000000000000000000000000000";
        } else if (mime.contains("mpegurl")) {
            // HLS：部分机型仅接受通配
            return "http-get:*:" + mime + ":*";
        } else {
            pn = "DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000";
        }
        return "http-get:*:" + mime + ":" + pn;
    }

    private static String lg(String mime) {
        if (mime.contains("mpegurl")) {
            return "http-get:*:" + mime + ":*";
        }
        return "http-get:*:" + mime
                + ":DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000";
    }

    private static String sony(String mime) {
        if (mime.contains("mp4")) {
            return "http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_BL_CIF15_AAC_520;DLNA.ORG_OP=01";
        }
        return "http-get:*:" + mime + ":*";
    }

    enum Brand {
        SAMSUNG, LG, SONY, XIAOMI, HISENSE, TCL, GENERIC;

        static Brand fromDescription(String xml) {
            if (TextUtils.isEmpty(xml)) return GENERIC;
            String low = xml.toLowerCase(Locale.US);
            if (low.contains("samsung")) return SAMSUNG;
            if (low.contains("lg electronics") || low.contains("<manufacturer>lg")
                    || low.contains("lge") || low.contains("webos")) return LG;
            if (low.contains("sony")) return SONY;
            if (low.contains("xiaomi") || low.contains("mitv") || low.contains("mi box")) return XIAOMI;
            if (low.contains("hisense")) return HISENSE;
            if (low.contains("tcl") || low.contains("roku")) return TCL;
            return GENERIC;
        }
    }
}

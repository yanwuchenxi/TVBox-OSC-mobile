package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DLNA AVTransport 一键推流：解析设备描述 → SetAVTransportURI → Play。
 */
public class DlnaCastController {

    public interface Callback {
        void onSuccess(String msg);

        void onError(String msg);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final Pattern CONTROL_URL = Pattern.compile(
            "<service>[\\s\\S]*?<serviceType>urn:schemas-upnp-org:service:AVTransport:\\d+</serviceType>[\\s\\S]*?<controlURL>([^<]+)</controlURL>[\\s\\S]*?</service>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FRIENDLY = Pattern.compile(
            "<friendlyName>([^<]+)</friendlyName>", Pattern.CASE_INSENSITIVE);

    public static void push(String deviceDescriptionUrl, String mediaUrl, String title, Callback cb) {
        EXEC.execute(() -> {
            try {
                if (TextUtils.isEmpty(deviceDescriptionUrl) || TextUtils.isEmpty(mediaUrl)) {
                    postErr(cb, "设备或播放地址为空");
                    return;
                }
                String desc = httpGet(deviceDescriptionUrl, 8000);
                if (TextUtils.isEmpty(desc)) {
                    postErr(cb, "无法读取设备描述");
                    return;
                }
                Matcher m = CONTROL_URL.matcher(desc);
                if (!m.find()) {
                    postErr(cb, "设备不支持 AVTransport，请换支持 DLNA 的电视/盒子");
                    return;
                }
                String controlPath = m.group(1).trim();
                String controlUrl = resolveUrl(deviceDescriptionUrl, controlPath);
                String name = title != null ? title : "TVBox";
                Matcher fn = FRIENDLY.matcher(desc);
                String deviceName = fn.find() ? fn.group(1) : "电视";

                String meta = buildDidl(name, mediaUrl, desc);
                String setUri = soapSetAvTransportUri(mediaUrl, meta);
                String setResp = httpSoap(controlUrl, "urn:schemas-upnp-org:service:AVTransport:1#SetAVTransportURI", setUri, 10000);
                if (setResp == null) {
                    // 尝试 AVTransport:2
                    setResp = httpSoap(controlUrl, "urn:schemas-upnp-org:service:AVTransport:2#SetAVTransportURI", setUri, 10000);
                }
                if (setResp == null) {
                    postErr(cb, "推流失败：无法设置播放地址（设备拒绝或地址格式不支持）");
                    return;
                }
                String playBody = soapPlay();
                String playResp = httpSoap(controlUrl, "urn:schemas-upnp-org:service:AVTransport:1#Play", playBody, 8000);
                if (playResp == null) {
                    playResp = httpSoap(controlUrl, "urn:schemas-upnp-org:service:AVTransport:2#Play", playBody, 8000);
                }
                if (playResp == null) {
                    postErr(cb, "已推送地址，但设备未开始播放，请在电视端确认");
                    return;
                }
                postOk(cb, "已推送到：" + deviceName);
            } catch (Throwable e) {
                postErr(cb, e.getMessage() != null ? e.getMessage() : "推流失败");
            }
        });
    }

    private static String buildDidl(String title, String url, String deviceDesc) {
        String safeTitle = escapeXml(title);
        String safeUrl = escapeXml(url);
        String protocol = DlnaProtocolInfo.resolve(url, deviceDesc);
        return "&lt;DIDL-Lite xmlns=&quot;urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/&quot; " +
                "xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot; " +
                "xmlns:upnp=&quot;urn:schemas-upnp-org:metadata-1-0/upnp/&quot;&gt;" +
                "&lt;item id=&quot;0&quot; parentID=&quot;-1&quot; restricted=&quot;1&quot;&gt;" +
                "&lt;dc:title&gt;" + safeTitle + "&lt;/dc:title&gt;" +
                "&lt;upnp:class&gt;object.item.videoItem&lt;/upnp:class&gt;" +
                "&lt;res protocolInfo=&quot;" + protocol + "&quot;&gt;" + safeUrl + "&lt;/res&gt;" +
                "&lt;/item&gt;&lt;/DIDL-Lite&gt;";
    }

    private static String soapSetAvTransportUri(String url, String meta) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:SetAVTransportURI xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">" +
                "<InstanceID>0</InstanceID>" +
                "<CurrentURI>" + escapeXml(url) + "</CurrentURI>" +
                "<CurrentURIMetaData>" + meta + "</CurrentURIMetaData>" +
                "</u:SetAVTransportURI>" +
                "</s:Body></s:Envelope>";
    }

    private static String soapPlay() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body>" +
                "<u:Play xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">" +
                "<InstanceID>0</InstanceID>" +
                "<Speed>1</Speed>" +
                "</u:Play>" +
                "</s:Body></s:Envelope>";
    }

    private static String httpSoap(String controlUrl, String soapAction, String body, int timeout) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(controlUrl);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            conn.setRequestProperty("SOAPAction", "\"" + soapAction + "\"");
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));
            OutputStream os = conn.getOutputStream();
            os.write(data);
            os.flush();
            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String resp = readStream(is);
            if (code >= 200 && code < 300) return resp != null ? resp : "";
            return null;
        } catch (Throwable e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String httpGet(String url, int timeout) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestMethod("GET");
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) return null;
        return readStream(conn.getInputStream());
    }

    private static String readStream(InputStream is) throws Exception {
        if (is == null) return null;
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        return sb.toString();
    }

    private static String resolveUrl(String base, String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        try {
            URL b = new URL(base);
            if (path.startsWith("/")) {
                return b.getProtocol() + "://" + b.getHost() + (b.getPort() > 0 ? ":" + b.getPort() : "") + path;
            }
            String p = b.getPath();
            int idx = p.lastIndexOf('/');
            String dir = idx >= 0 ? p.substring(0, idx + 1) : "/";
            return b.getProtocol() + "://" + b.getHost() + (b.getPort() > 0 ? ":" + b.getPort() : "") + dir + path;
        } catch (Exception e) {
            return path;
        }
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static void seek(String deviceDescriptionUrl, long positionMs, Callback cb) {
        EXEC.execute(() -> {
            try {
                String desc = httpGet(deviceDescriptionUrl, 8000);
                if (TextUtils.isEmpty(desc)) {
                    postErr(cb, "无法读取设备描述");
                    return;
                }
                Matcher m = CONTROL_URL.matcher(desc);
                if (!m.find()) {
                    postErr(cb, "设备无 AVTransport");
                    return;
                }
                String controlUrl = resolveUrl(deviceDescriptionUrl, m.group(1).trim());
                long h = positionMs / 3600000;
                long rem = positionMs % 3600000;
                long min = rem / 60000;
                long sec = (rem % 60000) / 1000;
                String target = String.format(Locale.US, "%d:%02d:%02d", h, min, sec);
                String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                        + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                        + "<s:Body><u:Seek xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">"
                        + "<InstanceID>0</InstanceID><Unit>REL_TIME</Unit><Target>" + target + "</Target>"
                        + "</u:Seek></s:Body></s:Envelope>";
                String resp = httpSoap(controlUrl, "urn:schemas-upnp-org:service:AVTransport:1#Seek", body, 8000);
                if (resp == null) postErr(cb, "Seek 失败");
                else postOk(cb, "已跳转到 " + target);
            } catch (Throwable e) {
                postErr(cb, e.getMessage() != null ? e.getMessage() : "Seek 失败");
            }
        });
    }

    public static void stop(String deviceDescriptionUrl, Callback cb) {
        EXEC.execute(() -> {
            try {
                String desc = httpGet(deviceDescriptionUrl, 8000);
                if (TextUtils.isEmpty(desc)) {
                    postErr(cb, "无法读取设备描述");
                    return;
                }
                Matcher m = CONTROL_URL.matcher(desc);
                if (!m.find()) {
                    postErr(cb, "设备无 AVTransport");
                    return;
                }
                String controlUrl = resolveUrl(deviceDescriptionUrl, m.group(1).trim());
                String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                        + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                        + "<s:Body><u:Stop xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">"
                        + "<InstanceID>0</InstanceID></u:Stop></s:Body></s:Envelope>";
                String resp = httpSoap(controlUrl, "urn:schemas-upnp-org:service:AVTransport:1#Stop", body, 8000);
                if (resp == null) {
                    postErr(cb, "停止播放失败");
                } else {
                    postOk(cb, "已停止电视播放");
                }
            } catch (Throwable e) {
                postErr(cb, e.getMessage() != null ? e.getMessage() : "停止失败");
            }
        });
    }

    private static void postOk(Callback cb, String msg) {
        MAIN.post(() -> {
            if (cb != null) cb.onSuccess(msg);
        });
    }

    private static void postErr(Callback cb, String msg) {
        MAIN.post(() -> {
            if (cb != null) cb.onError(msg);
        });
    }
}

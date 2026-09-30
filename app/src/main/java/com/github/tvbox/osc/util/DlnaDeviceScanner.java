package com.github.tvbox.osc.util;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 局域网 UPnP/DLNA 设备发现（SSDP M-SEARCH）。
 */
public class DlnaDeviceScanner {

    public static class Device {
        public String usn;
        public String location;
        public String server;
        public String st;
        public String friendlyName;

        public String displayName() {
            if (friendlyName != null && !friendlyName.isEmpty()) return friendlyName;
            if (server != null && !server.isEmpty()) return server;
            if (location != null) return location;
            return usn != null ? usn : "未知设备";
        }
    }

    public interface Callback {
        void onDevice(Device device);

        void onFinished(List<Device> all);

        void onError(String msg);
    }

    private static final String SSDP_ADDR = "239.255.255.250";
    private static final int SSDP_PORT = 1900;
    private static final String MSEARCH =
            "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n" +
                    "\r\n";

    private static final Pattern HDR = Pattern.compile("(?i)^(LOCATION|USN|SERVER|ST):\\s*(.+)$", Pattern.MULTILINE);
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Handler main = new Handler(Looper.getMainLooper());

    public void search(Callback cb, long timeoutMs) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        exec.execute(() -> {
            Map<String, Device> found = new LinkedHashMap<>();
            MulticastSocket socket = null;
            try {
                socket = new MulticastSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(0));
                socket.setSoTimeout(800);
                socket.setTimeToLive(4);
                byte[] req = MSEARCH.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(
                        req, req.length, InetAddress.getByName(SSDP_ADDR), SSDP_PORT);
                // 多发几次提高发现率
                for (int i = 0; i < 3; i++) {
                    socket.send(packet);
                    // 同时搜索 MediaServer / rootdevice
                    String alt = MSEARCH.replace(
                            "urn:schemas-upnp-org:device:MediaRenderer:1",
                            "ssdp:all");
                    byte[] req2 = alt.getBytes(StandardCharsets.UTF_8);
                    socket.send(new DatagramPacket(req2, req2.length, InetAddress.getByName(SSDP_ADDR), SSDP_PORT));
                    Thread.sleep(200);
                }

                long end = System.currentTimeMillis() + Math.max(1500, timeoutMs);
                byte[] buf = new byte[2048];
                while (System.currentTimeMillis() < end) {
                    try {
                        DatagramPacket resp = new DatagramPacket(buf, buf.length);
                        socket.receive(resp);
                        String text = new String(resp.getData(), 0, resp.getLength(), StandardCharsets.UTF_8);
                        Device d = parse(text);
                        if (d == null) continue;
                        // 过滤渲染器优先；也保留含 MediaRenderer 的
                        String key = d.usn != null ? d.usn : d.location;
                        if (key == null) continue;
                        boolean isRenderer = (d.st != null && d.st.toLowerCase(Locale.US).contains("mediarenderer"))
                                || (d.usn != null && d.usn.toLowerCase(Locale.US).contains("mediarenderer"))
                                || (d.server != null && d.server.toLowerCase(Locale.US).contains("dlna"));
                        if (!isRenderer && d.st != null && d.st.contains("MediaServer")) {
                            // 仍可展示，标记
                            if (d.friendlyName == null) d.friendlyName = "Server " + (d.location != null ? d.location : "");
                        }
                        if (!found.containsKey(key)) {
                            found.put(key, d);
                            Device copy = d;
                            main.post(() -> {
                                if (cb != null) cb.onDevice(copy);
                            });
                        }
                    } catch (SocketTimeoutException ignored) {
                    }
                }
            } catch (Throwable e) {
                main.post(() -> {
                    if (cb != null) cb.onError(e.getMessage() != null ? e.getMessage() : "搜索失败");
                });
            } finally {
                if (socket != null) socket.close();
                running.set(false);
                List<Device> list = new ArrayList<>(found.values());
                main.post(() -> {
                    if (cb != null) cb.onFinished(list);
                });
            }
        });
    }

    private static Device parse(String text) {
        if (text == null || !text.toUpperCase(Locale.US).contains("HTTP/1.")) return null;
        Device d = new Device();
        Matcher m = HDR.matcher(text);
        while (m.find()) {
            String k = m.group(1).toUpperCase(Locale.US);
            String v = m.group(2).trim();
            switch (k) {
                case "LOCATION":
                    d.location = v;
                    break;
                case "USN":
                    d.usn = v;
                    break;
                case "SERVER":
                    d.server = v;
                    break;
                case "ST":
                    d.st = v;
                    break;
            }
        }
        if (d.location == null && d.usn == null) return null;
        // 简单友好名
        if (d.server != null) {
            d.friendlyName = d.server;
        }
        return d;
    }

    public void shutdown() {
        exec.shutdownNow();
    }
}

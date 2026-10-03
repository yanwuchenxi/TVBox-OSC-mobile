package com.github.tvbox.osc.util;

import android.Manifest;
import android.app.Activity;
import android.os.Build;

import androidx.fragment.app.FragmentActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.XXPermissions;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

/**
 * targetSdk 34 运行时权限：通知、媒体读取。仅请求尚未授予的权限。
 */
public final class PermissionHelper {
    private static final String KEY_ASKED = "perm_runtime_asked_v1";

    private PermissionHelper() {
    }

    public static void requestStartupPermissions(FragmentActivity activity) {
        if (activity == null || activity.isFinishing()) return;
        if (ConfigStore.getBool(KEY_ASKED, false)) return;

        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            if (!XXPermissions.isGranted(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                need.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (!XXPermissions.isGranted(activity, Manifest.permission.READ_MEDIA_VIDEO)) {
                need.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (!XXPermissions.isGranted(activity, Manifest.permission.READ_MEDIA_IMAGES)) {
                need.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (!XXPermissions.isGranted(activity, Manifest.permission.READ_MEDIA_AUDIO)) {
                need.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (!XXPermissions.isGranted(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                need.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (need.isEmpty()) {
            ConfigStore.putBool(KEY_ASKED, true);
            return;
        }
        XXPermissions.with(activity)
                .permission(need)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        ConfigStore.putBool(KEY_ASKED, true);
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        ConfigStore.putBool(KEY_ASKED, true);
                    }
                });
    }

    public static void requestMediaIfNeeded(Activity activity, Runnable onDone) {
        if (activity == null) {
            if (onDone != null) onDone.run();
            return;
        }
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            if (!XXPermissions.isGranted(activity, Manifest.permission.READ_MEDIA_VIDEO)) {
                need.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
        } else if (!XXPermissions.isGranted(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            need.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (need.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }
        XXPermissions.with(activity)
                .permission(need)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (onDone != null) onDone.run();
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (onDone != null) onDone.run();
                    }
                });
    }
}

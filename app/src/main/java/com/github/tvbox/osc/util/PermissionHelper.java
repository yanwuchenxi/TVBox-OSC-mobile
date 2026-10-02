package com.github.tvbox.osc.util;

import android.app.Activity;
import android.os.Build;

import androidx.fragment.app.FragmentActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
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
        // 避免每次进首页都弹；用户拒绝后仍可在设置里手动开
        if (Hawk.get(KEY_ASKED, false)) return;

        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            if (!XXPermissions.isGranted(activity, Permission.POST_NOTIFICATIONS)) {
                need.add(Permission.POST_NOTIFICATIONS);
            }
            if (!XXPermissions.isGranted(activity, Permission.READ_MEDIA_VIDEO)) {
                need.add(Permission.READ_MEDIA_VIDEO);
            }
            if (!XXPermissions.isGranted(activity, Permission.READ_MEDIA_IMAGES)) {
                need.add(Permission.READ_MEDIA_IMAGES);
            }
            if (!XXPermissions.isGranted(activity, Permission.READ_MEDIA_AUDIO)) {
                need.add(Permission.READ_MEDIA_AUDIO);
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (!XXPermissions.isGranted(activity, Permission.READ_EXTERNAL_STORAGE)) {
                need.add(Permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (need.isEmpty()) {
            Hawk.put(KEY_ASKED, true);
            return;
        }
        XXPermissions.with(activity)
                .permission(need)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        Hawk.put(KEY_ASKED, true);
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        Hawk.put(KEY_ASKED, true);
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
            if (!XXPermissions.isGranted(activity, Permission.READ_MEDIA_VIDEO)) {
                need.add(Permission.READ_MEDIA_VIDEO);
            }
        } else if (!XXPermissions.isGranted(activity, Permission.READ_EXTERNAL_STORAGE)) {
            need.add(Permission.READ_EXTERNAL_STORAGE);
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

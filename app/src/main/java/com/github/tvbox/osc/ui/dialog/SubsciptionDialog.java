package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.databinding.DialogInputSubsriptionBinding;
import com.lxj.xpopup.core.CenterPopupView;

/**
 * 添加订阅弹窗：支持粘贴剪贴板、本地文件、名称可选。
 */
public class SubsciptionDialog extends CenterPopupView {

    public interface OnSubsciptionListener {
        void onConfirm(String name, String url, boolean check);

        void chooseLocal(boolean check);
    }

    private final String mDefaultName;
    private OnSubsciptionListener listener;

    public SubsciptionDialog(@NonNull Context context, String defaultName, OnSubsciptionListener listener) {
        super(context);
        mDefaultName = defaultName == null ? "" : defaultName;
        this.listener = listener;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_input_subsription;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        DialogInputSubsriptionBinding binding = DialogInputSubsriptionBinding.bind(getPopupImplView());
        if (!TextUtils.isEmpty(mDefaultName)) {
            binding.etName.setText(mDefaultName);
            binding.etName.setSelection(mDefaultName.length());
        }

        // 打开时若剪贴板是链接，自动填入地址
        try {
            CharSequence clip = ClipboardUtils.getText();
            if (clip != null) {
                String c = clip.toString().trim();
                if (looksLikeUrl(c)) {
                    binding.etUrl.setText(c);
                    binding.etUrl.setSelection(c.length());
                }
            }
        } catch (Throwable ignored) {
        }

        binding.tvPaste.setOnClickListener(v -> {
            try {
                CharSequence clip = ClipboardUtils.getText();
                if (clip == null || TextUtils.isEmpty(clip.toString().trim())) {
                    ToastUtils.showShort("剪贴板为空");
                    return;
                }
                String c = clip.toString().trim();
                binding.etUrl.setText(c);
                binding.etUrl.setSelection(c.length());
                ToastUtils.showShort("已粘贴");
            } catch (Throwable e) {
                ToastUtils.showShort("无法读取剪贴板");
            }
        });

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnConfirm.setOnClickListener(view -> {
            String name = binding.etName.getText() == null ? "" : binding.etName.getText().toString().trim();
            String url = binding.etUrl.getText() == null ? "" : binding.etUrl.getText().toString().trim();
            url = normalizeUrl(url);
            if (TextUtils.isEmpty(url)) {
                ToastUtils.showShort("请输入订阅地址");
                return;
            }
            if (!looksLikeUrl(url) && !url.startsWith("clan://")) {
                ToastUtils.showShort("地址格式不正确");
                return;
            }
            if (TextUtils.isEmpty(name)) {
                name = suggestName(url);
            }
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            if (listener != null) {
                listener.onConfirm(name, url, binding.cbCheck.isChecked());
            }
            dismiss();
        });

        binding.tvLocal.setOnClickListener(view -> {
            dismissWith(() -> {
                if (listener != null) {
                    listener.chooseLocal(binding.cbCheck.isChecked());
                }
            });
        });
    }

    public static boolean looksLikeUrl(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        return t.startsWith("http://") || t.startsWith("https://") || t.startsWith("clan://");
    }

    public static String normalizeUrl(String url) {
        if (url == null) return "";
        String u = url.trim();
        // 去掉首尾引号、多余空格
        if ((u.startsWith("\"") && u.endsWith("\"")) || (u.startsWith("'") && u.endsWith("'"))) {
            u = u.substring(1, u.length() - 1).trim();
        }
        return u;
    }

    public static String suggestName(String url) {
        try {
            if (url.startsWith("clan://")) {
                int idx = url.lastIndexOf('/');
                if (idx >= 0 && idx < url.length() - 1) {
                    return url.substring(idx + 1);
                }
                return "本地订阅";
            }
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return host.replace("www.", "");
            }
        } catch (Throwable ignored) {
        }
        return "订阅" + (System.currentTimeMillis() % 10000);
    }
}

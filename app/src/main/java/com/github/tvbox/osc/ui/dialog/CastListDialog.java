package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.CastVideo;
import com.lxj.xpopup.core.CenterPopupView;

import org.jetbrains.annotations.NotNull;

public class CastListDialog extends CenterPopupView {
    private final CastVideo castVideo;

    public CastListDialog(@NonNull @NotNull Context context, CastVideo castVideo) {
        super(context);
        this.castVideo = castVideo;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_cast;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        TextView title = findViewById(R.id.title);
        TextView tvUrl = findViewById(R.id.tv_url);
        if (title != null) {
            title.setText(TextUtils.isEmpty(castVideo.getName()) ? "投屏" : ("投屏 · " + castVideo.getName()));
        }
        if (tvUrl != null) {
            tvUrl.setText(castVideo.getUrl());
        }
        findViewById(R.id.btn_copy).setOnClickListener(v -> {
            if (TextUtils.isEmpty(castVideo.getUrl())) {
                ToastUtils.showShort("无播放地址");
                return;
            }
            ClipboardUtils.copyText(castVideo.getUrl());
            ToastUtils.showLong("已复制播放地址");
        });
        findViewById(R.id.btn_share).setOnClickListener(v -> {
            if (TextUtils.isEmpty(castVideo.getUrl())) {
                ToastUtils.showShort("无播放地址");
                return;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, castVideo.getName());
                intent.putExtra(Intent.EXTRA_TEXT, castVideo.getName() + "\n" + castVideo.getUrl());
                getContext().startActivity(Intent.createChooser(intent, "投屏/分享到"));
            } catch (Throwable e) {
                ClipboardUtils.copyText(castVideo.getUrl());
                ToastUtils.showShort("已复制地址");
            }
        });
        findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
    }
}

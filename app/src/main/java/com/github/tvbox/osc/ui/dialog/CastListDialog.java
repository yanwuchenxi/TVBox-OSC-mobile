package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.CastVideo;
import com.lxj.xpopup.core.CenterPopupView;

import org.jetbrains.annotations.NotNull;

/**
 * 投屏弹窗占位：原 DLNA 依赖在构建环境无法解析，暂提示不可用。
 */
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
        Toast.makeText(getContext(), "投屏组件暂不可用", Toast.LENGTH_SHORT).show();
        post(this::dismiss);
    }
}

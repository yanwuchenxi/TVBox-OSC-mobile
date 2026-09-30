package com.github.tvbox.osc.ui.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;

import java.util.ArrayList;

/** 投屏设备列表占位（无 DLNA 依赖） */
public class CastDevicesAdapter extends BaseQuickAdapter<Object, BaseViewHolder> {
    public CastDevicesAdapter() {
        super(R.layout.item_cast_device, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Object item) {
        helper.setText(R.id.tv_name, item == null ? "" : String.valueOf(item));
    }
}

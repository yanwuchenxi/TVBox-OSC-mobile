package com.github.tvbox.osc.ui.adapter;

import android.graphics.Color;
import android.view.View;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Subscription;

import java.util.Comparator;
import java.util.List;

public class SubscriptionAdapter extends BaseQuickAdapter<Subscription, BaseViewHolder> {
    public SubscriptionAdapter() {
        super(R.layout.item_subscription);
    }

    @Override
    protected void convert(BaseViewHolder helper, Subscription item) {
        helper.setText(R.id.tv_name, item.getName())
                .setText(R.id.tv_url, item.getUrl())
                .setChecked(R.id.cb, item.isChecked())
                .setVisible(R.id.iv_pushpin, item.isTop());

        helper.addOnClickListener(R.id.iv_del);

        View status = helper.getView(R.id.tv_status);
        if (status != null) {
            int hs = item.getHealthStatus();
            if (hs == Subscription.STATUS_UNKNOWN) {
                status.setVisibility(View.GONE);
            } else {
                status.setVisibility(View.VISIBLE);
                helper.setText(R.id.tv_status, item.getHealthMsg());
                int color;
                if (hs == Subscription.STATUS_OK) color = Color.parseColor("#4CAF50");
                else if (hs == Subscription.STATUS_FAIL) color = Color.parseColor("#F44336");
                else color = Color.parseColor("#FF9800");
                helper.setTextColor(R.id.tv_status, color);
            }
        }
    }

    @Override
    public void setNewData(@Nullable List<Subscription> data) {
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                for (int j = i + 1; j < data.size(); j++) {
                    if (data.get(i).getUrl().equals(data.get(j).getUrl())) {
                        data.remove(j);
                        j--;
                    }
                }
            }
            data.sort(mComparator);
        }
        super.setNewData(data);
    }

    Comparator<Subscription> mComparator = (s1, s2) -> {
        if (s1.isTop() && !s2.isTop()) return -1;
        if (!s1.isTop() && s2.isTop()) return 1;
        if (s1.isChecked() && !s2.isChecked()) return -1;
        if (!s1.isChecked() && s2.isChecked()) return 1;
        return 0;
    };
}

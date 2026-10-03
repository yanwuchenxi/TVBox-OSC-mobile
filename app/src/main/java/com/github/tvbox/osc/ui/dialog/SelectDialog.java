package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ConvertUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SelectDialog<T> extends BaseDialog {

    public SelectDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_select);
    }

    public SelectDialog(@NonNull @NotNull Context context, int resId) {
        super(context);
        setContentView(resId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.gravity = Gravity.CENTER;
        lp.width = ConvertUtils.dp2px(330);
        getWindow().setAttributes(lp);
        getWindow().setWindowAnimations(R.style.DialogFadeAnimation);
        findViewById(R.id.iv_close).setOnClickListener(view -> dismiss());
    }

    public void setTip(String tip) {
        ((TextView) findViewById(R.id.title)).setText(tip);
    }

    public void setAdapter(SelectDialogAdapter.SelectDialogInterface<T> sourceBeanSelectDialogInterface,
                           DiffUtil.ItemCallback<T> sourceBeanItemCallback, List<T> data, int select) {
        SelectDialogAdapter<T> adapter = new SelectDialogAdapter<>(sourceBeanSelectDialogInterface, sourceBeanItemCallback);
        final int itemCount = data == null ? 0 : data.size();
        final int safeSelect;
        if (itemCount <= 0) {
            safeSelect = 0;
        } else if (select < 0) {
            safeSelect = 0;
        } else if (select >= itemCount) {
            safeSelect = itemCount - 1;
        } else {
            safeSelect = select;
        }
        adapter.setData(data, safeSelect);
        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setAdapter(adapter);
        if (itemCount <= 0) {
            return;
        }
        recyclerView.post(() -> {
            try {
                if (safeSelect >= 0 && safeSelect < itemCount) {
                    recyclerView.smoothScrollToPosition(safeSelect);
                }
            } catch (IllegalArgumentException ignored) {
            }
        });
    }
}

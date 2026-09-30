package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Subscription;
import com.lxj.xpopup.core.CenterPopupView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 多线路一键预览并勾选导入。
 */
public class MultiLinePreviewDialog extends CenterPopupView {

    public interface OnImportListener {
        void onImport(List<Subscription> selected);
    }

    public static class LineItem {
        public String name;
        public String url;
        public boolean checked = true;

        public LineItem(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    private final List<LineItem> lines;
    private final OnImportListener listener;
    private LineAdapter adapter;

    public MultiLinePreviewDialog(@NonNull @NotNull Context context, List<LineItem> lines, OnImportListener listener) {
        super(context);
        this.lines = lines != null ? lines : new ArrayList<>();
        this.listener = listener;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_multiline_preview;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        TextView tip = findViewById(R.id.tv_tip);
        tip.setText("共 " + lines.size() + " 条线路，勾选后导入");
        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LineAdapter(lines);
        rv.setAdapter(adapter);

        findViewById(R.id.btn_all).setOnClickListener(v -> {
            boolean anyUnchecked = false;
            for (LineItem it : lines) {
                if (!it.checked) {
                    anyUnchecked = true;
                    break;
                }
            }
            for (LineItem it : lines) it.checked = anyUnchecked;
            adapter.notifyDataSetChanged();
        });
        findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        findViewById(R.id.btn_import).setOnClickListener(v -> {
            List<Subscription> selected = new ArrayList<>();
            for (LineItem it : lines) {
                if (it.checked) {
                    selected.add(new Subscription(it.name, it.url));
                }
            }
            if (selected.isEmpty()) {
                ToastUtils.showShort("请至少选择一条线路");
                return;
            }
            if (listener != null) listener.onImport(selected);
            dismiss();
        });
    }

    static class LineAdapter extends RecyclerView.Adapter<LineAdapter.VH> {
        private final List<LineItem> data;

        LineAdapter(List<LineItem> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_multiline_line, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            LineItem item = data.get(position);
            h.name.setText(item.name);
            h.url.setText(item.url);
            h.cb.setOnCheckedChangeListener(null);
            h.cb.setChecked(item.checked);
            h.cb.setOnCheckedChangeListener((buttonView, isChecked) -> item.checked = isChecked);
            h.itemView.setOnClickListener(v -> {
                item.checked = !item.checked;
                h.cb.setChecked(item.checked);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            CheckBox cb;
            TextView name, url;

            VH(@NonNull View itemView) {
                super(itemView);
                cb = itemView.findViewById(R.id.cb);
                name = itemView.findViewById(R.id.tv_name);
                url = itemView.findViewById(R.id.tv_url);
            }
        }
    }
}

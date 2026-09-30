package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.CastVideo;
import com.github.tvbox.osc.util.DlnaDeviceScanner;
import com.lxj.xpopup.core.CenterPopupView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 投屏：局域网 DLNA 设备搜索 + 复制/分享播放地址。
 * 完整 AVTransport 控制因设备差异大，选中设备后优先复制/分享到对应投屏应用。
 */
public class CastListDialog extends CenterPopupView {
    private final CastVideo castVideo;
    private final List<DlnaDeviceScanner.Device> devices = new ArrayList<>();
    private DeviceAdapter adapter;
    private DlnaDeviceScanner scanner;
    private TextView tvStatus;

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
        tvStatus = findViewById(R.id.tv_status);
        RecyclerView rv = findViewById(R.id.rv_devices);
        if (title != null) {
            title.setText(TextUtils.isEmpty(castVideo.getName()) ? "投屏" : ("投屏 · " + castVideo.getName()));
        }
        if (tvUrl != null) tvUrl.setText(castVideo.getUrl());

        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new DeviceAdapter(devices, device -> {
                // 选中设备：复制地址并提示用投屏 App / 电视端打开
                ClipboardUtils.copyText(castVideo.getUrl());
                ToastUtils.showLong("已复制播放地址\n设备：" + device.displayName() +
                        (device.location != null ? ("\n" + device.location) : ""));
            });
            rv.setAdapter(adapter);
        }

        View btnCopy = findViewById(R.id.btn_copy);
        View btnShare = findViewById(R.id.btn_share);
        View btnSearch = findViewById(R.id.btn_search);
        View btnCancel = findViewById(R.id.btn_cancel);

        if (btnCopy != null) btnCopy.setOnClickListener(v -> {
            if (TextUtils.isEmpty(castVideo.getUrl())) {
                ToastUtils.showShort("无播放地址");
                return;
            }
            ClipboardUtils.copyText(castVideo.getUrl());
            ToastUtils.showLong("已复制播放地址");
        });
        if (btnShare != null) btnShare.setOnClickListener(v -> {
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
            }
        });
        if (btnSearch != null) btnSearch.setOnClickListener(v -> startSearch());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dismiss());

        startSearch();
    }

    private void startSearch() {
        if (tvStatus != null) tvStatus.setText("正在搜索局域网 DLNA 设备…");
        devices.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (scanner != null) scanner.shutdown();
        scanner = new DlnaDeviceScanner();
        scanner.search(new DlnaDeviceScanner.Callback() {
            @Override
            public void onDevice(DlnaDeviceScanner.Device device) {
                devices.add(device);
                if (adapter != null) adapter.notifyItemInserted(devices.size() - 1);
                if (tvStatus != null) tvStatus.setText("已发现 " + devices.size() + " 台设备");
            }

            @Override
            public void onFinished(List<DlnaDeviceScanner.Device> all) {
                if (tvStatus != null) {
                    tvStatus.setText(all.isEmpty()
                            ? "未发现 DLNA 设备，仍可复制/分享链接"
                            : ("搜索完成，共 " + all.size() + " 台"));
                }
            }

            @Override
            public void onError(String msg) {
                if (tvStatus != null) tvStatus.setText("搜索异常：" + msg);
            }
        }, 4000);
    }

    @Override
    public void onDismiss() {
        super.onDismiss();
        if (scanner != null) scanner.shutdown();
    }

    interface OnPick {
        void onPick(DlnaDeviceScanner.Device d);
    }

    static class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.VH> {
        private final List<DlnaDeviceScanner.Device> data;
        private final OnPick pick;

        DeviceAdapter(List<DlnaDeviceScanner.Device> data, OnPick pick) {
            this.data = data;
            this.pick = pick;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cast_device, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            DlnaDeviceScanner.Device d = data.get(position);
            h.name.setText(d.displayName());
            h.itemView.setOnClickListener(v -> {
                if (pick != null) pick.onPick(d);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView name;

            VH(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_name);
            }
        }
    }
}

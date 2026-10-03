package com.github.tvbox.osc.util;

import androidx.recyclerview.widget.RecyclerView;

/**
 * 安全滚动：避免 RecyclerView / RecyclerView 在非法 position 上 smoothScroll 崩溃。
 */
public final class RecyclerViewSafe {
    private RecyclerViewSafe() {
    }

    public static int clamp(int position, int itemCount) {
        if (itemCount <= 0) return 0;
        if (position < 0) return 0;
        if (position >= itemCount) return itemCount - 1;
        return position;
    }

    public static void smoothScrollToPosition(RecyclerView rv, int position) {
        if (rv == null) return;
        RecyclerView.Adapter<?> adapter = rv.getAdapter();
        int count = adapter != null ? adapter.getItemCount() : 0;
        if (count <= 0) return;
        int pos = clamp(position, count);
        try {
            rv.smoothScrollToPosition(pos);
        } catch (IllegalArgumentException ignored) {
        }
    }
}

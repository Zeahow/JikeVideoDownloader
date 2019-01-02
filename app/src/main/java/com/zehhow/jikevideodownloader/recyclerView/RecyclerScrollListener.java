package com.zehhow.jikevideodownloader.recyclerView;

import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;

/**
 * RecyclerView滑动事件侦听器，用于控制悬浮按钮的显示与隐藏
 */
public class RecyclerScrollListener extends RecyclerView.OnScrollListener {
    private static final int THRESHOLD = 3;     // 滑动的距离
    private int distance = 0;
    private FloatingActionButton fabButton;     // 要控制的悬浮按钮
    private boolean visible = true;             // 是否可见

    /***
     * 构造函数
     * @param fabButton 要控制的悬浮按钮
     */
    public RecyclerScrollListener(FloatingActionButton fabButton) {
        this.fabButton = fabButton;
    }

    /***
     * 滚动时回调
     */
    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (visible && dy > 0 || (!visible && dy < 0))  // 累计滑动距离
            distance += dy;

        if (distance > THRESHOLD && visible) {          // 上滑隐藏
            visible = false;
            fabButton.hide();
            distance = 0;
        } else if (distance < -THRESHOLD && !visible) { // 下滑显示
            visible = true;
            fabButton.show();
            distance = 0;
        }
    }
}

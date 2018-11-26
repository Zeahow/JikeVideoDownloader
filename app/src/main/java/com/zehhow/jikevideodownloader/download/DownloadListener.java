package com.zehhow.jikevideodownloader.download;

import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;

import com.zehhow.jikevideodownloader.dao.TaskBean;

public class DownloadListener {

    private TaskBean task;
    private ProgressBar progressBar;
    private Button button;

    /**
     * 构造函数
     * @param task 下载任务信息
     */
    public DownloadListener(TaskBean task) {
        this.task = task;
    }

    /**
     * 绑定进度条
     * @param progressBar 要更新进度的进度条
     */
    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    /**
     * 绑定按钮，用以显示当前状态
     */
    public void setButton(Button button) {
        this.button = button;
    }

    /**
     * 下载中
     * @param progress 下载进度
     */
    void onProgress(int progress) {
        task.progress = progress;
        if(progressBar != null)
            progressBar.setProgress(progress);
        Log.d("JKVD", " Progress " + progress + ": " + task.name);
    }

    /**
     * 成功
     */
    void onSuceess() {
        task.status = TaskStatus.SUCCESS;
        if(button != null)
            button.setText("已完成");
        Log.d("JKVD", " Success: " + task.name);
    }

    /**
     * 失败
     */
    void onFailed() {
        task.status = TaskStatus.FAILED;
        if(button != null)
            button.setText("下载失败");
        Log.d("JKVD", " Failed: " + task.name);
    }

    /**
     * 暂停
     */
    void onPaused() {
        task.status = TaskStatus.PAUSED;
        Log.d("JKVD", " Paused: " + task.name);
    }

    /**
     * 取消
     */
    void onCanceled() {
        task.status = TaskStatus.CANCELED;
        Log.d("JKVD", " Canceled: " + task.name);
    }
}

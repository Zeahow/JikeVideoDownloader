package com.zehhow.jikevideodownloader.download;

import android.util.Log;
import android.widget.ProgressBar;

import com.zehhow.jikevideodownloader.dao.TaskBean;

public class DownloadListener {

    private TaskBean task;
    private ProgressBar progressBar;

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
     * 下载中
     * @param progress 下载进度
     */
    void onProgress(int progress) {
        task.progress = progress;
        if(progressBar != null)
            progressBar.setProgress(progress);
    }

    /**
     * 成功
     */
    void onSuceess() {
        task.status = TaskStatus.SUCCESS;
        Log.d("JKVD", " Success: " + task.name);
    }

    /**
     * 失败
     */
    void onFailed() {
        task.status = TaskStatus.FAILED;
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

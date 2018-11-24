package com.zehhow.jikevideodownloader.download;

public interface DownloadListener {

    /**
     * 下载中
     */
    void onProgress(int progress);

    /**
     * 成功
     */
    void onSuceess();

    /**
     * 失败
     */
    void onFailed();

    /**
     * 暂停
     */
    void onPaused();

    /**
     * 取消
     */
    void onCanceled();
}

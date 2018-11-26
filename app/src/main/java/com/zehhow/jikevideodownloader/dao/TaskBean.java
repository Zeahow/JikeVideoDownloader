package com.zehhow.jikevideodownloader.dao;

import com.zehhow.jikevideodownloader.download.DownloadListener;
import com.zehhow.jikevideodownloader.download.DownloadTask;
import com.zehhow.jikevideodownloader.download.TaskStatus;

// 下载任务bean类
public class TaskBean {
    // 视频链接的hashCode
    public int urlHashCode = 0;
    // 视频链接
    public String url = null;
    // 任务名
    public String name = null;
    // 任务保存路径
    public String path = null;
    // 任务文件已下载长度
    public long downloadedLength = 0;
    // 任务文件总长度
    public long totalLength = 0;
    // 任务下载进度
    public int progress = 0;
    // 任务状态
    public TaskStatus status = TaskStatus.PAUSED;
    // 所绑定的下载器
    public DownloadTask downloadTask = null;
    // 所绑定的下载侦听
    public DownloadListener downloadListener = null;  


    TaskBean() {}

    public TaskBean(String url, String name, String path) {
        this.url = url;
        this.name = name;
        this.path = path;
        this.urlHashCode = (url != null ? url.hashCode() : 0);
        this.downloadedLength = 0;
        this.totalLength = 0;
        this.progress = 0;
        this.status = TaskStatus.PAUSED;
        this.downloadTask = null;
    }

    public void setDownloadTask(DownloadTask downloadTask) {
        this.downloadTask = downloadTask;
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    String[] toStringArray() {
        return new String[]{
                urlHashCode + "",
                url,
                name,
                path,
                totalLength + "",
                downloadedLength + ""
            };
    }
}

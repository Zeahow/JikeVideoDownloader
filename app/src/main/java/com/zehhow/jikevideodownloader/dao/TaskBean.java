package com.zehhow.jikevideodownloader.dao;

// 下载任务bean类
public class TaskBean {
    public int urlHashCode;         // 下载连接的hashCode
    public String url;              // 下载链接
    public String name;             // 任务名
    public String path;             // 任务保存路径
    public long totalLength;        // 任务文件总长度
    public long downloadedLength;   // 任务文件已下载长度

    TaskBean() {}

    public TaskBean(String url, String name, String path) {
        this.url = url;
        this.name = name;
        this.path = path;
        this.urlHashCode = (url != null ? url.hashCode() : 0);
        this.downloadedLength = 0;
        this.totalLength = 0;
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

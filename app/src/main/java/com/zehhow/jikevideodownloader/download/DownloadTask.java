package com.zehhow.jikevideodownloader.download;

import android.os.AsyncTask;

public class DownloadTask extends AsyncTask<String, Integer, TaskStatus> {

    private DownloadListener listener;
    private TaskStatus taskStatus = TaskStatus.NORMAL;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected TaskStatus doInBackground(String... strings) {
        String realUrl = DownloadUtil.getM3u8Url(strings[0]);
        if(realUrl == null) return TaskStatus.FAILED;
        int urlHashCode = realUrl.hashCode();

        return null;
    }
}

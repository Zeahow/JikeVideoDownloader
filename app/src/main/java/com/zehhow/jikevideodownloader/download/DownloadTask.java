package com.zehhow.jikevideodownloader.download;

import android.os.AsyncTask;
import android.util.Log;

import com.zehhow.jikevideodownloader.okHttp.HttpClient;

import java.io.IOException;
import java.util.Vector;

import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, TaskStatus> {

    private DownloadListener listener;
    private TaskStatus taskStatus = TaskStatus.NORMAL;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected TaskStatus doInBackground(String... strings) {
        String realUrl = getVedioRealUrl(strings[0]);
        if(realUrl == null) return TaskStatus.FAILED;

        return null;
    }

    // 获取视频真实的m3u8地址
    public String getVedioRealUrl(String url) {
        if(url == null) return null;

        String prefix = "https://m.okjike.com/";
        String type = url.substring(prefix.length(), url.indexOf("s/"));
        if(type.equals("officialMessage"))
            type = "OFFICIAL_MESSAGE";
        else if(type.equals("originalPost"))
            type = "ORIGINAL_POST";

        String id = url.substring(prefix.length() + type.length() + 1, url.indexOf('?'));
        url = "https://app.jike.ruguoapp.com/1.0/mediaMeta/play?type=" + type + "&id=" + id;
        Log.d("JKVD", "URL: " + url);
        Request request = new Request.Builder().url(url).build();

        // 请求该网址后返回一个Json，里面含有真实地址
        try {
            Response response = HttpClient.getInstance().newCall(request).execute();
            if(!response.isSuccessful() || response.body() == null) return null;
            String realUrl = response.body().string();
            int beginIndex = realUrl.indexOf("\":\"");  // 返回内容为{"url":null}则说明分享的不是视频
            if(beginIndex == -1) {
                Log.d("JKVD", "Not vedio");
                return null;
            }

            realUrl = realUrl.substring(beginIndex + 3, realUrl.indexOf("\"}"));
            Log.d("JKVD", "RealURL: " + realUrl);
            return realUrl;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // m3u8地址获取所有ts分段的地址
    public Vector<String> getAllTsUrl(String url) {
        if(url == null) return null;
        Vector<String> urls = new Vector<>();
        String prefix = "https://media-txcdn.ruguoapp.com/";
        String strs[];

        Request request = new Request.Builder().url(url).build();
        try {
            Response response = HttpClient.getInstance().newCall(request).execute();
            if(!response.isSuccessful() || response.body() == null) return null;
            strs = response.body().string().split("\n");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 以#开头的为其它说明字段
        for (String s : strs) {
            if (s.startsWith("#")) continue;
            Log.d("JKVD", "ts URL: " + s);
            urls.add(prefix + s);
        }

        return urls;
    }
}

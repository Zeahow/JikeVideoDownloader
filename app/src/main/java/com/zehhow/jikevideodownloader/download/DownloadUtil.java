package com.zehhow.jikevideodownloader.download;

import android.util.Log;

import com.zehhow.jikevideodownloader.okHttp.HttpClient;

import java.io.IOException;
import java.util.Vector;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadUtil {
    /**
     * 获取视频真实的m3u8地址
     * @param url 视频网页地址
     * @return 视频的mu8地址
     */

    public static String getM3u8Url(String url) {
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
            String m3u8Url = response.body().string();
            int beginIndex = m3u8Url.indexOf("\":\"");  // 返回内容为{"url":null}则说明分享的不是视频
            if(beginIndex == -1) {
                Log.d("JKVD", "Not vedio");
                return null;
            }

            m3u8Url = m3u8Url.substring(beginIndex + 3, m3u8Url.indexOf("\"}"));
            Log.d("JKVD", "RealURL: " + m3u8Url);

            response.body().close();
            return m3u8Url;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据m3u8地址获取所有ts分段的地址
     * @param m3u8Url 视频的m3u8地址
     * @return 解析m3u8地址后获取的所有分段地址
     */
    static Vector<String> getAllTsUrls(String m3u8Url) {
        if(m3u8Url == null) return null;
        Vector<String> urls = new Vector<>();
        // 此地址开头的是一个mp4地址，故不需再获取ts分段地址
        if(m3u8Url.startsWith("https://videocdn.ruguoapp.com")) {
            Log.d("JKVD", "Not m3u8");
            Log.d("JKVD", "MP4 URL: " + m3u8Url);
            urls.add(m3u8Url);
            return urls;
        }

        String prefix = "https://media-txcdn.ruguoapp.com/";
        String strs[];

        Request request = new Request.Builder().url(m3u8Url).build();
        try {
            Response response = HttpClient.getInstance().newCall(request).execute();
            if(!response.isSuccessful() || response.body() == null) return null;
            strs = response.body().string().split("\n");
            response.body().close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 以#开头的为其它说明字段
        for (String s : strs) {
            if (s.startsWith("#")) continue;
            Log.d("JKVD", " Ts URL: " + s);
            urls.add(prefix + s);
        }

        return urls;
    }

    /**
     * 获取单个视频文件的长度
     * @param url 视频文件地址
     * @return 视频长度
     */
    private static long getVedioLength(String url) {
        if(url == null) return 0;
        Request request = new Request.Builder().url(url).head().build();

        try {
            Response response = HttpClient.getInstance().newCall(request).execute();
            if(!response.isSuccessful()) return 0;
            Headers headers = response.headers();

            String strLen = headers.get("Content-Length");
            return strLen == null ? 0 : Long.valueOf(strLen);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取多个视频文件的长度
     * @param urls 多个视频地址
     * @return 每个视频的长度，与urls一一对应
     */
    static Vector<Long> getVedioLength(Vector<String> urls) {
        Vector<Long> lengths = new Vector<>();
        if(urls == null || urls.isEmpty()) return lengths;

        for(String url : urls)
            lengths.add(getVedioLength(url));

        return lengths;
    }
}

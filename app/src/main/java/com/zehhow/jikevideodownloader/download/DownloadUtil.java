package com.zehhow.jikevideodownloader.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import com.zehhow.jikevideodownloader.okHttp.HttpClient;

import java.io.File;
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

    static String getM3u8Url(String url) {
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

            m3u8Url = m3u8Url.substring(beginIndex + 3, m3u8Url.indexOf("\","));
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

        String urlWithoutQuery = "";    // 出去查询部分的Url地址
        if(m3u8Url.indexOf('?') != -1)
            urlWithoutQuery = m3u8Url.substring(0, m3u8Url.indexOf('?'));
        // 此地址开头的是一个mp4地址，故不需再获取ts分段地址
        // 当urlWithoutQuery地址以.mp4结束时也是一个mp4地址
        if(m3u8Url.startsWith("https://videocdn.ruguoapp.com") || urlWithoutQuery.endsWith(".mp4")) {
            Log.d("JKVD", "Not m3u8");
            Log.d("JKVD", "MP4 URL: " + m3u8Url);
            urls.add(m3u8Url);
            return urls;
        }

        // 获取ts分段地址前缀
        String prefix = m3u8Url.substring(0, m3u8Url.lastIndexOf('/') + 1);
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

        // 判断获取到的是否是ts分段的地址。注：第5行才是第一个ts分段的下载地址
        if(strs.length <= 6 || !strs[5].endsWith(".ts"))
            return null;

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


    /**
     * 获取文件路径所对应的Uri
     * @param file 文件路径
     * @return 对应的Uri
     */
    public static Uri getUriForFile(Context context, File file) {
        // 安卓7以上版本需调用内容提供器
        Uri uri;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            // 微信不支持FileProvider提供的Uri，需要到MediaStore中去查询
            uri = getUriFromMediaStore(context, file.getAbsolutePath());
            //uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        else
            uri = Uri.fromFile(file);

        return uri;
    }

    /**
     * 在MediaStore数据库中查询视频的Uri
     * @param filePath 视频文件路径
     * @return 对应的Uri
     */
    private static Uri getUriFromMediaStore(Context context, String filePath) {
        Uri uri = null;
        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Media._ID}, MediaStore.Video.Media.DATA + "=?",
                new String[]{filePath}, null);

        // 数据库中存在则直接获取id，然后构建uri
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "" + id);
            }

            cursor.close();
        }

        // 数据库中不存在则新插入一条
        if (uri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DATA, filePath);
            uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }

        return uri;
    }
}

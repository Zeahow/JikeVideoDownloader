package com.zehhow.jikevideodownloader.okHttp;
import okhttp3.OkHttpClient;

public class HttpClient {
    private static OkHttpClient instance;

    private HttpClient(){ }

    public static OkHttpClient getInstance() {
        if(instance == null)
            instance = new OkHttpClient();

        return instance;
    }
}

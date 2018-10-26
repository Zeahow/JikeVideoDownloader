package com.zehhow.jikevideodownloader.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.zehhow.jikevideodownloader.R;
import com.zehhow.jikevideodownloader.download.DownloadTask;
import com.zehhow.jikevideodownloader.okHttp.HttpClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class AddTaskDialog {
    private Activity activity;

    public AddTaskDialog(Activity activity) {
        this.activity = activity;
    }

    public void show(String url) {
        View view = activity.getLayoutInflater().inflate(R.layout.add_task_dialog_layout, null);
        final EditText urlTxt = view.findViewById(R.id.url);
        final EditText nameTxt = view.findViewById(R.id.name);

        // 链接输入框失去焦点时自动填写名字
        urlTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(nameTxt.getText().toString().isEmpty()
                        || urlTxt.getText().toString().isEmpty())

                    if(!hasFocus && nameTxt.getText().toString().isEmpty()) {
                        String _url = urlTxt.getText().toString();
                        setVedioName(_url, nameTxt);
                    }
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("添加下载任务")
                .setView(view)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("开始", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String _url = urlTxt.getText().toString();
                        if(_url.isEmpty()) return;
                        Log.d("JKVD", "URL: " + _url);

                        // 若未指定视频名称则默认设置为当前时间.mp4
                        if(nameTxt.getText().toString().isEmpty()) {
                            String name = SimpleDateFormat.getDateTimeInstance()
                                    .format(new Date(System.currentTimeMillis()));
                            name += ".mp4";
                            nameTxt.setText(name);
                            Toast.makeText(activity,
                                    "未指定视频名称，默认设置为\n" + name,
                                    Toast.LENGTH_SHORT).show();
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                DownloadTask task = new DownloadTask(null);
                                String realUrl = task.getVedioRealUrl(_url);
                                task.getAllTsUrl(realUrl);
                            }
                        }).start();
                    }
                }).create();


        dialog.show();

        // 设置链接、名称
        if(url != null) {
            urlTxt.setText(url);
            setVedioName(url, nameTxt);
        }
    }

    // 设置视频的名字
    private void setVedioName(String url, final EditText nameTxt) {
        if(url == null || url.isEmpty() || nameTxt == null) return ;

        Request request = new Request.Builder()
                .url(url)
                .build();
        HttpClient.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        nameTxt.setHint("获取名称失败");
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) {
                // 控件不可见则不更新名称
                if(nameTxt.getVisibility() != View.VISIBLE) return;

                if(response.body() != null)
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // 从源代码获取标题
                                String res = response.body().string();
                                res = res.substring(res.indexOf("<title"), res.indexOf("</title>"));
                                res = res.substring(res.indexOf('>')+1, res.indexOf(" - 即刻"));
                                res += ".mp4";
                                nameTxt.setText(res);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            }
        });
    }
}

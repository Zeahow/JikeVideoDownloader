package com.zehhow.jikevideodownloader.dialog;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.zehhow.jikevideodownloader.R;
import com.zehhow.jikevideodownloader.dao.TaskBean;
import com.zehhow.jikevideodownloader.download.DownloadListener;
import com.zehhow.jikevideodownloader.download.DownloadTask;
import com.zehhow.jikevideodownloader.download.DownloadUtil;
import com.zehhow.jikevideodownloader.okHttp.HttpClient;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


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
        final EditText pathTxt = view.findViewById(R.id.path);

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

        // 设置对话框标题、试图、按钮点击事件等
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
                        // 安卓6以上版本进行权限检查
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                            if(activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                                activity.requestPermissions(new String[]{permission}, 1);
                            }
                        }

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
                                String m3u8Url = DownloadUtil.getM3u8Url(_url);
                                TaskBean task = new TaskBean(m3u8Url,
                                        nameTxt.getText().toString(),
                                        pathTxt.getText().toString());
                                DownloadTask downloadTask = new DownloadTask(new DownloadListener() {
                                    @Override
                                    public void onProgress(int progress) {
                                        Log.d("JKVD", " 更新进度: " + progress);
                                    }

                                    @Override
                                    public void onSuceess() {
                                        Log.d("JKVD", " 下载成功");
                                    }

                                    @Override
                                    public void onFailed() {
                                        Log.d("JKVD", " 下载失败");
                                    }

                                    @Override
                                    public void onPaused() {
                                        Log.d("JKVD", " 暂停下载");
                                    }

                                    @Override
                                    public void onCanceled() {
                                        Log.d("JKVD", " 取消下载");
                                    }
                                });
                                downloadTask.execute(task);
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        nameTxt.setHint("获取名称失败");
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) {
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

package com.zehhow.jikevideodownloader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.zehhow.jikevideodownloader.dao.SQLiteHelper;
import com.zehhow.jikevideodownloader.dialog.AddTaskDialog;


public class MainActivity extends AppCompatActivity {
    // 悬浮按钮的状态
    private enum FabState {
        ADD,
        DELETE
    }
    private FabState fabState = FabState.ADD;
    private String url;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (fabState) {
                    case ADD:
                        addTask(null);
                        break;
                    case DELETE:
                        deleteTask();
                        break;
                }
            }
        });

        SQLiteHelper.init(this);    // 初始化数据库

        Intent intent = getIntent();
        // 判断是否由分享功能调用本Activity
        if(Intent.ACTION_SEND.equals(intent.getAction())) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            url = url.substring(url.indexOf("https://m.okjike"));
            addTask(url);
        }
    }

    /**
     * 新建下载任务
     * @param url 下载地址
     */
    private void addTask(String url) {
        this.url = url;
        if(checkPermission(1))      // 已有权限
            addTaskCallBack();
    }

    /**
     * 权限检查后新建任务的回调函数
     */
    private void addTaskCallBack() {
        new AddTaskDialog(this).show(url);
    }

    /**
     * 删除下载任务
     */
    private void deleteTask() {
        if(checkPermission(2))      // 已有权限
            deleteTaskCallBack();
    }

    /**
     * 权限检查后删除任务的回调函数
     */
    private void deleteTaskCallBack() {

    }

    /**
     * 权限检查
     * @param requestCode 请求码，区分调用方，以此确定回调函数。1表示addTask，2表示deleteTask
     * @return 此时是否拥有权
     */
    private boolean checkPermission(int requestCode) {
        // 安卓6以上版本进行权限检查
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, requestCode);
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case 1:
            case 2:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    if(requestCode == 1) addTaskCallBack();
                    else deleteTaskCallBack();
                else
                    Toast.makeText(this, "你拒绝了存储权限，无法继续操作", Toast.LENGTH_SHORT).show();
                break;
        }
    }

}

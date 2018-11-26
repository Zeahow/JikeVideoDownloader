package com.zehhow.jikevideodownloader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.zehhow.jikevideodownloader.recyclerView.RecyclerScrollListener;
import com.zehhow.jikevideodownloader.recyclerView.TaskAdapter;
import com.zehhow.jikevideodownloader.dao.SQLiteHelper;
import com.zehhow.jikevideodownloader.dao.TaskBean;
import com.zehhow.jikevideodownloader.dialog.AddTaskDialog;

import java.io.File;
import java.util.Vector;


public class MainActivity extends AppCompatActivity {
    // 悬浮按钮的状态
    private enum FabState {
        ADD,        // 新增任务
        DELETE      // 删除任务
    }
    private FabState fabState = FabState.ADD;
    private TaskAdapter taskAdapter;

    public TaskAdapter getTaskAdapter() {
        return taskAdapter;
    }

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
                if(!checkPermission()) return;
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
        initTaskList();                     // 初始化任务列表

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 判断是否由分享功能调用本Activity
        if(Intent.ACTION_SEND.equals(intent.getAction())) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            url = url.substring(url.indexOf("https://m.okjike"));
            addTask(url);
        }
    }

    /**
     * 初始化任务列表
     */
    private void initTaskList() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        // 设置布局
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 设置适配器
        taskAdapter = new TaskAdapter(new Vector<TaskBean>());
        recyclerView.setAdapter(taskAdapter);
        // 设置滑动侦听器，控制悬浮按钮
        recyclerView.addOnScrollListener(new RecyclerScrollListener((FloatingActionButton) findViewById(R.id.fab)));
        // 设置分割线
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        final Vector<TaskBean> taskList = SQLiteHelper.getInstance().queryAllTasks();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(TaskBean task : taskList)
                    taskAdapter.addTaskItem(task, false);
            }
        });
    }

    /**
     * 新建下载任务
     * @param url 下载地址
     */
    private void addTask(String url) {
        if(!checkPermission()) return;
        new AddTaskDialog(this).show(url);
    }

    /**
     * 删除下载任务
     */
    private void deleteTask() {
        if(!checkPermission()) return;
        //Todo
    }

    /**
     * 权限检查
     * @return 是否拥有权限
     */
    private boolean checkPermission() {
        // 安卓6以上版本进行权限检查
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, 1);
                return false;
            }
        }

        return true;
    }

    /**
     * 权限请求回调函数
     * @param requestCode 请求码
     * @param permissions 权限
     * @param grantResults 结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case 1:
                if(grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "你拒绝了存储权限，无法继续操作", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TaskBean task = taskAdapter.getCurrentTask();
        switch (item.getItemId()) {
            case 0:     // 删除
                Toast.makeText(this, task.name, Toast.LENGTH_SHORT).show();
                break;
            case 1:     // 分享
                shareVideo(task);
                break;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * 分享视频到其它软件
     * @param task 要分享的视频的TaskBean
     */
    private void shareVideo(TaskBean task) {
        Uri uri = TaskAdapter.getUriForFile(new File(task.path, task.name));
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, "分享视频"));
    }
}

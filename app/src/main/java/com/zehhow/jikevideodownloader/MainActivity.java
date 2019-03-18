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
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.zehhow.jikevideodownloader.download.DownloadUtil;
import com.zehhow.jikevideodownloader.download.TaskStatus;
import com.zehhow.jikevideodownloader.recyclerView.RecyclerScrollListener;
import com.zehhow.jikevideodownloader.recyclerView.TaskAdapter;
import com.zehhow.jikevideodownloader.dao.SQLiteHelper;
import com.zehhow.jikevideodownloader.dao.TaskBean;
import com.zehhow.jikevideodownloader.dialog.AddTaskDialog;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Vector;


public class MainActivity extends AppCompatActivity {

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
                if(noPermission()) return;
                addTask(null);
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
            if(url.contains("http"))
                url = url.substring(url.indexOf("http"));
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
        // 为recyclerView注册ContextMenu
        registerForContextMenu(recyclerView);
        // 设置适配器
        taskAdapter = new TaskAdapter(new Vector<TaskBean>(), this);
        recyclerView.setAdapter(taskAdapter);
        // 设置滑动侦听器，控制悬浮按钮
        recyclerView.addOnScrollListener(new RecyclerScrollListener((FloatingActionButton) findViewById(R.id.fab)));
        // 设置分割线
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        Vector<TaskBean> taskList = SQLiteHelper.getInstance().queryAllTasks();
        for(TaskBean task : taskList)
            taskAdapter.addTaskItem(task, false);
    }

    /**
     * 新建下载任务
     * @param url 下载地址
     */
    private void addTask(String url) {
        if(noPermission()) return;
        new AddTaskDialog(this).show(url);
    }

    /**
     * 权限检查
     * @return 是否拥有权限
     */
    private boolean noPermission() {
        // 安卓6以上版本进行权限检查
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, 1);
                return true;
            }
        }

        return false;
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

    /**
     * 设置弹出的菜单项
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.recycler_view_menu, menu);
        if(taskAdapter.getCurrentTask().status != TaskStatus.SUCCESS)   // 下载未完成的视频不可分享
            menu.findItem(R.id.share_video).setVisible(false);
    }

    /**
     * 上下文弹出菜单点击事件
     * @param item 菜单项
     * @return 是否停止响应流程
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TaskBean task = taskAdapter.getCurrentTask();
        switch (item.getItemId()) {
            case R.id.share_video:      // 分享
                shareVideo(task);
                break;
            case R.id.delete_task:      // 删除
                deleteTask(task);
                break;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * 标题工具栏的弹出菜单
     * @return 是否显示菜单
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    /**
     * 标题工具栏的弹出菜单的点击事件
     * @param item 菜单项
     * @return 是否停止响应流程
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tutorial:      // 使用教程
                openUrl(this.getString(R.string.tutorial_url));
                break;
            case R.id.feedback:     // 反馈
                openUrl(this.getString(R.string.feedback_url));
                break;
            case R.id.about:        // 关于，显示版本号、作者
                try {
                    String versionName = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
                    String str = "当前版本：" + versionName + "\n作者：樱花小王子";
                    Toast.makeText(this, str, Toast.LENGTH_LONG).show();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.donate:       // 捐赠，跳转到支付宝付款页面
                try {
                    Intent intent = Intent.parseUri(getString(R.string.donate_url), Intent.URI_INTENT_SCHEME);
                    startActivity(intent);
                } catch (URISyntaxException e) {
                    Toast.makeText(this, "手机上未安装支付宝", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /***
     * 打开指定网址
     * @param url 网址
     */
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    /**
     * 分享视频到其它软件
     * @param task 要分享的视频的TaskBean
     */
    private void shareVideo(TaskBean task) {
        Uri uri = DownloadUtil.getUriForFile(this, new File(task.path, task.name));
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, "分享视频"));
    }

    /**
     * 删除任务
     * @param task 任务信息
     */
    private void deleteTask(TaskBean task) {
        // 任务下载中则取消下载
        if(task.status == TaskStatus.DOWNLOADING)
            task.downloadTask.cancelDownload();

         // 删除任务文件
        File file = new File(task.path, task.name);
        if (!file.exists() || !file.delete())
            Log.d("JKVD", " Failed to delete | " + file.getPath());

        // 从任务列表界面和数据库中删除
        taskAdapter.deleteTaskItem(task);
        SQLiteHelper.getInstance().deleteTask(task.urlHashCode);
    }
}

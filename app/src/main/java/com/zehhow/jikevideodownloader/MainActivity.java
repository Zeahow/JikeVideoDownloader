package com.zehhow.jikevideodownloader;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import com.zehhow.jikevideodownloader.dialog.AddTaskDialog;


public class MainActivity extends AppCompatActivity {
    // 悬浮按钮的状态
    private enum FabState {
        ADD,
        DELETE
    }
    private FabState fabState = FabState.ADD;


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

        Intent intent = getIntent();
        // 判断是否由分享功能调用本Activity
        if(Intent.ACTION_SEND.equals(intent.getAction())) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            url = url.substring(url.indexOf("https://m.okjike"));
            addTask(url);
        }
    }

    // 增加任务
    private void addTask(String url) {
        new AddTaskDialog(this).show(url);
    }

    // 删除任务
    private void deleteTask() {

    }

}

package com.zehhow.jikevideodownloader.recyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zehhow.jikevideodownloader.JkvdApplication;
import com.zehhow.jikevideodownloader.R;
import com.zehhow.jikevideodownloader.dao.TaskBean;
import com.zehhow.jikevideodownloader.download.DownloadListener;
import com.zehhow.jikevideodownloader.download.DownloadTask;
import com.zehhow.jikevideodownloader.download.TaskStatus;

import java.io.File;
import java.util.Vector;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private Vector<TaskBean> taskList;          // 下载任务列表

    private int position;

    /**
     * 获取当前position所对应的TaskBean
     * @return 当前position对应的TaskBean
     */
    public TaskBean getCurrentTask() {
        return taskList.get(position);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener{
        View view;
        TextView txtName;
        ProgressBar progressBar;
        Button btnStartOrPause;

        TaskViewHolder(View view) {
            super(view);
            this.view = view;
            txtName = view.findViewById(R.id.name);
            progressBar = view.findViewById(R.id.progressBar);
            btnStartOrPause = view.findViewById(R.id.startOrPause);

            view.setOnCreateContextMenuListener(this);

            /* 点击已完成的任务项自动打开播放器 */
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TaskBean task = getTaskByViewHolder(TaskViewHolder.this);
                    if(task.status == TaskStatus.SUCCESS) {
                        Context context = JkvdApplication.getContext();

                        Intent openVideoPlayer = new Intent(Intent.ACTION_VIEW);
                        openVideoPlayer.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Uri uri = getUriForFile(new File(task.path, task.name));
                        openVideoPlayer.setDataAndType(uri, "video/*");

                        try {
                            context.startActivity(openVideoPlayer);
                        } catch (Exception e) {
                            Toast.makeText(context, "没有默认播放器", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                }
            });

            /* 设置按钮点击事件 */
            btnStartOrPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TaskBean task = getTaskByViewHolder(TaskViewHolder.this);
                    switch (task.status) {
                        case PAUSED:            // 任务暂停，此时应开始下载
                            startDownload(task);
                            btnStartOrPause.setText("暂停");
                            break;
                        case DOWNLOADING:       // 任务下载中，此时应暂停下载
                            task.downloadTask.pausedDownload();
                            btnStartOrPause.setText("开始");
                            break;
                    }
                }
            });
        }

        /**
         * 设置弹出的菜单项
         */
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(ContextMenu.NONE, 0, ContextMenu.NONE, "删除");
            menu.add(ContextMenu.NONE, 1, ContextMenu.NONE, "分享");
        }
    }

    /**
     * 构造函数
     * @param taskList 下载任务列表
     */
    public TaskAdapter(Vector<TaskBean> taskList) {
        this.taskList = taskList;
    }

    /**
     * 添加新的下载任务项至任务列表
     * @param task 任务相关信息
     * @param startNow 是否立即开始任务
     */
    public void addTaskItem(TaskBean task, boolean startNow) {
        taskList.add(0 ,task);
        notifyItemInserted(0);
        DownloadListener downloadListener = new DownloadListener(task);
        task.setDownloadListener(downloadListener);

        if(startNow) {
            startDownload(task);
        }
    }

    /**
     * 开始下载
     * @param task 下载任务信息
     */
    private void startDownload(TaskBean task) {
        task.status = TaskStatus.DOWNLOADING;
        DownloadTask downloadTask = new DownloadTask(task.downloadListener);
        task.setDownloadTask(downloadTask);
        downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, task);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.task_item_layout, viewGroup, false);

        return new TaskViewHolder(view);
    }

    /**
     * 根据传入的ViewHolder获取对应的TaskBean
     * @param viewHolder 传入的ViewHolder
     * @return 对应的TaskBean
     */
    private TaskBean getTaskByViewHolder(TaskViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        return taskList.get(position);
    }

    @Override
    public void onBindViewHolder(@NonNull final TaskViewHolder viewHolder, int i) {
        TaskBean task = taskList.get(i);
        viewHolder.txtName.setText(task.name);
        viewHolder.progressBar.setProgress(task.progress);
        task.downloadListener.setProgressBar(viewHolder.progressBar);
        task.downloadListener.setButton(viewHolder.btnStartOrPause);

        // 根据任务状态设置按钮文本
        String displayText;
        switch(task.status) {
            case DOWNLOADING:   // 任务下载中
                displayText = "暂停";
                break;
            case FAILED:        // 任务失败
                displayText = "下载失败";
                break;
            case SUCCESS:       // 任务下载成功
                displayText = "已完成";
                break;
            case PAUSED:        // 任务已暂停
                displayText = "开始";
                break;
            default:
                displayText = "任务异常";
        }

        viewHolder.btnStartOrPause.setText(displayText);

        // 设置长按事件
        viewHolder.view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                position = viewHolder.getAdapterPosition();
                return false;
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull TaskViewHolder viewHolder) {
        // 取消长按事件
        viewHolder.view.setOnLongClickListener(null);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    /**
     * 获取文件路径所对应的Uri
     * @param file 文件路径
     * @return 对应的Uri
     */
    public static Uri getUriForFile(File file) {
        // 安卓7以上版本需调用内容提供器
        Uri uri;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Context context = JkvdApplication.getContext();
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        }
        else
            uri = Uri.fromFile(file);

        return uri;
    }
}

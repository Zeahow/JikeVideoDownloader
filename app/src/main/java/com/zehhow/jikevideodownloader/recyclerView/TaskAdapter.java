package com.zehhow.jikevideodownloader.recyclerView;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zehhow.jikevideodownloader.R;
import com.zehhow.jikevideodownloader.dao.TaskBean;
import com.zehhow.jikevideodownloader.download.DownloadListener;
import com.zehhow.jikevideodownloader.download.DownloadTask;
import com.zehhow.jikevideodownloader.download.TaskStatus;

import java.util.Vector;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private Vector<TaskBean> taskList;          // 下载任务列表

    class TaskViewHolder extends RecyclerView.ViewHolder {
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
     * 添加新的下载任务项至
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
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.task_item_layout, viewGroup, false);
        final TaskViewHolder taskViewHolder = new TaskViewHolder(view);

        /* 设置按钮点击事件 */
        taskViewHolder.btnStartOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = taskViewHolder.getAdapterPosition();
                TaskBean task = taskList.get(position);
                switch (task.status) {
                    case PAUSED:            // 任务暂停，此时应开始下载
                        startDownload(task);
                        //task.status = TaskStatus.DOWNLOADING;
                        taskViewHolder.btnStartOrPause.setText("暂停");
                        break;
                    case DOWNLOADING:       // 任务下载中，此时应暂停下载
                        task.downloadTask.pausedDownload();
                        //task.status = TaskStatus.PAUSED;
                        taskViewHolder.btnStartOrPause.setText("开始");
                        break;
                }
            }
        });

        return taskViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder viewHolder, int i) {
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
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }
}

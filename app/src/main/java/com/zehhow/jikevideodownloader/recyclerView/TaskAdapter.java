package com.zehhow.jikevideodownloader.recyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.zehhow.jikevideodownloader.R;
import com.zehhow.jikevideodownloader.dao.SQLiteHelper;
import com.zehhow.jikevideodownloader.dao.TaskBean;
import com.zehhow.jikevideodownloader.download.DownloadListener;
import com.zehhow.jikevideodownloader.download.DownloadTask;
import com.zehhow.jikevideodownloader.download.DownloadUtil;
import com.zehhow.jikevideodownloader.download.TaskStatus;

import java.io.File;
import java.util.Vector;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private Vector<TaskBean> taskList;          // 下载任务列表
    /**
     * viewType--分别为item以及空view
     */
    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_EMPTY = 1;

    private int position;

    /**
     * 获取当前position所对应的TaskBean
     * @return 当前position对应的TaskBean
     */
    public TaskBean getCurrentTask() {
        return taskList.get(position);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder{
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

            /* 点击已完成的任务项自动打开播放器 */
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TaskBean task = getTaskByViewHolder(TaskViewHolder.this);
                    if(task.status != TaskStatus.SUCCESS) return;
                    // 调用系统的视频播放器播放选中的视频
                    Intent openVideoPlayer = new Intent(Intent.ACTION_VIEW);
                    openVideoPlayer.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    openVideoPlayer.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Uri uri = DownloadUtil.getUriForFile(context, new File(task.path, task.name));
                    openVideoPlayer.setDataAndType(uri, "video/*");

                    try {
                        context.startActivity(openVideoPlayer);
                    } catch (Exception e) {
                        Toast.makeText(context, "没有默认播放器", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
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
    }

    /**
     * 构造函数
     * @param taskList 下载任务列表
     */
    public TaskAdapter(Vector<TaskBean> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
    }

    /**
     * 添加新的下载任务项至任务列表
     * @param task 任务相关信息
     * @param startNow 是否立即开始任务
     */
    public void addTaskItem(TaskBean task, boolean startNow) {
        // 如果任务已存在，则不添加任务
        if(existTask(task.urlHashCode)) {
            Toast.makeText(context, "任务已存在", Toast.LENGTH_SHORT).show();
            return;
        }

        /*
         * 当taskList的大小在插入新任务前为0时，RecyclerView显示的时空视图
         * 故而此时要先删除这个空视图，不然会触发闪退
         */
        if(taskList.size() == 0)
            notifyItemRemoved(0);

        // 添加至界面
        taskList.add(0 ,task);
        notifyItemInserted(0);
        DownloadListener downloadListener = new DownloadListener(task);
        task.setDownloadListener(downloadListener);

        // 若数据库中不存在该任务，则添加至数据库
        if(!SQLiteHelper.getInstance().existTask(task.urlHashCode))
            SQLiteHelper.getInstance().addTask(task);

        // 立即开始下载
        if(startNow) {
            startDownload(task);
        }
    }

    /**
     * 删除任务项
     * @param task 待删除的任务的信息
     */
    public void deleteTaskItem(TaskBean task) {
        int index = taskList.indexOf(task);
        if(index == -1) return;
        taskList.remove(index);
        notifyItemRemoved(index);
        notifyDataSetChanged();
    }

    /***
     * 判断任务列表中是否存在该任务
     * @param urlHashCode 任务下载链接的hashCode
     * @return 是否存在
     */
    private boolean existTask(int urlHashCode) {
        for(TaskBean task : taskList)
            if(task.urlHashCode == urlHashCode)
                return true;

        return false;
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

    /**
     * 根据传入的ViewHolder获取对应的TaskBean
     * @param viewHolder 传入的ViewHolder
     * @return 对应的TaskBean
     */
    private TaskBean getTaskByViewHolder(TaskViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        return taskList.get(position);
    }

    /***
     * 获取ItemView的视图类型
     * @return 视图类型
     */
    @Override
    public int getItemViewType(int position) {
        if(taskList.size() == 0) return VIEW_TYPE_EMPTY;
        else return VIEW_TYPE_ITEM;
    }

    /**
     * 根据不同的ViewType返回不同的ViewHolder
     * @param viewType 视图类型
     * @return 相应的ViewHolder
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_EMPTY) {   // 空视图
            View view = LayoutInflater.from(context).inflate(R.layout.task_item_empty_layout, parent, false);
            // 设置单击事件
            view.findViewById(R.id.click_me_to_show_tutorial).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(context.getString(R.string.tutorial_url)));
                    context.startActivity(intent);
                }
            });
            return new RecyclerView.ViewHolder(view){};
        }

        View view = LayoutInflater.from(context) .inflate(R.layout.task_item_layout, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int i) {
        if(!(holder instanceof TaskViewHolder)) return;     // 非任务项视图不进行处理

        final TaskViewHolder viewHolder = (TaskViewHolder)holder;
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
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if(!(holder instanceof TaskViewHolder)) return;     // 非任务项视图不进行处理

        TaskViewHolder viewHolder = (TaskViewHolder)holder;
        viewHolder.view.setOnLongClickListener(null);       // 取消长按事件
    }

    @Override
    public int getItemCount() {
        // 如果taskList.size()为0的话，只引入一个布局，就是emptyView
        // 那么，这个recyclerView的itemCount为1
        if (taskList.size() == 0) {
            return 1;
        }
        return taskList.size();
    }
}

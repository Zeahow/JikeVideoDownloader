package com.zehhow.jikevideodownloader.adapter;

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

import java.util.Vector;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private Vector<TaskBean> taskList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        ProgressBar progressBar;
        Button btnStartOrPause;

        public ViewHolder(View view) {
            super(view);
            txtName = view.findViewById(R.id.name);
            progressBar = view.findViewById(R.id.progressBar);
            btnStartOrPause = view.findViewById(R.id.startOrPause);

        }
    }

    public TaskAdapter(Vector<TaskBean> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.task_item_layout, viewGroup, false);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        TaskBean task = taskList.get(i);
        viewHolder.txtName.setText(task.name);
        int progress = 0;
        if(task.totalLength != 0)
            progress = (int)(task.downloadedLength * 100 / task.totalLength);
        viewHolder.progressBar.setProgress(50);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }
}

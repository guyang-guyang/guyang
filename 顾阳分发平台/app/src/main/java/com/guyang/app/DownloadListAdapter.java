package com.guyang.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class DownloadListAdapter extends BaseAdapter {

    private Context context;
    private List<InAppDownloadManager.DownloadTask> tasks;
    private InAppDownloadManager manager;
    private LayoutInflater inflater;

    public DownloadListAdapter(Context context, List<InAppDownloadManager.DownloadTask> tasks,
                               InAppDownloadManager manager) {
        this.context = context;
        this.tasks = tasks;
        this.manager = manager;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public Object getItem(int position) {
        return tasks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_download, parent, false);
            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tv_dl_name);
            holder.tvStatus = convertView.findViewById(R.id.tv_dl_status);
            holder.progressBar = convertView.findViewById(R.id.progress_dl);
            holder.tvPercent = convertView.findViewById(R.id.tv_dl_percent);
            holder.btnAction = convertView.findViewById(R.id.btn_dl_action);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        InAppDownloadManager.DownloadTask task = tasks.get(position);

        holder.tvName.setText(task.appName);
        holder.progressBar.setProgress(task.percent);
        holder.tvPercent.setText(String.format(Locale.getDefault(), "%d%%", task.percent));

        switch (task.status) {
            case 0:
                holder.tvStatus.setText("等待中");
                holder.btnAction.setText("等待");
                holder.btnAction.setEnabled(false);
                break;
            case 1:
                holder.tvStatus.setText("下载中");
                holder.btnAction.setText("暂停");
                holder.btnAction.setEnabled(true);
                break;
            case 2:
                holder.tvStatus.setText("已暂停");
                holder.btnAction.setText("继续");
                holder.btnAction.setEnabled(true);
                break;
            case 3:
                holder.tvStatus.setText("已完成");
                holder.btnAction.setText("安装");
                holder.btnAction.setEnabled(true);
                break;
            case 4:
                holder.tvStatus.setText("失败");
                holder.btnAction.setText("重试");
                holder.btnAction.setEnabled(true);
                break;
        }

        holder.btnAction.setOnClickListener(v -> {
            switch (task.status) {
                case 1:
                    manager.pauseDownload(task.taskId);
                    break;
                case 2:
                    manager.resumeDownload(task.taskId);
                    break;
                case 3:
                    manager.installApk(task.taskId);
                    break;
                case 4:
                    manager.cancelDownload(task.taskId);
                    manager.startDownload(task.url, task.fileName, task.appName, task.appId);
                    break;
            }
            notifyDataSetChanged();
        });

        return convertView;
    }

    static class ViewHolder {
        TextView tvName;
        TextView tvStatus;
        ProgressBar progressBar;
        TextView tvPercent;
        Button btnAction;
    }
}
package com.guyang.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;

public class AppBaseAdapter extends BaseAdapter {
    private Activity ctx;
    private List<JSONObject> list;

    public AppBaseAdapter(Activity c, List<JSONObject> l) {
        ctx = c;
        list = l;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = ctx.getLayoutInflater().inflate(R.layout.item_app, parent, false);
            holder = new ViewHolder();
            holder.ivIcon = convertView.findViewById(R.id.app_icon2);
            holder.tvName = convertView.findViewById(R.id.app_name);
            holder.tvInfo = convertView.findViewById(R.id.app_info);
            holder.btnAction = convertView.findViewById(R.id.btn_action);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        JSONObject app = list.get(position);
        String appName = app.optString("name", "未知应用");
        String category = app.optString("category", "未知分类");
        String appId = app.optString("id", "");
        String downloadUrl = app.optString("apk_url", "");
        String fileName = appName + ".apk";

        holder.tvName.setText(appName);
        holder.tvInfo.setText(category);
        holder.btnAction.setText("下载");

        holder.btnAction.setOnClickListener(v -> {
            handleDownload(appName, appId, fileName, downloadUrl);
        });

        convertView.setOnClickListener(v -> {
            // 点击整体进入详情页
            Intent intent = new Intent(ctx, AppDetailActivity.class);
            intent.putExtra("appName", appName);
            intent.putExtra("appDescription", app.optString("description", ""));
            intent.putExtra("appId", appId);
            intent.putExtra("downloadUrl", downloadUrl);
            intent.putExtra("fileName", fileName);
            ctx.startActivity(intent);
        });

        return convertView;
    }

    // 2.0: 下载按钮的完整链路
    private void handleDownload(String appName, String appId, String fileName, String downloadUrl) {
        SharedPreferences sp = ctx.getSharedPreferences("app_prefs", ctx.MODE_PRIVATE);
        String token = sp.getString("token", "");

        if (token.isEmpty()) {
            // 游客 → 跳转登录
            Intent loginIntent = new Intent(ctx, LoginActivity.class);
            loginIntent.putExtra("require_login", true);
            loginIntent.putExtra("pending_action", "download");
            loginIntent.putExtra("app_name", appName);
            loginIntent.putExtra("app_id", appId);
            loginIntent.putExtra("file_name", fileName);
            loginIntent.putExtra("download_url", downloadUrl);
            ctx.startActivity(loginIntent);
            Toast.makeText(ctx, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasShared = sp.getBoolean("has_shared", false);

        if (!hasShared) {
            // 已登录但未分享 → 强制分享弹窗
            new ShareGateDialog(ctx, appName, () -> {
                // 分享完成回调 → 进入下载
                goToDownload(appName, appId, fileName, downloadUrl);
            }).show();
        } else {
            // 已登录+已分享 → 直接下载
            goToDownload(appName, appId, fileName, downloadUrl);
        }
    }

    private void goToDownload(String appName, String appId, String fileName, String downloadUrl) {
        InAppDownloadActivity.start(ctx, appName, appId, fileName, downloadUrl);
    }

    static class ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvInfo;
        Button btnAction;
    }
}
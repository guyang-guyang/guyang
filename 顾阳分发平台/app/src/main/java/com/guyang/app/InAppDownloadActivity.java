package com.guyang.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Locale;

/**
 * App 内专属下载页面 - 替代系统下载器
 */
public class InAppDownloadActivity extends Activity implements InAppDownloadManager.DownloadListener {

    private static final String EXTRA_TASK_ID = "task_id";
    private static final String EXTRA_APP_NAME = "app_name";
    private static final String EXTRA_APP_ID = "app_id";
    private static final String EXTRA_FILE_NAME = "file_name";
    private static final String EXTRA_DOWNLOAD_URL = "download_url";

    private InAppDownloadManager downloadManager;
    private String taskId;
    private String appName;
    private String appId;
    private String fileName;
    private String downloadUrl;

    private ImageView ivIcon;
    private TextView tvAppName;
    private TextView tvStatus;
    private TextView tvProgress;
    private ProgressBar progressBar;
    private Button btnAction;
    private Button btnCancel;
    private Button btnInstall;

    private Handler mainHandler;

    public static void start(Activity context, String appName, String appId, String fileName, String downloadUrl) {
        Intent intent = new Intent(context, InAppDownloadActivity.class);
        intent.putExtra(EXTRA_APP_NAME, appName);
        intent.putExtra(EXTRA_APP_ID, appId);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        intent.putExtra(EXTRA_DOWNLOAD_URL, downloadUrl);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inapp_download);

        // 沉浸式状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primary));
        }

        initViews();
        parseIntent();
        initDownloadManager();
        startDownload();
    }

    private void initViews() {
        ivIcon = findViewById(R.id.iv_download_icon);
        tvAppName = findViewById(R.id.tv_download_app_name);
        tvStatus = findViewById(R.id.tv_download_status);
        tvProgress = findViewById(R.id.tv_download_progress);
        progressBar = findViewById(R.id.progress_download);
        btnAction = findViewById(R.id.btn_download_action);
        btnCancel = findViewById(R.id.btn_download_cancel);
        btnInstall = findViewById(R.id.btn_download_install);

        mainHandler = new Handler(Looper.getMainLooper());

        btnAction.setOnClickListener(v -> handleAction());
        btnCancel.setOnClickListener(v -> handleCancel());
        btnInstall.setOnClickListener(v -> handleInstall());
    }

    private void parseIntent() {
        appName = getIntent().getStringExtra(EXTRA_APP_NAME);
        appId = getIntent().getStringExtra(EXTRA_APP_ID);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        downloadUrl = getIntent().getStringExtra(EXTRA_DOWNLOAD_URL);

        if (appName == null) appName = "未知应用";
        if (fileName == null) fileName = appName + ".apk";

        tvAppName.setText(appName);
    }

    private void initDownloadManager() {
        downloadManager = InAppDownloadManager.getInstance(this);
    }

    private void startDownload() {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            Toast.makeText(this, "下载链接无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        taskId = downloadManager.startDownload(downloadUrl, fileName, appName, appId);
        downloadManager.setListener(taskId, this);

        updateUI("准备下载...", 0, false, false, false);
    }

    private void handleAction() {
        InAppDownloadManager.DownloadTask task = downloadManager.getTask(taskId);
        if (task == null) return;

        switch (task.status) {
            case 1: // downloading
                downloadManager.pauseDownload(taskId);
                break;
            case 2: // paused
                downloadManager.resumeDownload(taskId);
                break;
            case 3: // completed
                downloadManager.installApk(taskId);
                break;
            case 4: // error
                startDownload();
                break;
        }
    }

    private void handleCancel() {
        downloadManager.cancelDownload(taskId);
        finish();
    }

    private void handleInstall() {
        downloadManager.installApk(taskId);
    }

    private void updateUI(String status, int percent, boolean showAction, boolean showCancel, boolean showInstall) {
        mainHandler.post(() -> {
            tvStatus.setText(status);
            progressBar.setProgress(percent);
            tvProgress.setText(String.format(Locale.getDefault(), "%d%%", percent));

            btnAction.setVisibility(showAction ? View.VISIBLE : View.GONE);
            btnCancel.setVisibility(showCancel ? View.VISIBLE : View.GONE);
            btnInstall.setVisibility(showInstall ? View.VISIBLE : View.GONE);

            // 更新按钮文本
            InAppDownloadManager.DownloadTask task = downloadManager.getTask(taskId);
            if (task != null) {
                switch (task.status) {
                    case 1:
                        btnAction.setText("暂停");
                        break;
                    case 2:
                        btnAction.setText("继续");
                        break;
                    case 3:
                        btnAction.setText("安装");
                        break;
                    case 4:
                        btnAction.setText("重试");
                        break;
                }
            }
        });
    }

    @Override
    public void onProgress(String taskId, long downloaded, long total, int percent) {
        String status;
        if (total > 0) {
            String downloadedStr = formatSize(downloaded);
            String totalStr = formatSize(total);
            status = String.format(Locale.getDefault(), "下载中: %s / %s", downloadedStr, totalStr);
        } else {
            status = String.format(Locale.getDefault(), "下载中: %s", formatSize(downloaded));
        }

        updateUI(status, percent, true, true, false);
    }

    @Override
    public void onComplete(String taskId, String localPath) {
        updateUI("下载完成", 100, true, false, true);
        Toast.makeText(this, appName + " 下载完成", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String taskId, String error) {
        updateUI("下载失败: " + error, 0, true, true, false);
        Toast.makeText(this, "下载失败: " + error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPaused(String taskId) {
        updateUI("已暂停", progressBar.getProgress(), true, true, false);
        Toast.makeText(this, "下载已暂停", Toast.LENGTH_SHORT).show();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1fKB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1fMB", bytes / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1fGB", bytes / (1024.0 * 1024 * 1024));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadManager != null && taskId != null) {
            downloadManager.removeListener(taskId);
        }
    }
}
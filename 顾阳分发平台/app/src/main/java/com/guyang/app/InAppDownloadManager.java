package com.guyang.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;
import androidx.core.content.FileProvider;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * App内置下载管理器 - 替代系统DownloadManager
 * 支持：暂停/恢复/进度回调/断点续传
 */
public class InAppDownloadManager {

    private static final String TAG = "InAppDM";
    private static final int BUFFER_SIZE = 8192;
    private static final String DOWNLOAD_DIR = "GuyangDownloads";

    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Map<String, DownloadTask> activeTasks;
    private final Map<String, DownloadListener> listeners;

    private static volatile InAppDownloadManager instance;

    public interface DownloadListener {
        void onProgress(String taskId, long downloaded, long total, int percent);
        void onComplete(String taskId, String localPath);
        void onError(String taskId, String error);
        void onPaused(String taskId);
    }

    public static class DownloadTask {
        public String taskId;
        public String url;
        public String fileName;
        public String appName;
        public String appId;
        public String localPath;
        public long totalSize;
        public long downloadedSize;
        public int percent;
        public int status; // 0=waiting, 1=downloading, 2=paused, 3=completed, 4=error
        public volatile boolean cancelled;
        public volatile boolean paused;
        public HttpURLConnection connection;
        public FileOutputStream outputStream;
        public InputStream inputStream;

        public DownloadTask(String taskId, String url, String fileName, String appName, String appId) {
            this.taskId = taskId;
            this.url = url;
            this.fileName = fileName;
            this.appName = appName;
            this.appId = appId;
            this.status = 0;
        }
    }

    private InAppDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.activeTasks = new ConcurrentHashMap<>();
        this.listeners = new ConcurrentHashMap<>();
    }

    public static InAppDownloadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (InAppDownloadManager.class) {
                if (instance == null) {
                    instance = new InAppDownloadManager(context);
                }
            }
        }
        return instance;
    }

    public void setListener(String taskId, DownloadListener listener) {
        listeners.put(taskId, listener);
    }

    public void removeListener(String taskId) {
        listeners.remove(taskId);
    }

    public DownloadTask getTask(String taskId) {
        return activeTasks.get(taskId);
    }

    private File getDownloadDir() {
        File dir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_DIR);
        } else {
            dir = new File(Environment.getExternalStorageDirectory(), "Download/" + DOWNLOAD_DIR);
        }
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public String startDownload(String url, String fileName, String appName, String appId) {
        String taskId = appId + "_" + System.currentTimeMillis();
        DownloadTask task = new DownloadTask(taskId, url, fileName, appName, appId);
        task.localPath = new File(getDownloadDir(), fileName).getAbsolutePath();
        activeTasks.put(taskId, task);

        executor.execute(() -> executeDownload(task));

        return taskId;
    }

    private void executeDownload(DownloadTask task) {
        if (task.status == 1) return;
        task.status = 1;
        task.cancelled = false;
        task.paused = false;

        try {
            URL downloadUrl = new URL(task.url);
            task.connection = (HttpURLConnection) downloadUrl.openConnection();
            task.connection.setConnectTimeout(30000);
            task.connection.setReadTimeout(30000);
            task.connection.setRequestMethod("GET");
            task.connection.setRequestProperty("User-Agent", "GuyangApp/1.0");

            // 断点续传：检查已下载部分
            File partialFile = new File(task.localPath + ".part");
            long startByte = 0;
            if (partialFile.exists()) {
                startByte = partialFile.length();
                if (startByte > 0) {
                    task.connection.setRequestProperty("Range", "bytes=" + startByte + "-");
                }
            }

            task.connection.connect();

            int responseCode = task.connection.getResponseCode();
            long contentLength = task.connection.getContentLengthLong();

            // 处理断点续传内容大小
            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                task.totalSize = startByte + contentLength;
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                task.totalSize = contentLength;
                if (partialFile.exists()) partialFile.delete();
            } else {
                notifyError(task.taskId, "服务器返回错误: HTTP " + responseCode);
                return;
            }

            task.downloadedSize = startByte;

            // 打开流
            task.inputStream = task.connection.getInputStream();
            task.outputStream = new FileOutputStream(partialFile, startByte > 0);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long lastNotifyTime = 0;
            long lastNotifyBytes = 0;

            while ((bytesRead = task.inputStream.read(buffer)) != -1) {
                if (task.cancelled) {
                    cleanup(task);
                    return;
                }

                if (task.paused) {
                    notifyPaused(task.taskId);
                    cleanup(task);
                    task.status = 2;
                    return;
                }

                task.outputStream.write(buffer, 0, bytesRead);
                task.downloadedSize += bytesRead;

                // 节流通知：每500ms或每1MB通知一次
                long now = System.currentTimeMillis();
                if (task.totalSize > 0) {
                    task.percent = (int) (task.downloadedSize * 100 / task.totalSize);
                }
                if (now - lastNotifyTime > 500 || task.downloadedSize - lastNotifyBytes > 1048576) {
                    notifyProgress(task);
                    lastNotifyTime = now;
                    lastNotifyBytes = task.downloadedSize;
                }
            }

            // 下载完成，重命名.part文件
            task.outputStream.close();
            task.inputStream.close();

            File finalFile = new File(task.localPath);
            if (finalFile.exists()) finalFile.delete();
            partialFile.renameTo(finalFile);

            task.status = 3;
            task.percent = 100;
            notifyComplete(task.taskId, task.localPath);

        } catch (Exception e) {
            Log.e(TAG, "Download error: " + e.getMessage());
            task.status = 4;
            notifyError(task.taskId, e.getMessage() != null ? e.getMessage() : "下载失败");
        }
    }

    public void pauseDownload(String taskId) {
        DownloadTask task = activeTasks.get(taskId);
        if (task != null) {
            task.paused = true;
        }
    }

    public void resumeDownload(String taskId) {
        DownloadTask task = activeTasks.get(taskId);
        if (task != null && task.status == 2) {
            task.paused = false;
            task.status = 0;
            executor.execute(() -> executeDownload(task));
        }
    }

    public void cancelDownload(String taskId) {
        DownloadTask task = activeTasks.get(taskId);
        if (task != null) {
            task.cancelled = true;
            activeTasks.remove(taskId);
            listeners.remove(taskId);
        }
    }

    public void installApk(String taskId) {
        DownloadTask task = activeTasks.get(taskId);
        if (task == null || task.status != 3) return;

        File apkFile = new File(task.localPath);
        if (!apkFile.exists()) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 使用 FileProvider
                Uri apkUri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider", apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Install failed: " + e.getMessage());
        }
    }

    private void cleanup(DownloadTask task) {
        try {
            if (task.outputStream != null) task.outputStream.close();
            if (task.inputStream != null) task.inputStream.close();
            if (task.connection != null) task.connection.disconnect();
        } catch (Exception ignored) {}
    }

    private void notifyProgress(DownloadTask task) {
        DownloadListener listener = listeners.get(task.taskId);
        if (listener != null) {
            mainHandler.post(() -> listener.onProgress(
                    task.taskId, task.downloadedSize, task.totalSize, task.percent));
        }
    }

    private void notifyComplete(String taskId, String localPath) {
        DownloadListener listener = listeners.get(taskId);
        if (listener != null) {
            mainHandler.post(() -> listener.onComplete(taskId, localPath));
        }
    }

    private void notifyError(String taskId, String error) {
        DownloadListener listener = listeners.get(taskId);
        if (listener != null) {
            mainHandler.post(() -> listener.onError(taskId, error));
        }
    }

    private void notifyPaused(String taskId) {
        DownloadListener listener = listeners.get(taskId);
        if (listener != null) {
            mainHandler.post(() -> listener.onPaused(taskId));
        }
    }
}
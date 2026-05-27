package com.guyang.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private boolean done;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        done = false;
        new Handler().postDelayed(this::checkUpdate, 1500);
    }

    private void checkUpdate() {
        if (done) return;
        new Thread(() -> {
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                String ver = pi.versionName;
                int vc = pi.versionCode;
                String s = ApiClient.get(ApiClient.A + "/app/update.php?version=" + ver + "&version_code=" + vc);
                if (s == null || s.startsWith("ERROR:")) { goHome(); return; }
                JSONObject d = new JSONObject(s);
                if (d.optInt("code", -1) != 0) { goHome(); return; }
                JSONObject data = d.optJSONObject("data");
                if (data == null) { goHome(); return; }
                boolean hasUpdate = data.optBoolean("has_update", false);
                int forceRaw = data.optInt("force_update", 0);
                boolean force = data.optBoolean("force_update", false) || (forceRaw == 1);
                String url = data.optString("apk_url", "");
                String log = data.optString("update_log", "");
                String announcement = data.optString("announcement", "");

                if (!announcement.isEmpty()) {
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(this)
                            .setTitle("公告")
                            .setMessage(announcement)
                            .setPositiveButton("知道了", (d2, w) -> {})
                            .show();
                    });
                }

                final boolean hasUpd = hasUpdate;
                final boolean isForce = force;
                final String apkUrl = url;
                runOnUiThread(() -> {
                    if (hasUpd) {
                        AlertDialog.Builder b = new AlertDialog.Builder(this)
                            .setTitle("发现新版本")
                            .setMessage(log.isEmpty() ? "有新版本可用" : log);
                        if (isForce) {
                            b.setCancelable(false);
                            b.setPositiveButton("立即更新", (d2, w) -> startUpdateDownload(apkUrl));
                        } else {
                            b.setPositiveButton("立即更新", (d2, w) -> startUpdateDownload(apkUrl));
                            b.setNegativeButton("稍后再说", (d2, w) -> goHome());
                        }
                        b.show();
                    } else {
                        goHome();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(this::goHome);
            }
        }).start();
    }

    private void startUpdateDownload(String url) {
        InAppDownloadActivity.start(this,
                "顾阳分发平台", "guyang_app_update",
                "guyang_update.apk", url);
        Toast.makeText(this, "进入下载页面", Toast.LENGTH_SHORT).show();
        goHome();
    }

    private void goHome() {
        done = true;
        // 2.0: 无论是否登录，都进入首页（游客模式）
        SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String token = sp.getString("token", "");
        Intent intent = new Intent(this, HomeActivity.class);
        if (!token.isEmpty()) {
            ApiClient.setToken(token);
        }
        startActivity(intent);
        finish();
    }
}
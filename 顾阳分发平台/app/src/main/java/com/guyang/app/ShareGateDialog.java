package com.guyang.app;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 强制分享关卡弹窗 - A+B混合模式
 * 点击分享按钮 → 3秒倒计时 → "我已完成分享"按钮点亮
 * 用户必须点击分享按钮后才能继续
 */
public class ShareGateDialog extends Dialog {

    private TextView tvTitle, tvCountdown, tvShareContent;
    private Button btnShareQQ, btnShareQzone, btnCopy;
    private Button btnDone;
    private CountDownTimer timer;
    private boolean hasSharedThis = false;
    private Context ctx;
    private Runnable onComplete;
    private String appName;
    private String shareTitle;
    private String shareText;
    private String shareLink;

    public ShareGateDialog(Context context, String appName, Runnable onComplete) {
        super(context);
        this.ctx = context;
        this.appName = appName;
        this.onComplete = onComplete;
        init();
    }

    private void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(createView());
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            // 可点击外部？不！强制不可取消
        }

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        // 加载分享配置
        loadShareConfig();
    }

    private void loadShareConfig() {
        new Thread(() -> {
            try {
                String resp = ApiClient.get(ApiClient.A + "/app/share_config.php");
                if (resp != null && !resp.startsWith("ERROR:")) {
                    org.json.JSONObject j = new org.json.JSONObject(resp);
                    if (j.optInt("code", -1) == 0) {
                        org.json.JSONObject data = j.optJSONObject("data");
                        if (data != null) {
                            shareTitle = data.optString("share_title", "发现一款超好用的软件盒");
                            shareText = data.optString("share_text",
                                    "顾阳软件盒-海量应用免费下载，" + appName + "快来下载！");
                            shareLink = data.optString("share_link", "http://47.108.209.71");
                        }
                    }
                }
            } catch (Exception ignored) {}

            // 默认值
            if (shareTitle == null) shareTitle = "发现一款超好用的软件盒";
            if (shareText == null) shareText = "顾阳软件盒-海量应用免费下载，" + appName + "快来下载！";
            if (shareLink == null) shareLink = "http://47.108.209.71";

            // 更新UI
            tvShareContent.post(() -> {
                tvShareContent.setText("📱 " + appName + "\n" + shareText);
            });
        }).start();
    }

    private View createView() {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_card_white);
        root.setPadding(48, 36, 48, 36);

        // 标题
        tvTitle = new TextView(ctx);
        tvTitle.setText("\uD83C\uDF81 分享解锁下载");
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(0xFF1A1C18);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 8);
        root.addView(tvTitle);

        // 分享内容预览
        tvShareContent = new TextView(ctx);
        tvShareContent.setText("分享后即可开始下载");
        tvShareContent.setTextSize(14);
        tvShareContent.setTextColor(0xFF6B7266);
        tvShareContent.setGravity(Gravity.CENTER);
        tvShareContent.setPadding(0, 8, 0, 24);
        tvShareContent.setLineSpacing(4, 1.2f);
        root.addView(tvShareContent);

        // 分享按钮 - QQ好友
        btnShareQQ = createShareButton("\uD83D\uDCE4 分享到QQ好友", 0xFF12B7F5);
        btnShareQQ.setOnClickListener(v -> {
            hasSharedThis = true;
            String content = shareTitle + "\n" + shareText + "\n" + shareLink;
            // 尝试打开QQ分享
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, content);
                intent.putExtra(Intent.EXTRA_SUBJECT, shareTitle);
                intent.setPackage("com.tencent.mobileqq");
                ctx.startActivity(intent);
            } catch (Exception e) {
                // QQ没安装，复制到剪贴板
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("share", content));
                Toast.makeText(ctx, "分享文案已复制，请打开QQ粘贴发送", Toast.LENGTH_LONG).show();
            }
            startCountdown();
        });
        root.addView(btnShareQQ);

        // 分享按钮 - QQ空间
        btnShareQzone = createShareButton("\uD83C\uDF10 分享到QQ空间", 0xFFF5A623);
        btnShareQzone.setOnClickListener(v -> {
            hasSharedThis = true;
            String content = shareTitle + "\n" + shareText + "\n" + shareLink;
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, content);
                intent.setPackage("com.qzone");
                ctx.startActivity(intent);
            } catch (Exception e) {
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("share", content));
                Toast.makeText(ctx, "分享文案已复制，请打开QQ空间粘贴发布", Toast.LENGTH_LONG).show();
            }
            startCountdown();
        });
        root.addView(btnShareQzone);

        // 复制按钮
        btnCopy = createShareButton("\uD83D\uDCCB 复制分享文案", 0xFF54634D);
        btnCopy.setOnClickListener(v -> {
            hasSharedThis = true;
            String content = shareTitle + "\n" + shareText + "\n" + shareLink;
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("share", content));
            Toast.makeText(ctx, "已复制，请发送给好友或群聊", Toast.LENGTH_LONG).show();
            startCountdown();
        });
        root.addView(btnCopy);

        // 倒计时区域
        tvCountdown = new TextView(ctx);
        tvCountdown.setText("分享后按钮将自动解锁");
        tvCountdown.setTextSize(12);
        tvCountdown.setTextColor(0xFF416835);
        tvCountdown.setGravity(Gravity.CENTER);
        tvCountdown.setPadding(0, 16, 0, 8);
        root.addView(tvCountdown);

        // 确认按钮（初始禁用）
        btnDone = new Button(ctx);
        btnDone.setText("分享后自动解锁 (3秒)");
        btnDone.setTextSize(15);
        btnDone.setTextColor(0xFF888888);
        btnDone.setEnabled(false);
        btnDone.setBackground(getGrayBg());
        btnDone.setPadding(32, 14, 32, 14);
        LinearLayout.LayoutParams doneLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        doneLp.setMargins(0, 8, 0, 0);
        btnDone.setLayoutParams(doneLp);
        btnDone.setOnClickListener(v -> {
            if (hasSharedThis && timer != null) {
                markSharedAndContinue();
            }
        });
        root.addView(btnDone);

        return root;
    }

    private Button createShareButton(String text, int color) {
        Button btn = new Button(ctx);
        btn.setText(text);
        btn.setTextSize(15);
        btn.setTextColor(0xFFFFFFFF);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(48f);
        bg.setColor(color);
        btn.setBackground(bg);
        btn.setPadding(32, 16, 32, 16);
        btn.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 12);
        btn.setLayoutParams(lp);
        return btn;
    }

    private android.graphics.drawable.GradientDrawable getGrayBg() {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(48f);
        bg.setColor(0xFFE0E0E0);
        return bg;
    }

    private android.graphics.drawable.GradientDrawable getGreenBg() {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(48f);
        bg.setColor(0xFF416835);
        return bg;
    }

    private void startCountdown() {
        // 视觉反馈：分享按钮变色
        btnShareQQ.setText("\u2705 分享到QQ好友");
        btnShareQzone.setText("\u2705 分享到QQ空间");
        btnCopy.setText("\u2705 复制分享文案");

        // 启动3秒倒计时
        timer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                btnDone.setText("我已完成分享 (" + seconds + "秒)");
                tvCountdown.setText("请确认已完成分享操作");
            }

            @Override
            public void onFinish() {
                btnDone.setText("\uD83D\uDE80 我已完成分享，开始下载");
                btnDone.setTextColor(0xFFFFFFFF);
                btnDone.setEnabled(true);
                btnDone.setBackground(getGreenBg());
                tvCountdown.setText("分享成功！获得30积分奖励");
            }
        };
        timer.start();

        // 分享按钮变半透明，不可再点
        btnShareQQ.setAlpha(0.6f);
        btnShareQQ.setEnabled(false);
        btnShareQzone.setAlpha(0.6f);
        btnShareQzone.setEnabled(false);
        btnCopy.setAlpha(0.6f);
        btnCopy.setEnabled(false);
    }

    private void markSharedAndContinue() {
        // 本地标记
        ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("has_shared", true).apply();

        // 通知服务端
        new Thread(() -> {
            try {
                String resp = ApiClient.post(ApiClient.A + "/user/share_complete.php",
                        new org.json.JSONObject().put("share_type", "app"));
            } catch (Exception ignored) {}
        }).start();

        Toast.makeText(ctx, "分享成功！+30积分", Toast.LENGTH_SHORT).show();
        dismiss();

        // 执行回调（进入下载）
        if (onComplete != null) {
            onComplete.run();
        }
    }

    @Override
    public void onBackPressed() {
        // 完全拦截返回键 - 不可退出
        // 什么都不做
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (timer != null) {
            timer.cancel();
        }
    }
}
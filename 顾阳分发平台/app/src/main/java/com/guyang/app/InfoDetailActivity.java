package com.guyang.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class InfoDetailActivity extends Activity {
    private ImageView iv;
    private TextView tvTitle, tvContent, tvMeta;
    private Button btnAction, btnCopyPan;
    private String infoId, panUrl, panCode, panType, panTypeName;
    private int price;
    private boolean unlocked;
    private JSONObject fullData;
    private LinearLayout panCardLayout;

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_info_detail);
        iv = findViewById(R.id.info_cover);
        tvTitle = findViewById(R.id.info_title);
        tvContent = findViewById(R.id.info_content);
        tvMeta = findViewById(R.id.info_meta);
        btnAction = findViewById(R.id.btn_get);
        btnCopyPan = findViewById(R.id.btn_copy_pan);

        infoId = getIntent().getStringExtra("info_id");
        loadFromServer();
    }

    private void loadFromServer() {
        btnAction.setEnabled(false);
        btnAction.setText("加 载 中 ...");
        String token = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("token", "");
        new Thread(() -> {
            try {
                String resp = ApiClient.get(ApiClient.A + "/info/detail.php?id=" + infoId + "&token=" + token);
                if (resp == null || resp.startsWith("ERROR:")) {
                    runOnUiThread(() -> { tvContent.setText("加 载失败"); btnAction.setVisibility(View.GONE); });
                    return;
                }
                JSONObject d = new JSONObject(resp);
                if (d.optInt("code", -1) != 0) {
                    runOnUiThread(() -> { tvContent.setText(d.optString("message", "加 载失败")); btnAction.setVisibility(View.GONE); });
                    return;
                }
                fullData = d.optJSONObject("data");
                if (fullData != null) {
                    unlocked = fullData.optBoolean("unlocked", false);
                    panUrl = fullData.optString("pan_url", "");
                    panCode = fullData.optString("pan_code", "");
                    panType = fullData.optString("pan_type", "other");
                    panTypeName = fullData.optString("pan_type_name", "网 盘");
                    price = fullData.optInt("price", 0);
                    runOnUiThread(this::display);
                }
            } catch (Exception e) {
                runOnUiThread(() -> { tvContent.setText("加 载失败"); btnAction.setVisibility(View.GONE); });
            }
        }).start();
    }

    private void display() {
        tvTitle.setText(fullData.optString("title", ""));
        String category = fullData.optString("category", "");
        String time = fullData.optString("time", "");
        tvMeta.setText((price == 0 ? "免 费" : price + "积 分") +
            (category.isEmpty() ? "" : " | " + category) +
            (time.isEmpty() ? "" : " | " + time));

        String content = fullData.optString("content", "");
        tvContent.setText(content.isEmpty() ? "暂 无 内 容" : content);

        // Load cover
        String coverUrl = fullData.optString("cover_img", "");
        if (!coverUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    URL url = new URL(coverUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    InputStream is = conn.getInputStream();
                    Bitmap bm = BitmapFactory.decodeStream(is);
                    is.close();
                    runOnUiThread(() -> { if (bm != null) iv.setImageBitmap(bm); });
                } catch (Exception ignored) {}
            }).start();
        }

        // 2.0: 动态创建网盘卡片
        addPanCard();

        // Legacy button
        handleActionButton();
    }

    private void addPanCard() {
        if (panUrl == null || panUrl.isEmpty()) return;

        // 找到内容区的父容器（ScrollView/LinearLayout）
        View root = findViewById(android.R.id.content);
        ViewGroup parent = (ViewGroup) tvContent.getParent(); // ScrollView内的LinearLayout
        if (parent == null) parent = (ViewGroup) ((ViewGroup) tvContent.getParent().getParent());
        if (parent == null) return;

        // 网盘卡片容器
        panCardLayout = new LinearLayout(this);
        panCardLayout.setOrientation(LinearLayout.VERTICAL);
        panCardLayout.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 16, 0, 32);
        panCardLayout.setLayoutParams(cardLp);
        panCardLayout.setBackgroundColor(0xFFF0F4E8);
        float density = getResources().getDisplayMetrics().density;
        panCardLayout.setElevation(4 * density);

        // 标题行: 图标 + 网盘名称 + 状态标签
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, 12);

        ImageView panIcon = new ImageView(this);
        panIcon.setLayoutParams(new LinearLayout.LayoutParams(28, 28));
        panIcon.setImageResource(getPanIconId(panType));
        headerRow.addView(panIcon);

        TextView tvPanLabel = new TextView(this);
        tvPanLabel.setText("  " + panTypeName);
        tvPanLabel.setTextSize(16);
        tvPanLabel.setTextColor(0xFF1A1C18);
        tvPanLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        headerRow.addView(tvPanLabel);

        TextView statusBadge = new TextView(this);
        statusBadge.setTextSize(12);
        statusBadge.setPadding(12, 4, 12, 4);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(-2, -2);
        badgeLp.setMargins(16, 0, 0, 0);
        statusBadge.setLayoutParams(badgeLp);

        if (price == 0 || unlocked) {
            statusBadge.setText(" 已解锁");
            statusBadge.setTextColor(0xFF2E7D32);
            statusBadge.setBackgroundColor(0xFFC8E6C9);
        } else {
            statusBadge.setText(" 付费内容");
            statusBadge.setTextColor(0xFFC62828);
            statusBadge.setBackgroundColor(0xFFFFCDD2);
        }
        headerRow.addView(statusBadge);
        panCardLayout.addView(headerRow);

        // 链接行
        TextView tvLink = new TextView(this);
        tvLink.setTextSize(14);
        tvLink.setTextColor(price == 0 || unlocked ? 0xFF1565C0 : 0xFF9E9E9E);
        tvLink.setPadding(0, 4, 0, 4);
        tvLink.setText(price == 0 || unlocked ? panUrl : "https://*** (付费后可见)");
        panCardLayout.addView(tvLink);

        // 提取码行
        if (!panCode.isEmpty() && (price == 0 || unlocked)) {
            TextView tvCode = new TextView(this);
            tvCode.setTextSize(14);
            tvCode.setTextColor(0xFF1A1C18);
            tvCode.setPadding(0, 4, 0, 12);
            tvCode.setText(" 提 取 码 : " + panCode);
            tvCode.setTypeface(null, android.graphics.Typeface.BOLD);
            panCardLayout.addView(tvCode);
        }

        // 按钮行
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 8, 0, 0);

        if (price == 0 || unlocked) {
            // 打开网盘
            Button btnOpen = new Button(this);
            btnOpen.setText("  打开网 盘  ");
            btnOpen.setTextSize(14);
            btnOpen.setTextColor(0xFFFFFFFF);
            btnOpen.setBackgroundColor(0xFF1B5E20);
            btnOpen.setOnClickListener(v -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(panUrl))); }
                catch (Exception e) { Toast.makeText(this, "无 法打开链接", Toast.LENGTH_SHORT).show(); }
            });
            btnRow.addView(btnOpen);

            // 复制全部
            Button btnCopy = new Button(this);
            btnCopy.setText("  复制全部  ");
            btnCopy.setTextSize(14);
            btnCopy.setTextColor(0xFF1B5E20);
            btnCopy.setBackgroundColor(0xFFE8F5E9);
            LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(-2, -2);
            copyLp.setMargins(16, 0, 0, 0);
            btnCopy.setLayoutParams(copyLp);
            btnCopy.setOnClickListener(v -> {
                String copyText = "【" + tvTitle.getText() + "】\n"
                    + "网 盘 : " + panTypeName + "\n"
                    + "链接: " + panUrl;
                if (!panCode.isEmpty()) copyText += "\n提 取 码 : " + panCode;
                copyText += "\n\n—— 来 自顾阳软件 盒 ";
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("pan_info", copyText));
                Toast.makeText(this, "已复制链接和提取码", Toast.LENGTH_SHORT).show();
            });
            btnRow.addView(btnCopy);
        } else {
            // 付费解锁按钮
            Button btnBuy = new Button(this);
            btnBuy.setText("   " + price + " 积 分 解锁查看  ");
            btnBuy.setTextSize(14);
            btnBuy.setTextColor(0xFFFFFFFF);
            btnBuy.setBackgroundColor(0xFFFF6D00);
            final Button buyBtn = btnBuy;
            btnBuy.setOnClickListener(v -> {
                buyBtn.setEnabled(false);
                new Thread(() -> {
                    try {
                        String resp = ApiClient.infoBuy(infoId);
                        JSONObject r = new JSONObject(resp);
                        runOnUiThread(() -> {
                            if (r.optInt("code", -1) == 0) {
                                Toast.makeText(this, "购 买成功！", Toast.LENGTH_SHORT).show();
                                loadFromServer();
                            } else {
                                buyBtn.setEnabled(true);
                                Toast.makeText(this, r.optString("message", "积 分不足"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> { buyBtn.setEnabled(true); });
                    }
                }).start();
            });
            btnRow.addView(btnBuy);
        }
        panCardLayout.addView(btnRow);

        // 添加到tvContent下方
        int index = parent.indexOfChild(tvContent);
        if (index >= 0) {
            parent.addView(panCardLayout, index + 1);
        } else {
            parent.addView(panCardLayout);
        }
    }

    private int getPanIconId(String type) {
        switch (type != null ? type : "") {
            case "baidu": return android.R.drawable.ic_menu_save;
            case "lanzou": return android.R.drawable.ic_menu_upload;
            case "aliyun": return android.R.drawable.ic_menu_gallery;
            default: return android.R.drawable.ic_menu_info_details;
        }
    }

    private void handleActionButton() {
        if (price <= 0) {
            btnAction.setVisibility(View.GONE);
            btnCopyPan.setVisibility(View.GONE);
            return;
        }
        if (!unlocked) {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setEnabled(true);
            btnAction.setText("购 买(" + price + "积 分)");
            btnAction.setOnClickListener(v -> {
                btnAction.setEnabled(false);
                new Thread(() -> {
                    try {
                        String resp = ApiClient.post(ApiClient.A + "/info/buy.php",
                            new JSONObject().put("info_id", infoId));
                        JSONObject r = new JSONObject(resp);
                        runOnUiThread(() -> {
                            if (r.optInt("code", -1) == 0) {
                                Toast.makeText(this, "购 买成功！", Toast.LENGTH_SHORT).show();
                                loadFromServer();
                            } else {
                                btnAction.setEnabled(true);
                                Toast.makeText(this, r.optString("message", "购 买失败"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> { btnAction.setEnabled(true); });
                    }
                }).start();
            });
            btnCopyPan.setVisibility(View.GONE);
        } else {
            btnAction.setVisibility(View.GONE);
            btnCopyPan.setVisibility(View.GONE);
        }
    }
}
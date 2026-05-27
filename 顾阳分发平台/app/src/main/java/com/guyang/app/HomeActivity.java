package com.guyang.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class HomeActivity extends Activity {
    private ViewGroup contentFrame;
    private View tabHome, tabApps, tabInfo, tabMe;
    private TextView tvBottom1, tvBottom2, tvBottom3, tvBottom4;
    private List<JSONObject> apps = new ArrayList<>();
    private List<JSONObject> infos = new ArrayList<>();
    private AppBaseAdapter homeAppAdapter, appsAdapter;
    private InfoBaseAdapter infoAdapter;
    private Handler bannerHandler = new Handler();
    private int bannerIndex = 0;
    private List<JSONObject> bannerList = new ArrayList<>();
    private ImageView bannerIv;
    private View pageHome, pageApps, pageInfo, pageMe;
    private ListView lvHome, lvApps, lvInfo;
    private String rechargeUrl = "";
    private String currentCategory = "";

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_home);
        contentFrame = findViewById(R.id.content_frame);
        tabHome = findViewById(R.id.tab_home);
        tabApps = findViewById(R.id.tab_apps);
        tabInfo = findViewById(R.id.tab_info);
        tabMe   = findViewById(R.id.tab_me);
        tvBottom1 = findViewById(R.id.tv_bottom1);
        tvBottom2 = findViewById(R.id.tv_bottom2);
        tvBottom3 = findViewById(R.id.tv_bottom3);
        tvBottom4 = findViewById(R.id.tv_bottom4);

        View.OnClickListener tabClick = v -> {
            int id = v.getId();
            if (id == R.id.tab_home) switchTab(0);
            else if (id == R.id.tab_apps) switchTab(1);
            else if (id == R.id.tab_info) switchTab(2);
            else if (id == R.id.tab_me) switchTab(3);
        };
        tabHome.setOnClickListener(tabClick);
        tabApps.setOnClickListener(tabClick);
        tabInfo.setOnClickListener(tabClick);
        tabMe.setOnClickListener(tabClick);

        String token = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("token", "");
        if (!token.isEmpty()) ApiClient.setToken(token);

        pageHome = buildHomePage();
        pageApps = buildAppsPage();
        pageInfo = buildInfoPage();
        pageMe   = buildMePage();

        switchTab(0);
        loadSystemConfig();
        loadBanners();
        loadApps();
        loadInfos();
    }

    private boolean isLoggedIn() {
        return !getSharedPreferences("app_prefs", MODE_PRIVATE).getString("token", "").isEmpty();
    }

    private void requireLogin() {
        if (!isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("require_login", true);
            startActivity(intent);
        }
    }

    private void switchTab(int tab) {
        tvBottom1.setTextColor(tab == 0 ? 0xFF416835 : 0xFF888888);
        tvBottom2.setTextColor(tab == 1 ? 0xFF416835 : 0xFF888888);
        tvBottom3.setTextColor(tab == 2 ? 0xFF416835 : 0xFF888888);
        tvBottom4.setTextColor(tab == 3 ? 0xFF416835 : 0xFF888888);
        View page = null;
        switch (tab) {
            case 0: page = pageHome; break;
            case 1: page = pageApps; break;
            case 2: page = pageInfo; break;
            case 3:
                if (!isLoggedIn()) {
                    requireLogin();
                    return;
                }
                page = pageMe;
                break;
        }
        if (page == null) return;
        if (page.getParent() != null) ((ViewGroup) page.getParent()).removeView(page);
        contentFrame.removeAllViews();
        contentFrame.addView(page);
        if (tab == 3) refreshUserInfo();
    }

    private View buildHomePage() {
        View v = getLayoutInflater().inflate(R.layout.page_home, null);
        FrameLayout bannerBox = v.findViewById(R.id.banner_box);
        bannerIv = new ImageView(this);
        bannerIv.setScaleType(ImageView.ScaleType.FIT_XY);
        bannerIv.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        bannerBox.addView(bannerIv);
        bannerIv.setOnClickListener(bv -> {
            if (!bannerList.isEmpty()) {
                JSONObject b = bannerList.get(bannerIndex % bannerList.size());
                String infoId = b.optString("info_id", b.optString("id", ""));
                if (!infoId.isEmpty()) {
                    Intent it = new Intent(this, InfoDetailActivity.class);
                    it.putExtra("info_id", infoId);
                    startActivity(it);
                }
            }
        });
        v.findViewById(R.id.cat_software).setOnClickListener(cv -> { currentCategory = "软件"; switchTab(1); });
        v.findViewById(R.id.cat_tools).setOnClickListener(cv -> { currentCategory = "工具"; switchTab(1); });
        v.findViewById(R.id.cat_netearn).setOnClickListener(cv -> { currentCategory = "网赚"; switchTab(1); });
        v.findViewById(R.id.cat_video).setOnClickListener(cv -> { currentCategory = "视频"; switchTab(1); });
        v.findViewById(R.id.card_shop).setOnClickListener(cv -> {
            if (!rechargeUrl.isEmpty()) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(rechargeUrl))); }
                catch (Exception e) { Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show(); }
            } else { Toast.makeText(this, "暂无购买链接", Toast.LENGTH_SHORT).show(); }
        });
        lvHome = v.findViewById(R.id.lv_home);
        EditText etSearch = v.findViewById(R.id.et_search);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                public void afterTextChanged(android.text.Editable e) { filterHomeApps(e.toString().trim()); }
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                public void onTextChanged(CharSequence s, int st, int b, int c) {}
            });
        }
        return v;
    }

    private void filterHomeApps(String kw) {
        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject app : apps) {
            if (kw.isEmpty() || app.optString("name", "").contains(kw)
                || app.optString("desc", "").contains(kw))
                filtered.add(app);
        }
        homeAppAdapter = new AppBaseAdapter(this, filtered);
        lvHome.setAdapter(homeAppAdapter);
    }

    private View buildAppsPage() {
        View v = getLayoutInflater().inflate(R.layout.page_apps, null);
        lvApps = v.findViewById(R.id.lv_apps);
        return v;
    }

    private View buildInfoPage() {
        View v = getLayoutInflater().inflate(R.layout.page_info, null);
        lvInfo = v.findViewById(R.id.lv_info);
        return v;
    }


    private View buildMePage() {
        View v = getLayoutInflater().inflate(R.layout.page_me, null);
        v.findViewById(R.id.btn_checkin).setOnClickListener(cv -> {
            Button b = (Button) v.findViewById(R.id.btn_checkin);
            b.setEnabled(false);
            b.setText("签到中...");
            new Thread(() -> {
                try {
                    String r = ApiClient.checkin();
                    JSONObject d = new JSONObject(r);
                    int code = d.optInt("code", -1);
                    String msg = d.optString("message", "");
                    JSONObject dat = d.optJSONObject("data");
                    int pts = dat != null ? dat.optInt("points", 0) : 0;
                    runOnUiThread(() -> {
                        Toast.makeText(this, code == 0 ? "签到成功 +" + pts + "积分" : (msg.isEmpty() ? "签到失败" : msg), Toast.LENGTH_SHORT).show();
                        b.setEnabled(true);
                        b.setText("每日签到");
                        if (code == 0) refreshUserInfo();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> { b.setEnabled(true); b.setText("每日签到"); });
                }
            }).start();
        });
        v.findViewById(R.id.menu_points_history).setOnClickListener(cv -> {
            new AlertDialog.Builder(this).setTitle("积分记录").setMessage("加载中...").setPositiveButton("关闭", null).show();
            loadPointsHistory();
        });
        v.findViewById(R.id.menu_downloads).setOnClickListener(cv ->
            startActivity(new Intent(this, DownloadListActivity.class)));
        v.findViewById(R.id.menu_redeem).setOnClickListener(cv -> {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setTitle("卡密兑换");
            EditText et = new EditText(this);
            et.setHint("输入卡密");
            ab.setView(et);
            ab.setPositiveButton("兑换", (d, w) -> {
                String code = et.getText().toString().trim();
                new Thread(() -> {
                    try {
                        String r = ApiClient.redeemCard(code);
                        JSONObject j = new JSONObject(r);
                        runOnUiThread(() -> Toast.makeText(this, j.optInt("code", -1) == 0 ? "兑换成功" : j.optString("message", "失败"), Toast.LENGTH_SHORT).show());
                    } catch (Exception ignored) {}
                }).start();
            });
            ab.setNegativeButton("取消", null);
            ab.show();
        });
        v.findViewById(R.id.menu_customer_service).setOnClickListener(cv -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&card_type=group&uin=854934959")));
            } catch (Exception e) {
                Toast.makeText(this, "请先安装QQ", Toast.LENGTH_SHORT).show();
            }
        });
        // 2.0: 添加邀请好友按钮（动态插入）
        addInviteButton(v);
        
        v.findViewById(R.id.btn_logout).setOnClickListener(cv -> {
            new AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setPositiveButton("退出", (d, w) -> {
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit().remove("token").apply();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
        });
        return v;
    }

    // 2.0: 动态添加邀请按钮
    private void addInviteButton(View pageMeView) {
        View btnLogout = pageMeView.findViewById(R.id.btn_logout);
        ViewGroup parent = (ViewGroup) btnLogout.getParent();
        if (parent == null) return;

        Button btnInvite = new Button(this);
        btnInvite.setText("  邀请好友 +30积分");
        btnInvite.setTextSize(14);
        btnInvite.setTextColor(0xFFFFFFFF);
        btnInvite.setBackgroundColor(0xFFFF6D00);
        btnInvite.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 12, 0, 0);
        btnInvite.setLayoutParams(lp);
        btnInvite.setOnClickListener(v -> {
            if (!isLoggedIn()) { requireLogin(); return; }
            loadInviteInfo();
        });

        // 插入到退出按钮上方
        int idx = parent.indexOfChild(btnLogout);
        if (idx >= 0) {
            parent.addView(btnInvite, idx);
        } else {
            parent.addView(btnInvite);
        }
    }

    // 2.0: 加载邀请信息
    private void loadInviteInfo() {
        new Thread(() -> {
            try {
                String r = ApiClient.inviteStats();
                JSONObject d = new JSONObject(r);
                if (d.optInt("code", -1) == 0) {
                    JSONObject data = d.optJSONObject("data");
                    if (data != null) {
                        String code = data.optString("invite_code", "");
                        int count = data.optInt("invite_count", 0);
                        int reward = data.optInt("invite_reward", 30);
                        String link = "http://47.108.209.71/landing.html?invite=" + code;
                        String msg = "邀请码: " + code + "\n已邀请: " + count + "人\n每邀请1人奖励" + reward + "积分\n\n分享链接:\n" + link;
                        runOnUiThread(() -> {
                            new AlertDialog.Builder(HomeActivity.this)
                                .setTitle("  邀请好友")
                                .setMessage(msg)
                                .setPositiveButton("复制链接", (di, w) -> {
                                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    cm.setPrimaryClip(ClipData.newPlainText("invite", link));
                                    Toast.makeText(HomeActivity.this, "链接已复制", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("关闭", null)
                                .show();
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void refreshUserInfo() {
        if (pageMe == null) return;
        new Thread(() -> {
            try {
                String r = ApiClient.userInfo();
                JSONObject d = new JSONObject(r);
                if (d.optInt("code", -1) == 0) {
                    JSONObject u = d.optJSONObject("data");
                    if (u != null) {
                        String avatarUrl = u.optString("avatar", "");
                        runOnUiThread(() -> {
                            TextView tvNick = pageMe.findViewById(R.id.tv_nickname);
                            TextView tvPts = pageMe.findViewById(R.id.tv_points);
                            ImageView ivAvatar = pageMe.findViewById(R.id.iv_avatar);
                            if (tvNick != null)
                                tvNick.setText(u.optString("username", u.optString("nickname", "用户")));
                            if (tvPts != null)
                                tvPts.setText("积分: " + u.optInt("points", 0));
                            if (ivAvatar != null && !avatarUrl.isEmpty()) {
                                new Thread(() -> {
                                    try {
                                        java.net.URL url = new java.net.URL(avatarUrl);
                                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                                        conn.setConnectTimeout(8000);
                                        java.io.InputStream is = conn.getInputStream();
                                        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeStream(is);
                                        is.close();
                                        runOnUiThread(() -> { if (bm != null) ivAvatar.setImageBitmap(bm); });
                                    } catch (Exception ignored) {}
                                }).start();
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }


    private void loadSystemConfig() {
        new Thread(() -> {
            try {
                String r = ApiClient.get(ApiClient.A + "/admin/system_config.php");
                JSONObject d = new JSONObject(r);
                if (d.optInt("code", -1) == 0) {
                    JSONObject data = d.optJSONObject("data");
                    if (data != null) {
                        String url = data.optString("recharge_url", "");
                        if (!url.isEmpty() && !url.startsWith("http")) url = "https://" + url;
                        rechargeUrl = url;
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadBanners() {
        new Thread(() -> {
            try {
                String r = ApiClient.banners();
                JSONObject d = new JSONObject(r);
                if (d.optInt("code", -1) != 0) return;
                JSONArray arr = d.optJSONArray("data");
                if (arr == null) return;
                bannerList.clear();
                for (int i = 0; i < arr.length(); i++) bannerList.add(arr.optJSONObject(i));
                if (!bannerList.isEmpty()) runOnUiThread(this::startBanner);
            } catch (Exception ignored) {}
        }).start();
    }

    private void startBanner() {
        if (bannerList.isEmpty() || bannerIv == null) return;
        showBannerImage(0);
        bannerHandler.removeCallbacksAndMessages(null);
        bannerHandler.postDelayed(new Runnable() {
            public void run() {
                bannerIndex = (bannerIndex + 1) % bannerList.size();
                showBannerImage(bannerIndex);
                bannerHandler.postDelayed(this, 4000);
            }
        }, 4000);
    }

    private void showBannerImage(int idx) {
        if (idx >= bannerList.size() || bannerIv == null) return;
        JSONObject b = bannerList.get(idx);
        String imgUrl = b.optString("image_url", "");
        if (!imgUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    URL url = new URL(imgUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(8000);
                    InputStream is = conn.getInputStream();
                    Bitmap bm = BitmapFactory.decodeStream(is);
                    is.close();
                    runOnUiThread(() -> { if (bm != null && bannerIv != null) bannerIv.setImageBitmap(bm); });
                } catch (Exception ignored) {}
            }).start();
        }
    }

    private void loadApps() {
        new Thread(() -> {
            try {
                String r = currentCategory.isEmpty() ? ApiClient.apps("") : ApiClient.apps(currentCategory);
                JSONObject d = new JSONObject(r);
                if (d.optInt("code", -1) != 0) return;
                JSONArray arr = d.optJSONArray("data");
                if (arr == null) return;
                apps.clear();
                for (int i = 0; i < arr.length(); i++) apps.add(arr.optJSONObject(i));
                runOnUiThread(() -> {
                    filterHomeApps("");
                    appsAdapter = new AppBaseAdapter(this, apps);
                    if (lvApps != null) lvApps.setAdapter(appsAdapter);
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadInfos() {
        new Thread(() -> {
            try {
                String r = ApiClient.infoList();
                JSONObject d = new JSONObject(r);
                if (d.optInt("code", -1) != 0) return;
                JSONArray arr = d.optJSONArray("data");
                if (arr == null) return;
                infos.clear();
                for (int i = 0; i < arr.length(); i++) infos.add(arr.optJSONObject(i));
                runOnUiThread(() -> {
                    infoAdapter = new InfoBaseAdapter(this, infos);
                    if (lvInfo != null) lvInfo.setAdapter(infoAdapter);
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadPointsHistory() {
        new Thread(() -> {
            try {
                String r = ApiClient.get(ApiClient.A + "/points/log.php");
                JSONObject d = new JSONObject(r);
                StringBuilder sb = new StringBuilder();
                if (d.optInt("code", -1) == 0) {
                    JSONArray arr = d.optJSONArray("data");
                    if (arr != null && arr.length() > 0) {
                        for (int i = 0; i < Math.min(arr.length(), 20); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item != null)
                                sb.append(item.optString("desc", "")).append(" +")
                                  .append(item.optInt("points", 0)).append("积分\n");
                        }
                    } else { sb.append("暂无积分记录"); }
                } else { sb.append("加载失败"); }
                final String msg = sb.toString();
                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("积分记录").setMessage(msg)
                    .setPositiveButton("关闭", null).show());
            } catch (Exception ignored) {}
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bannerHandler.removeCallbacksAndMessages(null);
    }
}
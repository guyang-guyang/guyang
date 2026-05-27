package com.guyang.app;
import android.app.Activity; import android.content.*; import android.os.*; import android.widget.*;
import java.io.*; import java.net.*;
import org.json.JSONObject;

public class RegisterActivity extends Activity {
    private EditText u, p, qq, inv; private TextView e;

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_register);
        u = findViewById(R.id.username);
        p = findViewById(R.id.password);
        qq = findViewById(R.id.qq);
        inv = findViewById(R.id.invite_code);
        e = findViewById(R.id.error);

        // 2.0: 自动读取剪贴板中的邀请码
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
                if (item != null) {
                    String clipText = item.getText() != null ? item.getText().toString() : "";
                    if (clipText != null && clipText.length() >= 4 && clipText.length() <= 10) {
                        inv.setText(clipText.toUpperCase());
                    }
                }
            }
        } catch (Exception ignored) {}

        findViewById(R.id.btn_register).setOnClickListener(v -> {
            String un = u.getText().toString().trim();
            String pw = p.getText().toString().trim();
            String q = qq.getText().toString().trim();
            String iv = inv.getText().toString().trim();

            if (un.isEmpty() || pw.isEmpty()) { e.setText("请输入用户名和密码"); return; }
            if (un.length() < 2 || un.length() > 20) { e.setText("用户名需2-20位"); return; }
            if (pw.length() < 6) { e.setText("密码至少6位"); return; }
            // QQ号选填，如果填了就校验
            if (!q.isEmpty()) {
                if (q.length() < 5 || q.length() > 15) { e.setText("请输入正确的QQ号"); return; }
                for (int i = 0; i < q.length(); i++) { if (!Character.isDigit(q.charAt(i))) { e.setText("QQ号只能包含数字"); return; } }
            }
            e.setText("");

            final String dm = android.os.Build.MODEL;
            final String ds = String.valueOf(android.os.Build.VERSION.SDK_INT);

            new Thread(() -> {
                String r = ApiClient.register(un, pw, q, iv, dm, ds);
                runOnUiThread(() -> hr(r, q));
            }).start();
        });

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void hr(String r, String qqNum) {
        if (r == null || r.isEmpty()) { e.setText("网络请求失败"); return; }
        try {
            JSONObject j = new JSONObject(r);
            if (j.optInt("code") == 0) {
                JSONObject data = j.getJSONObject("data");
                String t = data.optString("token", "");
                // 2.0: QQ头像URL
                String avatarUrl = "";
                if (!qqNum.isEmpty()) {
                    avatarUrl = "https://q.qlogo.cn/headimg_dl?dst_uin=" + qqNum + "&spec=100";
                }
                if (!t.isEmpty()) {
                    SharedPreferences sp = getSharedPreferences("app_prefs", 0);
                    sp.edit()
                        .putString("token", t)
                        .putString("avatar_url", avatarUrl)
                        .putString("qq", qqNum)
                        .putString("username", u.getText().toString().trim())
                        .putBoolean("has_shared", false) // 新用户未分享
                        .apply();

                    // 下载头像到本地
                    if (!avatarUrl.isEmpty()) {
                        final String aUrl = avatarUrl;
                        new Thread(() -> downloadAvatar(aUrl)).start();
                    }

                    Toast.makeText(this, "注册成功！", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return;
                }
            }
            e.setText(j.optString("message", "注册失败"));
        } catch (Exception ex) {
            e.setText(r.startsWith("ERROR:") ? r.substring(6) : r);
        }
    }

    private void downloadAvatar(String url) {
        if (url == null || url.isEmpty()) return;
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            InputStream in = c.getInputStream();
            FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "avatar.png"));
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            out.close(); in.close();
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
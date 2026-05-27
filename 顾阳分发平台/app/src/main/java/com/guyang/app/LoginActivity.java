package com.guyang.app;
import android.app.Activity; import android.content.*; import android.os.Bundle; import android.widget.*;
import org.json.JSONObject;
import java.io.*; import java.net.*;

public class LoginActivity extends Activity {
    private EditText u,p; private TextView e;

    // 2.0: 接收pending intent
    private String pendingAction;
    private String pendingAppName, pendingAppId, pendingFileName, pendingDownloadUrl;

    protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_login);
        u=findViewById(R.id.username);
        p=findViewById(R.id.password);
        e=findViewById(R.id.error);

        // 2.0: 读取pending参数
        Intent intent = getIntent();
        pendingAction = intent.getStringExtra("pending_action");
        if (pendingAction != null) {
            pendingAppName = intent.getStringExtra("app_name");
            pendingAppId = intent.getStringExtra("app_id");
            pendingFileName = intent.getStringExtra("file_name");
            pendingDownloadUrl = intent.getStringExtra("download_url");
        }

        findViewById(R.id.btn_login).setOnClickListener(v->{
            String un=u.getText().toString().trim(),pw=p.getText().toString().trim();
            if(un.isEmpty()||pw.isEmpty()){e.setText("请输入用户名和密码");return;}
            e.setText("");new Thread(()->{
                String r=ApiClient.login(un,pw);
                runOnUiThread(()->hl(r));
            }).start();
        });
        findViewById(R.id.btn_register).setOnClickListener(v->
            startActivity(new Intent(this,RegisterActivity.class)));
    }

    private void hl(String r){
        if(r==null||r.isEmpty()){e.setText("网络请求失败");return;}
        try{
            JSONObject j=new JSONObject(r);
            if(j.optInt("code")==0){
                JSONObject data=j.getJSONObject("data");
                String t=data.optString("token","");
                if(!t.isEmpty()){
                    SharedPreferences sp=getSharedPreferences("app_prefs",0);
                    SharedPreferences.Editor ed=sp.edit();
                    ed.putString("token",t);

                    JSONObject user=data.optJSONObject("user");
                    if(user!=null){
                        String avatarUrl=user.optString("avatar","");
                        if(!avatarUrl.isEmpty()){
                            ed.putString("avatar_url",avatarUrl);
                            final String aUrl=avatarUrl;
                            new Thread(()->downloadAvatar(aUrl)).start();
                        }
                        // 2.0: 同步用户分享状态
                        boolean hasShared = user.optBoolean("has_shared", false);
                        ed.putBoolean("has_shared", hasShared);
                    }
                    ed.apply();

                    // 2.0: 登录后处理pending
                    handlePendingAction();
                    return;
                }
            }
            e.setText(j.optString("message","登录失败"));
        }catch(Exception ex){
            e.setText(r.startsWith("ERROR:")?r.substring(6):r);
        }
    }

    // 2.0: 登录后恢复pending操作
    private void handlePendingAction() {
        if ("download".equals(pendingAction) && pendingAppName != null) {
            SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean hasShared = sp.getBoolean("has_shared", false);
            
            if (!hasShared) {
                // 弹出分享弹窗
                new ShareGateDialog(this, pendingAppName, () -> {
                    InAppDownloadActivity.start(this, pendingAppName, pendingAppId,
                            pendingFileName != null ? pendingFileName : pendingAppName + ".apk",
                            pendingDownloadUrl);
                    finish();
                }).show();
                return;
            }
            
            // 已分享 → 直接下载
            InAppDownloadActivity.start(this, pendingAppName, pendingAppId,
                    pendingFileName != null ? pendingFileName : pendingAppName + ".apk",
                    pendingDownloadUrl);
            finish();
            return;
        }
        
        // 无pending → 直接进主页
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void downloadAvatar(String url){
        if(url==null||url.isEmpty())return;
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(url).openConnection();
            c.setConnectTimeout(8000);c.setReadTimeout(8000);
            InputStream in=c.getInputStream();
            FileOutputStream out=new FileOutputStream(new File(getFilesDir(),"avatar.png"));
            byte[] buf=new byte[4096];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);
            out.close();in.close();
        }catch(Exception ignored){
        }finally{if(c!=null)c.disconnect();}
    }
}
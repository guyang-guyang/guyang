package com.guyang.admin;
import android.app.Activity; import android.content.*; import android.os.Bundle;
import android.text.TextUtils; import android.view.KeyEvent; import android.view.View; import android.widget.*;
import org.json.JSONObject;

public class LoginActivity extends Activity {
    private EditText u,p; private TextView e; private Button btn;

    protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_login);
        u=findViewById(R.id.username); p=findViewById(R.id.password);
        e=findViewById(R.id.error); btn=findViewById(R.id.btnLogin);
        SharedPreferences sp=getSharedPreferences("admin",MODE_PRIVATE);
        String lastUser=sp.getString("last_user","");
        if(!lastUser.isEmpty())u.setText(lastUser);
        String savedToken=sp.getString("token","");
        if(!savedToken.isEmpty()){ApiClient.setToken(savedToken);startMain();return;}
        p.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            public boolean onEditorAction(TextView v,int actionId,KeyEvent ev){doLogin();return true;}});
        btn.setOnClickListener(new View.OnClickListener(){public void onClick(View v){doLogin();}});
    }

    private void doLogin(){
        final String un=u.getText().toString().trim();final String pw=p.getText().toString().trim();
        if(TextUtils.isEmpty(un)){showError("请输入用户名");u.requestFocus();return;}
        if(TextUtils.isEmpty(pw)){showError("请输入密码");p.requestFocus();return;}
        e.setVisibility(View.GONE);btn.setEnabled(false);btn.setText("登录中…");
        new Thread(new Runnable(){public void run(){
            final String r=ApiClient.login(un,pw);
            runOnUiThread(new Runnable(){public void run(){
                btn.setEnabled(true);btn.setText("登录");
                try{
                    JSONObject j=new JSONObject(r);
                    if(j.optInt("code",-1)==0||j.optBoolean("success",false)||j.has("token")){
                        String tok=j.optString("token","");
                        if(tok.isEmpty()&&j.has("data"))tok=j.optJSONObject("data").optString("token","");
                        ApiClient.setToken(tok);
                        getSharedPreferences("admin",MODE_PRIVATE).edit().putString("token",tok).putString("last_user",un).apply();
                        startMain();
                    }else{
                        String msg=j.optString("message","");if(msg.isEmpty())msg=j.optString("msg","");if(msg.isEmpty())msg="登录失败";
                        showError(msg);
                        if(msg.contains("用户")||msg.contains("账号"))u.requestFocus();else if(msg.contains("密码"))p.requestFocus();
                    }
                }catch(Exception ex){showError("服务器响应异常");}
            }});
        }}).start();
    }

    private void showError(String msg){e.setText(msg);e.setVisibility(View.VISIBLE);}
    private void startMain(){startActivity(new Intent(this,DashboardActivity.class));finish();}
}

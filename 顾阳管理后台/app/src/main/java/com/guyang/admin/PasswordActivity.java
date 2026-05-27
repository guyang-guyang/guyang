package com.guyang.admin;
import android.app.Activity; import android.os.Bundle; import android.view.View; import android.widget.*;
import org.json.JSONObject;
public class PasswordActivity extends Activity {
    private EditText etOld,etNew,etConfirm;
    protected void onCreate(Bundle s){
        super.onCreate(s);LinearLayout ll=new LinearLayout(this);ll.setOrientation(LinearLayout.VERTICAL);ll.setPadding(32,48,32,32);ll.setBackgroundColor(0xFFF8FAF0);
        TextView t=new TextView(this);t.setText("修改管理员密码");t.setTextSize(20);t.setTextColor(0xFF1A1C18);t.setPadding(0,0,0,24);ll.addView(t);
        etOld=new EditText(this);etOld.setHint("当前密码");etOld.setInputType(0x81);ll.addView(etOld);
        etNew=new EditText(this);etNew.setHint("新密码");etNew.setInputType(0x81);ll.addView(etNew);
        etConfirm=new EditText(this);etConfirm.setHint("确认新密码");etConfirm.setInputType(0x81);ll.addView(etConfirm);
        for(EditText et:new EditText[]{etOld,etNew,etConfirm}){et.setTextSize(15);et.setPadding(16,16,16,16);et.setBackground(getResources().getDrawable(R.drawable.input_bg));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,16);et.setLayoutParams(lp);}
        Button btn=new Button(this);btn.setText("修改密码");btn.setTextSize(16);btn.setTextColor(0xFFFFFFFF);btn.setBackground(getResources().getDrawable(R.drawable.btn_bg));btn.setOnClickListener(v->change());ll.addView(btn);
        setContentView(ll);
    }
    private void change(){String old=etOld.getText().toString().trim();String nw=etNew.getText().toString().trim();String cf=etConfirm.getText().toString().trim();if(old.isEmpty()||nw.isEmpty()){Toast.makeText(this,"请填写完整",Toast.LENGTH_SHORT).show();return;}if(!nw.equals(cf)){Toast.makeText(this,"两次密码不一致",Toast.LENGTH_SHORT).show();return;}new Thread(()->{String r=ApiClient.changePassword(old,nw);runOnUiThread(()->{try{JSONObject j=new JSONObject(r);Toast.makeText(this,j.optInt("code",-1)==0?"密码已修改":"失败: "+j.optString("message",""),Toast.LENGTH_SHORT).show();if(j.optInt("code",-1)==0)finish();}catch(Exception e){Toast.makeText(this,"操作失败",Toast.LENGTH_SHORT).show();}});}).start();}
}

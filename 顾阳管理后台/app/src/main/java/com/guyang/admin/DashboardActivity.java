package com.guyang.admin;
import android.app.Activity; import android.content.*; import android.os.Bundle; import android.widget.*;
import org.json.JSONArray; import org.json.JSONObject;
import java.util.Calendar;

public class DashboardActivity extends Activity {
    private TextView tvWelcome,tvTime,tvTotalUsers,tvTodayNew,tvOnlineApps,tvInfoCount;
    private LinearLayout recentList;

    protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_dashboard);
        tvWelcome=findViewById(R.id.tvWelcome);tvTime=findViewById(R.id.tvTime);
        tvTotalUsers=findViewById(R.id.tvTotalUsers);tvTodayNew=findViewById(R.id.tvTodayNew);
        tvOnlineApps=findViewById(R.id.tvOnlineApps);tvInfoCount=findViewById(R.id.tvInfoCount);
        recentList=findViewById(R.id.recentList);
        int hour=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet;if(hour<6)greet="夜深了";else if(hour<12)greet="上午好";else if(hour<14)greet="中午好";else if(hour<18)greet="下午好";else greet="晚上好";
        tvWelcome.setText("欢迎回来，管理员");tvTime.setText(greet);

        findViewById(R.id.btnAddApp).setOnClickListener(v->startActivity(new Intent(this,AppsActivity.class)));
        findViewById(R.id.btnCards).setOnClickListener(v->startActivity(new Intent(this,CardsActivity.class)));
        findViewById(R.id.btnNews).setOnClickListener(v->startActivity(new Intent(this,InfoActivity.class)));
        findViewById(R.id.btnApps).setOnClickListener(v->startActivity(new Intent(this,AppsActivity.class)));
        findViewById(R.id.btnUsers).setOnClickListener(v->startActivity(new Intent(this,UsersActivity.class)));
        findViewById(R.id.btnBanners).setOnClickListener(v->startActivity(new Intent(this,BannersActivity.class)));
        findViewById(R.id.btnInfo).setOnClickListener(v->startActivity(new Intent(this,InfoActivity.class)));
        findViewById(R.id.btnImages).setOnClickListener(v->startActivity(new Intent(this,ImagesActivity.class)));
        findViewById(R.id.btnUpload).setOnClickListener(v->startActivity(new Intent(this,UploadActivity.class)));
        findViewById(R.id.btnSettings).setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        findViewById(R.id.btnPassword).setOnClickListener(v->startActivity(new Intent(this,PasswordActivity.class)));
        findViewById(R.id.btnAbout).setOnClickListener(v->startActivity(new Intent(this,AboutActivity.class)));
        loadStats();
    }

    private void loadStats(){
        new Thread(()->{
            String r=ApiClient.getStats();
            runOnUiThread(()->{
                try{
                    JSONObject j=new JSONObject(r);JSONObject d=j.optJSONObject("data");if(d==null)d=j;
                    JSONObject ov=d.optJSONObject("overview");
                    if(ov!=null){tvTotalUsers.setText(String.valueOf(ov.optInt("total_users",0)));tvTodayNew.setText(String.valueOf(ov.optInt("new_users_today",0)));tvOnlineApps.setText(String.valueOf(ov.optInt("online_apps",0)));tvInfoCount.setText(String.valueOf(ov.optInt("online_info",0)));}
                    JSONObject tr=d.optJSONObject("traffic");
                    if(tr!=null){recentList.removeAllViews();
                        for(String item:new String[]{"总下载量: "+tr.optInt("total_downloads",0),"今日下载: "+tr.optInt("downloads_today",0),"资讯浏览量: "+tr.optInt("total_info_views",0)}){
                            TextView tv=new TextView(this);tv.setText("• "+item);tv.setTextSize(13);tv.setTextColor(0xFF6B7266);tv.setPadding(0,8,0,8);recentList.addView(tv);}}
                }catch(Exception e){}
            });
        }).start();
    }

    @Override protected void onResume(){super.onResume();loadStats();}
}

package com.guyang.admin;
import android.app.Activity; import android.os.Bundle; import android.widget.*;
public class AboutActivity extends Activity {
    protected void onCreate(Bundle s){
        super.onCreate(s);LinearLayout ll=new LinearLayout(this);ll.setOrientation(LinearLayout.VERTICAL);ll.setGravity(android.view.Gravity.CENTER);ll.setPadding(32,64,32,32);ll.setBackgroundColor(0xFFF8FAF0);
        TextView t1=new TextView(this);t1.setText("顾阳管理后台");t1.setTextSize(24);t1.setTextColor(0xFF416835);t1.setTypeface(null,android.graphics.Typeface.BOLD);ll.addView(t1);
        TextView t2=new TextView(this);t2.setText("v1.0");t2.setTextSize(14);t2.setTextColor(0xFF6B7266);t2.setPadding(0,8,0,24);ll.addView(t2);
        TextView t3=new TextView(this);t3.setText("顾阳软件盒管理后台App\n管理应用/用户/卡密/资讯/Banner等\n所有字段以用户端为准");t3.setTextSize(13);t3.setTextColor(0xFF6B7266);t3.setGravity(android.view.Gravity.CENTER);t3.setLineSpacing(4,1);ll.addView(t3);
        setContentView(ll);
    }
}

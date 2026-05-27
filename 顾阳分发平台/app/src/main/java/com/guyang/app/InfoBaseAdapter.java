package com.guyang.app;
import android.app.Activity;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class InfoBaseAdapter extends BaseAdapter {
    private Activity ctx; private List<JSONObject> list;
    public InfoBaseAdapter(Activity c, List<JSONObject> l){ctx=c;list=l;}
    public int getCount(){return list.size();}
    public Object getItem(int i){return list.get(i);}
    public long getItemId(int i){return i;}
    public View getView(int i, View v, ViewGroup p){
        if(v==null)v=ctx.getLayoutInflater().inflate(R.layout.item_info,null);
        ImageView iv=v.findViewById(R.id.info_cover);
        TextView t1=v.findViewById(R.id.info_title);
        TextView t2=v.findViewById(R.id.info_meta);
        TextView t3=v.findViewById(R.id.info_price);
        JSONObject j=list.get(i);
        t1.setText(j.optString("title",""));
        String summary=j.optString("summary","");
        if(summary.isEmpty())summary=j.optString("content_preview","");
        t2.setText(summary);
        int price=j.optInt("price",0);
        t3.setText(price==0?"免费":price+"积分");
        String cover=j.optString("cover_img","");
        if(cover!=null&&!cover.isEmpty()){
            loadImg(iv,cover);
        }
        v.setOnClickListener(v2->{
            Intent it=new Intent(ctx,InfoDetailActivity.class);
            it.putExtra("info_id",j.optString("id",""));
            ctx.startActivity(it);
        });
        return v;
    }
    private void loadImg(ImageView iv,String url){
        new Thread(()->{
            try{
                URL u=new URL(url);
                HttpURLConnection c=(HttpURLConnection)u.openConnection();
                c.setConnectTimeout(8000);
                InputStream is=c.getInputStream();
                Bitmap b=BitmapFactory.decodeStream(is);
                is.close();
                ctx.runOnUiThread(()->{if(b!=null)iv.setImageBitmap(b);});
            }catch(Exception ignored){}
        }).start();
    }
}

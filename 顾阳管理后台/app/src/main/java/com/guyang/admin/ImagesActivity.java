package com.guyang.admin;
import android.app.Activity; import android.content.*; import android.graphics.Bitmap; import android.graphics.BitmapFactory;
import android.os.Bundle; import android.view.*; import android.widget.*; import org.json.JSONArray; import org.json.JSONObject;
import java.io.*; import java.net.*; import java.util.*;
public class ImagesActivity extends Activity {
    private ListView list; private List<JSONObject> images=new ArrayList<>(); private Map<String,Bitmap> cache=new HashMap<>();
    protected void onCreate(Bundle s){
        super.onCreate(s);LinearLayout ll=new LinearLayout(this);ll.setOrientation(LinearLayout.VERTICAL);ll.setBackgroundColor(0xFFF8FAF0);ll.setPadding(8,8,8,8);
        TextView t=new TextView(this);t.setText("图片库（点击复制直链）");t.setTextSize(16);t.setTextColor(0xFF1A1C18);t.setPadding(8,24,8,12);t.setTypeface(null,android.graphics.Typeface.BOLD);ll.addView(t);
        list=new ListView(this);list.setDividerHeight(1);list.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
        list.setOnItemClickListener((p,v,pos,id)->{String url=images.get(pos).optString("url","");ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("url",url));Toast.makeText(this,"已复制直链",Toast.LENGTH_SHORT).show();});ll.addView(list);
        Button btn=new Button(this);btn.setText("+ 上传图片");btn.setTextSize(14);btn.setTextColor(0xFFFFFFFF);btn.setBackground(getResources().getDrawable(R.drawable.btn_bg));btn.setOnClickListener(v->startActivity(new Intent(this,UploadActivity.class)));ll.addView(btn);
        setContentView(ll);load();
    }
    private void load(){new Thread(()->{String r=ApiClient.getImages();runOnUiThread(()->{try{JSONObject j=new JSONObject(r);JSONObject d=j.optJSONObject("data");JSONArray arr=d!=null?d.optJSONArray("list"):null;if(arr==null)arr=new JSONArray();images.clear();for(int i=0;i<arr.length();i++){JSONObject img=arr.optJSONObject(i);if(img!=null)images.add(img);}list.setAdapter(new ImageAdapter());}catch(Exception e){}});}).start();}
    class ImageAdapter extends BaseAdapter {
        public int getCount(){return images.size();}public Object getItem(int p){return images.get(p);}public long getItemId(int p){return p;}
        public View getView(int p,View v,ViewGroup parent){
            if(v==null)v=getLayoutInflater().inflate(R.layout.item_image,parent,false);
            JSONObject img=images.get(p);String url=img.optString("url","");
            ImageView iv=v.findViewById(R.id.ivThumb);((TextView)v.findViewById(R.id.tvFilename)).setText(img.optString("filename",""));((TextView)v.findViewById(R.id.tvSize)).setText(img.optString("size_text",""));((TextView)v.findViewById(R.id.tvUrl)).setText(url);
            iv.setImageBitmap(null);
            if(cache.containsKey(url)){iv.setImageBitmap(cache.get(url));}else{iv.setImageResource(android.R.drawable.ic_menu_gallery);
                new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(10000);Bitmap bm=BitmapFactory.decodeStream(c.getInputStream());if(bm!=null){cache.put(url,bm);runOnUiThread(()->iv.setImageBitmap(bm));}}catch(Exception e){}}).start();}
            return v;
        }
    }
    @Override protected void onResume(){super.onResume();load();}
}

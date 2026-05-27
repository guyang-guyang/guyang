package com.guyang.admin;
import android.app.Activity; import android.app.AlertDialog; import android.content.*; import android.database.Cursor;
import android.net.Uri; import android.os.Bundle; import android.provider.OpenableColumns; import android.view.*; import android.widget.*;
import org.json.JSONArray; import org.json.JSONObject; import java.io.*; import java.util.*;

public class InfoActivity extends Activity {
    private ListView list; private EditText search; private List<JSONObject> all=new ArrayList<>(),fil=new ArrayList<>();
    private InfoAdapter adapter; private String pendingCover; private static final int REQ_IMG=100;

    protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_info);list=findViewById(R.id.listInfo);search=findViewById(R.id.etSearch);
        adapter=new InfoAdapter();list.setAdapter(adapter);
        list.setOnItemClickListener((p,v,pos,id)->showEditDialog(fil.get(pos)));
        list.setOnItemLongClickListener((p,v,pos,id)->{confirmDelete(fil.get(pos));return true;});
        search.addTextChangedListener(new android.text.TextWatcher(){public void onTextChanged(CharSequence c,int a,int b,int d){filter();}public void beforeTextChanged(CharSequence c,int a,int b,int d){}public void afterTextChanged(android.text.Editable e){}});
        findViewById(R.id.btnAdd).setOnClickListener(v->showEditDialog(null));load();
    }
    private void load(){new Thread(()->{String r=ApiClient.getInfoList();runOnUiThread(()->{try{JSONArray arr=new JSONObject(r).optJSONArray("data");if(arr==null)arr=new JSONArray();all.clear();for(int i=0;i<arr.length();i++){JSONObject info=arr.optJSONObject(i);if(info!=null)all.add(info);}filter();}catch(Exception e){}});}).start();}
    private void filter(){String kw=search.getText().toString().trim().toLowerCase();fil.clear();for(JSONObject i:all){if(!kw.isEmpty()&&!i.optString("title","").toLowerCase().contains(kw))continue;fil.add(i);}adapter.notifyDataSetChanged();}
    private void showEditDialog(JSONObject info){
        boolean isNew=(info==null);final JSONObject ci=(info==null)?new JSONObject():info;pendingCover=null;
        LinearLayout ll=new LinearLayout(this);ll.setOrientation(LinearLayout.VERTICAL);ll.setPadding(32,16,32,16);
        EditText etTitle=ed(ll,ci.optString("title",""),"标题");
        LinearLayout cr=new LinearLayout(this);cr.setOrientation(LinearLayout.HORIZONTAL);
        EditText etCover=ed(null,ci.optString("cover_img",""),"封面图URL");etCover.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));cr.addView(etCover);
        Button btnImg=new Button(this);btnImg.setText("上传");btnImg.setTextSize(12);btnImg.setTextColor(0xFFFFFFFF);btnImg.setBackground(getResources().getDrawable(R.drawable.btn_bg));
        btnImg.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQ_IMG);});cr.addView(btnImg);ll.addView(cr);
        EditText etSum=ed(ll,ci.optString("summary",""),"摘要");EditText etCont=ed(ll,ci.optString("content",ci.optString("desc","")),"正文内容");etCont.setMinLines(5);
        EditText etCat=ed(ll,ci.optString("category",""),"分类");EditText etPrice=ed(ll,String.valueOf(ci.optInt("price",0)),"价格(积分)");etPrice.setInputType(2);
        EditText etPan=ed(ll,ci.optString("pan_url",ci.optString("cloud_link","")),"网盘链接(可选)");
        new AlertDialog.Builder(this).setTitle(isNew?"发布资讯":"编辑资讯").setView(ll)
            .setPositiveButton("保存",(d,w)->{
                try{JSONObject b=new JSONObject();b.put("title",etTitle.getText().toString().trim());String cov=etCover.getText().toString().trim();if(cov.isEmpty()&&pendingCover!=null)cov=pendingCover;b.put("cover_img",cov);b.put("summary",etSum.getText().toString().trim());b.put("content",etCont.getText().toString().trim());b.put("category",etCat.getText().toString().trim());b.put("price",Integer.parseInt(etPrice.getText().toString().trim()));b.put("pan_url",etPan.getText().toString().trim());if(!isNew)b.put("id",ci.optString("id"));save(isNew,b);}catch(Exception e){Toast.makeText(this,"输入有误",Toast.LENGTH_SHORT).show();}
            }).setNegativeButton("取消",null).show();
    }
    private EditText ed(LinearLayout l,String v,String h){EditText e=new EditText(this);e.setText(v);e.setHint(h);if(l!=null)l.addView(e);return e;}
    protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);if(req==REQ_IMG&&res==RESULT_OK&&data!=null){
            Toast.makeText(this,"上传中...",Toast.LENGTH_SHORT).show();Uri uri=data.getData();
            new Thread(()->{try{InputStream is=getContentResolver().openInputStream(uri);File tmp=new File(getCacheDir(),"img_"+System.currentTimeMillis()+".jpg");FileOutputStream fos=new FileOutputStream(tmp);byte[] buf=new byte[8192];int n;while((n=is.read(buf))>0)fos.write(buf,0,n);fos.close();is.close();String r=ApiClient.uploadFile(tmp.getAbsolutePath());tmp.delete();JSONObject j=new JSONObject(r);if(j.optInt("code",-1)==0){String url="";if(j.optJSONObject("data")!=null)url=j.optJSONObject("data").optString("url","");if(url.isEmpty())url=j.optString("url","");pendingCover=url;runOnUiThread(()->Toast.makeText(this,"已上传，点击保存生效",Toast.LENGTH_SHORT).show());}else{runOnUiThread(()->Toast.makeText(this,"上传失败",Toast.LENGTH_SHORT).show());}}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"上传错误",Toast.LENGTH_SHORT).show());}}).start();
        }
    }
    private void save(boolean isNew,JSONObject b){new Thread(()->{String r=isNew?ApiClient.createInfo(b):ApiClient.updateInfo(b);runOnUiThread(()->{Toast.makeText(this,r.contains("\"code\":0")?"保存成功":"保存失败",Toast.LENGTH_SHORT).show();load();});}).start();}
    private void confirmDelete(JSONObject info){new AlertDialog.Builder(this).setTitle("删除").setMessage("确定删除「"+info.optString("title","")+"」？").setPositiveButton("删除",(d,w)->{new Thread(()->{String r=ApiClient.get("/admin/info.php?id="+info.optString("id")+"&_method=DELETE");runOnUiThread(()->{Toast.makeText(this,r.contains("\"code\":0")?"已删除":"失败",Toast.LENGTH_SHORT).show();load();});}).start();}).setNegativeButton("取消",null).show();}
    class InfoAdapter extends BaseAdapter {
        public int getCount(){return fil.size();}public Object getItem(int p){return fil.get(p);}public long getItemId(int p){return p;}
        public View getView(int p,View v,ViewGroup parent){
            if(v==null)v=getLayoutInflater().inflate(R.layout.item_info,parent,false);
            JSONObject info=fil.get(p);
            ((TextView)v.findViewById(R.id.tvTitle)).setText(info.optString("title","无标题"));
            ((TextView)v.findViewById(R.id.tvCat)).setText(info.optString("category","未分类"));
            ((TextView)v.findViewById(R.id.tvMeta)).setText("浏览:"+info.optInt("views",0)+" | "+info.optInt("price",0)+"积分");
            TextView st=v.findViewById(R.id.tvStatus);String s=info.optString("status","online");st.setText("online".equals(s)?"上架":"下架");st.setTextColor("online".equals(s)?0xFF416835:0xFFBA1A1A);
            return v;
        }
    }
    @Override protected void onResume(){super.onResume();load();}
}

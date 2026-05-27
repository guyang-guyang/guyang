package com.guyang.admin;
import android.app.Activity; import android.app.AlertDialog; import android.content.*; import android.database.Cursor;
import android.graphics.BitmapFactory; import android.net.Uri; import android.os.Bundle;
import android.provider.OpenableColumns; import android.text.TextUtils; import android.view.*; import android.widget.*;
import org.json.JSONArray; import org.json.JSONObject;
import java.io.*; import java.util.*;

public class AppsActivity extends Activity {
    private ListView list; private EditText search; private Spinner category;
    private List<JSONObject> allApps=new ArrayList<>(), filtered=new ArrayList<>();
    private List<String> categories=new ArrayList<>(); private AppAdapter adapter;
    private Uri editIconUri; private String editIconName, editIconUrl;

    protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_apps);
        list=findViewById(R.id.listApps);search=findViewById(R.id.etSearch);category=findViewById(R.id.spCategory);
        findViewById(R.id.btnAdd).setOnClickListener(v->startActivity(new Intent(this,ApkUploadActivity.class)));
        adapter=new AppAdapter();list.setAdapter(adapter);
        list.setOnItemClickListener((p,v,pos,id)->showEditDialog(filtered.get(pos)));
        list.setOnItemLongClickListener((p,v,pos,id)->{confirmDelete(filtered.get(pos));return true;});
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void onTextChanged(CharSequence c,int a,int b,int d){filterApps();}
            public void beforeTextChanged(CharSequence c,int a,int b,int d){}
            public void afterTextChanged(android.text.Editable e){}
        });
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){filterApps();}
            public void onNothingSelected(AdapterView<?> p){}
        });
        loadApps();
    }
    private void loadApps(){
        new Thread(()->{
            String r=ApiClient.getApps();
            runOnUiThread(()->{
                try{
                    JSONArray arr=new JSONObject(r).optJSONArray("data");if(arr==null)arr=new JSONArray();
                    allApps.clear();categories.clear();categories.add("全部分类");
                    Set<String> cs=new LinkedHashSet<>();
                    for(int i=0;i<arr.length();i++){JSONObject a=arr.optJSONObject(i);if(a!=null){allApps.add(a);String c=a.optString("category","");if(!c.isEmpty())cs.add(c);}}
                    categories.addAll(cs);
                    ArrayAdapter<String> ca=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,categories);
                    ca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);category.setAdapter(ca);
                    filterApps();
                }catch(Exception e){Toast.makeText(this,"加载失败",Toast.LENGTH_SHORT).show();}
            });
        }).start();
    }
    private void filterApps(){
        String kw=search.getText().toString().trim().toLowerCase();
        String cat=category.getSelectedItem()!=null?category.getSelectedItem().toString():"";
        if("全部分类".equals(cat))cat="";filtered.clear();
        for(JSONObject a:allApps){String n=a.optString("name","").toLowerCase();String ac=a.optString("category","");if(!kw.isEmpty()&&!n.contains(kw))continue;if(!cat.isEmpty()&&!cat.equals(ac))continue;filtered.add(a);}
        adapter.notifyDataSetChanged();
    }
    private void showEditDialog(JSONObject app){
        editIconUri=null;editIconName=null;editIconUrl=app.optString("icon","");
        LinearLayout ll=new LinearLayout(this);ll.setOrientation(LinearLayout.VERTICAL);ll.setPadding(32,16,32,16);
        EditText etName=ed(ll,app.optString("name",""),"应用名称");EditText etVer=ed(ll,app.optString("version",""),"版本");
        EditText etSize=ed(ll,app.optString("size",""),"大小");EditText etCat=ed(ll,app.optString("category",""),"分类");
        EditText etPrice=ed(ll,String.valueOf(app.optInt("price",0)),"价格(积分)");etPrice.setInputType(2);
        EditText etDesc=ed(ll,app.optString("desc",""),"描述");etDesc.setMinLines(3);
        EditText etPan=ed(ll,app.optString("browser_url",app.optString("pan_url","")),"购买/跳转地址");
        // Icon picker
        TextView tvIcon=new TextView(this);tvIcon.setText("图标:");tvIcon.setTextSize(13);tvIcon.setTextColor(0xFF6B7266);ll.addView(tvIcon);
        LinearLayout iconRow=new LinearLayout(this);iconRow.setOrientation(LinearLayout.HORIZONTAL);iconRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView iv=new ImageView(this);iv.setLayoutParams(new LinearLayout.LayoutParams(64,64));iv.setBackgroundColor(0xFFE0E4D9);
        if(editIconUrl!=null&&!editIconUrl.isEmpty()){
            new Thread(()->{try{java.net.URL u=new java.net.URL(editIconUrl);android.graphics.Bitmap bm=BitmapFactory.decodeStream(u.openStream());runOnUiThread(()->iv.setImageBitmap(bm));}catch(Exception e){}}).start();
        }
        iconRow.addView(iv);
        Button btnIcon=new Button(this);btnIcon.setText("选择图片");btnIcon.setTextSize(13);btnIcon.setPadding(16,8,16,8);
        btnIcon.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,100);});
        iconRow.addView(btnIcon);ll.addView(iconRow);
        new AlertDialog.Builder(this).setTitle("编辑应用").setView(ll)
            .setPositiveButton("保存",(d,w)->{
                new Thread(()->{
                    try{
                        // Upload icon if new one selected
                        String iconUrl=editIconUrl;
                        if(editIconUri!=null){
                            iconUrl=uploadImageSync(editIconUri,editIconName);
                            if(iconUrl.isEmpty())iconUrl=editIconUrl;
                        }
                        JSONObject b=new JSONObject();b.put("id",app.optString("id"));b.put("name",etName.getText().toString().trim());
                        b.put("version",etVer.getText().toString().trim());b.put("size",etSize.getText().toString().trim());
                        b.put("category",etCat.getText().toString().trim());b.put("price",Integer.parseInt(etPrice.getText().toString().trim()));
                        b.put("desc",etDesc.getText().toString().trim());b.put("browser_url",etPan.getText().toString().trim());
                        b.put("icon",iconUrl);
                        final String ic=iconUrl;
                        String r=ApiClient.updateApp(b);
                        runOnUiThread(()->{Toast.makeText(this,r.contains("\"code\":0")?"保存成功":"保存失败",Toast.LENGTH_SHORT).show();loadApps();});
                    }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"输入有误",Toast.LENGTH_SHORT).show());}
                }).start();
            }).setNeutralButton("上下架",(d,w)->{
                try{JSONObject b=new JSONObject();b.put("id",app.optString("id"));String st=app.optString("status","online");b.put("status","online".equals(st)?"offline":"online");updateApp(b);}catch(Exception e){}
            }).setNegativeButton("取消",null).show();
    }
    private String uploadImageSync(Uri uri,String fname){
        try{
            InputStream is=getContentResolver().openInputStream(uri);
            File tmp=new File(getCacheDir(),fname!=null?fname:"icon.png");
            FileOutputStream fos=new FileOutputStream(tmp);
            byte[] buf=new byte[8192];int n;while((n=is.read(buf))>0)fos.write(buf,0,n);fos.close();is.close();
            String ur=ApiClient.uploadFile(tmp.getAbsolutePath());tmp.delete();
            JSONObject uj=new JSONObject(ur);
            if(uj.optInt("code",-1)==0){
                JSONObject d=uj.optJSONObject("data");
                if(d!=null)return d.optString("url","");
            }
        }catch(Exception e){}
        return "";
    }
    protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==100&&res==RESULT_OK&&data!=null){
            editIconUri=data.getData();
            Cursor c=getContentResolver().query(editIconUri,null,null,null,null);
            if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)editIconName=c.getString(i);c.close();}
        }
    }
    private EditText ed(LinearLayout l,String v,String h){EditText e=new EditText(this);e.setText(v);e.setHint(h);l.addView(e);return e;}
    private void updateApp(JSONObject b){new Thread(()->{String r=ApiClient.updateApp(b);runOnUiThread(()->{Toast.makeText(this,r.contains("\"code\":0")?"保存成功":"保存失败",Toast.LENGTH_SHORT).show();loadApps();});}).start();}
    private void confirmDelete(JSONObject app){
        new AlertDialog.Builder(this).setTitle("删除").setMessage("确定删除「"+app.optString("name","")+"」？")
            .setPositiveButton("删除",(d,w)->{new Thread(()->{String r=ApiClient.deleteApp(app.optString("id"));runOnUiThread(()->{Toast.makeText(this,r.contains("\"code\":0")?"已删除":"失败",Toast.LENGTH_SHORT).show();loadApps();});}).start();}).setNegativeButton("取消",null).show();
    }
    class AppAdapter extends BaseAdapter {
        public int getCount(){return filtered.size();}public Object getItem(int p){return filtered.get(p);}public long getItemId(int p){return p;}
        public View getView(int p,View v,ViewGroup parent){
            if(v==null)v=getLayoutInflater().inflate(R.layout.item_app,parent,false);
            JSONObject a=filtered.get(p);
            ((TextView)v.findViewById(R.id.tvName)).setText(a.optString("name",""));
            ((TextView)v.findViewById(R.id.tvInfo)).setText(a.optString("version","")+" | "+a.optString("size","")+" | 下载:"+a.optInt("downloads",0));
            ((TextView)v.findViewById(R.id.tvCat)).setText(a.optString("category",""));
            TextView st=v.findViewById(R.id.tvStatus);String s=a.optString("status","online");st.setText("online".equals(s)?"上架":"下架");st.setTextColor("online".equals(s)?0xFF416835:0xFFBA1A1A);
            return v;
        }
    }
    @Override protected void onResume(){super.onResume();loadApps();}
}

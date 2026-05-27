package com.guyang.admin;
import android.app.Activity; import android.app.AlertDialog; import android.os.Bundle;
import android.view.*; import android.widget.*; import org.json.JSONArray; import org.json.JSONObject;
import java.util.*;
public class UsersActivity extends Activity {
    private ListView list; private EditText search; private TextView tvTotal;
    private List<JSONObject> all=new ArrayList<>(),fil=new ArrayList<>(); private UserAdapter adapter;
    protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_users);
        list=findViewById(R.id.listUsers);search=findViewById(R.id.etSearch);tvTotal=findViewById(R.id.tvTotal);
        adapter=new UserAdapter();list.setAdapter(adapter);
        list.setOnItemClickListener((p,v,pos,id)->showEditDialog(fil.get(pos)));
        search.addTextChangedListener(new android.text.TextWatcher(){public void onTextChanged(CharSequence c,int a,int b,int d){filter();}public void beforeTextChanged(CharSequence c,int a,int b,int d){}public void afterTextChanged(android.text.Editable e){}});
        load();
    }
    private void load(){new Thread(()->{String r=ApiClient.getUsers();runOnUiThread(()->{try{JSONObject j=new JSONObject(r);JSONObject d=j.optJSONObject("data");JSONArray arr=d!=null?d.optJSONArray("list"):null;if(arr==null)arr=new JSONArray();all.clear();for(int i=0;i<arr.length();i++){JSONObject u=arr.optJSONObject(i);if(u!=null)all.add(u);}tvTotal.setText("共 "+(d!=null?d.optInt("total",all.size()):all.size())+" 人");filter();}catch(Exception e){}});}).start();}
    private void filter(){String kw=search.getText().toString().trim().toLowerCase();fil.clear();for(JSONObject u:all){if(!kw.isEmpty()){String n=u.optString("username","").toLowerCase();String id=u.optString("id","").toLowerCase();if(!n.contains(kw)&&!id.contains(kw))continue;}fil.add(u);}adapter.notifyDataSetChanged();}
    private void showEditDialog(JSONObject u){
        LinearLayout ll=new LinearLayout(this);ll.setOrientation(LinearLayout.VERTICAL);ll.setPadding(32,16,32,16);
        TextView tv=new TextView(this);tv.setText("用户: "+u.optString("username","")+" | 积分: "+u.optInt("points",0));tv.setTextSize(14);tv.setTextColor(0xFF1A1C18);tv.setPadding(0,0,0,16);ll.addView(tv);
        EditText et=new EditText(this);et.setHint("调整积分 (+加/-减)");et.setInputType(0x1000|2);et.setPadding(12,12,12,12);et.setBackground(getResources().getDrawable(R.drawable.input_bg));ll.addView(et);
        new AlertDialog.Builder(this).setTitle("调整积分").setView(ll)
            .setPositiveButton("确认",(d,w)->{try{int pts=Integer.parseInt(et.getText().toString().trim());JSONObject b=new JSONObject();b.put("id",u.optString("id"));b.put("points",pts);b.put("action","adjust");update(b);}catch(Exception e){Toast.makeText(this,"请输入有效数字",Toast.LENGTH_SHORT).show();}})
            .setNeutralButton("设为0",(d,w)->{try{JSONObject b=new JSONObject();b.put("id",u.optString("id"));b.put("points",0);b.put("action","reset");update(b);}catch(Exception e){}})
            .setNegativeButton("取消",null).show();
    }
    private void update(JSONObject b){new Thread(()->{String r=ApiClient.updateUser(b);runOnUiThread(()->{Toast.makeText(this,r.contains("\"code\":0")?"操作成功":"操作失败",Toast.LENGTH_SHORT).show();load();});}).start();}
    class UserAdapter extends BaseAdapter {
        public int getCount(){return fil.size();}public Object getItem(int p){return fil.get(p);}public long getItemId(int p){return p;}
        public View getView(int p,View v,ViewGroup parent){
            if(v==null)v=getLayoutInflater().inflate(R.layout.item_user,parent,false);
            JSONObject u=fil.get(p);
            ((TextView)v.findViewById(R.id.tvUsername)).setText(u.optString("username","未知"));
            ((TextView)v.findViewById(R.id.tvUserId)).setText("ID: "+u.optString("id",""));
            ((TextView)v.findViewById(R.id.tvPoints)).setText(u.optInt("points",0)+" 积分");
            TextView st=v.findViewById(R.id.tvStatus);String s=u.optString("status","active");st.setText("active".equals(s)?"正常":"禁用");st.setTextColor("active".equals(s)?0xFF416835:0xFFBA1A1A);
            return v;
        }
    }
    @Override protected void onResume(){super.onResume();load();}
}

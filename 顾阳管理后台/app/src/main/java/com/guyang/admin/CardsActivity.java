package com.guyang.admin;
import android.app.Activity; import android.app.AlertDialog; import android.content.*; import android.os.Bundle;
import android.view.*; import android.widget.*; import org.json.JSONArray; import org.json.JSONObject;
import java.util.*;
public class CardsActivity extends Activity {
    private ListView list; private EditText search; private TextView tvTotal;
    private List<JSONObject> all=new ArrayList<>(),fil=new ArrayList<>(); private CardAdapter adapter;
    protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_cards);
        list=findViewById(R.id.listCards);search=findViewById(R.id.etSearch);tvTotal=findViewById(R.id.tvTotal);
        adapter=new CardAdapter();list.setAdapter(adapter);
        search.addTextChangedListener(new android.text.TextWatcher(){public void onTextChanged(CharSequence c,int a,int b,int d){filter();}public void beforeTextChanged(CharSequence c,int a,int b,int d){}public void afterTextChanged(android.text.Editable e){}});
        findViewById(R.id.btnGenerate).setOnClickListener(v->showGen());load();
    }
    private void load(){new Thread(()->{String r=ApiClient.getCards();runOnUiThread(()->{try{JSONObject j=new JSONObject(r);JSONObject d=j.optJSONObject("data");JSONArray arr=d!=null?d.optJSONArray("list"):null;if(arr==null)arr=new JSONArray();all.clear();for(int i=0;i<arr.length();i++){JSONObject c=arr.optJSONObject(i);if(c!=null)all.add(c);}tvTotal.setText("共 "+(d!=null?d.optInt("total",all.size()):all.size())+" 张");filter();}catch(Exception e){}});}).start();}
    private void filter(){String kw=search.getText().toString().trim().toLowerCase();fil.clear();for(JSONObject c:all){if(!kw.isEmpty()&&!c.optString("code","").toLowerCase().contains(kw))continue;fil.add(c);}adapter.notifyDataSetChanged();}
    private void showGen(){
        LinearLayout ll=new LinearLayout(this);ll.setOrientation(LinearLayout.VERTICAL);ll.setPadding(32,16,32,16);
        EditText ec=new EditText(this);ec.setHint("数量(1-100)");ec.setInputType(2);ll.addView(ec);
        EditText ep=new EditText(this);ep.setHint("每张积分");ep.setInputType(2);ll.addView(ep);
        EditText ee=new EditText(this);ee.setHint("有效期(天,0=永久)");ee.setInputType(2);ll.addView(ee);
        new AlertDialog.Builder(this).setTitle("批量生成卡密").setView(ll)
            .setPositiveButton("生成",(d,w)->{try{int cnt=Integer.parseInt(ec.getText().toString().trim());int pts=Integer.parseInt(ep.getText().toString().trim());int exp=Integer.parseInt(ee.getText().toString().trim());if(cnt<1||cnt>100){Toast.makeText(CardsActivity.this,"数量1-100",Toast.LENGTH_SHORT).show();return;}JSONObject b=new JSONObject();b.put("count",cnt);b.put("points",pts);b.put("expire_days",exp);gen(b);}catch(Exception e){Toast.makeText(CardsActivity.this,"输入有效数字",Toast.LENGTH_SHORT).show();}})
            .setNegativeButton("取消",null).show();
    }
    private void gen(JSONObject b){new Thread(()->{String r=ApiClient.createCard(b);runOnUiThread(()->{Toast.makeText(CardsActivity.this,r.contains("\"code\":0")?"生成成功":"生成失败",Toast.LENGTH_SHORT).show();load();});}).start();}
    class CardAdapter extends BaseAdapter {
        public int getCount(){return fil.size();}public Object getItem(int p){return fil.get(p);}public long getItemId(int p){return p;}
        public View getView(int p,View v,ViewGroup parent){
            if(v==null)v=getLayoutInflater().inflate(R.layout.item_card,parent,false);
            JSONObject c=fil.get(p);String code=c.optString("code","");
            ((TextView)v.findViewById(R.id.tvCode)).setText(code);
            String used="1".equals(c.optString("used",""))||"true".equals(c.optString("used",""))?"已使用":"未使用";
            ((TextView)v.findViewById(R.id.tvInfo)).setText(c.optInt("points",0)+"积分 | "+used+" | 到期:"+c.optString("expire_time","永久"));
            v.findViewById(R.id.btnCopy).setOnClickListener(view->{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("card",code));Toast.makeText(CardsActivity.this,"已复制",Toast.LENGTH_SHORT).show();});
            return v;
        }
    }
    @Override protected void onResume(){super.onResume();load();}
}

package com.guyang.admin;
import android.app.Activity; import android.content.*; import android.net.Uri; import android.os.Bundle;
import android.provider.OpenableColumns; import android.database.Cursor; import android.widget.*; import java.io.*;
public class UploadActivity extends Activity {
    private TextView tvFile; private Uri fileUri; private String fileName;
    protected void onCreate(Bundle s){
        super.onCreate(s);LinearLayout ll=new LinearLayout(this);ll.setOrientation(LinearLayout.VERTICAL);ll.setPadding(32,48,32,32);ll.setBackgroundColor(0xFFF8FAF0);
        TextView t=new TextView(this);t.setText("文件上传");t.setTextSize(20);t.setTextColor(0xFF1A1C18);t.setPadding(0,0,0,24);ll.addView(t);
        Button bp=btn(ll,"选择文件");bp.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,2);});
        tvFile=new TextView(this);tvFile.setText("未选择");tvFile.setTextSize(13);tvFile.setTextColor(0xFF6B7266);tvFile.setPadding(0,12,0,12);ll.addView(tvFile);
        Button bu=btn(ll,"上传");bu.setOnClickListener(v->doUpload());setContentView(ll);
    }
    private Button btn(LinearLayout l,String t){Button b=new Button(this);b.setText(t);b.setTextSize(16);b.setTextColor(0xFFFFFFFF);b.setBackground(getResources().getDrawable(R.drawable.btn_bg));l.addView(b);return b;}
    protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==2&&res==RESULT_OK&&data!=null){fileUri=data.getData();Cursor c=getContentResolver().query(fileUri,null,null,null,null);if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)fileName=c.getString(i);c.close();}tvFile.setText(fileName!=null?fileName:fileUri.getLastPathSegment());}}
    private void doUpload(){if(fileUri==null){Toast.makeText(this,"请选择文件",Toast.LENGTH_SHORT).show();return;}new Thread(()->{try{InputStream is=getContentResolver().openInputStream(fileUri);File tmp=new File(getCacheDir(),fileName!=null?fileName:"upload");FileOutputStream fos=new FileOutputStream(tmp);byte[] buf=new byte[8192];int n;while((n=is.read(buf))>0)fos.write(buf,0,n);fos.close();is.close();String r=ApiClient.uploadFile(tmp.getAbsolutePath());tmp.delete();runOnUiThread(()->{try{org.json.JSONObject j=new org.json.JSONObject(r);Toast.makeText(this,j.optInt("code",-1)==0?"上传成功":"失败",Toast.LENGTH_SHORT).show();if(j.optInt("code",-1)==0)finish();}catch(Exception e){Toast.makeText(this,"上传失败",Toast.LENGTH_SHORT).show();}});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"错误: "+e.getMessage(),Toast.LENGTH_SHORT).show());}}).start();}
}

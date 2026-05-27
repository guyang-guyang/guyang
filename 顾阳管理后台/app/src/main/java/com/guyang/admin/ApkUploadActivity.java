package com.guyang.admin;
import android.app.Activity; import android.content.*; import android.database.Cursor; import android.graphics.BitmapFactory;
import android.net.Uri; import android.os.Bundle; import android.provider.OpenableColumns;
import android.view.View; import android.widget.*;
import java.io.*; import java.net.*; import org.json.JSONObject;

public class ApkUploadActivity extends Activity {
    private static final int REQ_FILE=1, REQ_ICON=2;
    private TextView tvFile,tvProgress; private EditText etName,etVersion,etCategory,etPrice,etDesc,etPan;
    private ProgressBar progress; private Button btnUpload,btnPick;
    private ImageView ivIcon;
    private Uri fileUri, iconUri; private String fileName, iconFileName, iconUrl="";

    protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_apk_upload);
        tvFile=findViewById(R.id.tvFileName);tvProgress=findViewById(R.id.tvProgress);
        etName=findViewById(R.id.etName);etVersion=findViewById(R.id.etVersion);
        etCategory=findViewById(R.id.etCategory);etPrice=findViewById(R.id.etPrice);
        etDesc=findViewById(R.id.etDesc);etPan=findViewById(R.id.etCloud);
        progress=findViewById(R.id.progress);btnPick=findViewById(R.id.btnPickFile);
        btnUpload=findViewById(R.id.btnUpload);ivIcon=findViewById(R.id.ivIcon);
        btnPick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,REQ_FILE);});
        findViewById(R.id.btnPickIcon).setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQ_ICON);});
        btnUpload.setOnClickListener(v->uploadApk());
    }
    protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==REQ_FILE&&res==RESULT_OK&&data!=null){fileUri=data.getData();fileName=getFileName(fileUri);tvFile.setText(fileName);if(etName.getText().toString().isEmpty())etName.setText(fileName.replace(".apk","").replace(".APK",""));}
        if(req==REQ_ICON&&res==RESULT_OK&&data!=null){iconUri=data.getData();iconFileName=getFileName(iconUri);try{ivIcon.setImageBitmap(BitmapFactory.decodeStream(getContentResolver().openInputStream(iconUri)));}catch(Exception e){}}
    }
    private String getFileName(Uri uri){Cursor c=getContentResolver().query(uri,null,null,null,null);if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0){String n=c.getString(i);c.close();return n;}c.close();}return uri.getLastPathSegment();}
    private String uploadImage(Uri uri,String fname){
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
    private void uploadApk(){
        if(fileUri==null){Toast.makeText(this,"请选择APK",Toast.LENGTH_SHORT).show();return;}
        final String name=etName.getText().toString().trim();if(name.isEmpty()){Toast.makeText(this,"请输入应用名称",Toast.LENGTH_SHORT).show();return;}
        btnUpload.setEnabled(false);btnPick.setEnabled(false);progress.setVisibility(View.VISIBLE);tvProgress.setVisibility(View.VISIBLE);tvProgress.setText("上传中...");
        new Thread(()->{
            try{
                // 1. 先上传图标（如果选了）
                if(iconUri!=null){
                    runOnUiThread(()->tvProgress.setText("上传图标..."));
                    iconUrl=uploadImage(iconUri,iconFileName);
                }
                // 2. 上传APK
                InputStream is=getContentResolver().openInputStream(fileUri);File tmp=new File(getCacheDir(),fileName);
                FileOutputStream fos=new FileOutputStream(tmp);byte[] buf=new byte[8192];int n;while((n=is.read(buf))>0)fos.write(buf,0,n);fos.close();is.close();
                final long fs=tmp.length();
                runOnUiThread(()->tvProgress.setText("上传APK ("+(fs/1024/1024)+"MB)..."));
                String ur=ApiClient.uploadFile(tmp.getAbsolutePath());tmp.delete();
                JSONObject uj=new JSONObject(ur);
                if(uj.optInt("code",-1)!=0){runOnUiThread(()->{progress.setVisibility(View.GONE);tvProgress.setVisibility(View.GONE);btnUpload.setEnabled(true);btnPick.setEnabled(true);Toast.makeText(this,"上传失败: "+uj.optString("message",""),Toast.LENGTH_SHORT).show();});return;}
                JSONObject data=uj.optJSONObject("data");
                String fileUrl=data!=null?data.optString("url",""):"";
                if(fileUrl.isEmpty())fileUrl=uj.optString("url","");

                // 3. 从APK提取的图标（只有没手动选图标时才用）
                if(iconUrl.isEmpty()){
                    JSONObject apkInfo=data!=null?data.optJSONObject("apk_info"):null;
                    if(apkInfo!=null){String aicon=apkInfo.optString("icon_url","");if(!aicon.isEmpty())iconUrl=aicon;}
                }

                runOnUiThread(()->tvProgress.setText("创建应用中..."));
                JSONObject ad=new JSONObject();ad.put("name",name);ad.put("version",etVersion.getText().toString().trim());
                ad.put("size",fs);ad.put("category",etCategory.getText().toString().trim());
                ad.put("price",Integer.parseInt(etPrice.getText().toString().trim()));ad.put("desc",etDesc.getText().toString().trim());
                ad.put("file_path",fileUrl);ad.put("browser_url",etPan.getText().toString().trim());
                if(!iconUrl.isEmpty())ad.put("icon",iconUrl);
                String cr=ApiClient.createApp(ad);
                final String fcr=cr;
                runOnUiThread(()->{progress.setVisibility(View.GONE);tvProgress.setVisibility(View.GONE);btnUpload.setEnabled(true);btnPick.setEnabled(true);
                    try{JSONObject j=new JSONObject(fcr);Toast.makeText(this,j.optInt("code",-1)==0?"发布成功":"失败: "+j.optString("message",""),Toast.LENGTH_SHORT).show();if(j.optInt("code",-1)==0)finish();}catch(Exception e){Toast.makeText(this,"发布失败",Toast.LENGTH_SHORT).show();}});
            }catch(Exception e){runOnUiThread(()->{progress.setVisibility(View.GONE);tvProgress.setVisibility(View.GONE);btnUpload.setEnabled(true);btnPick.setEnabled(true);Toast.makeText(this,"错误: "+e.getMessage(),Toast.LENGTH_SHORT).show();});}
        }).start();
    }
}

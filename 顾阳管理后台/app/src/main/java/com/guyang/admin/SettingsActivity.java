package com.guyang.admin;
import android.app.Activity; import android.content.*; import android.net.Uri;
import android.os.Bundle; import android.provider.OpenableColumns; import android.database.Cursor;
import android.view.*; import android.widget.*;
import org.json.JSONObject;

public class SettingsActivity extends Activity {
    private EditText etVersion, etVersionCode, etApkUrl, etApkSize, etUpdateLog;
    private EditText etAppName, etNotice, etRechargeUrl;
    private CheckBox cbForce;
    private Button btnSave, btnPickApk;
    private String pickedApkPath = "";

    protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_settings);
        etAppName=findViewById(R.id.et_app_name);
        etNotice=findViewById(R.id.et_notice);
        etRechargeUrl=findViewById(R.id.et_recharge_url);
        etVersion=findViewById(R.id.et_version);
        etVersionCode=findViewById(R.id.et_version_code);
        etApkUrl=findViewById(R.id.et_apk_url);
        etApkSize=findViewById(R.id.et_apk_size);
        etUpdateLog=findViewById(R.id.et_update_log);
        cbForce=findViewById(R.id.cb_force);
        btnSave=findViewById(R.id.btn_save);
        btnPickApk=findViewById(R.id.btn_pick_apk);

        btnPickApk.setOnClickListener(v->{
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/vnd.android.package-archive");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent,100);
        });

        btnSave.setOnClickListener(v->doSave());
        loadConfig();
    }

    private void loadConfig(){
        new Thread(()->{
            try{
                String r=ApiClient.get("/admin/system_config.php");
                if(r==null||r.startsWith("ERROR:"))return;
                JSONObject d=new JSONObject(r);
                if(d.optInt("code",-1)!=0)return;
                JSONObject data=d.optJSONObject("data");
                if(data==null)return;
                runOnUiThread(()->{
                    etAppName.setText(data.optString("app_name",""));
                    etNotice.setText(data.optString("notice",""));
                    etRechargeUrl.setText(data.optString("recharge_url",""));
                    etVersion.setText(data.optString("app_version",""));
                    etVersionCode.setText(String.valueOf(data.optInt("app_version_code",0)));
                    etApkUrl.setText(data.optString("app_apk_url",""));
                    etApkSize.setText(String.valueOf(data.optInt("app_apk_size",0)));
                    etUpdateLog.setText(data.optString("app_update_log",""));
                    int forceRaw=data.optInt("app_force_update",0);
                    cbForce.setChecked(forceRaw==1||data.optBoolean("app_force_update",false));
                });
            }catch(Exception ignored){}
        }).start();
    }

    private void doSave(){
        new Thread(()->{
            try{
                JSONObject b=new JSONObject();
                b.put("app_name",etAppName.getText().toString().trim());
                b.put("notice",etNotice.getText().toString().trim());
                String rurl=etRechargeUrl.getText().toString().trim();if(!rurl.isEmpty()&&!rurl.startsWith("http"))rurl="https://"+rurl;b.put("recharge_url",rurl);
                b.put("app_version",etVersion.getText().toString().trim());
                b.put("app_version_code",Integer.parseInt(etVersionCode.getText().toString().trim()));
                b.put("app_apk_url",etApkUrl.getText().toString().trim());
                b.put("app_apk_size",Integer.parseInt(etApkSize.getText().toString().trim()));
                b.put("app_update_log",etUpdateLog.getText().toString().trim());
                b.put("app_force_update",cbForce.isChecked()?1:0);
                b.put("app_update_time",java.text.SimpleDateFormat.getDateTimeInstance().format(new java.util.Date()));

                String r=ApiClient.post("/admin/system_config.php",b);
                if(r==null||r.startsWith("ERROR:")){
                    runOnUiThread(()->Toast.makeText(this,"保存失败",Toast.LENGTH_SHORT).show());
                    return;
                }
                JSONObject d=new JSONObject(r);
                int code=d.optInt("code",-1);
                runOnUiThread(()->{
                    if(code==0){
                        Toast.makeText(this,"保存成功",Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(this,"保存失败:"+d.optString("message",""),Toast.LENGTH_SHORT).show();
                    }
                });
            }catch(Exception e){
                runOnUiThread(()->Toast.makeText(this,"保存异常:"+e.getMessage(),Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==100&&res==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();
            try{
                Cursor c=getContentResolver().query(uri,null,null,null,null);
                if(c!=null&&c.moveToFirst()){
                    int ni=c.getColumnIndex(OpenableColumns.SIZE);
                    long size=ni>=0?c.getLong(ni):0;
                    etApkSize.setText(String.valueOf(size));
                    c.close();
                }
                // Upload APK
                new Thread(()->{
                    try{
                        java.io.InputStream is=getContentResolver().openInputStream(uri);
                        String fn="apk_"+System.currentTimeMillis()+".apk";
                        java.io.File tmp=new java.io.File(getCacheDir(),fn);
                        java.io.FileOutputStream fos=new java.io.FileOutputStream(tmp);
                        byte[] buf=new byte[8192];int n;while((n=is.read(buf))>0)fos.write(buf,0,n);fos.close();is.close();
                        String upResp=ApiClient.uploadFile(tmp.getAbsolutePath());tmp.delete();
                        runOnUiThread(()->{
                            try{
                                JSONObject r=new JSONObject(upResp);
                                if(r.optInt("code",-1)==0){
                                    String fileUrl=r.optJSONObject("data").optString("url","");
                                    etApkUrl.setText(fileUrl);
                                    Toast.makeText(this,"APK上传成功",Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(this,"上传失败:"+r.optString("message",""),Toast.LENGTH_SHORT).show();
                                }
                            }catch(Exception e){}
                        });
                    }catch(Exception e){
                        runOnUiThread(()->Toast.makeText(this,"上传异常",Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }catch(Exception ignored){}
        }
    }
}

package com.guyang.app;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import java.io.*;import java.net.*;import org.json.JSONObject;

public class ApiClient{
public static final String S="http://47.108.209.71";public static final String A=S+"/backend/api";
private static String token="";
public static void setToken(String t){token=t;}
public static String getToken(){return token;}

private static String req(String m,String u,String b)throws Exception{
HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
c.setRequestMethod(m);c.setConnectTimeout(10000);c.setReadTimeout(10000);
c.setRequestProperty("Content-Type","application/json");
if(token!=null&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);
if(b!=null){c.setDoOutput(true);c.getOutputStream().write(b.getBytes("UTF-8"));}
int code=c.getResponseCode();
InputStream is=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();
if(is==null)return "ERROR:HTTP "+code;
String r=new String(readAll(is),"UTF-8");
if(code==401){return r;}
return r;
}

private static byte[] readAll(InputStream is)throws IOException{
ByteArrayOutputStream bos=new ByteArrayOutputStream();
byte[] buf=new byte[4096];int n;
while((n=is.read(buf))!=-1)bos.write(buf,0,n);
return bos.toByteArray();
}

public static String get(String u){try{return req("GET",u,null);}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String post(String u,JSONObject b){try{return req("POST",u,b.toString());}catch(Exception e){return"ERROR:"+e.getMessage();}}

public static boolean is401(String resp, Context ctx){
    try{
        JSONObject r=new JSONObject(resp);
        if(r.optInt("code",-1)==401){
            if(ctx!=null){
                Toast.makeText(ctx,"登录已过期，请重新登录",Toast.LENGTH_SHORT).show();
                ctx.getSharedPreferences("app_prefs",Context.MODE_PRIVATE).edit().remove("token").apply();
                ctx.startActivity(new Intent(ctx,LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }
            return true;
        }
    }catch(Exception e){}
    return false;
}

public static String login(String u,String p){try{JSONObject b=new JSONObject();b.put("username",u);b.put("password",p);return post(A+"/user/login.php",b);}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String register(String u,String p,String qq,String inv,String dm,String ds){try{JSONObject b=new JSONObject();b.put("username",u);b.put("password",p);if(qq!=null)b.put("qq",qq);if(inv!=null)b.put("invite_code",inv);b.put("device_model",dm!=null?dm:"");b.put("device_sdk",ds!=null?ds:"");return post(A+"/user/register.php",b);}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String checkin(){try{return post(A+"/user/checkin.php",new JSONObject());}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String userInfo(){try{return get(A+"/user/info.php");}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String apps(String cat){try{String u=A+"/app/apps.php";if(cat!=null&&!cat.isEmpty())u+="?category="+URLEncoder.encode(cat,"UTF-8");return get(u);}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String appDetail(String id){try{return get(A+"/app/detail.php?id="+id);}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String downloadApp(String token, String id){try{JSONObject b=new JSONObject();b.put("app_id",id);return post(A+"/app/download.php",b);}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String banners(){try{return get(A+"/app/banners.php");}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String infoList(){try{return get(A+"/info/list.php");}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String infoDetail(String id){try{return get(A+"/info/detail.php?id="+id+"&token="+token);}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String infoBuy(String id){try{JSONObject b=new JSONObject();b.put("info_id",id);return post(A+"/info/buy.php",b);}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String appDownload(String id){try{return post(A+"/app/download.php",new JSONObject().put("app_id",id));}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String redeemCard(String code){try{JSONObject b=new JSONObject();b.put("code",code);return post(A+"/points/redeem.php",b);}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String update(String ver,int vc){try{return get(A+"/app/update.php?version="+ver+"&version_code="+vc);}catch(Exception e){return"ERROR:"+e.getMessage();}}
// 2.0 新增
public static String shareConfig(){try{return get(A+"/app/share_config.php");}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String shareComplete(){try{return post(A+"/user/share_complete.php",new JSONObject());}catch(Exception e){return"ERROR:"+e.getMessage();}}
public static String inviteStats(){try{return get(A+"/user/invite_stats.php");}catch(Exception e){return"ERROR:"+e.getMessage();}}
}
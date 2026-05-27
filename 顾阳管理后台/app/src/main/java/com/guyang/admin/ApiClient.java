package com.guyang.admin;
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
int code=c.getResponseCode();InputStream is=code>=400?c.getErrorStream():c.getInputStream();
if(is==null)return "{\"code\":"+code+"}";
BufferedReader r=new BufferedReader(new InputStreamReader(is,"UTF-8"));
StringBuilder sb=new StringBuilder();String l;while((l=r.readLine())!=null)sb.append(l);return sb.toString();
}
public static String get(String u){try{return req("GET",A+u,null);}catch(Exception e){return "{\"error\":\""+e.getMessage()+"\"}";}}
public static String post(String u,JSONObject b){try{return req("POST",A+u,b.toString());}catch(Exception e){return "{\"error\":\""+e.getMessage()+"\"}";}}
public static String put(String u,JSONObject b){try{return req("PUT",A+u,b.toString());}catch(Exception e){return "{\"error\":\""+e.getMessage()+"\"}";}}
// Auth
public static String login(String u,String p){try{JSONObject b=new JSONObject();b.put("username",u);b.put("password",p);return post("/admin/login.php",b);}catch(Exception e){return "{\"error\":\""+e.getMessage()+"\"}";}}
public static String changePassword(String o,String n){try{JSONObject b=new JSONObject();b.put("old_password",o);b.put("new_password",n);return post("/admin/password.php",b);}catch(Exception e){return "{\"error\":\""+e.getMessage()+"\"}";}}
// Dashboard
public static String getStats(){return get("/admin/stats.php");}
// Apps
public static String getApps(){return get("/admin/apps.php");}
public static String createApp(JSONObject b){return post("/admin/apps.php",b);}
public static String updateApp(JSONObject b){return put("/admin/apps.php",b);}
public static String deleteApp(String id){return get("/admin/apps.php?id="+id+"&_method=DELETE");}
// Users
public static String getUsers(){return get("/admin/users.php");}
public static String updateUser(JSONObject b){return post("/admin/users.php",b);}
// Cards
public static String getCards(){return get("/admin/cards.php");}
public static String createCard(JSONObject b){return post("/admin/cards.php",b);}
// Info
public static String getInfoList(){return get("/admin/info.php");}
public static String createInfo(JSONObject b){return post("/admin/info.php",b);}
public static String updateInfo(JSONObject b){return put("/admin/info.php",b);}
// Banners
public static String getBanners(){return get("/admin/banners.php");}
public static String createBanner(JSONObject b){return post("/admin/banners.php",b);}
public static String updateBanner(JSONObject b){return put("/admin/banners.php",b);}
// System Config
public static String getSystemConfig(){return get("/admin/system_config.php");}
public static String updateSystemConfig(JSONObject b){return post("/admin/system_config.php",b);}
// Upload
public static String uploadFile(String path)throws Exception{
HttpURLConnection c=(HttpURLConnection)new URL(A+"/admin/upload.php").openConnection();
c.setRequestMethod("POST");c.setConnectTimeout(30000);c.setReadTimeout(120000);
c.setRequestProperty("Content-Type","multipart/form-data; boundary=---BOUNDARY");
if(token!=null&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);
c.setDoOutput(true);
OutputStream os=c.getOutputStream();
File f=new File(path);
os.write(("-----BOUNDARY\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+f.getName()+"\"\r\n\r\n").getBytes());
FileInputStream fi=new FileInputStream(f);byte[]buf=new byte[4096];int n;while((n=fi.read(buf))>0)os.write(buf,0,n);fi.close();
os.write("\r\n-----BOUNDARY--\r\n".getBytes());os.flush();os.close();
BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));
StringBuilder sb=new StringBuilder();String l;while((l=r.readLine())!=null)sb.append(l);return sb.toString();
}
// Images
public static String getImages(){return get("/admin/images.php");}
}

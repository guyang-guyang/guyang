package com.guyang.app;
public class AuthHelper{
    public static String login(String u,String p){return ApiClient.login(u,p);}
    public static String register(String u,String p,String qq,String inv,String dm,String ds){return ApiClient.register(u,p,qq,inv,dm,ds);}
}

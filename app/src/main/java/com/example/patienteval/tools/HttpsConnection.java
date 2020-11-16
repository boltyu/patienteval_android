package com.example.patienteval.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

// https://developer.android.google.cn/training/articles/security-ssl?hl=zh_cn#SelfSigned

public class HttpsConnection{

    private String sessionstr = "";
    private String hostaddress = "47.92.133.87";
    private String urlHost = "";
    private String urlLogin = "", urlLogout = "";
    private String urlTable = "";

    HostnameVerifier hostnameVerifier;
    Context pContext;

    public HttpsConnection(Context context){
        pContext = context;
        SharedPreferences sharedPreferences = pContext.getSharedPreferences("cache",Context.MODE_PRIVATE);
        hostaddress = sharedPreferences.getString("hostaddress",hostaddress);
        sessionstr = sharedPreferences.getString("cookie","");
        initUrl(hostaddress);
        hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                // 不安全的自签名验证
                boolean rrrr = false;
                if( hostname.equals(hostaddress) || hostname.equals("neuroglance.com") )
                    rrrr = true;
                return rrrr;
                //return hv.verify(hostaddress, session);
            }
        };

    }

    public String getHostaddress(){  // self verify

        return hostaddress;
    }

    public void initUrl(String targetaddress){
        hostaddress = targetaddress;
        urlHost = "https://" + hostaddress;
        urlLogin = urlHost + "/api/login";
        urlLogout = urlHost + "/api/logout";
        urlTable = urlHost + "/api/scoring/GetScoringTableDetail";
    }


    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return bytesToHexString(digest.digest());
    }

    public JSONArray getTable(int index){
        try{
            String urlTarget = urlTable + "?id=" + index;
            URL targetUrl = new URL(urlTarget);
            HttpsURLConnection privateconnection = (HttpsURLConnection)targetUrl.openConnection();
            privateconnection.setHostnameVerifier(hostnameVerifier);
            privateconnection.setRequestMethod("GET");
            privateconnection.setConnectTimeout(5000);
            privateconnection.setRequestProperty("Content-Type","application/json;charset=utf-8");
            privateconnection.setRequestProperty("Cookie",sessionstr);
            int rcode = privateconnection.getResponseCode(); // http result code
            if(rcode == 200) {
                InputStream inputStream = privateconnection.getInputStream();
                StringBuilder sb=new StringBuilder();
                int flag;
                byte[] buf=new byte[1048576];  //  应增加防止溢出 1M
                while((flag=inputStream.read(buf))!=-1){
                    sb.append(new String(buf,0,flag));
                }
                //Log.d("httpinfo","recvdata:"+sb.toString());
                int len = 0, total = privateconnection.getContentLength();
                JSONObject re = new JSONObject(sb.toString());
                privateconnection.disconnect();
                if(re.optInt("code") == 0) {    // json result code
                    //return re.optJSONObject("data");    null ptr
                    return re.optJSONArray("data");
                }
            }else{

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    public Boolean loginUser(String username, String password, Boolean iflogout){
        try{
            String urlTarget = urlLogin;
            if(iflogout)
                urlTarget = urlLogout;
            URL targetUrl = new URL(urlTarget);
            HttpsURLConnection privateconnection = (HttpsURLConnection)targetUrl.openConnection();
            privateconnection.setHostnameVerifier(hostnameVerifier);
            privateconnection.setRequestMethod("POST");
            privateconnection.setConnectTimeout(5000);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userName",username);
            jsonObject.put("password",password);
            privateconnection.setRequestProperty("Content-Type","application/json;charset=utf-8");
            int contentlength = jsonObject.toString().length();
            privateconnection.setRequestProperty("Content-Length", String.valueOf(contentlength));
            privateconnection.setDoOutput(true);
            OutputStream outputStream = privateconnection.getOutputStream();
            outputStream.write(jsonObject.toString().getBytes());
            outputStream.flush();
            outputStream.close();
            int rcode = privateconnection.getResponseCode(); // http result code
            Log.d("httpinfo","rcode:"+String.valueOf(rcode)+",  contentlength:"+contentlength+",  content:"+jsonObject.toString());
            if(rcode == 200) {
                if(iflogout)
                    return true;

                InputStream inputStream = privateconnection.getInputStream();
                StringBuilder sb=new StringBuilder();
                int flag;
                byte[] buf=new byte[1024];
                while((flag=inputStream.read(buf))!=-1){
                    sb.append(new String(buf,0,flag));
                }
                Log.d("httpinfo","recvdata:"+sb.toString());
                int len = 0, total = privateconnection.getContentLength();
                JSONObject re = new JSONObject(sb.toString());
                privateconnection.disconnect();
                if(re.optInt("code") == 0) {    // json result code
                    String cookieVal = privateconnection.getHeaderField("Set-Cookie");
                    sessionstr = cookieVal.substring(0, cookieVal.indexOf(";"));
                    SharedPreferences sharedPreferences = pContext.getSharedPreferences("cache",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor =  sharedPreferences.edit();
                    editor.putString("cookie",sessionstr);
                    editor.apply();
                    return true;
                }
            }else{

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

}

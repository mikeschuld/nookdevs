package com.nookdevs.market;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import com.nookdevs.common.nookBaseActivity;

public class NookMarket extends nookBaseActivity {
    private String FEED_URL="http://nookdevs.googlecode.com/svn/trunk/updatesfeed.xml";
    ConnectivityManager.WakeLock lock;
    ArrayList<AppInfo> availableApps = new ArrayList<AppInfo>(10);
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "nookMarket";
        NAME = "nookMarket";
        super.onCreate(savedInstanceState);
        ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        lock= cmgr.newWakeLock(1, "NookMarket" + hashCode());
        setContentView(R.layout.main);
    }
    @Override
    public void onResume() {
        super.onResume();
        waitForNetwork(lock);
        loadApps(FEED_URL);
        System.out.println("App Size =" + availableApps.size());
        for(int i=0; i< availableApps.size(); i++) {
            System.out.println(" AppInfo = " + availableApps.get(i));
        }
    }
    @Override
    public void onPause() {
        if( lock.isHeld()) 
            lock.release();
        super.onPause();
    }
    private void loadApps(String url) {
        try {
            HttpGet request = new HttpGet(url);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(request);
            XmlPullParserFactory fact = XmlPullParserFactory.newInstance();
            fact.setNamespaceAware(false);
            XmlPullParser parser = fact.newPullParser();
//            String res="";
//            BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent(), 1024);
//            byte[] buffer = new byte[1024];
//            int len;
//            while ((len = bis.read(buffer)) >= 0) {
//                res += new String(buffer, 0, len);
//            }
//            bis.close();
//            System.out.println(res);
            parser.setInput(response.getEntity().getContent(), null);
            int type;
            AppInfo app=null;
            String name="";
            String title=null;
            boolean entry=false;
            boolean apk = false;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    name = parser.getName();
                    if( name.equals("entry")) {
                        entry=true;
                        app = new AppInfo();
                    } else
                    if( entry && name.equals("link")) {
                        String val = parser.getAttributeValue(null, "type");
                        if (val != null && val.equals("application/vnd.android.package-archive")) {
                            apk = true;
                        } else if( val != null && val.equals("application/atom+xml")) {
                            String url_new = parser.getAttributeValue(null, "href");
                            loadApps(url_new);
                            entry=false;
                            title=null;
                            continue;
                        } else {
                            entry=false;
                            title=null;
                            continue;
                        }
                        app.version = parser.getAttributeValue(null, "version");
                        app.url = parser.getAttributeValue(null, "href");
                        app.pkg = parser.getAttributeValue(null, "pkg");
                    }
                } else if( entry && type ==XmlPullParser.TEXT){
                    String text = parser.getText().trim();
                    if( text.equals("")) 
                        continue;
                    if( name.equals("title")) { 
                        app.title=text;
                    }
                    else if( name.equals("content")){
                        app.text=text;
                    } else if( name.equals("version")) {
                        app.version=text;
                    }
                    else if( name.equals("pkg")) {
                        app.pkg=text;
                    }
                } else if( entry && type == XmlPullParser.END_TAG && parser.getName().equals("entry") && apk){
                    apk=false;
                    entry=false;
                    availableApps.add(app);
                    app=null;
                }
            }
        } catch(Exception ex) {
            Log.e(LOGTAG, ex.getMessage(), ex);
        }
//     PackageManager manager = getPackageManager();
//     Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
//     mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//     final LayoutInflater inflater = getLayoutInflater();
//     final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
//        
    }
    class AppInfo {
        String url;
        String version;
        String text;
        String title;
        String pkg;
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Title =");
            sb.append(title);
            sb.append(" URL =");
            sb.append(url);
            sb.append(" version=");
            sb.append(version);
            sb.append(" package=");
            sb.append(pkg);
            sb.append(" desc=");
            sb.append(text);
            sb.append("\n");
            return sb.toString();
        }
    }
}
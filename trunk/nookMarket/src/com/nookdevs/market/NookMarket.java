package com.nookdevs.market;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
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

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nookdevs.common.nookBaseActivity;

public class NookMarket extends nookBaseActivity {
    private String FEED_URL="http://nookdevs.googlecode.com/svn/trunk/updatesfeed.xml";
    ConnectivityManager.WakeLock lock;
    ArrayList<AppInfo> availableApps = new ArrayList<AppInfo>(10);
    HashMap<String,PackageInfo> installedApps = new HashMap<String,PackageInfo>();
    ArrayList<AppInfo> documents = new ArrayList<AppInfo>();
    LinearLayout m_Content;
    private static String m_BaseDir="";
    static {
        try {
            File file = new File(nookBaseActivity.EXTERNAL_SDFOLDER + "/" + "my packages/");
            if (!file.exists()) {
                file = new File(nookBaseActivity.SDFOLDER + "/" + "my packages/");
                file.mkdir();
            }
            m_BaseDir = file.getAbsolutePath() + "/";
            file = new File(m_BaseDir + ".skip");
            file.createNewFile();
        } catch (Exception ex) {
            Log.e("nookMarket", "exception in init static block", ex);
        }
    }
     private View.OnClickListener appListener = new View.OnClickListener() {
        public void onClick(final View v) {
            if( v.getTag() instanceof AppInfo) {
                final AppInfo app = (AppInfo)v.getTag();
                //are you sure?
                Toast.makeText(NookMarket.this, R.string.install_in_background, Toast.LENGTH_SHORT).show();
                Runnable run = new Runnable() {
                    public void run() {
                        try {
                            final String apk = downloadPackage( app.url);
                            final PackageManager pm = getPackageManager();
                            if( apk != null) {
                                if( app.installed) {
                                    IPackageDeleteObserver.Stub observer = new IPackageDeleteObserver.Stub() {
                                        public void packageDeleted(boolean succeeded) {
                                            System.out.println("Delete status = " + succeeded);
                                            //install anyway.
                                            installPackage(apk);
                                        }
                                    };
                                    pm.deletePackage(app.pkg, observer, 1);
                                } else {
                                    installPackage(apk);
                                }
                                
                            }
                        } catch(Exception ex) {
                            Log.e(LOGTAG, ex.getMessage(), ex);
                        }
                    }
                };
                (new Thread(run)).start();
            }
        }
    };
    private void installPackage(String uri) {
        String path = "file://" + uri;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(path), "application/vnd.android.package-archive");
        startActivity(intent);
    }
    private String downloadPackage(String url) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            String type = response.getEntity().getContentType().getValue();
            InputStream in = response.getEntity().getContent();
            if (!type.contains("archive")) {
                // failed.
                System.out.println("Type returned = " + type);
                return null;
            }
            int idx = url.lastIndexOf('/');
            String name = m_BaseDir + url.substring(idx + 1);
            BufferedInputStream bis = new BufferedInputStream(in, 8096);
            FileOutputStream fout = new FileOutputStream(new File(name));
            byte[] buffer = new byte[8096];
            int len;
            while ((len = bis.read(buffer)) >= 0) {
                fout.write(buffer, 0, len);
            }
            bis.close();
            fout.close();
            return name;
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    private View.OnClickListener docListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                if( v.getTag() instanceof AppInfo) {
                    AppInfo app = (AppInfo)v.getTag();
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.ACTION_DEFAULT);
                    intent.setComponent(new ComponentName("com.nookdevs.browser", "com.nookdevs.browser.nookBrowser"));
                    intent.setData(Uri.parse(app.url));
                    startActivity(intent);
                    return;
                }
            } catch (Exception ex) {
                Log.e(LOGTAG, ex.getMessage(), ex);
                return;
            }
        }
    };
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "nookMarket";
        NAME = getString(R.string.app_name);
        super.onCreate(savedInstanceState);
        ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        lock= cmgr.newWakeLock(1, "NookMarket" + hashCode());
        lock.setReferenceCounted(false);
        setContentView(R.layout.main);
        m_Content = (LinearLayout) findViewById(R.id.appcontainer);
    }
    @Override
    public void onResume() {
        super.onResume();
        lock.acquire();
        installedApps.clear();
        availableApps.clear();
        PackageManager manager = getPackageManager();
        final List<PackageInfo> apps = manager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for(PackageInfo app:apps) {
        //    System.out.println("package name =" + app.packageName);
        //    System.out.println("version name =" + app.versionName);
            installedApps.put( app.packageName, app);
        }
        waitForNetwork(lock);
        loadApps(FEED_URL);
//        System.out.println("App Size =" + availableApps.size());
//        for(int i=0; i< availableApps.size(); i++) {
//            System.out.println(" AppInfo = " + availableApps.get(i));
//        }
        populateScreen();
    }
    private void populateScreen() {
        m_Content.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for(AppInfo app:availableApps) {
            RelativeLayout appdetails =
                (RelativeLayout) inflater.inflate(R.layout.addapp, m_Content, false);
            ImageButton icon = (ImageButton) appdetails.findViewById(R.id.appicon);
            icon.setOnClickListener(appListener);
            if( !app.installed) {
                icon.setImageResource(R.drawable.icon);
            } else {
                try {
                    if( installedApps.get(app.pkg).activities[0] != null)
                        icon.setImageDrawable( installedApps.get(app.pkg).activities[0].loadIcon(getPackageManager()));
                    else
                        icon.setImageDrawable( installedApps.get(app.pkg).applicationInfo.loadIcon(getPackageManager()));
                } catch(Exception ex) {
                    icon.setImageResource(R.drawable.icon);
                }
            }
            icon.setTag( app);
            TextView text = (TextView) appdetails.findViewById(R.id.desc);
            TextView title = (TextView) appdetails.findViewById(R.id.title);
            text.setText(app.text);
            title.setText(app.title);
            m_Content.addView(appdetails);
            
        }
        for(AppInfo app:documents) {
            RelativeLayout appdetails =
                (RelativeLayout) inflater.inflate(R.layout.addapp, m_Content, false);
            ImageButton icon = (ImageButton) appdetails.findViewById(R.id.appicon);
            icon.setImageResource(R.drawable.info);
            icon.setOnClickListener(docListener);
            icon.setTag(app);
            TextView text = (TextView) appdetails.findViewById(R.id.desc);
            TextView title = (TextView) appdetails.findViewById(R.id.title);
            text.setText(app.text);
            title.setText(app.title);
            m_Content.addView(appdetails);
        }
    }
    @Override
    public void onPause() {
        if( lock.isHeld()) 
            lock.release();
        super.onPause();
    }
    private void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
        System.out.println("Source =" + sourceLocation);
        System.out.println("Dest =" + targetLocation);
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            String[] children = sourceLocation.list();
            for (String element : children) {
                copyDirectory(new File(sourceLocation, element), new File(targetLocation, element));
            }
        } else {
            FileInputStream in = new FileInputStream(sourceLocation);
            FileOutputStream out = new FileOutputStream(targetLocation);
            float size = sourceLocation.length();
            float current = 0;
            int prevProgress = 0;
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                current += len;
            }
            in.close();
            out.close();
        }
    }
    private void loadApps(String url) {
        try {
            HttpGet request = new HttpGet(url);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(request);
            XmlPullParserFactory fact = XmlPullParserFactory.newInstance();
            fact.setNamespaceAware(false);
            XmlPullParser parser = fact.newPullParser();
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
                        } else if( val != null && val.equals("text/html")) {
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
                } else if( entry && type == XmlPullParser.END_TAG && parser.getName().equals("entry")){
                    entry=false;
                    if( apk) {
                        apk=false;
                        if( installedApps.containsKey(app.pkg)){
                            PackageInfo info = installedApps.get(app.pkg);
                            app.installed=true;
                            if( app.version != null) {
                                app.title = app.title + " " + app.version;
                                if(!app.version.equals(info.versionName)) {
                                    app.updateAvailable=true;
                                    app.text ="***Update Available***\n" + app.text;
                                    System.out.println("current version=" + app.version);
                                    System.out.println("installed version=" + info.versionName);
                                }
                            }
                        }
                        availableApps.add(app);
                    } else {
                        app.installed=false;
                        app.updateAvailable=false;
                        app.version=null;
                        app.pkg=null;
                        documents.add(app);
                        
                    }
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
        boolean installed=false;
        boolean updateAvailable=false;
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
            sb.append(" installed=");
            sb.append(installed);
            sb.append(" updateAvailable=");
            sb.append(updateAvailable);
            sb.append(" desc=");
            sb.append(text);
            sb.append("\n");
            return sb.toString();
        }
    }
}
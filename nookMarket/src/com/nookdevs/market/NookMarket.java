/* 
 * Copyright 2010 nookDevs - Hari Swaminathan
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.nookdevs.market;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

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

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nookdevs.common.nookBaseActivity;

public class NookMarket extends nookBaseActivity {
    //protected static String FEED_URL="http://nookdevs.googlecode.com/svn/trunk/nookMarket/updatesfeed_test.xml";
    protected static String FEED_URL="http://nookdevs.googlecode.com/svn/trunk/updatesfeed.xml";
    ConnectivityManager.WakeLock lock;
    LinkedList<AppInfo> availableApps = new LinkedList<AppInfo>();
    HashMap<String,PackageInfo> installedApps = new HashMap<String,PackageInfo>();
    ArrayList<AppInfo> documents = new ArrayList<AppInfo>();
    LinearLayout m_Content;
    private static String m_BaseDir="";
    Handler m_Handler = new Handler();
    String lastApk=null;
    private boolean m_NookletInstalled=false;
    private String m_NookletFolder = null;
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
     private View.OnLongClickListener appdelListener = new View.OnLongClickListener() {

        public boolean onLongClick(final View arg0) {
            //confirm
            AlertDialog.Builder builder = new AlertDialog.Builder(NookMarket.this);
            builder.setTitle(R.string.delete);
            builder.setMessage(R.string.confirm);
            builder.setNegativeButton(android.R.string.no, null).setCancelable(true);
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    AppInfo app = (AppInfo)arg0.getTag();
                    IPackageDeleteObserver.Stub observer = new IPackageDeleteObserver.Stub() {
                        public void packageDeleted(boolean succeeded) {
                            m_Handler.post( new Runnable() {
                                public void run() {
                                    init();
                                }
                            });
                        }
                    };
                    if( app.pkg.startsWith("Nooklet")) {
                        int idx = app.pkg.lastIndexOf("/");
                        String name = app.pkg.substring(idx+1);
                        File f = new File( m_NookletFolder +"/" + name);
                        if( f.exists()) {
                           boolean status = deleteDir(f);
                           Log.e(LOGTAG, "Deleting " + app.pkg + " status =" + status);
                           try {
                               observer.packageDeleted(status);
                           } catch(Exception ex) {
                           }
                        }
                    }
                    NookMarket.this.getPackageManager().deletePackage(app.pkg, observer, 0);
                }
            });
            builder.show();
            return true;
        }
         
     };
     Comparator myComp = new Comparator<AppInfo>() {
         public int compare(AppInfo arg0, AppInfo arg1) {
             if( arg0.updateAvailable && !arg1.updateAvailable)
                     return -1;
             else if( arg1.updateAvailable && !arg0.updateAvailable)
                     return 1;
             else if( !arg0.installed && arg1.installed)
                     return -1;
             else if( !arg1.installed && arg0.installed)
                     return 1;
             return arg0.title.compareToIgnoreCase(arg1.title);
         }
         
     };
     //Copied from exampledepot.com & modified
     public static boolean deleteDir(File dir) {
         if (dir.isDirectory()) {
             File[] children = dir.listFiles();
             for (int i=0; i<children.length; i++) {
                 boolean success = deleteDir(children[i]);
                 if (!success) {
                     return false;
                 }
             }
         }

         // The directory is now empty so delete it
         return dir.delete();
     }

     private View.OnClickListener appListener = new View.OnClickListener() {
        public void onClick(final View v) {
            if( v.getTag() instanceof AppInfo) {
                final AppInfo app = (AppInfo)v.getTag();
                Toast.makeText(NookMarket.this, R.string.install_in_background, Toast.LENGTH_SHORT).show();
                Runnable run = new Runnable() {
                    public void run() {
                        try {
                            final String apk = downloadPackage( app.url);
                            if( app.pkg.startsWith("Nooklet")) {
                                try {
                                    String name=null;
                                    int idx = app.pkg.lastIndexOf('/');
                                    name = app.pkg.substring(idx+1);
                                    //zip unzip Logic from java-tips.org
                                    ZipInputStream zipinputstream = null;
                                    ZipEntry zipentry;
                                    zipinputstream = new ZipInputStream(
                                        new FileInputStream(apk));
                                    zipentry = zipinputstream.getNextEntry();
                                    while (zipentry != null) 
                                    { 
                                        //for each entry to be extracted
                                        String entryName = zipentry.getName();
                                        if( zipentry.isDirectory()) {
                                            File tmp = new File(m_NookletFolder + "/" + entryName);
                                            tmp.mkdir();
                                            zipentry = zipinputstream.getNextEntry();
                                            continue;
                                        }
                                        int n;
                                        FileOutputStream fileoutputstream;
                                        File newFile = new File(entryName);
                                        byte[] buf = new byte[1024];
                                        fileoutputstream = new FileOutputStream(
                                           m_NookletFolder+"/" + entryName);             
    
                                        while ((n = zipinputstream.read(buf, 0, 1024)) > -1)
                                            fileoutputstream.write(buf, 0, n);
    
                                        fileoutputstream.close(); 
                                        zipinputstream.closeEntry();
                                        zipentry = zipinputstream.getNextEntry();
    
                                    }//while
                                    zipinputstream.close();
                                    File f = new File(m_NookletFolder + "/" + name +  "/version.txt");
                                    FileOutputStream fout = new FileOutputStream(f);
                                    fout.write( app.version.getBytes());
                                    fout.close();
                                    m_Handler.post( new Runnable() {
                                       public void run() {
                                           Toast.makeText(NookMarket.this, R.string.install_complete, Toast.LENGTH_SHORT).show();
                                           init();
                                       }
                                    });
                                    return;
                                } catch(Exception ex) {
                                    Log.e(LOGTAG, ex.getMessage(), ex);
                                    m_Handler.post( new Runnable() {
                                        public void run() {
                                            Toast.makeText(NookMarket.this, R.string.install_failed, Toast.LENGTH_SHORT).show();
                                        }
                                     });
                                    return;
                                }

                            }
                            final PackageManager pm = getPackageManager();
                            if( apk != null) {
                                if( app.installed && !allowUpgrades()) {
                                    IPackageDeleteObserver.Stub observer = new IPackageDeleteObserver.Stub() {
                                        public void packageDeleted(boolean succeeded) {
                                            //install anyway.
                                            installPackage(apk);
                                            lastApk=apk;
                                        }
                                    };
                                    pm.deletePackage(app.pkg, observer, 1);
                                } else {
                                    installPackage(apk);
                                    lastApk=apk;
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
    private void loadWallpaper() {
        String wallPaperFile = getWallpaperFile();
        if (wallPaperFile != null) {
            try {
                ImageView img = (ImageView) findViewById(R.id.mainimage);
                wallPaperFile = wallPaperFile.substring(7);
                Bitmap bMap = BitmapFactory.decodeFile(wallPaperFile);
                img.setImageBitmap(bMap);
            } catch (Exception ex) {
                Log.e(LOGTAG, ex.getMessage(), ex);
            }
        }
    }
    private boolean allowUpgrades() {
        try {
            File f = new File(nookBaseActivity.SDFOLDER + "/" + "market.xml");
            if (!f.exists()) {
                f = new File(nookBaseActivity.EXTERNAL_SDFOLDER + "/" + "market.xml");
            }
            if (f.exists()) {
                FileInputStream inp = new FileInputStream(f);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inp, null);
                int type;
                boolean allowUpgrades=false;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (type == XmlPullParser.START_TAG) {
                        String txt = parser.getName();
                        if (txt != null && txt.equals("allowUpgrades")) {
                            allowUpgrades=true;
                        }
                    } else if( type == XmlPullParser.TEXT && allowUpgrades) {
                        String txt = parser.getText();
                        if( txt != null && !txt.trim().equals("")) {
                            txt = txt.toLowerCase();
                            allowUpgrades = txt.startsWith("y");
                            break;
                        }
                    }
                }
                inp.close();
                return allowUpgrades;
            }
            return true;
        } catch(Exception ex) {
            return true;
        }
    }
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
            httpClient.getConnectionManager().closeExpiredConnections();
            return name;
        } catch(Exception ex) {
            Log.e(LOGTAG, ex.getMessage(), ex);
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
        loadWallpaper();
        if( lastApk != null) {
            try {
                //( new File(apk)).delete();
                System.out.println("Delete status =" + ( new File(lastApk)).delete());
            } catch(Exception ex) {
                Log.e(LOGTAG, ex.getMessage(),ex);
            }
        }
        if( isAirplaneModeOn()) {
            displayAlert(getString(R.string.network), getString(R.string.network_error), 2, 
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                       finish();
                    }
                },-1);
            return;
        } 
        lock.acquire();
        init();
    }
    private void init() {
        displayAlert(getString(R.string.connecting), getString(R.string.please_wait), 1, null, -1);
        installedApps.clear();
        availableApps.clear();
        documents.clear();
        m_Content.removeAllViews();
        PackageManager manager = getPackageManager();
        final List<PackageInfo> apps = 
            manager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for(PackageInfo app:apps) {
            if( app.packageName.equals("com.kbs.nooklet")) {
                m_NookletInstalled=true;
                File f = new File(SDFOLDER+"/nooklets");
                if( !f.exists()) {
                    f = new File(EXTERNAL_SDFOLDER+"/nooklets");
                    if( !f.exists()) {
                        m_NookletInstalled=false;
                    }
                    System.out.println("Nook Installed =" + m_NookletInstalled);
                }
                m_NookletFolder=f.getAbsolutePath();
            }
            installedApps.put( app.packageName, app);
        }
        WifiTask task = new WifiTask();
        task.execute();
  
    }
    @Override
    public void onPause() {
        if( lock.isHeld()) 
            lock.release();
        super.onPause();
    }

    private void loadApps(String url) {
        try {
            final LayoutInflater inflater = getLayoutInflater();
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
            boolean nooklet=false;
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
                        } else if("application/zip".equals(val)) {
                            if( m_NookletInstalled) {
                                nooklet=true;
                            } else {
                                entry=false;
                                title=null;
                                continue;
                            }
                        } else {
                            entry=false;
                            title=null;
                            continue;
                        }
                        if( app.version ==null)
                            app.version = parser.getAttributeValue(null, "version");
                        app.url = parser.getAttributeValue(null, "href");
                        if( app.pkg == null)
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
                    final String orgPkg = app.pkg;
                    if( apk || nooklet) {
                        if(nooklet) {
                            String currVersion=null;
                            File f = new File( m_NookletFolder +"/" + app.pkg + "/" + "version.txt");
                            if( f.exists()) {
                                BufferedReader fin = new BufferedReader(new FileReader(f));
                                currVersion = fin.readLine();
                                fin.close();
                                app.installed=true;
                            }
                            if( app.version != null) {
                                if( !app.title.contains(app.version))
                                    app.title = app.title + " " + app.version;
                                if(currVersion != null && !app.version.trim().equals(currVersion)) {
                                    app.updateAvailable=true;
                                    app.text ="***Update Available***\n" + app.text;
                                }
                            }
                            app.pkg = "Nooklet/" + app.pkg;
                        } else {
                            apk=false;
                            if( "MyNook.Ru Launcher".equals(app.title) && 
                                "1.5.6".equals(app.version)) {
                                app=null;
                                continue;
                            }
                            if( installedApps.containsKey(app.pkg)){
                                PackageInfo info = installedApps.get(app.pkg);
                                app.installed=true;
                                if( app.version != null) {
                                    if( !app.title.contains(app.version))
                                        app.title = app.title + " " + app.version;
                                    if(!app.version.trim().equals(info.versionName)) {
                                        app.updateAvailable=true;
                                        app.text ="***Update Available***\n" + app.text;
                                    }
                                }
                            } else if( app.version != null && !app.title.contains(app.version)) {
                                app.title = app.title + " " + app.version;
                            }
                        }
                        final int idx =-Collections.binarySearch(availableApps, app, myComp)-1;
                        availableApps.add(idx,app);
                        final AppInfo app1 = app;
                        final boolean nooklet1 = nooklet;
                        Runnable run = new Runnable() {
                            public void run() {
                                RelativeLayout appdetails =
                                (RelativeLayout) inflater.inflate(R.layout.addapp, m_Content, false);
                                ImageButton icon = (ImageButton) appdetails.findViewById(R.id.appicon);
                                icon.setOnClickListener(appListener);
                                if( !app1.installed) {
                                    icon.setImageResource(R.drawable.icon);
                                } else {
                                   icon.setOnLongClickListener(appdelListener);
                                    try {
                                        if( nooklet1) {
                                            File f = new File(m_NookletFolder+"/"+ orgPkg + "/icon.png");
                                            if( f.exists()) 
                                                icon.setImageURI(Uri.parse(m_NookletFolder+"/"+ orgPkg + "/icon.png"));
                                            else
                                                icon.setImageResource(R.drawable.icon);
                                        } else if( installedApps.get(app1.pkg).activities != null && 
                                            installedApps.get(app1.pkg).activities[0] != null)
                                            icon.setImageDrawable( installedApps.get(app1.pkg).activities[0].loadIcon(getPackageManager()));
                                        else
                                            icon.setImageDrawable( installedApps.get(app1.pkg).applicationInfo.loadIcon(getPackageManager()));
                                    } catch(Exception ex) {
                                        icon.setImageResource(R.drawable.icon);
                                    }
                                }
                                icon.setTag( app1);
                                TextView text = (TextView) appdetails.findViewById(R.id.desc);
                                TextView title1 = (TextView) appdetails.findViewById(R.id.title);
                                text.setText(app1.text);
                                title1.setText(app1.title);
                                
                                m_Content.addView(appdetails, idx);
                                if( idx ==0) 
                                    closeAlert();
                            }
                        };
                        m_Handler.post(run);
                        nooklet=false;
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
            //alert and exit.
            if( availableApps.size() ==0)
                m_Handler.post( new Runnable() {
                    public void run() {
                        closeAlert();
                        displayAlert(getString(R.string.network), getString(R.string.feed_error), 2, 
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            },-1);
                    }
                });
        }
    }
    class WifiTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
        }
        
        @Override
        protected Boolean doInBackground(Void... arg0) {
            ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            // NetworkInfo info =
            // cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo info = cmgr.getActiveNetworkInfo();
            boolean connection = (info == null) ? false : info.isConnected();
            int attempts = 1;
            while (!connection && attempts < 15) {
                try {
                    Thread.sleep(3000);
                } catch (Exception ex) {
                    
                }
                // info = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                info = cmgr.getActiveNetworkInfo();
                connection = (info == null) ? false : info.isConnected();
                attempts++;
            }
            return connection;
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if( result) {
                loadApps(FEED_URL);
            } else {
                closeAlert();
                displayAlert(getString(R.string.network), getString(R.string.network_error), 2, 
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                           finish();
                        }
                    },-1);
            }
        }

    }
    private boolean isAirplaneModeOn() {
        try {
            int mode = android.provider.Settings.System.getInt(getApplicationContext().getContentResolver(),android.provider.Settings.System.AIRPLANE_MODE_ON); 
            if( mode ==1) {
                return true;
            } else {
                return false;
            }
        } catch(Exception ex) {
            return true;
        }
    }
}
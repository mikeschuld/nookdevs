/* 
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.nookdevs.launcher;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.Extensions;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.ImageView.ScaleType;

import com.nookdevs.common.ImageAdapter;
import com.nookdevs.common.nookBaseActivity;

public class LauncherSettings extends nookBaseActivity implements Gallery.OnItemClickListener {
    private ClickListener m_ClickListener;
    private HashMap<ImageButton, AppDetail> m_IntentsList;
    private HashMap<String, Integer> m_SystemIcons = new HashMap<String, Integer>();
    LinearLayout m_LinearLayout = null;
    LinearLayout m_AddAppLayout = null;
    ViewAnimator m_ViewAnimatorMain = null;
    ViewAnimator m_ViewAnimator = null;
    ImageButton m_Current;
    View m_EmptyView;
    DBHelper m_DBHelper = null;
    Button m_AddBtn;
    Button m_CloseBtn;
    Button m_BackBtn;
    Vector<String> m_LauncherApps = new Vector<String>();
    Vector<Button> m_AddAppsList = new Vector<Button>();
    boolean m_InAddPanel = false;
    Gallery m_IconGallery = null;
    boolean m_AddAppsInitDone = false;
    boolean m_IconsLoaded = false;
    ImageAdapter m_IconAdapter = null;
    boolean m_mainSettings=false;
    Uri m_CurrentUri = null;
    ImageButton m_CurrentButton;
    boolean m_ImageChanged=false;
    
    
    public static String[] apps =
        {
    		"com.bravo.thedaily.Daily", "com.bravo.library.LibraryActivity", "com.bravo.store.StoreFrontActivity",
    		"com.bravo.ereader.activities.ReaderActivity", "com.bravo.app.settings.SettingsActivity",
    		"com.bravo.app.settings.wifi.WifiActivity",
    		"com.nookdevs.launcher.LauncherSettings", "com.bravo.home.HomeActivity",
    		"com.nookdevs.launcher.LauncherSelector", "com.bravo.chess.ChessActivity",
    		"com.bravo.sudoku.SudokuActivity", "com.bravo.app.browser.BrowserActivity"
        };
    
    public static int[] appIcons =
        {
    		R.drawable.select_home_dailyedition, R.drawable.select_home_library, R.drawable.select_home_store,
    		R.drawable.select_home_mybook, R.drawable.select_home_settings, R.drawable.select_home_wifi,
    		R.drawable.select_home_launcher_settings,
    		R.drawable.select_home_bnhome, R.drawable.select_default_launcher, R.drawable.select_home_chess,
    		R.drawable.select_home_sudoku, R.drawable.select_home_browser
        };
    
    WebView m_WebView;
    Vector<String> m_IconFiles = new Vector<String>();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "nookLauncher";
        NAME = "Launcher Settings";
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        for (int i = 0; i < apps.length; i++) {
            m_SystemIcons.put(apps[i], appIcons[i]);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        m_ClickListener = new ClickListener();
        m_IntentsList = new HashMap<ImageButton, AppDetail>();
        loadApps();
        Runnable thrd = new Runnable() {
            public void run() {
                initAvailableApps();
                m_AddAppsInitDone = true;
                loadIcons();
                m_IconsLoaded = true;
            }
        };
        (new Thread(thrd)).start();
    }
    
    private void loadIcons() {
        try {
            File file = new File(SDFOLDER +"/my icons");
            File external = new File(EXTERNAL_SDFOLDER +"/my icons");
            FileFilter filter = new FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) { return true; }
                    String extension = f.getName().toLowerCase();
                    if (extension.endsWith("jpeg") || extension.endsWith("tif") || extension.endsWith("gif")
                        || extension.endsWith("jpg") || extension.endsWith("png")) {
                        if( extension.contains("_sel.") || extension.contains("_focused.")) {
                            return false;
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
                
            };
            retrieveFiles(file, filter);
            retrieveFiles(external, filter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void retrieveFiles(File base, FileFilter filter) {
        File[] files = base.listFiles(filter);
        if (files == null) { return; }
        for (File file : files) {
            if (file.isDirectory()) {
                retrieveFiles(file, filter);
            } else {
                m_IconFiles.add(file.getAbsolutePath());
            }
        }
        return;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        m_DBHelper.close();
    }
    
    protected final synchronized void loadApps() {
        m_LinearLayout = (LinearLayout) (findViewById(R.id.settingscontainer));
        m_AddAppLayout = (LinearLayout) (findViewById(R.id.addappcontainer));
        m_ViewAnimatorMain = (ViewAnimator) (findViewById(R.id.mainanimator));
        m_ViewAnimator = (ViewAnimator) (findViewById(R.id.settingsanim));
        m_ViewAnimator.setInAnimation(this, android.R.anim.slide_in_left);
        m_ViewAnimatorMain.setInAnimation(this, android.R.anim.fade_in);
        m_AddBtn = (Button) (findViewById(R.id.addButton));
        m_CloseBtn = (Button) (findViewById(R.id.closeButton));
        m_BackBtn = (Button) (findViewById(R.id.backbutton));
        m_BackBtn.setOnClickListener(m_ClickListener);
        m_CloseBtn.setVisibility(View.INVISIBLE);
        m_CloseBtn.setOnClickListener(m_ClickListener);
        m_AddBtn.setOnClickListener(m_ClickListener);
        m_Current = (ImageButton) (findViewById(R.id.imgbutton));
        m_Current.setOnClickListener(m_ClickListener);
        m_LinearLayout.removeAllViews();
        m_IconGallery = ((Gallery) findViewById(R.id.icongallery));
        TypedArray a = this.obtainStyledAttributes(R.styleable.default_gallery);
        int backid = a.getResourceId(R.styleable.default_gallery_android_galleryItemBackground, 0);
        a.recycle();
        
        m_IconAdapter = new ImageAdapter(this, null, null);
        m_IconAdapter.setBackgroundStyle(backid);
        // m_IconGallery.setAdapter(m_IconAdapter);
        m_IconGallery.setVisibility(View.INVISIBLE);
        m_IconGallery.setOnItemClickListener(this);
        final LayoutInflater inflater = getLayoutInflater();
        ImageView view = (ImageView) inflater.inflate(R.layout.emptyview, m_LinearLayout, false);
        m_LinearLayout.addView(view);
        HorizontalScrollView hscroll = (HorizontalScrollView) findViewById(R.id.horizontalscroll02);
        hscroll.setSmoothScrollingEnabled(true);
        m_DBHelper = new DBHelper(this, null, NookLauncher.DB_VERSION);
        Cursor cursor = m_DBHelper.getApps();
        int count = cursor == null ? 0 : cursor.getCount();
        for (int i = 0; i < count; i++) {
            ImageButton btn = (ImageButton) inflater.inflate(R.layout.settingsbutton, m_LinearLayout, false);
            fillButton(btn, cursor.getString(0), cursor.getInt(1), cursor.getString(2));
            m_LauncherApps.add(cursor.getString(0));
            m_LinearLayout.addView(btn);
            cursor.moveToNext();
        }
        cursor.close();
        view = (ImageView) inflater.inflate(R.layout.emptyview, m_LinearLayout, false);
        m_LinearLayout.addView(view);
        m_EmptyView = view;
        m_WebView = (WebView) findViewById(R.id.webview);
        m_WebView.getSettings().setTextSize(WebSettings.TextSize.LARGER);
        m_WebView.loadUrl("file:///android_asset/settings_main.htm");
        
    }
    
    public void setMainImage(Drawable img) {
        if (img == null) {
            m_ViewAnimator.showNext();
            return;
        }
        m_Current.setImageDrawable(img);
        m_ViewAnimator.showNext();
    }
    
    private final void fillButton(ImageButton b, String appName, int appIconId, String uri) {
        if (appIconId > 0) {
            b.setImageResource(appIconId);
        } else if (uri != null) {
            b.setImageURI(Uri.parse(uri));
        } else {
            int idx = appName.lastIndexOf('.');
            String name = appName.substring(idx + 1);
            String pkg = appName.substring(0, idx);
            ComponentName comp = new ComponentName(pkg, pkg + "." + name);
            PackageManager manager = getPackageManager();
            try {
                b.setImageDrawable(manager.getActivityIcon(comp));
            } catch (Exception ex) {
                b.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }
        b.setOnClickListener(m_ClickListener);
        AppDetail app = new AppDetail();
        app.appName = appName;
        app.id = appIconId;
        app.uri = uri;
        m_IntentsList.put(b, app);
    }
    
    public void addAppToAvailable(String name, int id, String uri) {
        if (name == null || name.equals("null")) { return; }
        PackageManager manager = getPackageManager();
        LayoutInflater inflater = getLayoutInflater();
        RelativeLayout addApp = (RelativeLayout) inflater.inflate(R.layout.addapp, m_AddAppLayout, false);
        AppImageButton btn = (AppImageButton) addApp.getChildAt(0);
        TextView txt = (TextView) addApp.getChildAt(1);
        int idx = name.lastIndexOf('.');
        String pkgName = name.substring(0, idx);
        String tmp = name.substring(idx + 1);
        btn.setName(tmp);
        btn.setPackage(pkgName);
        ComponentName comp = new ComponentName(pkgName, name);
        try {
            if (id > 0) {
                btn.setImageResource(id);
            } else if (uri == null) {
                btn.setImageDrawable(manager.getActivityIcon(comp));
            } else {
                btn.setImageURI(Uri.parse(uri));
            }
            
        } catch (Exception ex) {
            btn.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        m_DBHelper.removeData(name);
        name = name.substring(idx + 1);
        txt.setText(pkgName + "\n" + name);
        m_AddAppLayout.addView(addApp);
        
        btn.setOnClickListener(m_ClickListener);
        Button btn1 = (Button) addApp.getChildAt(2);
        btn1.setOnClickListener(m_AddAppListener);
    }
    
    AddAppListener m_AddAppListener = new AddAppListener();
    protected void addAppsToLauncher() {
        try {
            for (Button app : m_AddAppsList) {
                RelativeLayout layout = (RelativeLayout) app.getParent();
                AppImageButton btn = (AppImageButton) layout.getChildAt(0);
                String imageUri = btn.getURI();
                String name = btn.getName();
                String pkg = btn.getPackage();
                m_AddAppLayout.removeView(layout);
                int resid = -1;
                String appName = pkg + "." + name;
                if (appName.equals("com.bravo.app.settings.WifiActivity")) {
                	// a kludge, not sure why it's dropping the .wifi, probably because I had to drop it in NookLauncher
                	appName="com.bravo.app.settings.wifi.WifiActivity";
                }
                if( imageUri == null || imageUri.trim().equals("")) {
                    if (m_SystemIcons.get(appName) != null) {
                        resid = m_SystemIcons.get(appName);
                        Log.w(LOGTAG, "system app " + name + " back to launcher");
                    
                    }
                } else {
                    File f = new File(imageUri);
                    imageUri = "/data/data/com.nookdevs.launcher/files/" + f.getName();
                    int num=0;
                    while ( num < 2) {
                        if( f.exists() && !f.getAbsolutePath().startsWith("/data/data/com.nookdevs.launcher")) {
                            InputStream in = new FileInputStream(f);
                            OutputStream out = openFileOutput(f.getName(), MODE_PRIVATE);
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0){
                                out.write(buf, 0, len);
                            }
                            in.close();
                            out.close();
                        }
                        num++;
                        if( num > 1) break;
                        f = new File( f.getAbsolutePath().replace(".", "_sel."));
                        if( !f.exists()) {
                            f = new File( f.getAbsolutePath().replace("_sel.", "_focused."));
                            if( !f.exists())
                                break;
                        }
                        if( !f.exists()) {
                            break;
                        }
                    }
                }
               
                m_DBHelper.addData(appName, resid, imageUri, "0");
                LayoutInflater inflater = getLayoutInflater();
                ImageButton btn1 = (ImageButton) inflater.inflate(R.layout.settingsbutton, m_LinearLayout, false);
                fillButton(btn1, pkg + "." + name, resid, imageUri);
                int idx = m_LinearLayout.indexOfChild(m_EmptyView);
                m_LinearLayout.addView(btn1, idx);
                
            }
            m_AddAppsList.clear();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void initAvailableApps() {
        PackageManager manager = getPackageManager();
        
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        m_AddAppLayout.removeAllViews();
        final LayoutInflater inflater = getLayoutInflater();
        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        try {
            for (ResolveInfo ri : apps) {
                String name = ri.activityInfo.name;
                if (m_LauncherApps.contains(name)) {
                    continue;
                }
                RelativeLayout addApp = (RelativeLayout) inflater.inflate(R.layout.addapp, m_AddAppLayout, false);
                AppImageButton btn = (AppImageButton) addApp.getChildAt(0);
                TextView txt = (TextView) addApp.getChildAt(1);
                ComponentName comp =
                    new ComponentName(ri.activityInfo.applicationInfo.packageName, ri.activityInfo.name);
                if (m_SystemIcons.get(ri.activityInfo.name) != null) {
                    btn.setImageResource(m_SystemIcons.get(ri.activityInfo.name));
                } else {
                    try {
                        btn.setImageDrawable(manager.getActivityIcon(comp));
                    } catch (Exception ex) {
                        btn.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                }
                int idx = name.lastIndexOf('.');
                name = name.substring(idx + 1);
                
                txt.setText(ri.activityInfo.packageName + "\n" + name);
                m_AddAppLayout.addView(addApp);
                btn.setName(name);
                btn.setPackage(ri.activityInfo.packageName);
                btn.setImageURI(null);
                btn.setOnClickListener(m_ClickListener);
                Button btn1 = (Button) addApp.getChildAt(2);
                btn1.setOnClickListener(m_AddAppListener);
            }
        } catch (Exception ex) {
            System.out.println("No icon ...");
        }
        
    }
    
    private final class AddAppListener implements View.OnClickListener {
        
        public void onClick(View app) {
            Button btn = (Button) app;
            if (m_AddAppsList.contains(btn)) {
                m_AddAppsList.remove(btn);
                btn.setBackgroundResource(R.drawable.small_add_button);
                return;
            } else {
                m_AddAppsList.add(btn);
                btn.setBackgroundResource(R.drawable.remove_button);
            }
        }
        
    }
    
    private final class ClickListener implements View.OnClickListener {
        
        Drawable m_CurrentImg;
        
        public void setCurrentImg(Drawable img) {
            m_CurrentImg = img;
            m_CurrentUri = null;
            closeButtonClicked();
        }
        
        public void setCurrentUri(String path) {
            m_CurrentUri = Uri.parse(path);
            m_CurrentImg = null;
            closeButtonClicked();
        }
        
        private void closeButtonClicked() {
            System.out.println("Close btton clicked -" + m_CurrentImg + " uri =" + m_CurrentUri);
            if( m_mainSettings) {
                AppDetail app = m_IntentsList.get(m_CurrentButton);
                if (m_CurrentUri != null) {
                    app.uri = m_CurrentUri.toString();
                    m_ImageChanged=true;
                    m_Current.setImageURI(m_CurrentUri);
                }
                m_WebView.loadUrl("file:///android_asset/settings_main.htm");
                m_mainSettings=false;
                m_ViewAnimatorMain.setVisibility(View.VISIBLE);
                m_ViewAnimator.setVisibility(View.VISIBLE);
                m_IconGallery.setVisibility(View.INVISIBLE);
                m_CloseBtn.setVisibility(View.INVISIBLE);
                return;
            }
            m_ViewAnimatorMain.setVisibility(View.VISIBLE);
            m_ViewAnimator.setVisibility(View.VISIBLE);
            m_IconGallery.setVisibility(View.INVISIBLE);
            m_CloseBtn.setVisibility(View.INVISIBLE);
            m_CurrentButton.setScaleType(ScaleType.CENTER);
            m_CurrentButton.setImageDrawable(null);
            if (m_CurrentUri != null) {
                m_CurrentButton.setImageURI(m_CurrentUri);
            } else {
                m_CurrentButton.setImageDrawable(m_CurrentImg);
            }
            m_WebView.loadUrl("file:///android_asset/settings_addapp.htm");
            m_CurrentButton = null;
        }
        
        public void onClick(View button) {
            if (button.equals(m_BackBtn)) {
                try {
                    setResult(RESULT_OK);
                    finish();
                } catch (Exception ex) {
                    goHome();
                }
                return;
            }
            if (button.equals(m_CloseBtn)) {
                m_CurrentUri = null;
                closeButtonClicked();
                return;
            }
            if (button.equals(m_AddBtn)) {
                if (m_InAddPanel) {
                    m_IconGallery.setVisibility(View.INVISIBLE);
                    m_CloseBtn.setVisibility(View.INVISIBLE);
                    addAppsToLauncher();
                    m_ViewAnimatorMain.showNext();
                    // add apps selected till now.
                    m_InAddPanel = false;
                    m_AddBtn.setBackgroundResource(R.drawable.small_add_button);
                    m_BackBtn.setVisibility(View.VISIBLE);
                    m_WebView.loadUrl("file:///android_asset/settings_main.htm");
                    return;
                }
                while (!m_AddAppsInitDone) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception ex) {
                        
                    }
                }
                if (m_AddAppsInitDone) {
                    m_ViewAnimatorMain.showNext();
                    m_InAddPanel = true;
                    m_BackBtn.setVisibility(View.INVISIBLE);
                    m_WebView.loadUrl("file:///android_asset/settings_addapp.htm");
                }
                m_AddBtn.setBackgroundResource(R.drawable.back_button);
                return;
            } else if (m_InAddPanel) {
                // Change Icons
                // display icons gallery
                while (!m_IconsLoaded) {
                    try {
                        Thread.sleep(2000);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                m_ViewAnimatorMain.setVisibility(View.INVISIBLE);
                m_ViewAnimator.setVisibility(View.INVISIBLE);
                m_IconGallery.setVisibility(View.VISIBLE);
                m_CloseBtn.setVisibility(View.VISIBLE);
                m_CurrentButton = (ImageButton) button;
                m_CurrentImg = m_CurrentButton.getDrawable();
                m_IconAdapter.setCurrentImage(m_CurrentButton.getDrawable());
                m_IconAdapter.setImageUrls(m_IconFiles);
                m_IconGallery.setAdapter(m_IconAdapter);
                m_WebView.loadUrl("file:///android_asset/settings_changeicons.htm");
                
            }

            else if (button.equals(m_Current)) {
                // removed
                AppDetail app = m_IntentsList.get(m_CurrentButton);
                if ("com.bravo.home.HomeActivity".equals(app.appName)
                    || "com.nookdevs.launcher.LauncherSettings".equals(app.appName)) { 
                    m_mainSettings=true;
                    m_ImageChanged=false;
                // can't delete these 2.
                    // Change Icons
                    // display icons gallery
                    while (!m_IconsLoaded) {
                        try {
                            Thread.sleep(2000);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    m_ViewAnimatorMain.setVisibility(View.INVISIBLE);
                    m_ViewAnimator.setVisibility(View.INVISIBLE);
                    m_IconGallery.setVisibility(View.VISIBLE);
                    m_CloseBtn.setVisibility(View.VISIBLE);
                    m_IconAdapter.setCurrentImage(m_CurrentButton.getDrawable());
                    m_IconAdapter.setImageUrls(m_IconFiles);
                    m_IconGallery.setAdapter(m_IconAdapter);
                    m_WebView.loadUrl("file:///android_asset/settings_changeicons.htm");
                    return;
                }
                
                setMainImage(null);
                m_LauncherApps.remove(app.appName);
                addAppToAvailable(app.appName, app.id, app.uri);
                m_CurrentButton = null;
                m_BackBtn.setVisibility(View.VISIBLE);
                
            } else if (m_CurrentButton == null) {
                // Pick button.
                m_CurrentButton = (ImageButton) button;
                m_LinearLayout.removeView(m_CurrentButton);
                setMainImage(m_CurrentButton.getDrawable());
                m_BackBtn.setVisibility(View.INVISIBLE);
                
            } else {
                // put button back.
                if( m_ImageChanged && m_CurrentUri != null) {
                    AppDetail app = m_IntentsList.get(m_CurrentButton);
                    m_LauncherApps.remove(app.appName);
                    m_DBHelper.removeData(app.appName);
                    setMainImage(null);
                    m_BackBtn.setVisibility(View.VISIBLE);
                    updateButtonImage(app.appName, m_CurrentUri.toString(), button);
                    AppDetail appPrev = m_IntentsList.get(button);
                    m_DBHelper.updateData(app.appName, appPrev.appName);
                    m_CurrentButton=null;
                    return;
                }
                final LayoutInflater inflater = getLayoutInflater();
                ImageButton btn = (ImageButton) inflater.inflate(R.layout.settingsbutton, m_LinearLayout, false);
                AppDetail app = m_IntentsList.get(m_CurrentButton);
                fillButton(btn, app.appName, app.id, app.uri);
                int idx = m_LinearLayout.indexOfChild(button);
                m_LinearLayout.addView(btn, idx);
                m_CurrentButton = null;
                setMainImage(null);
                AppDetail appPrev = m_IntentsList.get(button);
                m_DBHelper.updateData(app.appName, appPrev.appName);
                m_BackBtn.setVisibility(View.VISIBLE);
            }
            
        }
        
    }
    protected void updateButtonImage(String appName, String imageUri,View button) {
        try {
                int resid = -1;
                if( imageUri == null || imageUri.trim().equals("")) {
                    if (m_SystemIcons.get(appName) != null) {
                        resid = m_SystemIcons.get(appName);
                        Log.w(LOGTAG, "system app " + appName + " back to launcher");
                    
                    }
                } else {
                    File f = new File(imageUri);
                    imageUri = "/data/data/com.nookdevs.launcher/files/" + f.getName();
                    int num=0;
                    while ( num < 2) {
                        if( f.exists()) {
                            InputStream in = new FileInputStream(f);
                            OutputStream out = openFileOutput(f.getName(), MODE_PRIVATE);
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0){
                                out.write(buf, 0, len);
                            }
                            in.close();
                            out.close();
                        }
                        num++;
                        if( num > 1) break;
                        f = new File( f.getAbsolutePath().replace(".", "_sel."));
                        if( !f.exists()) {
                            f = new File( f.getAbsolutePath().replace("_sel.", "_focused."));
                            if( !f.exists())
                                break;
                        }
                    }
                }
               
                m_DBHelper.addData(appName, resid, imageUri, "0");
                LayoutInflater inflater = getLayoutInflater();
                ImageButton btn1 = (ImageButton) inflater.inflate(R.layout.settingsbutton, m_LinearLayout, false);
                fillButton(btn1, appName, resid, imageUri);
                int idx = m_LinearLayout.indexOfChild(button);
                m_LinearLayout.addView(btn1, idx);
                
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    
    private final class AppDetail {
        String appName;
        int id;
        String uri;
    }
    
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        View selected = ((Gallery) arg0).getSelectedView();
        if (arg1.equals(selected)) {
            ImageView img = (ImageView) arg1;
            String uri = m_IconAdapter.getImageUri(position);
            if (uri != null) {
                m_ClickListener.setCurrentUri(uri);
            } else {
                m_ClickListener.setCurrentImg(img.getDrawable());
            }
        }
    }
}

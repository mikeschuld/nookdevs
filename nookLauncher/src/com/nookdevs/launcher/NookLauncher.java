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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.util.HashMap;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nookdevs.common.nookBaseActivity;

public class NookLauncher extends nookBaseActivity {
    private String m_WallPaperFile = null;
    
    String[] apps =
        {
            "com.bravo.thedaily.Daily", "com.bravo.library.LibraryActivity", "com.bravo.store.StoreFrontActivity",
            "com.bravo.ereader.activities.ReaderActivity", "com.bravo.app.settings.SettingsActivity",
            "com.bravo.app.settings.wifi.WifiActivity",
            "com.nookdevs.launcher.LauncherSettings", "com.bravo.home.HomeActivity",
            "com.nookdevs.launcher.LauncherSelector", "com.bravo.chess.ChessActivity",
            "com.bravo.sudoku.SudokuActivity", "com.bravo.app.browser.BrowserActivity"
        };
    
    int[] appIcons =
        {
            R.drawable.select_home_dailyedition, R.drawable.select_home_library, R.drawable.select_home_store,
            R.drawable.select_home_mybook, R.drawable.select_home_settings, R.drawable.select_home_wifi,
            R.drawable.select_home_launcher_settings,
            R.drawable.select_home_bnhome, R.drawable.select_default_launcher, R.drawable.select_home_chess,
            R.drawable.select_home_sudoku, R.drawable.select_home_browser
        };
    
    final static String readingNowUri = "content://com.reader.android/last";
    ImageButton m_LastButton = null;
    private Uri m_LastImageUri=null;
    
    public final static int DB_VERSION = 11;
    private boolean m_SettingsChanged = false;
    private HashMap<ImageButton,String> m_UriMap = new HashMap<ImageButton,String>();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "nookLauncher";
        NAME = null;
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        loadApps();
    }
    
    @Override
    public void onResume() {
        NAME=null;
        super.onResume();
        if (m_SettingsChanged) {
            loadApps();
        }
        loadWallpaper();
        if (m_LastButton != null) {
            m_LastButton.setBackgroundResource(android.R.color.transparent);
            String icon = m_UriMap.get(m_LastButton);
            if( icon != null)
                m_LastButton.setImageURI(Uri.parse(icon));
        }
    }
    
    private void loadWallpaper() {
        m_WallPaperFile = getWallpaperFile();
        if (m_WallPaperFile != null) {
            try {
                ImageView img = (ImageView) findViewById(R.id.mainimage);
                m_WallPaperFile = m_WallPaperFile.substring(7);
                Bitmap bMap = BitmapFactory.decodeFile(m_WallPaperFile);
                img.setImageBitmap(bMap);
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private final synchronized void loadApps() {
        LinearLayout ll = (LinearLayout) (findViewById(R.id.appcontainer));
        ll.removeAllViews();
        
        final LayoutInflater inflater = getLayoutInflater();
        DBHelper db = new DBHelper(this, null, DB_VERSION);
        Cursor cursor = db.getApps();
        if (cursor == null || cursor.getCount() == 0) {
            db.addInitData(apps, appIcons);
            if (cursor != null) {
                cursor.close();
            }
            cursor = db.getApps();
        }
        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            ImageButton bnv = (ImageButton) inflater.inflate(R.layout.appbutton, ll, false);
            fillButton(bnv, cursor.getString(0), cursor.getInt(1), cursor.getString(2));
            ll.addView(bnv);
            cursor.moveToNext();
        }
        cursor.close();
        db.close();
        inflater.inflate(R.layout.appbutton, ll, false);
    }
    
    private final void fillButton(ImageButton b, String appName, int appIconId, String iconpath) {
        boolean systemApp = false;
        Intent intent = null;
        int idx = appName.lastIndexOf(".");
        String pkgName = appName.substring(0, idx);
        if (pkgName.equals("com.bravo.app.settings.wifi")) {
            pkgName = "com.bravo.app.settings";
       }
        intent = new Intent(Intent.ACTION_MAIN);
        if (appName.endsWith("HomeActivity")) {
            intent.addCategory(Intent.CATEGORY_DEFAULT);
        } else {
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        intent.setComponent(new ComponentName(pkgName, appName));
        boolean settings = false;
        if (appName.endsWith("LauncherSettings")) {
            settings = true;
        }
        
        boolean readingNow = false;
        if (appName.endsWith("ReaderActivity")) {
            readingNow = true;
        }
        b.setOnClickListener(new ClickListener(intent, readingNow, settings));
        if (iconpath == null) {
            if (appIconId > 0) {
                b.setImageResource(appIconId);
                systemApp = true;
            } else {
                PackageManager manager = getPackageManager();
                try {
                    b.setImageDrawable(manager.getActivityIcon(intent.getComponent()));
                } catch (Exception ex) {
                    Log.e(LOGTAG, "Exception loading image -", ex);
                }
            }
        } else {
                Uri iconUri = Uri.parse(iconpath);
                b.setImageURI(iconUri);
                m_UriMap.put(b, iconpath);
                b.setOnTouchListener(new OnTouchListener() {
                    float x,y;
                    public boolean onTouch(View b, MotionEvent arg1) {
                        if( arg1.getAction() == MotionEvent.ACTION_MOVE) {
                            if( Math.abs(x-arg1.getX()) > 10 ||
                                Math.abs(y-arg1.getY()) > 10 ) {
                                ImageButton img  = (ImageButton) b;
                                String icon = m_UriMap.get(img);
                                if( icon != null) {
                                    img.setImageURI(Uri.parse(icon));
                                }
                            }
                        } else
                        if( arg1.getAction() == MotionEvent.ACTION_DOWN) {
                            x=arg1.getX();
                            y=arg1.getY();
                            ImageButton img  = (ImageButton) b;
                            String icon = m_UriMap.get(img);
                            if( icon != null) {
                                int idx = icon.lastIndexOf('.');
                                String ext = icon.substring(idx);
                                String icon1 =icon.replace(ext, "_sel"+ext);
                                File f = new File(icon1);
                                if( f.exists()) {
                                    img.setImageURI( Uri.parse(icon1));
                                } else {
                                    icon1 =icon.replace(ext, "_focused"+ext);
                                    f = new File(icon1);
                                    if( f.exists()) {
                                        img.setImageURI( Uri.parse(icon1));
                                    }
                                    else    
                                        img.setImageURI(Uri.parse(icon));
                                }
                            }
                        }
                        return false;
                    }
                });
        }
        b.setBackgroundResource(android.R.color.transparent);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // doesn't matter what happens. just reload the apps again.
        // loadApps();
    }
    
    private final class ClickListener implements View.OnClickListener {
        private ClickListener(Intent i, boolean readingNow, boolean settings) {
            m_intent = i;
            m_readingNow = readingNow;
            m_Settings = settings;
        }
        
        public void onClick(View v) {
            v.setBackgroundResource(android.R.color.darker_gray);
            m_LastButton = (ImageButton) v;
            if (m_readingNow) {
                try {
                    Intent intent = getReadingNowIntent();
                    startActivity(intent);
                } catch (Exception ex) {
                    Log.e(LOGTAG, "Exception starting reading now activity-", ex);
                }
            } else {
                try {
                    if (!m_Settings) {
                        startActivity(m_intent);
                    } else {
                        m_SettingsChanged = true;
                        startActivityForResult(m_intent, 1);
                    }
                } catch (Exception ex) {
                    Log.e(LOGTAG, "Exception starting activity -", ex);
                }
            }
        }
        
        private final Intent m_intent;
        private final boolean m_readingNow;
        private final boolean m_Settings;
    }
    
    // This logic is from B&N dex file. We may have to check this after each new
    // B&N firmware upgrade.
    private Intent getReadingNowIntent() {
        Intent intent = null;
        try {
            Cursor c =
                getContentResolver().query(Uri.parse("content://com.ereader.android/last"), null, null, null, null);
            if (c != null) {
                c.moveToFirst();
                byte[] data = c.getBlob(0);
                c.close();
                c.deactivate();
                if (data == null) { return null; }
                DataInputStream din = new DataInputStream(new ByteArrayInputStream(data));
                intent = new Intent();
                String tmp = din.readUTF();
                intent.setAction(tmp);
                tmp = din.readUTF();
                String tmp1 = din.readUTF();
                if (tmp != null && tmp.length() > 0) {
                    Uri uri = Uri.parse(tmp);
                    if (tmp1 != null && tmp1.length() > 0) {
                        intent.setDataAndType(uri, tmp1);
                    } else {
                        intent.setData(uri);
                    }
                }
                byte b = din.readByte();
                if (b > 0) {
                    tmp = din.readUTF();
                    tmp1 = din.readUTF();
                    intent.putExtra(tmp, tmp1);
                }
            }
        } catch (Exception ex) {
            intent = null;
        } finally {
            if (intent == null) {
                Toast.makeText(this, "You do not have a current book open right now.", Toast.LENGTH_SHORT).show();
            }
        }
        return intent;
    }
}

/**
 *     This file is part of nookCommon.

    nookCommon is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    nookCommon is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with nookCommon.  If not, see <http://www.gnu.org/licenses/>.

 */

package com.nookdevs.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class nookBaseActivity extends Activity {
    
    public static final int SOFT_KEYBOARD_CLEAR = -13;
    public static final int SOFT_KEYBOARD_SUBMIT = -8;
    public static final int SOFT_KEYBOARD_CANCEL = -3;
    protected static final int NOOK_PAGE_UP_KEY_RIGHT = 98;
    protected static final int NOOK_PAGE_DOWN_KEY_RIGHT = 97;
    protected static final int NOOK_PAGE_UP_KEY_LEFT = 96;
    protected static final int NOOK_PAGE_DOWN_KEY_LEFT = 95;
    
    PowerManager.WakeLock screenLock = null;
    boolean m_AirplaneMode = false;
    long m_ScreenSaverDelay = 600000;
    String m_WallPaper = null;
    AlertDialog m_AlertDialog = null;
    public static final String SDFOLDER = "/system/media/sdcard/";
    public static final String EXTERNAL_SDFOLDER = "/sdcard";
    public final static String UPDATE_TITLE = "com.bravo.intent.UPDATE_TITLE";
    public final static String UPDATE_STATUSBAR = "com.bravo.intent.UPDATE_STATUSBAR";
    
    public final static String STATUSBAR_ICON = "Statusbar.icon";
    public final static String STATUSBAR_ACTION = "Statusbar.action";
    private boolean m_FirstTime = true;
    
    protected String getWallpaperFile() {
        return m_WallPaper;
    }
    
    protected boolean getAirplaneMode() {
        return m_AirplaneMode;
    }
    
    protected long getScreenSaverDelay() {
        return m_ScreenSaverDelay;
    }
    
    protected static String LOGTAG = "nookActivity";
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        screenLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "nookactivity" + hashCode());
        screenLock.setReferenceCounted(false);
        readSettings();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        try {
            if (screenLock != null) {
                screenLock.release();
            }
        } catch (Exception ex) {
            Log.e(LOGTAG, "exception in onPause - ", ex);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (screenLock != null) {
            screenLock.acquire(m_ScreenSaverDelay);
        }
        if (!m_FirstTime) {
            closeAlert();
        }
        m_FirstTime = true;
        
    }
    
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (screenLock != null) {
            screenLock.acquire(m_ScreenSaverDelay);
        }
    }
    
    protected void goHome() {
        String action = "android.intent.action.MAIN";
        String category = "android.intent.category.HOME";
        Intent intent = new Intent();
        intent.setAction(action);
        intent.addCategory(category);
        startActivity(intent);
    }
    
    protected void goBack() {
        try {
            Intent intent = new Intent();
            if (getCallingActivity() != null) {
                intent.setComponent(getCallingActivity());
                startActivity(intent);
            } else {
                goHome();
            }
        } catch (Exception ex) {
            goHome();
        }
    }
    
    public void closeAlert() {
        if (m_AlertDialog != null) {
            m_AlertDialog.dismiss();
        }
    }
    
    public void displayAlert(String title, String msg, final int type, AlertDialog.OnClickListener listener,
        int drawable) {
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(msg);
        if (type == 1) {
            builder.setNegativeButton(android.R.string.cancel, listener).setCancelable(true);
            if (drawable != -1) {
                builder.setIcon(drawable);
            }
        } else if (type == 2 || type == 3) {
            builder.setPositiveButton(android.R.string.ok, listener);
            if (drawable != -1) {
                builder.setIcon(drawable);
            }
        }
        if (type == 3 && drawable != -1) {
            builder.setIcon(drawable);
        }
        m_AlertDialog = builder.show();
    }
    
    protected void updateTitle(String title) {
        try {
            Intent intent = new Intent(UPDATE_TITLE);
            String key = "apptitle";
            intent.putExtra(key, title);
            sendBroadcast(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    protected void showPageNumber(int curpage, int maxpage) {
        Intent msg = new Intent(UPDATE_STATUSBAR);
        msg.putExtra(STATUSBAR_ICON, 7);
        msg.putExtra(STATUSBAR_ACTION, 1);
        msg.putExtra("current", curpage);
        msg.putExtra("max", maxpage);
        sendBroadcast(msg);
    }
    
    private void printCursor(Cursor c) {
        c.moveToFirst();
        int columns = c.getColumnCount();
        String[] names = c.getColumnNames();
        while (!c.isAfterLast()) {
            for (int i = 0; i < columns; i++) {
                System.out.println(names[i] + " = " + c.getString(i));
            }
            c.moveToNext();
        }
    }
    
    protected void readSettings() {
        String[] values = {
            "value"
        };
        String name = null;
        String[] fields = {
            "airplane_mode_on", "bnScreensaverDelay", "bnWallpaper"
        };
        
        try {
            for (String field : fields) {
                if (name == null) {
                    name = "name=?";
                } else {
                    name += " or name=?";
                }
            }
            Cursor c = getContentResolver().query(Uri.parse("content://settings/system"), values, name, fields, "name");
            // printCursor(c);
            if (c != null) {
                c.moveToFirst();
                int value = c.getInt(0);
                if (value == 0) {
                    m_AirplaneMode = false;
                } else {
                    m_AirplaneMode = true;
                }
                c.moveToNext();
                long lvalue = c.getLong(0);
                if (lvalue > 0) {
                    m_ScreenSaverDelay = lvalue;
                }
                c.moveToNext();
                m_WallPaper = c.getString(0);
                Log.d(LOGTAG, "m_Wallpaper = " + m_WallPaper);
                
            }
            c.close();
            c.deactivate();
            
        } catch (Exception ex) {
            Log.e(LOGTAG, "Error reading system settings... keeping hardcoded values");
            ex.printStackTrace();
        }
    }
    
}
/*
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *              http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.nookdevs.mines;

//
//  This is just a base class that the rest of the program inherits from
//

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.database.Cursor;
import android.content.Intent;
import android.util.Log;
import android.os.PowerManager;
import android.net.Uri;
import android.content.SharedPreferences;

public abstract class MinesActivity extends Activity {
    PowerManager.WakeLock screenLock = null;
    long m_ScreenSaverDelay = 600000;  // default value; will be replaced with Nook setting
    public static String TITLE = "Mines";

    SharedPreferences mSettings ;
    static final String MINES_PREFERENCES = "nookMinesPreferences" ;
    static final String MINES_PREFERENCES_ROWS = "ROWS" ;
    static final String MINES_PREFERENCES_COLS = "COLS" ;
    static final String MINES_PREFERENCES_NUM_MINES = "NUM_MINES" ;
    
    //  {rows, cols, num_mines}
	public static final int[][] mineFieldSizes = {
		{ 8, 8, 10 },
		{ 10, 8, 13 },
		{ 13, 10, 21 },
		{ 16, 12, 30 },
		{ 16, 16, 40 },
		{ 22, 16, 55 },
	} ;
    static final int DEFAULT_ROWS =  mineFieldSizes[1][0] ;
    static final int DEFAULT_COLS =  mineFieldSizes[1][1] ;
    static final int DEFAULT_NUM_MINES = mineFieldSizes[1][2] ;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        
        // android power management screen stuff:
        initializeScreenLock();
        
        // settings:
        mSettings = getSharedPreferences( MINES_PREFERENCES, Context.MODE_PRIVATE ) ;

    } // onCreate

    // onResume:
    @Override
    public void onResume() {
        super.onResume();
        acquireScreenLock();
        updateTitle(TITLE);
    }  // onResume

    @Override
    public void onPause() {
        super.onPause();
        releaseScreenLock();
    } // onPause

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        acquireScreenLock();
    } // onUserInteraction

    ///////////////////////////////////////////////////////////////////////////////

    //  Android was designed for phones with back-lit screens; it doesn't know
    //  that the Nook's e-ink display doesn't use power when displaying a static
    //  image.
    //  So, we want to prevent android from blanking the e-ink display on us.
    //  Called from onCreate:
    //  Adapted from nookDevs code:
    private void initializeScreenLock() {
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        screenLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "nookactivity" + hashCode());
        screenLock.setReferenceCounted(false);
        String[] values = {
                "value"
        };
        String[] fields = {
                        "bnScreensaverDelay"
        } ;
        Cursor c = getContentResolver().query(Uri.parse("content://settings/system"), values, "name=?", fields, "name");
        if (c != null) {
            c.moveToFirst();
            long lvalue = c.getLong(0);
            if (lvalue > 0) {
                m_ScreenSaverDelay = lvalue;
            }
        }
    }  // initializeScreenLock
    // Called from onPause:
    private void releaseScreenLock() {
        try {
            if (screenLock != null) {
                screenLock.release();
            }
        } catch (Exception ex) {
            Log.e(this.toString(), "exception releasing screenLock - ", ex);
            finish();
        }
    } // releaseScreenLock
    //  Called from onResume and onUserInteraction:
    private void acquireScreenLock() {
        if (screenLock != null) {
            screenLock.acquire(m_ScreenSaverDelay);
        }
    }  // acquireScreenLock

    //  Update the title bar:
    //  Taken from nookDevs common:
    public final static String UPDATE_TITLE = "com.bravo.intent.UPDATE_TITLE";
    protected void updateTitle(String title) {
        try {
            Intent intent = new Intent(UPDATE_TITLE);
            String key = "apptitle";
            intent.putExtra(key, title);
            sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // updateTitle

    //  Taken from nookDevs common:
    protected void goHome() {
        String action = "android.intent.action.MAIN";
        String category = "android.intent.category.HOME";
        Intent intent = new Intent();
        intent.setAction(action);
        intent.addCategory(category);
        startActivity(intent);
    } // goHome
    //  Taken from nookDevs common:
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
    } // goBack
    
}
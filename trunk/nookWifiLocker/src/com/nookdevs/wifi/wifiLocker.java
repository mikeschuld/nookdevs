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
package com.nookdevs.wifi;

import android.net.ConnectivityManager;
//import android.net.MobileDataStateTracker;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.nookdevs.common.nookBaseActivity;

public class wifiLocker extends nookBaseActivity implements OnClickListener {
    
    ConnectivityManager cmgr;
    ConnectivityManager.WakeLock lock;
    PowerManager.WakeLock touchscreenLock = null;
    PowerManager.WakeLock screensaverLock = null;
    Button back;
    Button wifi;
    Button touchscreen;
    Button screensaver;
    boolean locked = false;
    Handler m_Handler = new Handler();
    //MobileDataStateTracker tracker = null; -for 3G locking

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        lock = cmgr.newWakeLock(1, "wifiLocker" + hashCode());
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        screensaverLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "wifiLocker" + hashCode());
        touchscreenLock = power.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "wifiLocker" + hashCode());
        back = (Button) findViewById(R.id.back);
        back.setOnClickListener( new OnClickListener() {
            public void onClick(View arg0) {
                goBack();
            }
        }
        );
        wifi = (Button) findViewById(R.id.wifi);
        wifi.setText(R.string.wifilock);
        wifi.setOnClickListener( new OnClickListener() {
            public void onClick(View arg0) {
                if (lock.isHeld()) {
                    lock.release();
                    wifi.setText(R.string.wifilock);
                } else {
                    lock.acquire();
                    WifiTask task = new WifiTask();
                    task.execute();
                } 
            }
        }
        );
        touchscreen = (Button) findViewById(R.id.touchscreen);
        touchscreen.setText(R.string.touchscreenlock);
        screensaver = (Button) findViewById(R.id.screensaver);
        screensaver.setText(R.string.screensaverlock);
        touchscreen.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (touchscreenLock.isHeld()) {
                    touchscreenLock.release();
                    touchscreen.setText(R.string.touchscreenlock);
                } else {
                    touchscreenLock.acquire();
                    touchscreen.setText(R.string.touchscreenunlock);
                } 
            }
        }
        );
        screensaver.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (screensaverLock.isHeld()) {
                    screensaverLock.release();
                    screensaver.setText(R.string.screensaverlock);
                } else {
                    screensaverLock.acquire();
                    screensaver.setText(R.string.screensaverunlock);
                } 
            }
        }
        );
      //  tracker = new MobileDataStateTracker(this, m_Handler);
    }
    
    public void onClick(View v) {
            /*
            if( !locked) {
                tracker.reconnect();
                tracker.setRadio(true);
                locked=true;
                wifi.setText(R.string.unlock);
            } else {
                tracker.setRadio(false);
                locked=false;
                wifi.setText(R.string.lock);
            } */
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (lock.isHeld()) {
            lock.release();
        }
        if( screensaverLock.isHeld()) {
            screensaverLock.release();
        }
        if( touchscreenLock.isHeld()) {
            touchscreenLock.release();
        }
    }
    
    class WifiTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            displayAlert(getString(R.string.start_wifi), getString(R.string.please_wait), 1, null, -1);
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
            ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            boolean connection = (info == null) ? false : info.isConnected();
            int attempts = 1;
            while (!connection && attempts < 20) {
                try {
                    Thread.sleep(3000);
                } catch (Exception ex) {
                    
                }
                info = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                connection = (info == null) ? false : info.isConnected();
                attempts++;
            }
            return connection;
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            closeAlert();
            if (!result) {
                displayAlert(getString(R.string.wifi_timeout), "", 2, null, -1);
            } else {
                wifi.setText(R.string.wifiunlock);
            }
        }
    }
}
package com.nookdevs.wifi;

import com.nookdevs.common.nookBaseActivity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class wifiLocker extends nookBaseActivity implements OnClickListener {
	
	ConnectivityManager cmgr;
	ConnectivityManager.WakeLock lock;
	Button back;
	Button wifi;
	boolean locked=false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        cmgr = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        lock = cmgr.newWakeLock(1,"wifiLocker" + hashCode());
        back = (Button) findViewById(R.id.back);
        wifi = (Button) findViewById(R.id.wifi);
        wifi.setText(R.string.lock);
        wifi.setOnClickListener(this);
        back.setOnClickListener(this);
        locked=false;
    }

	public void onClick(View v) {
		if( v.equals(back)) {
			goHome();
		} else {
			if( locked && lock.isHeld()) {
				lock.release();
				wifi.setText(R.string.lock);
				locked=false;
			} else {
				lock.acquire();
				WifiTask task = new WifiTask();
				task.execute();
			}
		}
		
	}
	public void onDestroy() {
		if( lock.isHeld()) 
			lock.release();
	}
	 class WifiTask extends AsyncTask<Void, Integer, Boolean> {
	        @Override
	        protected void onPreExecute() {
	            displayAlert(getString(R.string.start_wifi), 
	            		getString(R.string.please_wait), 1, null, -1);
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
	            if( !result) {
	            	displayAlert(getString(R.string.wifi_timeout), 
		            		"", 2, null, -1);
	            } else {
	            	wifi.setText(R.string.unlock);
	            	locked=true;
	            }
	        }
	    }
}
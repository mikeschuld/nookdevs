/*
 * Copyright 2011 nookDevs
 * 
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
 * 
 * Written by Kevin Vajk
 */
package com.nookdevs.buttonswapper;
import android.app.Activity;
import android.os.Bundle;
import android.database.Cursor;
import android.content.Intent;
import android.util.Log;
import android.os.PowerManager;
import android.net.Uri;


public abstract class BaseActivity extends Activity {

	protected static final String LOGTAG = "NookButtonSwapper" ;

	PowerManager.WakeLock screenLock = null;
	long m_ScreenSaverDelay = 600000; // default value; will be replaced with
										// Nook setting

	///////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// android power management screen stuff:
		initializeScreenLock();

		updateTitle( getString(R.string.app_name) );
	} // onCreate

	@Override
	public void onResume() {
		super.onResume();
		acquireScreenLock();
		updateTitle( getString(R.string.app_name) );
	} // onResume

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

	protected static final int NOOK_PAGE_UP_KEY_RIGHT = 98;
	protected static final int NOOK_PAGE_DOWN_KEY_RIGHT = 97;
	protected static final int NOOK_PAGE_UP_KEY_LEFT = 96;
	protected static final int NOOK_PAGE_DOWN_KEY_LEFT = 95;
	protected static final int NOOK_PAGE_DOWN_SWIPE = 100;
	protected static final int NOOK_PAGE_UP_SWIPE = 101;

	// Android was designed for phones with back-lit screens; it doesn't know
	// that the Nook's e-ink display doesn't use power when displaying a static
	// image.
	// So, we want to prevent android from blanking the e-ink display on us.
	// Called from onCreate:
	// Adapted from nookDevs code:
	private void initializeScreenLock() {
		PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
		screenLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"nookactivity" + hashCode());
		screenLock.setReferenceCounted(false);
		String[] values = { "value" };
		String[] fields = { "bnScreensaverDelay" };
		Cursor c = getContentResolver().query(
				Uri.parse("content://settings/system"), values, "name=?",
				fields, "name");
		if (c != null) {
			c.moveToFirst();
			long lvalue = c.getLong(0);
			if (lvalue > 0) {
				m_ScreenSaverDelay = lvalue;
			}
		}
	} // initializeScreenLock

	// Called from onPause:
	private void releaseScreenLock() {
		try {
			if (screenLock != null) {
				screenLock.release();
			}
		} catch (Exception ex) {
			Log.e(LOGTAG, "exception releasing screenLock - ", ex);
			finish();
		}
	} // releaseScreenLock

	// Called from onResume and onUserInteraction:
	private void acquireScreenLock() {
		if (screenLock != null) {
			screenLock.acquire(m_ScreenSaverDelay);
		}
	} // acquireScreenLock

	// Update the title bar:
	// Taken from nookDevs common:
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

	// Taken from nookDevs common:
	protected void goHome() {
		String action = "android.intent.action.MAIN";
		String category = "android.intent.category.HOME";
		Intent intent = new Intent();
		intent.setAction(action);
		intent.addCategory(category);
		startActivity(intent);
	} // goHome

	// Taken from nookDevs common:
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

	
} // CrossWord class

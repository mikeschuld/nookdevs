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


package com.nookdevs.mtextview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View ;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Typeface;


public class BaseActivity extends Activity {
	
	protected static final String DEFAULT_TITLE = "Text Viewer";
	protected static final String DEFAULT_FOLDER = "/system/media/sdcard/my documents" ;

	private PowerManager.WakeLock screenLock = null;
	private long m_ScreenSaverDelay = 600000;  // default value; will be replaced with Nook setting

	protected SharedPreferences mSettings ;
	protected static final String TEXTEDITOR_PREFERENCES = "mTextViewPreferences" ;
	protected static final String TEXTEDITOR_PREFERENCES_FONTNAME = "FONTNAME" ;
	protected static final String TEXTEDITOR_PREFERENCES_FONTSIZE = "FONTSIZE" ;
	//protected static final String TEXTEDITOR_PREFERENCES_SHOWSCROLLBAR = "SHOWSCROLLBAR" ;
	protected final FontDescription[] fonts = {
			new FontDescription("Serif", Typeface.SERIF),
			new FontDescription("Sans Serif", Typeface.SANS_SERIF),
			new FontDescription("Monospace", Typeface.MONOSPACE),
	} ;
	protected final float[] fontsizes = {
		20f, 25f, 30f, 35f, 40f, 45f, 50f, 55f,
	} ;
	protected static final String DEFAULT_FONTNAME = "Sans Serif" ;
	protected static final float  DEFAULT_FONTSIZE = 30f ;
	protected static final boolean DEFAULT_SHOWSCROLLBAR = false ;

	protected final int[] defaultPadding = {40, 25, 40, 30 } ;  // left top right bottom

	protected static final String FILE = "FILE" ;
	protected static final String SEARCHTERM = "SEARCHTERM" ;

	protected static final int SOFT_KEYBOARD_CLEAR = -13;
	protected static final int SOFT_KEYBOARD_SUBMIT = -8;
	protected static final int SOFT_KEYBOARD_CANCEL = -3;
	protected static final int SOFT_KEYBOARD_DOWN_KEY = 20;
	protected static final int SOFT_KEYBOARD_UP_KEY = 19;
	protected static final int NOOK_PAGE_UP_KEY_RIGHT = 98;
	protected static final int NOOK_PAGE_DOWN_KEY_RIGHT = 97;
	protected static final int NOOK_PAGE_UP_KEY_LEFT = 96;
	protected static final int NOOK_PAGE_DOWN_KEY_LEFT = 95;
	protected static final int NOOK_PAGE_DOWN_SWIPE = 100;
	protected static final int NOOK_PAGE_UP_SWIPE = 101;

	//  TODO: these are redundant:
	protected static final String[] allowed_extensions_filter = {".*\\.txt", ".*\\.html", ".*\\.htm",
		".*\\.xml", ".*\\.css", ".*\\.js", ".*\\.sh", ".*\\.java", ".*\\.py", ".*\\.pl",
		".*\\.c", ".*\\.xpf"  };
	protected static final String[] allowed_extensions = {".txt", ".html", ".htm",
		".xml", ".css", ".js", ".sh", ".java", ".py", ".pl",
		".c", ".xpf"  };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// android power management screen stuff:
		initializeScreenLock();

		// settings:
		mSettings = getSharedPreferences( TEXTEDITOR_PREFERENCES, Context.MODE_PRIVATE ) ;

	} // onCreate

	// onResume:
	@Override
	public void onResume() {
		super.onResume();
		acquireScreenLock();
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

	protected void showKeyboard(View v) {
		InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
	} // showKeyboard

	protected final void hideKeyboard(View v) {
		InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	} // hideKeyboard
	
	///////////////////////////////////////////////////////////////////////////////

	protected Typeface getTypeface(String fontName) {
		for( FontDescription f : fonts ) {
			if ( f.name.equals(fontName) ) {
				return(f.typeface);
			}
		}
		Log.e( this.toString(), "Internal error: getTypeface() fell through" );
		return( Typeface.DEFAULT ); // should never happen
	} // getTypeface

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
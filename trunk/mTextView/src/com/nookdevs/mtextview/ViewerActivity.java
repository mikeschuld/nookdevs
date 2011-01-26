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

/*
 * This program is a text file viewer for the nook classic.
 */

package com.nookdevs.mtextview;

import android.os.Bundle;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import java.util.ArrayList;
import android.widget.ViewAnimator;
import android.util.Log;
import android.widget.Toast;
import android.view.Gravity;
import android.widget.SeekBar;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

// TODO: progress indicator on eink screen
// TODO: include search term on search buttons
// TODO: search in a background thread, and maybe use a cleaner algorithm


public class ViewerActivity extends BaseActivity implements NookMenuInterface {
	//
	private String currentFileName = null ;
	private TextFileView textFileView ;
	//
	private NookMenu nookMenu ;
	private SeekBar gotoSeekBar ;
	private ViewAnimator touchscreenanimator;
	private TextView sliderPercentage ;
	//
	private DatabaseHelper databaseHelper;
	//
	private float currentFontSize;
	private String currentFontName;
	//
	String currentSearchTerm = null ;
	long[] firstMatch = null ;
	long[] currentMatch = null ;
	long matchDepth = 0 ; // used to detect search wrap-around, can be positive or negative

	//  The views we animate between on the touchscreen:
	private static final int NOOKMENU_VIEWNUM = 0 ;
	private static final int SEEKBAR_VIEWNUM = 1 ;

	//  Activities we'll launch
	private static final int ACTIVITY_SETTINGS = 1;
	private static final int ACTIVITY_OPEN_FILE = 2;
	private static final int ACTIVITY_SEARCH_TERM_DIALOG = 3;

	//  Define our menus:
	private static final int MAIN_MENU = 0 ;
	private static final int FIND_MENU = 1 ;
	private static final int SETTINGS_MENU = 2 ;
	private static final int SETTINGS_SUB_MENU_FONTSIZE = 3 ;
	private static final int SETTINGS_SUB_MENU_FONTNAME = 4 ;
	private int[] mainmenu_items = {
			//R.string.open_file_button_label,
			R.string.search_button_label,
			R.string.goto_button_label,
			R.string.settings_button_label,
	};
	private int[] findmenu_items = {
			R.string.find_next_button_label,
			R.string.find_prev_button_label,
			R.string.find_new_button_label,
	};
	private int[] settingsmenu_items = {
			R.string.fontsizebutton_label,
			R.string.fontnamebutton_label,
	};



	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		databaseHelper = new DatabaseHelper(this);

		//  Our main reading window:
		textFileView = new TextFileView( (TextView)findViewById(R.id.text) );
		textFileView.setPadding(defaultPadding);

		//  Animate between the nook menu, and the slider bar:
		touchscreenanimator = (ViewAnimator) findViewById(R.id.touchscreen_animator);

		//  Our menus:
		nookMenu = new NookMenu( this, (ViewAnimator)findViewById(R.id.nookMenuAnimator) );
		nookMenu.addMenu( MAIN_MENU, mainmenu_items );
		nookMenu.addMenu( FIND_MENU, findmenu_items );
		nookMenu.addMenu( SETTINGS_MENU, settingsmenu_items );
		nookMenu.addMenu( SETTINGS_SUB_MENU_FONTSIZE, fontsizes.length );
		nookMenu.addMenu( SETTINGS_SUB_MENU_FONTNAME, fonts.length );
		nookMenu.setMainMenu(MAIN_MENU);

		// Touchscreen back button:
		((Button) findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( touchscreenanimator.getDisplayedChild() == NOOKMENU_VIEWNUM ) {
					if ( nookMenu.getMenuDepth() == 0 ) {
						finish();
					} else {
						// special case if we're leaving the search menu:
						if ( nookMenu.getCurrentMenuID() == FIND_MENU ) {
							textFileView.setHighlightedRegions(null);
							textFileView.setSecondaryHighlightedRegions(null);
						}
						nookMenu.goBack();
					}
				} else {
					touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromleft);
					touchscreenanimator.setDisplayedChild( NOOKMENU_VIEWNUM );
				}
			}
		});

		//  The "Go to" horizontal seekbar:
		//goto_seekbar_container = (LinearLayout) findViewById(R.id.goto_seekbar_container) ;
		gotoSeekBar = (SeekBar) findViewById( R.id.goto_seekbar );
		gotoSeekBar.setOnSeekBarChangeListener( new GotoSeekBarListener() );
		sliderPercentage = (TextView) findViewById( R.id.goto_seekbar_text );

		//  Get current preferences values and apply to views:
		loadPreferences();  // Note: this depends on edittext being defined already!

		//  If a filename was passed in to us, open it:
		Intent intent = getIntent();
		if ( intent != null ) {
			Uri uri = intent.getData();
			if ( uri != null ) {
				String filename = uri.getPath();
				if ( filename != null ) {
					openFile(filename) ;  // Note: this needs textFileView to be set
				}
			}
		}

	} // onCreate

	@Override
	public void onResume() {
		super.onResume();
		setViewerTitle();
	} // onResume

	@Override
	public void onPause() {
		if (currentFileName != null) saveOffset();
		super.onPause();
	} // onPause

	@Override    
	protected void onDestroy() {        
	    super.onDestroy();
	    databaseHelper.close();
	}

	/**********************************************************************************/

	@Override 
	protected void onActivityResult(int requestCode,int resultCode,Intent data) {
		switch(requestCode) {
		case ACTIVITY_SETTINGS:
			loadPreferences();
			break ;
		case ACTIVITY_OPEN_FILE:
			if( resultCode == RESULT_OK && data != null) {
				Bundle b = data.getExtras();
				if( b != null) {
					String filename = b.getString(FILE);
					if( filename != null) {
						openFile( filename );
						nookMenu.goToMainMenu();
					}
				}
			}
			if ( currentFileName == null ) {
				goBack(); // finish
			}
			break ;
		case ACTIVITY_SEARCH_TERM_DIALOG:
			boolean startedSearch = false ;
			if( resultCode == RESULT_OK && data != null) {
				Bundle b = data.getExtras();
				if( b != null) {
					currentSearchTerm = b.getString(SEARCHTERM) ;
					if (currentSearchTerm != null) {
						currentSearchTerm = currentSearchTerm.trim();
					}
					if ( (currentSearchTerm != null) && (! currentSearchTerm.equals("")) ) {
						//  We got a valid search term
						if ( nookMenu.getCurrentMenuID() != FIND_MENU ) {
							nookMenu.goIntoSubMenu(FIND_MENU);              
						}
						startedSearch = true ;
						findFirst();
					}
				}
			}
			if (! startedSearch) nookMenu.goToMainMenu();
			break ;
		}
	} // onActivityResult

	/**********************************************************************************/

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case NOOK_PAGE_UP_KEY_LEFT:
		case NOOK_PAGE_UP_KEY_RIGHT:
		case NOOK_PAGE_UP_SWIPE:
			textFileView.pageUp();
			if (  touchscreenanimator.getDisplayedChild() == SEEKBAR_VIEWNUM ) {
				gotoSeekBarRefresh();
			}
			return (true);
		case NOOK_PAGE_DOWN_KEY_LEFT:
		case NOOK_PAGE_DOWN_KEY_RIGHT:
		case NOOK_PAGE_DOWN_SWIPE:
			textFileView.pageDown();
			if (  touchscreenanimator.getDisplayedChild() == SEEKBAR_VIEWNUM ) {
				gotoSeekBarRefresh();
			}
			return (true);
		}
		return (false); // we didn't handle this key event
	} // onKeyDown

	/**********************************************************************************/

	/*
	 *  Our menus:
	 */

	public Integer nookMenuGetItemIcon( int menuID, int itemID ) {
		switch(menuID) {
		case SETTINGS_SUB_MENU_FONTSIZE:
			if ( currentFontSize ==  fontsizes[itemID] ) {
				return( R.drawable.check_mark_pressable );
			} else {
				return null ;
			}
		case SETTINGS_SUB_MENU_FONTNAME:
			if ( currentFontName.equals( fonts[itemID].name ) ) {
				return( R.drawable.check_mark_pressable );
			} else {
				return null ;
			}
		default:
			switch(itemID) {
			case R.string.file_button_label:
			case R.string.goto_button_label:
			case R.string.settings_button_label:
			case R.string.fontnamebutton_label:
			case R.string.fontsizebutton_label:
			case R.string.open_file_button_label:
				return( R.drawable.submenu_image );
			case R.string.search_button_label:
				return( R.drawable.search_image );
			}
			return null;
		}
	} // nookMenuGetItemIcon

	public String nookMenuGetItemLabel( int menuID, int itemID ) {
		switch(menuID) {
		case SETTINGS_SUB_MENU_FONTSIZE:
			return( "" + fontsizes[itemID] );
		case SETTINGS_SUB_MENU_FONTNAME:
			return( fonts[itemID].name );
		default:
			return( getString(itemID) ) ;
		}
	} // nookMenuGetItemLabel

	//  The NookMenu class calls back to us here when the user clicks
	//  on a menu item:
	public void nookMenuOnClickAction( int menuID, int itemID ) {
		switch(menuID) {
		case SETTINGS_SUB_MENU_FONTSIZE:
			changeFontSize( fontsizes[itemID] );
			nookMenu.refresh();
			break;
		case SETTINGS_SUB_MENU_FONTNAME:
			changeFontName( (fonts[itemID]).name );
			nookMenu.refresh();
			break;
		default:
			switch(itemID) {
			case R.string.search_button_label:
				launchSearchTermDialog();
				break;
			case R.string.goto_button_label:
				gotoSeekBarRefresh();
				touchscreenanimator.setInAnimation( getApplicationContext(), R.anim.fromright);
				touchscreenanimator.setDisplayedChild(SEEKBAR_VIEWNUM);
				break;
			case R.string.settings_button_label:
				nookMenu.goIntoSubMenu( SETTINGS_MENU );
				break;
			case R.string.fontnamebutton_label:
				nookMenu.goIntoSubMenu( SETTINGS_SUB_MENU_FONTNAME );
				break;
			case R.string.fontsizebutton_label:
				nookMenu.goIntoSubMenu( SETTINGS_SUB_MENU_FONTSIZE );
				break;
			case R.string.open_file_button_label:
				launchFileSelector( DEFAULT_FOLDER );
				break;
			case R.string.find_next_button_label:
				findNext();
				break;
			case R.string.find_prev_button_label:
				findPrevious();
				break;
			case R.string.find_new_button_label:
				launchSearchTermDialog();
				break;
			}
		}
	} // nookMenuOnClickAction


	/**********************************************************************************/

	private void launchSearchTermDialog() {
		try {
			Intent intent = new Intent(ViewerActivity.this, SearchTermDialog.class) ;
			if ( currentSearchTerm != null ) {
				intent.putExtra(SEARCHTERM, currentSearchTerm );
			}
			startActivityForResult( intent, ACTIVITY_SEARCH_TERM_DIALOG );
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception launching SearchTermDialog activity: " + ex );
		}
	} //launchSearchTermDialog

	/**********************************************************************************/

	private boolean launchFileSelector(String m_folder) {
		try {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.ACTION_DEFAULT);
			intent.setComponent(new ComponentName("com.nookdevs.fileselector", "com.nookdevs.fileselector.FileSelector"));
			intent.putExtra("ROOT", m_folder);
			intent.putExtra("FILTER", allowed_extensions_filter );
			intent.putExtra("TITLE", getString(R.string.my_documents));
			startActivityForResult(intent, ACTIVITY_OPEN_FILE);
			return true;
		} catch(Exception ex) {
			return false;
		}
	} // launchFileSelector

	/**********************************************************************************/

	/*
	 * These implement our "go to" slider:
	 */

	private class GotoSeekBarListener implements SeekBar.OnSeekBarChangeListener {
		int progress ;
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			//  Called repeatedly as the slider is moved around
			this.progress = progress ;
			sliderPercentage.setText( progress + "%" );
		} // onProgressChanged
		public void onStartTrackingTouch(SeekBar seekBar) {
		} // onStartTrackingTouch
		public void onStopTrackingTouch(SeekBar seekBar) {
			textFileView.seekToPercentageOffset( progress );
		} // onStopTrackingTouch
	} // GotoSeekBarListener

	private void gotoSeekBarRefresh() {
		//gotoSeekBar.setMax(100);
		int progress = textFileView.getPercentageOffset() ;
		gotoSeekBar.setProgress( progress );
		sliderPercentage.setText( progress + "%" );
	} // gotoSeekBarRefresh


	/**********************************************************************************/

	private void setViewerTitle() {
		String title ;
		if ( currentFileName == null ) {
			title = DEFAULT_TITLE;
		} else {
			title = currentFileName.replaceAll("^.*/","") ;
		}
		updateTitle(title);
	} // setViewerTitle

	/**********************************************************************************/

	//  This is run when we start up, and also whenever we change our settings:
	protected void loadPreferences() {
		try {
			currentFontName = mSettings.getString(TEXTEDITOR_PREFERENCES_FONTNAME, DEFAULT_FONTNAME);
			currentFontSize = mSettings.getFloat(TEXTEDITOR_PREFERENCES_FONTSIZE, DEFAULT_FONTSIZE);
			textFileView.setFont(  getTypeface(currentFontName), currentFontSize );
		} catch (Exception ex) {
			Log.e( this.toString(), "Error loading preferences: " + ex );
		}
	} // loadPreferences

	/**********************************************************************************/

	private void changeFontSize(float newFontSize) {
		Editor editor = mSettings.edit();
		editor.putFloat( TEXTEDITOR_PREFERENCES_FONTSIZE, newFontSize );
		editor.commit();
		currentFontSize = newFontSize ;
		textFileView.setFont(  getTypeface(currentFontName), currentFontSize );
	} // changeFontSize

	private void changeFontName(String newFontName) {
		Editor editor = mSettings.edit();
		editor.putString( TEXTEDITOR_PREFERENCES_FONTNAME, newFontName );
		editor.commit();
		currentFontName = newFontName ;
		textFileView.setFont(  getTypeface(currentFontName), currentFontSize );
	} // changeFontName

	/**********************************************************************************/

	private void showToast(String s) {
		try {
			Toast toast = Toast.makeText( getApplicationContext(), s, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0); // grav, x-off, y-off
			toast.show();
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: exception in showToast(): " + ex );
		}
	} // showToast
	private void showToast(int stringId) {
		try {
			showToast( getString(stringId) );
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: exception in showToast(): " + ex );
		}
	} // showToast

	/**********************************************************************************/

	private void openFile( String filename ) {
		boolean open_succeeded = textFileView.loadFile( filename, getSavedOffset(filename) );
		if ( ! open_succeeded ) {
			showToast( R.string.error_file_open_failed );
			return;
		}
		currentFileName = filename ;
		setViewerTitle();
	} // openFile

	/**********************************************************************************/

	private long getSavedOffset(String fileName) {
		long offset ;
		try {
			offset = databaseHelper.getSavedOffset(fileName) ;
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception trying to get saved offset: " + ex );
			offset = 0 ;
		}
		return(offset);
	} // getSavedOffset

	private void saveOffset() {
		if ( currentFileName == null ) return;
		long offset ;
		long fileLength = textFileView.getFileLength() ;
		long pageStart = textFileView.currentPage_StartOffset();
		long pageEnd = textFileView.currentPage_EndOffset();
		if ( pageEnd == textFileView.getFileLength() ) {
			offset = pageEnd ;
		} else {
			offset = pageStart ;
		}
		databaseHelper.saveOffset( currentFileName, fileLength, offset );
	} // saveOffset

	/**********************************************************************************/

	//  Searches a region of the file:
	//  Each long[] returned is a matching word; the first number is the start of
	//  the word, the second number is the end.
	private ArrayList<long[]> findMatches(String searchterm, long start, long end) {
		ArrayList<long []> matches = new ArrayList<long []>() ;
		long numbytes = end - start + searchterm.length() - 1 ;
		String s = textFileView.readFromCurrentFile(start, numbytes);
		byte[] filebytes = s.toLowerCase().getBytes();
		byte[] searchbytes = searchterm.toLowerCase().getBytes();
		int maxi = filebytes.length - searchbytes.length ;
		for( int i = 0 ; i < maxi ; i++ ) {
			for( int j = 0 ; j < searchbytes.length ; j++ ) {
				if ( filebytes[i+j] != searchbytes[j] ) break;
				if ( j == searchbytes.length - 1 ) {
					long[] r = new long[2];
					r[0] = start + i ; r[1] = r[0] + searchterm.length();
					matches.add(r);
				}
			}
		}
		return(matches);
	} // findMatches

	//  Search forwards from current location:
	//  (Called by findNext().)
	private long[] findNextMatch( long[] lastMatch ) {
		long stepsize = 8192 ;
		if ( (currentSearchTerm == null) || currentSearchTerm.equals("") ) return(null) ;
		long startOffset ;
		if ( lastMatch == null ) {
			startOffset = textFileView.currentPage_StartOffset();
		} else {
			startOffset = lastMatch[1];
		}
		//  Seek from current location towards EOF:
		for(long start = startOffset ; start < textFileView.getFileLength() ; start += stepsize) {
			long end = start + stepsize ;
			if (end > textFileView.getFileLength()) end = textFileView.getFileLength();
			ArrayList<long[]> localmatches = findMatches(currentSearchTerm, start, end) ;
			if ( localmatches != null && localmatches.size() > 0 ) {
				return( localmatches.get(0) );
			}
		}
		//  OK, try from the beginning of the file:
		for(long start = 0 ; start < startOffset ; start += stepsize) {
			long end = start + stepsize ;
			if (end > startOffset) end = startOffset;
			ArrayList<long[]> localmatches = findMatches(currentSearchTerm, start, end) ;
			if ( localmatches != null && localmatches.size() > 0 ) {
				return( localmatches.get(0) );
			}
		}
		return(null);
	} // findNextMatch

	//  Search backwards from current location:
	//  (Called by findPrevious().)
	private long[] findPreviousMatch( long[] previousMatch ) {
		long stepsize = 8192 ;
		if ( (currentSearchTerm == null) || currentSearchTerm.equals("") ) return(null) ;
		long endOffset ;
		if ( previousMatch == null ) {
			endOffset = textFileView.currentPage_EndOffset();  // won't happen
		} else {
			endOffset = previousMatch[0];
		}
		//  Seek from current location towards beginning of file:
		for(long end = endOffset ; end >= 0 ; end -= stepsize) {
			long start = end - stepsize ;
			if (start < 0) start = 0;
			ArrayList<long[]> localmatches = findMatches(currentSearchTerm, start, end) ;
			if ( localmatches != null && localmatches.size() > 0 ) {
				return( localmatches.get( localmatches.size() - 1 ) );
			}
		}
		//  OK, try from the end of the file:
		for(long end = textFileView.getFileLength() ; end > endOffset ; end -= stepsize) {
			long start = end - stepsize ;
			if (start < endOffset) start = endOffset;
			ArrayList<long[]> localmatches = findMatches(currentSearchTerm, start, end) ;
			if ( localmatches != null && localmatches.size() > 0 ) {
				return( localmatches.get( localmatches.size() - 1 ) );
			}
		}
		return(null);
	} // findPreviousMatch


	private void gotoSearchMatch(long[] match) {
		if (match == null) return;
		if ( (match[0] > textFileView.currentPage_EndOffset()) ||
			 (match[0] < textFileView.currentPage_StartOffset()) ) {
			textFileView.gotoApproximateOffset( match[0] );
		}
		// Highlighting:
		ArrayList<long[]> hl = new ArrayList<long[]>();
		hl.add( match );
		textFileView.setHighlightedRegions( hl );
		hl = findMatches( currentSearchTerm, textFileView.currentPage_StartOffset(), textFileView.currentPage_EndOffset() );
		textFileView.setSecondaryHighlightedRegions(hl);
	} // gotoSearchMatch

	private void findFirst() {
		if (currentSearchTerm == null || currentSearchTerm.equals("")) {
			nookMenu.goToMainMenu();
			return;
		}
		firstMatch = null ;
		currentMatch = null ;
		findNext();
	} // findFirst

	private void findNext() {
		long[] match = findNextMatch(currentMatch);
		if ( match == null ) {
			showToast( R.string.search_no_match );
			nookMenu.goToMainMenu();
			return;
		}
		currentMatch = match; matchDepth++ ;
		gotoSearchMatch(currentMatch);
		if (firstMatch == null) {
			firstMatch = currentMatch;
			matchDepth = 0;
		}
		if ( (currentMatch[0] == firstMatch[0]) && (currentMatch[1] == firstMatch[1]) && (matchDepth != 0) ) {
			showToast( R.string.search_complete );
			matchDepth = 0 ;
		}
	} // findNext

	private void findPrevious() {
		long[] match = findPreviousMatch(currentMatch);
		if ( match == null ) {
			showToast( R.string.search_no_match );
			nookMenu.goToMainMenu();
			return;
		}
		currentMatch = match; matchDepth-- ;
		gotoSearchMatch(currentMatch);
		if (firstMatch == null) {
			firstMatch = currentMatch; // won't happen
			matchDepth = 0;
		}
		Log.d("DEBUG", "matchDepth=" + matchDepth );
		if ( (currentMatch[0] == firstMatch[0]) && (currentMatch[1] == firstMatch[1]) && (matchDepth != 0) ) {
			showToast( R.string.search_complete );
			matchDepth = 0 ;
		}
	} // findPrevious
	
}

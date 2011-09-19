package com.nookdevs.buttonswapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import android.util.Log;

public class KeyLayout {
	public static final int TOP_LEFT = 525 ;
	public static final int BOTTOM_LEFT = 524 ;
	public static final int TOP_RIGHT = 527 ;
	public static final int BOTTOM_RIGHT = 526 ;

	public static final int PREV_PAGE = 1 ;
	public static final int NEXT_PAGE = 2 ;
	
	public static final String keyLayoutFile = "/system/usr/keylayout/s3c-button.kl" ;

	public static boolean layoutIsWriteable() {
		File f = new File(keyLayoutFile);
		if (! f.canRead()) return false;
		if (! f.canWrite()) return false;
		return true;
	} // layoutIsWriteable

	public static void setMapping(int keyNum, int mapping) {
		String map ;
		if ( (mapping != PREV_PAGE) && (mapping != NEXT_PAGE) ) {
			Log.e(BaseActivity.LOGTAG, "Internal error: invalid value");
			return;
		}
		if ( keyNum == TOP_LEFT || keyNum == BOTTOM_LEFT ) {
			if (mapping == PREV_PAGE) {
				map = "LEFT_PREVPAGE" ;
			} else {
				map = "LEFT_NEXTPAGE" ;
			}
		} else if ( keyNum == TOP_RIGHT || keyNum == BOTTOM_RIGHT ) {
			if (mapping == PREV_PAGE) {
				map = "RIGHT_PREVPAGE" ;
			} else {
				map = "RIGHT_NEXTPAGE" ;
			}
		} else {
			Log.e(BaseActivity.LOGTAG, "Internal error: invalid value");
			return ;
		}

		StringBuilder fileContents = new StringBuilder(600) ;
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(keyLayoutFile));
			String line ;
			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = line.split("\\s+");
				if ( (tokens != null) && (tokens.length == 3) && (tokens[0].equals("key"))
						&& (tokens[1].equals("" + keyNum)) ) {
					fileContents.append( "key " + keyNum + "   " + map + "\n" );
				} else {
					fileContents.append(line);
					fileContents.append("\n");
				}
			}
			bufferedReader.close();
		} catch (Exception ex) {
			Log.e(BaseActivity.LOGTAG, "Exception: " + ex);
			return ;
		}

		try {
			FileWriter fw = new FileWriter(keyLayoutFile, false);
			fw.write( fileContents.toString() );
			fw.close();
		} catch (Exception ex) {
			Log.e(BaseActivity.LOGTAG, "Exception: " + ex);
			return ;
		}

	} // setMapping

	private static int getMapping(int keyNum) {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(keyLayoutFile));
			String line ;
			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = line.split("\\s+");
				if (tokens == null) continue ;
				if (tokens.length != 3) continue ;
				if (! tokens[0].equals("key") ) continue ;
				if ( tokens[1].equals("" + keyNum) ) {
					if (tokens[2].endsWith("NEXTPAGE")) {
						bufferedReader.close();
						return( NEXT_PAGE );
					} else if (tokens[2].endsWith("PREVPAGE")) {
						bufferedReader.close();
						return( PREV_PAGE );
					}
				}
			}
			bufferedReader.close();
		} catch (Exception ex) {
			Log.e(BaseActivity.LOGTAG, "Exception: " + ex);
			return(0);
		}
		Log.e(BaseActivity.LOGTAG, "Error: could not find key in s3c-button.kl");
		return 0;
	} // getMapping

	public static int getTopLeftMapping() {
		return getMapping(TOP_LEFT) ;
	} // getTopLeftMapping
	public static int getTopRightMapping() {
		return getMapping(TOP_RIGHT) ;
	} // getTopRightMapping
	public static int getBottomLeftMapping() {
		return getMapping(BOTTOM_LEFT) ;
	} // getBottomLeftMapping
	public static int getBottomRightMapping() {
		return getMapping(BOTTOM_RIGHT) ;
	} // getBottomRightMapping

	public static void setTopLeftMapping(int mapping) {
		setMapping(TOP_LEFT, mapping);
	}
	public static void setBottomLeftMapping(int mapping) {
		setMapping(BOTTOM_LEFT, mapping);
	}
	public static void setTopRightMapping(int mapping) {
		setMapping(TOP_RIGHT, mapping);
	}
	public static void setBottomRightMapping(int mapping) {
		setMapping(BOTTOM_RIGHT, mapping);
	}

}

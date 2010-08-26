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
package com.nookdevs.crossword;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Stack;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;


import android.util.Log;

public class PuzzleIO {
	CrossWord crossword_activity = null ;

	PuzzleIO( CrossWord xp ) {
		crossword_activity = xp ;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////

	Puzzle loadPuzzle( String puzzlefilename ) {
		Puzzle puzzle = null ;
		try {
			if ( puzzlefilename.toLowerCase().endsWith(".xml") || puzzlefilename.toLowerCase().endsWith(".xpf") ) {
				PuzzleXPF puz = new PuzzleXPF(crossword_activity);
				puzzle = puz.loadPuzzle(puzzlefilename);
			} else if ( puzzlefilename.toLowerCase().endsWith(".puz") ) {
				PuzzlePuz puz = new PuzzlePuz(crossword_activity);
				puzzle = puz.loadPuzzle(puzzlefilename);
			}
		} catch (Exception ex ) {
			Log.e( this.toString(), "Exception loading file: " + ex );
			puzzle = null ;
		}
		return(puzzle);
	} // loadPuzzle	

	
	/////////////////////////////////////////////////////////////////////////////////////////////

	
	
	String loadPuzzleWIP( String puzzlefilename ) {
		FileInputStream is ;
		Stack<String> tagstack = new Stack<String>();
		String useranswers = null ;
				
		//  Does a "wip" file exist?:
		File wipfile = new File(puzzlefilename + ".wip");
		try {
			if (! wipfile.exists()) return( null );  // Nope
		} catch( Exception ex ) {
			Log.e( this.toString(), "Exception - " + ex.toString() );
			return(null) ;
		}
		
		//  Open the file for reading:
		try {
	        is = new FileInputStream( puzzlefilename + ".wip" ) ;
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception - " + ex);
			return(null);
		}

		//  Parse it:
		XmlPullParser xpp ;
		try {
	        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        xpp = factory.newPullParser();
	        InputStreamReader isr = new InputStreamReader( is );
	        xpp.setInput( isr );
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception - " + ex);
			return(null);
		}

	    try {
			int eventType = xpp.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
			    if(eventType == XmlPullParser.START_DOCUMENT) {
			    } else if (eventType == XmlPullParser.END_DOCUMENT) {
			    } else if (eventType == XmlPullParser.END_TAG) {
			    	tagstack.pop();
			    } else if (eventType == XmlPullParser.START_TAG) {
			    	tagstack.push( (String) xpp.getName() );
			    } else if (eventType == XmlPullParser.TEXT) {
			    	String currenttag = tagstack.peek();
			    	if ( currenttag.equalsIgnoreCase("UserAnswers") ) {
			            useranswers = xpp.getText();
			    	}
			    }
			    eventType = xpp.next();
			}
	    } catch (Exception ex) {
			Log.e( this.toString(), "Exception - " + ex);
			return(null);
		}

	    // Close it:
		try {
			is.close();
		} catch ( Exception ex ) {
			Log.e( this.toString(), "Caught exception: " + ex ) ;
		}

	    return(useranswers);			    
	} // loadPuzzleWIP
	
	///////////////////////////////////////////////
	
	/*  Save the answers the user has entered so far on the puzzle they're doing:  */
	void savePuzzleWIP(String usergridstring, String puzzlefilename) {
		File wipfile = new File(puzzlefilename + ".wip");
		try {
			if (wipfile.exists()) wipfile.delete();
		} catch( Exception ex ) {
			Log.e( this.toString(), "Caught exception trying to save work - " + ex.toString() );
			return ;
		}
		String s =  "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"  +
					"<!-- Temporary storage for solving a crossword puzzle -->\n"   +
					"<!-- Created by nookCrossWord -->\n"   +
					"<CrossWordWIP Version=\"1.0\">\n"   +
					"<UserAnswers>" + usergridstring  + "</UserAnswers>\n"   +
					"</CrossWordWIP>\n"  ;
		try {
			FileWriter fw = new FileWriter(wipfile);
			fw.write(s);
			fw.close();
		} catch( Exception ex ) {
			Log.e( this.toString(), "Caught exception trying to save work - " + ex.toString() );
			return ;
		}
			
	} // savePuzzleWIP
	
	/////////////////////////////////////////////////////////////////////////////////////////////

	

	

} // PuzzleIO class

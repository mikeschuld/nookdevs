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
import java.util.ArrayList;
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
		
		//  Load any work they user has already done:
		if ( puzzle != null ) {
			loadPuzzleWIP( puzzle );
		}
		
		return(puzzle);
	} // loadPuzzle	

	
	/////////////////////////////////////////////////////////////////////////////////////////////

	
	
	void loadPuzzleWIP( Puzzle puzzle ) {
		String puzzlefilename = puzzle.getFileName() ;
		FileInputStream is ;
		Stack<String> tagstack = new Stack<String>();
		String useranswers = null ;
		boolean hasRebusEntries = false ;
		ArrayList<int[]> mRebusCells = new ArrayList<int[]>() ;
		ArrayList<String> mRebusValues = new ArrayList<String>() ;
		int rebus_row = -1; int rebus_col = -1;

		//  Does a "wip" file exist?:
		File wipfile = new File(puzzlefilename + ".wip");
		try {
			if (! wipfile.exists()) return;  // Nope
		} catch( Exception ex ) {
			Log.e( this.toString(), "Exception - " + ex.toString() );
			return ;
		}
		
		//  Open the file for reading:
		try {
	        is = new FileInputStream( puzzlefilename + ".wip" ) ;
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception - " + ex);
			return ;
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
			return ;
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
			    	if ( (xpp.getName()).equalsIgnoreCase("UserRebus") ) {
			        	hasRebusEntries = true ;
			            rebus_row = -1 ; rebus_col = -1 ;
			            for ( int i = 0 ; i < xpp.getAttributeCount() ; i++ ) {
			                if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Row") ) {
			                    rebus_row = Integer.parseInt((String)xpp.getAttributeValue(i)) - 1;
			                } else if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Col") ) {
			                    rebus_col = Integer.parseInt((String)xpp.getAttributeValue(i) ) - 1;
			                }
			            }
			    	}
			    } else if (eventType == XmlPullParser.TEXT) {
			    	String currenttag = tagstack.peek();
			    	if ( currenttag.equalsIgnoreCase("UserAnswers") ) {
			            useranswers = xpp.getText();
			    	} else if (currenttag.equalsIgnoreCase("UserRebus") ) {
			    		if ( (rebus_row != -1) && (rebus_col != -1) ) {
			    			String rebusString = xpp.getText() ;
			    			int loc[] = { rebus_row, rebus_col } ;
			    			hasRebusEntries = true ;
			    			mRebusCells.add(loc);
			    			mRebusValues.add(rebusString);
			    		}
				    	rebus_row = -1 ; rebus_col = -1 ;
			    	}
			    }
			    eventType = xpp.next();
			}
	    } catch (Exception ex) {
			Log.e( this.toString(), "Exception - " + ex);
		}

	    // Close it:
		try {
			is.close();
		} catch ( Exception ex ) {
			Log.e( this.toString(), "Caught exception: " + ex ) ;
		}
		
		//  Did we get anything valid?  If so, apply it to the puzzle:
		if ( (useranswers != null) && ( useranswers.length() == (puzzle.rows * puzzle.cols) ) ) {
			//  Apply the answer grid:
			for ( int i = 0 ; i < puzzle.rows ; i++ ) {
				for ( int j = 0 ; j < puzzle.cols ; j++ ) {
					puzzle.getCell(i,j).setUserText( useranswers.charAt(j + (i*puzzle.cols)) ) ;
				}
			}
			// Do they have any multi-character answers?  If so, set those:
			if ( hasRebusEntries ) {
				if ( mRebusCells != null && mRebusValues != null ) {
					for ( int c = 0 ; c < mRebusCells.size() ; c++ ) {
						int row = mRebusCells.get(c)[0] ; int col = mRebusCells.get(c)[1];
						puzzle.getCell(row,col).setUserText( mRebusValues.get(c) );
					}
				}
			}
		}
	} // loadPuzzleWIP
	
	///////////////////////////////////////////////
	
	/*  Save the answers the user has entered so far on the puzzle they're doing:  */
	//  Note that for our WIP format, unlike XPF or .PUZ, we're only treating multi-character
	//  answers as rebus entries.  But something ".", which would be problematic for those
	//  formats, we can just treat as a regular single-character answer.
	void savePuzzleWIP( Puzzle puzzle ) {
		String puzzlefilename = puzzle.getFileName() ;
		File wipfile = new File(puzzlefilename + ".wip");
		try {
			if (wipfile.exists()) wipfile.delete();
		} catch( Exception ex ) {
			Log.e( this.toString(), "Caught exception trying to save work - " + ex.toString() );
			return ;
		}
		String beginning =  "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"  +
					"<!-- Temporary storage for solving a crossword puzzle -->\n"   +
					"<!-- Created by nookCrossWord -->\n"   +
					"<CrossWordWIP Version=\"1.1\">\n"   +
					"<Size><Rows>" + puzzle.rows + "</Rows><Cols>" + puzzle.cols + "</Cols></Size>\n" ;
		String usergridstring = "" ;
		String userRebuses = "" ;
		for ( int i = 0 ; i < puzzle.rows ; i++ ) {
			for ( int j = 0 ; j < puzzle.cols ; j++ ) {
				String usertext = (puzzle.getCell(i, j)).usertext ;
				usergridstring = usergridstring + escapeXMLData( usertext.charAt(0) ) ;
				if ( usertext.length() > 1 ) {
					userRebuses = userRebuses + 
						"<UserRebus Row=\"" + (i+1) + "\" Col=\"" + (j+1) + "\">" + escapeXMLData(usertext) + "</UserRebus>\n"   ;
				}
			}
		}
		String middle = "<UserAnswers>" + usergridstring + "</UserAnswers>\n" ;
		if ( userRebuses.length() > 0 ) {
			middle = middle + "<UserRebusEntries>\n" + userRebuses + "</UserRebusEntries>\n" ;
		}
		String end = "</CrossWordWIP>\n"  ;
		String xmlWIP = beginning + middle + end ;
		try {
			FileWriter fw = new FileWriter(wipfile);
			fw.write( xmlWIP );
			fw.close();
		} catch( Exception ex ) {
			Log.e( this.toString(), "Caught exception trying to save work - " + ex.toString() );
			return ;
		}
	} // savePuzzleWIP

	private String escapeXMLData(String s) {
		return( s.replace("&", "&amp;").replace("'", "&apos;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;") );
	} // escapeXMLData
	private String escapeXMLData(char c) {
		return ( escapeXMLData(c + "") );
	} // escapeXMLData

	/////////////////////////////////////////////////////////////////////////////////////////////

	

} // PuzzleIO class

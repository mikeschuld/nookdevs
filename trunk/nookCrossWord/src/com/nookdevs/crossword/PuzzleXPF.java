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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Stack;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.graphics.Color;
import android.util.Log;
//import java.io.FileNotFoundException;


public class PuzzleXPF {
    private CrossWord mActivity;

    public PuzzleXPF(CrossWord activity) {
        mActivity = activity;
    }
	
	//  Note that when reading XPF, whenever I deal with row and column numbers, I have to
	//  subtract (or add) one to switch between XPF (starting at 1) and Java (starting at 0)
	Puzzle loadPuzzle( String puzzlefilename ) {
		//Log.d( this.toString(), "DEBUG: Entering loadPuzzleXPF()..." );
		int rows = -1 ;
		int cols = -1 ;
		String gridstring = "" ;
		String notes = "" ;
		String title = "" ;
		String author = "" ;
		String editor = "" ;
		String publisher = "" ;
		String copyright = "" ;
		String date = "" ;
		boolean hasRebusEntries = false ;
		ArrayList<Clue> clues = new ArrayList<Clue>() ;
		ArrayList<int[]> circles = new ArrayList<int[]>() ;
		ArrayList<int[]> shades = new ArrayList<int[]>() ;
		ArrayList<int[]> mRebusCells = new ArrayList<int[]>() ;
		ArrayList<String> mRebusValues = new ArrayList<String>() ;
		Stack<String> tagstack = new Stack<String>();
		Clue latestclue = null ;
		FileInputStream is ;
		int shade_row = -1 ; int shade_col = -1 ;
		int rebus_row = -1 ; int rebus_col = -1 ;

		//  Create an input stream from the filename:
		try {
	        is = new FileInputStream( puzzlefilename ) ;
		} catch (Exception ex) {
			Log.e( this.toString(), "FileInputStream exception: " + ex);
			return(null);
		}
		
		//  Sometimes Microsoft puts three bytes at the start of UTF-8 files which
		//  we need to skip over (called the Byte Order Marker, or BOM).  If we
		//  don't skip them, the XML parser will choke on them.
		try {
			if ( is.read() == 0xEF && is.read() == 0xBB && is.read() ==  0xBF ) {
				//  Yup, we needed to skip the first three bytes
			} else {
				//  No, it's a normal file, so rewind (by just closing and re-opening):
				is.close();
				is = new FileInputStream( puzzlefilename ) ;
			}
		} catch (Exception ex) {
			Log.e( this.toString(), "FileInputStream exception: " + ex);
			return(null);
		}
		
		//  Parse it:
		XmlPullParser xpp ;
		try {
	        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        xpp = factory.newPullParser();
	        InputStreamReader isr = new InputStreamReader( is, "UTF8" );
	        xpp.setInput( isr );
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception - " + ex);
			return(null);
		}
	    try {
			int eventType = xpp.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
			    if(eventType == XmlPullParser.START_DOCUMENT) {
			    } else if(eventType == XmlPullParser.END_DOCUMENT) {
			    } else if(eventType == XmlPullParser.END_TAG) {
			    	tagstack.pop();
			    	latestclue = null ;
			    	if ( (xpp.getName()).equals("Puzzle") ) {
			    		break ;  // TODO: we don't support multiple crosswords in one file yet
			    	}
			    } else if(eventType == XmlPullParser.START_TAG) {
			    	tagstack.push( (String) xpp.getName() );
			        if ( (xpp.getName()).equals("Circle") ) {
			            // /Puzzles/Puzzle/Circles/Circle
			            int row = -1 ; int col = -1 ;
			            for ( int i = 0 ; i < xpp.getAttributeCount() ; i++ ) {
			                if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Row") ) {
			                    row = Integer.parseInt((String)xpp.getAttributeValue(i) ) - 1;
			                } else if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Col") ) {
			                    col = Integer.parseInt((String)xpp.getAttributeValue(i) ) - 1;
			                }
			            }
			            if ( (row != -1) && (col != -1) ) {
			            	int position[] = { row, col } ;
			            	circles.add( position );
			            }
			        } else if ( (xpp.getName()).equalsIgnoreCase("Shade") ) {
			        	shade_row = -1 ; shade_col = -1 ;
			            // /Puzzles/Puzzle/Shades/Shade
			            for ( int i = 0 ; i < xpp.getAttributeCount() ; i++ ) {
			                if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Row") ) {
			                    shade_row = Integer.parseInt((String)xpp.getAttributeValue(i) ) - 1;
			                } else if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Col") ) {
			                    shade_col = Integer.parseInt((String)xpp.getAttributeValue(i) ) - 1;
			                }
			            }
			        } else if ( (xpp.getName()).equalsIgnoreCase("Rebus") ) {
			        	hasRebusEntries = true ;
			            rebus_row = -1 ; rebus_col = -1 ;
			            // /Puzzles/Puzzle/RebusEntries/Rebus
			            for ( int i = 0 ; i < xpp.getAttributeCount() ; i++ ) {
			                if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Row") ) {
			                    rebus_row = Integer.parseInt((String)xpp.getAttributeValue(i)) - 1;
			                } else if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Col") ) {
			                    rebus_col = Integer.parseInt((String)xpp.getAttributeValue(i) ) - 1;
			                //} else if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Short") ) {
			                    //String shortanswer = xpp.getAttributeValue(i);
			                }
			            }
			        } else if ( (xpp.getName()).equalsIgnoreCase("Clue") ) {
			            // /Puzzles/Puzzle/Clues/Clue
			            int row = -1 ; int col = -1 ; int num = -1;
			            int dir = -1 ;
			            for ( int i = 0 ; i < xpp.getAttributeCount() ; i++ ) {
			                if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Row") ) {
		                        row = Integer.parseInt((String)xpp.getAttributeValue(i)) - 1;
			                } else if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Col") ) {
			                    col = Integer.parseInt((String)xpp.getAttributeValue(i)) - 1;
			                } else if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Num") ) {
			                    num = Integer.parseInt((String)xpp.getAttributeValue(i)) ;
			                } else if ( (xpp.getAttributeName(i)).equalsIgnoreCase("Dir") ) {
			                    String sdir = xpp.getAttributeValue(i);
			                    if ( sdir.equalsIgnoreCase("Across") ) {
			                        dir = CrossWord.ACROSS ;
			                    } else if ( sdir.equalsIgnoreCase("Down") ) {
			                        dir = CrossWord.DOWN ;
			                    }
			                }
			            }
			            if ( (num != -1) && (row != -1) && (col != -1) && ((dir == CrossWord.ACROSS) || (dir == CrossWord.DOWN)) ) {
			            	latestclue = new Clue("", dir, num, row, col) ;
			            }
			        }
			    } else if(eventType == XmlPullParser.TEXT) {
			    	String currenttag = tagstack.peek();

			    	if ( currenttag.equalsIgnoreCase("Row") ) {
			            // /Puzzles/Puzzle/Grid/Row
			            String puzzlerow = xpp.getText();
			            gridstring = gridstring + puzzlerow ;
			        } else if ( currenttag.equalsIgnoreCase("Rows") ) {
		                // /Puzzles/Puzzle/Size/Rows
			    	    rows = Integer.parseInt( xpp.getText() );
			    	} else if ( currenttag.equalsIgnoreCase("Cols") ) {
	                    // /Puzzles/Puzzle/Size/Cols
		    	        cols = Integer.parseInt( xpp.getText() );
			    	} else if ( currenttag.equalsIgnoreCase("Notepad") ) {
	                    // /Puzzles/Puzzle/Size/Notepad
		    	        notes = xpp.getText() ;
			    	} else if ( currenttag.equalsIgnoreCase("Title") ) {
	                    // /Puzzles/Puzzle/Size/Title
		    	        title = xpp.getText() ;
			    	} else if ( currenttag.equalsIgnoreCase("Author") ) {
	                    // /Puzzles/Puzzle/Size/Author
		    	        author = xpp.getText() ;
			    	} else if ( currenttag.equalsIgnoreCase("Editor") ) {
	                    // /Puzzles/Puzzle/Size/Editor
		    	        editor = xpp.getText() ;
			    	} else if ( currenttag.equalsIgnoreCase("Copyright") ) {
	                    // /Puzzles/Puzzle/Size/Copyright
		    	        copyright = "© " + xpp.getText() ;
			    	} else if ( currenttag.equalsIgnoreCase("Publisher") ) {
	                    // /Puzzles/Puzzle/Size/Publisher
		    	        publisher = xpp.getText() ;
			    	} else if ( currenttag.equalsIgnoreCase("Date") ) {
	                    // /Puzzles/Puzzle/Size/Date
		    	        date = xpp.getText() ;
			    	} else if ( currenttag.equalsIgnoreCase("Clue") ) {
			    		if ( latestclue != null ) {
			    			latestclue.text = xpp.getText() ;
			    			clues.add(latestclue);
			    		}
			    	} else if (currenttag.equalsIgnoreCase("Rebus") ) {
			    		if ( (rebus_row != -1) && (rebus_col != -1) ) {
			    			String rebusString = xpp.getText() ;
			    			int loc[] = { rebus_row, rebus_col } ;
			    			mRebusCells.add(loc);
			    			mRebusValues.add(rebusString);
			    		}
				    	rebus_row = -1 ; rebus_col = -1 ;
			    	} else if ( currenttag.equalsIgnoreCase("Shade") ) {
			    		if ( (shade_row != -1) && (shade_col != -1) ) {
			    			String colorstring = xpp.getText() ;
			    			if ( colorstring.equalsIgnoreCase("gray") || colorstring.equalsIgnoreCase("grey") ) {
			    				int shade[] = { shade_row, shade_col, Color.GRAY } ;
			    				shades.add( shade ) ;
			    			} else if ( (colorstring.charAt(0) == '#') && (colorstring.length() == 7) ) {
			    				int shade[] = { shade_row, shade_col, Color.parseColor( colorstring ) } ;
			    				shades.add( shade );
			    			}
			    		}
			    		shade_row = -1 ; shade_col = -1 ;
			    	}
			    	
			    }
			    eventType = xpp.next();
			}
		} catch (XmlPullParserException ex) {
			Log.e( this.toString(), "XML exception: " + ex );
			return(null);
		} catch (IOException ex) {
			Log.e( this.toString(), "IO exception: " + ex );
			return(null);
		}
		
		// Close it:
		try {
			is.close();
		} catch ( Exception ex ) {
			Log.e( this.toString(), "Caught exception: " + ex ) ;
		}		
		Puzzle puzzle = new Puzzle(mActivity, puzzlefilename, rows, cols, gridstring, circles, shades,
								false, mRebusCells, mRebusValues, clues, title, author, copyright, date, editor, publisher, notes ) ;
		
		//Log.d( this.toString(), "DEBUG: Leaving loadPuzzleXPF()." );
		return(puzzle);
	} // loadPuzzle

}

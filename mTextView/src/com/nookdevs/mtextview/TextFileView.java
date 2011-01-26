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
 * This class displays a text file in a TextView
 * It loads only a section at a time, since the file size may be too
 * large for our limited RAM.
 */


package com.nookdevs.mtextview;

import android.widget.TextView;
import android.text.Layout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import android.util.Log;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import java.util.ArrayList;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.graphics.Color;

//  TODO: detect under-runs and increase bufSize if necessary


public class TextFileView {
	private TextView textView ;
	private int maxLinesOnScreen;
	int paddingLeft = 0 ; int paddingRight = 0 ;
	int paddingTop = 0 ; int paddingBottom = 0 ;
	//  The current file offset is the offset of the first character on the screen:
	private long currentFileOffset = 0 ;  // current offset in file
	private long currentFileLength = 0 ;  // total number of chars in file
	private RandomAccessFile currentRandomAccessFile = null ;
	private ArrayList<long[]> primaryHighlightedRegions = null ;
	private ArrayList<long[]> secondaryHighlightedRegions = null ;
	private int bufSize = 4096 ;

	TextFileView(TextView textView) {
		this(textView, (File)null, null);
	}
	TextFileView(TextView textView, int[] padding) {
		this(textView, (File)null, padding);
	}
	TextFileView(TextView textView, String fileName) {
		this(textView, new File(fileName), null);
	}
	TextFileView(TextView textView, File f) {
		this(textView, f, null);
	}
	TextFileView(TextView textView, String fileName, int[] padding) {
		this(textView, new File(fileName), padding);
	}
	TextFileView(TextView textView, File f, int[] padding) {
		this.textView = textView ;
		if (padding != null) setPadding( padding[0], padding[1], padding[2], padding[3] );
		if (f != null) loadFile(f);
		getTextViewMeasurements();
	}

	//////////////////////////////////////////////////////////////////////////////


	public long getFileLength() {
		return currentFileLength;
	} // getFileLength

	//  Returns the file offset of the first visible character on the screen:
	public long currentPage_StartOffset() {
		return( currentFileOffset );
	} // currentPage_StartOffset

	//  Returns the file offset of the last visible character on the screen:
	public long currentPage_EndOffset() {
		return( currentFileOffset + numDisplayedChars() );
	} // currentPage_EndOffset

	//  Returns how far into the file we are, from 0 to 100
	public int getPercentageOffset() {
		if ( currentFileLength == 0 ) return(0);
		Double d = ( (new Double(currentFileOffset)) / (new Double(currentFileLength))) * 100.0 ;
		int i;
		i = d.intValue();
		if ( i < 0 ) i = 0 ;
		if ( i > 100 ) i = 100 ;
		return i;
	} // getPercentageOffset

	public void seekToPercentageOffset(int percent) {
		Double d = ((new Double(percent)) / 100.0) * (new Double(currentFileLength)) ;
		gotoApproximateOffset( d.intValue() );
	} // seekToPercentageOffset

	public void setFont( Typeface tf, float size ) {
		textView.setTypeface( tf );
		textView.setTextSize(size);
		getTextViewMeasurements();
	} // setFont

	public void setTextSize( int size ) {
		textView.setTextSize(size);
		getTextViewMeasurements();
	} // setTextSize

	public void setTypeface( Typeface tf ) {
		textView.setTypeface( tf );
		getTextViewMeasurements();
	} // setTypeface

	public void setPadding(int left, int top, int right, int bottom) {
		textView.setPadding(left, top, right, bottom);
		paddingLeft = left ; paddingRight = right ;
		paddingTop = top ; paddingBottom = bottom ;
		getTextViewMeasurements();
	} // setPadding
	public void setPadding(int [] padding) {
		setPadding( padding[0], padding[1], padding[2], padding[3] );
	} // setPadding

	public String getText() {
		String s = textView.getText().toString().substring( 0, numDisplayedChars() ) ;
		return(s);
	} // getText

	public void setHighlightedRegions( ArrayList<long[]> regions ) {
		try {
			this.primaryHighlightedRegions = regions ;
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception: " + ex);
			this.primaryHighlightedRegions = null;
		}
		gotoExactOffset(currentFileOffset);  // re-load the page
	} // setHighlightedRegions

	public void setSecondaryHighlightedRegions( ArrayList<long[]> regions ) {
		try {
			this.secondaryHighlightedRegions = regions ;
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception: " + ex);
			this.secondaryHighlightedRegions = null;
		}
		gotoExactOffset(currentFileOffset);  // re-load the page
	} // setSecondaryHighlightedRegions


	//////////////////////////////////////////////////////////////////////////////

	public void closeCurrentFile() {
		if ( currentRandomAccessFile != null ) {
			try {
				currentRandomAccessFile.close();
			} catch (Exception ex) {
				Log.e(this.toString(), "Error closing previous file: " + ex );
			}
			currentRandomAccessFile = null;
			textView.setText("");
		}
		currentFileOffset = 0 ;
		currentFileLength = 0 ;
	} // closeCurrentFile

	public boolean loadFile( File f, long startOffset ) {
		//  Close the previous file, if there was one:
		closeCurrentFile();
		//  Open the file:
		try {
			currentRandomAccessFile = new RandomAccessFile(f, "r");
			currentFileLength = f.length() ;
			currentFileOffset = 0 ;
		} catch (FileNotFoundException ex) {
			Log.e(this.toString(), "File not found: " + f.getAbsolutePath() );
			currentRandomAccessFile = null ;
			currentFileLength = 0; currentFileOffset = 0;
			return(false);
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception loading file: " + ex );
			currentRandomAccessFile = null ;
			currentFileLength = 0; currentFileOffset = 0;
			return(false);
		}
		// if we get here, it worked:
		gotoExactOffset( startOffset );
		return(true);
	} // loadFile
	public boolean loadFile( File f ) {
		return( loadFile(f, 0l) );
	} // loadFile
	public boolean loadFile( String filename, long startOffset ) {
		return( loadFile( new File(filename), startOffset ) );
	} // loadFile
	public boolean loadFile( String filename ) {
		return( loadFile( new File(filename), 0l ) );
	} // loadFile


	public String readFromCurrentFile(long offset, int numbytes) {
		if ( currentRandomAccessFile == null ) {
			return("");
		}
		try {
			byte[] buffer = new byte[numbytes];
			currentRandomAccessFile.seek(offset);
			int numread = currentRandomAccessFile.read(buffer, 0, numbytes);
			if ( (numread == 0) || (numread == -1) ) {
				return("");
			}
			String s = new String(buffer, "Windows-1252") ;
			s = s.substring(0, numread);
			return(s);
		} catch ( Exception ex ) {
			Log.e(this.toString(), "Exception reading from file: " + ex );
			return("");
		}
	} // readFromCurrentFile
	public String readFromCurrentFile(long offset, Long numbytes) {
		return( readFromCurrentFile(offset, numbytes.intValue()) ) ;
	} // readFromCurrentFile

	/**********************************************************************************/

	//  Returns how many characters are visible in the TextView:
	//  (Often, we know where our buffer starts, but we don't
	//  know where the TextView truncated it to fit on the screen;
	//  this returns how many characters fit.)
	private int numDisplayedChars() {
		Layout l = textView.getLayout() ;
		if ( l == null ) {
			Log.e(this.toString(), "WARNING: getLayout called too early");
			return(0);
		}
		if ( maxLinesOnScreen == 0 ) {
			Log.d(this.toString(), "WARNING: maxLinesOnScreen still 0");
			return(0);
		}
		int n = l.getLineCount();
		if ( n == 0 ) return(0);   // the TextView is empty
		if ( n > maxLinesOnScreen ) n = maxLinesOnScreen ;  // don't include off-screen lines
		return( l.getLineEnd(n - 1) );
	} // numDisplayedChars

	//  Page down:
	public void pageDown() {
		long newOffset = currentPage_EndOffset() ;
		if ( newOffset >= currentFileLength ) return ;  // we're already on the last page
		loadFileRegion( newOffset, bufSize );
	} // pageDown

	//  Paging up is harder than paging down, since the number of characters per page
	//  varies from one page to the next.  We know where our current page begins, but
	//  we don't know where the previous page will begin once the TextView has rendered
	//  it.  So, what we do is rewind way too far first; when all that data is put into
	//  the TextView, the TextView breaks it up into lines so we can figure out page
	//  boundaries.
	//  Also note that if you're half a page down from the top, pageUp will result in
	//  a page that contains the current page start.
	public void pageUp() {
		if ( currentFileOffset == 0 ) return ;
		//  Remember where we were:
		long oldFileOffset = currentFileOffset ;
		//  Get the offset of the last character on the currently visible screen:
		//  (We won't need to read anything from the file past this point)
		long maxFileOffset = currentPage_EndOffset() ;
		//  Rewind way more than we need to, and then read it all back into our
		//  TextView, ending with the last character on the currently visible screen:
		long tmpOffset = oldFileOffset - bufSize ;
		if ( tmpOffset < 0 ) tmpOffset = 0 ;
		loadFileRegion( tmpOffset, (maxFileOffset - tmpOffset) );		
		//  By how many lines back did we over-shoot?:
		int linesTooFarBack = textView.getLineCount() - (maxLinesOnScreen * 2) ;
		//  Hack: Don't count the additional empty line the TextView includes in
		//  case the "real" last line ends with a new-line:
		if ( ( textView.getLayout().getLineStart(textView.getLineCount() - 1) ) ==
			( textView.getLayout().getLineEnd(textView.getLineCount() - 1 ) ) ) {
			linesTooFarBack-- ;
		}
		//  See where we are in relation to the actual page start:
		if ( linesTooFarBack > 0 ) {
			//  Move forward to the actual start of the page:
			tmpOffset = tmpOffset + textView.getLayout().getLineEnd( linesTooFarBack - 1 ) ;
			loadFileRegion( tmpOffset, (oldFileOffset - tmpOffset) );
			return ;
		} else if ( tmpOffset > 0 ) {
			//  This would mean we didn't rewind far enough, so we probably need
			//  to increase bufSize (I don't expect this to happen).
			//  TODO: handle this case
			Log.e( this.toString(), "Error: bufSize probably too small" );			
			return;
		} else {
			//  If we get here, then we've already rewound to the start of the file,
			//  so there's no reason to seek forward again.
			return ;
		}
	} // pageUp

	public void gotoExactOffset(long offset) {
		loadFileRegion( offset, bufSize );
	} // gotoExactOffset


	public void gotoApproximateOffset(long targetOffset) {
		if ( targetOffset > currentFileLength ) targetOffset = currentFileLength - 1;
		if ( targetOffset <= 0 ) {
			gotoExactOffset(0);
			return;
		}
		long tryOffset = targetOffset - (bufSize / 2) ;
		if ( tryOffset < 0 ) tryOffset = 0;
		loadFileRegion( tryOffset, bufSize );

		Long l = targetOffset - tryOffset ;
		int n = l.intValue() ;
		int linenum = textView.getLayout().getLineForOffset( n );

		if ( (textView.getLineCount() - linenum) < maxLinesOnScreen ) {
			int newlinenum = textView.getLineCount() - maxLinesOnScreen ;
			if ( newlinenum < 0 ) newlinenum = 0 ;
			gotoExactOffset( tryOffset + textView.getLayout().getLineStart(newlinenum) );
			return ;
		}

		long newOffset = tryOffset + textView.getLayout().getLineStart(linenum);
		loadFileRegion( newOffset, bufSize );
	} // gotoApproximateOffset


	//  Note that endOffset may be larger than will fit on the screen; we often
	//  don't know how many characters actually fit until after we call setText().
	private void loadFileRegion(long startOffset, long endOffset) {
		String s = readFromCurrentFile(startOffset, endOffset);
		currentFileOffset = startOffset ;
		//  Convert to UNIX text format from DOS (and really old Apples):
		//  (Note that I cannot change the number of characters, or the
		//  seeking logic will have troubles, so I use a space.)
		s = s.replace( "\r\n" , " \n");  // from DOS
		s = s.replace( "\r" , "\n");     // from ancient Apple
		//  Put it on the screen:
		textView.setText(s, TextView.BufferType.SPANNABLE);
		//  Now check for any highlighting:
		doHighlighting();
	} // loadFileRegion


	private void doHighlighting() {
		if ( (primaryHighlightedRegions == null) && (secondaryHighlightedRegions == null) ) {
			return ;
		}
		long startOffset = currentPage_StartOffset();
		long endOffset = currentPage_EndOffset();
	    Spannable s = (Spannable) textView.getText();

	    ArrayList<long[]> mHighlightedRegions ;
	    int highlightColor ;

	    for( int c = 0 ; c < 2 ; c++ ) {
	    	//  First do the "secondary" selections, then the primary selection(s):
	    	if ( c == 0 ) {
	    		mHighlightedRegions = secondaryHighlightedRegions ;
	    		try {
					highlightColor = textView.getResources().getColor(R.color.secondaryhighlightcolor) ;
				} catch (NotFoundException ex) {
					highlightColor = Color.LTGRAY ;
				}
	    	} else {
	    		mHighlightedRegions = primaryHighlightedRegions ;
	    		try {
					highlightColor = textView.getResources().getColor(R.color.primaryhighlightcolor) ;
				} catch (NotFoundException ex) {
					highlightColor = Color.GRAY ;
				}
	    	}
	    	//  The actual work:
	    	if ( mHighlightedRegions != null ) { 
	    		for( long[] region : mHighlightedRegions ) {
	    			if ( (startOffset <= region[0]) && (endOffset >= region[0])  ) {
	    				Long a = region[0] - startOffset  ;
	    				Long b ;
	    				if ( region[1] > endOffset ) {
	    					b = endOffset - startOffset ;
	    				} else {
	    					b = region[1] - startOffset ;
	    				}
	    				s.setSpan(new BackgroundColorSpan(highlightColor), a.intValue(), b.intValue(), 0);
	    			}
	    		}
	    	}
	    }
	} // doHighlighting


	//  This should be called at startup, and whenever the window geometry,
	//  padding, or the font changes.  The results are not immediately
	//  available.
	private void getTextViewMeasurements() {
		//  These measurements can only be taken after the View is on-screen:
		textView.post( new Runnable() {
	        public void run() {
	        	// TODO: what if there is spacing between the lines?
	        	Double height = new Double( textView.getMeasuredHeight() ) ;
	        	height = height - paddingTop - paddingBottom ;
	    		Double maxLines = height / textView.getLineHeight() ;
	    		maxLines = Math.floor(maxLines);
	    		maxLinesOnScreen = maxLines.intValue();
	    		textView.setLines(maxLinesOnScreen);
	        }
		} );
	} // getTextViewMeasurements

}
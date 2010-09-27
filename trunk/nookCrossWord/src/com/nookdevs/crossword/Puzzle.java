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

import android.app.Activity;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
//import android.widget.LinearLayout;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
//import android.widget.ScrollView;
import java.util.ArrayList;
import java.util.Collections;
import android.os.Handler;
//import java.util.*;


public class Puzzle extends Activity {
	CrossWord crossword_activity = null ;
	int direction = CrossWord.ACROSS ;
	private Cell[][] cellgrid = null;
	int rows = 0;
	int cols = 0;
	private String gridstring ;
	ArrayList<Clue> clues ;
	Thread cluesupdaterthread ;
	Handler current_clues_handler ;
	private String notes = "";
	private String title = "";
	private String author= "" ;
	private String copyright = "" ;
	private String date = "" ;
	private String editor = "" ;
	private String publisher = "" ;
	int cursor_col = 0;
	int cursor_row = 0;
	private String filename = null ;
	private boolean has_rebus_entries = false ;
	SizeDependent sizedependent = null ;
	View grid_and_selected_clues = null; // the main eink view
	TableLayout gridlayout; // contained in grid_and_selected_clues View
	TextView selected_clue_a; // contained in grid_and_selected_clues View
	TextView selected_clue_d; // contained in grid_and_selected_clues View
	TextView einkcluespage = null;
	TouchScreenClues touchscreenclues ;
	
	//  TODO: the build_Views and populate_* stuff should be merged more

	
	Puzzle(CrossWord xw, String fn, int tmprows, int tmpcols, String tmpgridstring, ArrayList<int[]> circles,
			ArrayList<int[]> shades, boolean has_reebies, ArrayList<Clue> tmpclues, String tmptitle, String tmpauthor,
			String tmpcopyright, String tmpdate, String tmpeditor, String tmppublisher, String tmpnotes ) {
		//Log.d(this.toString(), "DEBUG: Entering Puzzle()...");
		crossword_activity = xw ;
		filename = fn ;
		rows = tmprows ; cols = tmpcols ;
		gridstring = tmpgridstring ;
		has_rebus_entries = has_reebies ;
		
		
		clues = new ArrayList<Clue>() ;
		for ( Clue tmpclue : tmpclues ) {
			clues.add(tmpclue);
		}
		Collections.sort(clues);  // they have to be in order!
		
		if (title != null) title = tmptitle ;
		if (author != null) author = tmpauthor ;
		if (copyright != null) copyright = tmpcopyright ;
		if (date != null) date = tmpdate ;
		if (editor != null) editor = tmpeditor ;
		if (publisher != null) publisher = tmppublisher ;
		if (notes != null) notes = tmpnotes ;
		
		cellgrid = new Cell[rows][cols] ;
		
		sizedependent = new SizeDependent(rows, cols);
		
		build_puzzle_views();

		//  This takes a long time:
		create_cells_and_populate_gridlayout(gridstring);

		synchronizeCellsAndClues();
		
		populate_eink_clues_page();

		markCircleCells(circles);
		
		markShadedCells(shades);
		
		touchscreenclues = new TouchScreenClues(this);
		
		cluesupdaterthread = new Thread() {
			public void run() {
				currentCluesUpdaterThread();
			}
		};
		cluesupdaterthread.start();
		current_clues_handler = new Handler();
		
		set_Cursor_Start_Position();
		displayCurrentClues();
		
		//Log.d(this.toString(), "DEBUG: Leaving Puzzle().");
	} // Puzzle constructor
	
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//  These functions are called when the puzzle is initially loaded.
	//
	
	private void markCircleCells(ArrayList<int[]> circles) {
		if ( circles == null ) return ;
		for( int[] position : circles ) {
			int i = position[0];
			int j = position[1];
			Cell cell = getCell(i,j);
			cell.is_a_circle = true ;
			cell.drawBackground() ;
		}
	} // Puzzle.markCircleCells
	
	private void markShadedCells(ArrayList<int[]> shades) {
		if (shades == null) return ;
		for ( int shade[] : shades ) {
			int i = shade[0];
			int j = shade[1];
			int color = shade[2];
			getCell(i,j).setCellShade( color );	
		}
	} // markShadedCells

	// This method creates the Views our puzzle method draws into.
	// Note that these are not visible until the main program which calls
	// us decides to "attach" them to the screen.
	private void build_puzzle_views() {
		//Log.d(this.toString(), "DEBUG: Entering Puzzle.build_Views()...");
		LayoutInflater inflater = crossword_activity.getLayoutInflater();
		grid_and_selected_clues = (View) inflater.inflate(
				R.layout.eink_puzzle_main_page, null);
		gridlayout = (TableLayout) grid_and_selected_clues
				.findViewById(R.id.xwordgrid);
		selected_clue_a = (TextView) grid_and_selected_clues.findViewById(R.id.selected_clue_across);
		selected_clue_d = (TextView) grid_and_selected_clues.findViewById(R.id.selected_clue_down);
		einkcluespage = (TextView) inflater.inflate(R.layout.eink_cluespage, null);
		//Log.d(this.toString(), "DEBUG: Leaving Puzzle.build_Views().");
	} // Puzzle.build_puzzle_views
	
	//  This is an important function.  TODO: explain what I'm doing here.
	//  TODO: this takes way too long.
	private void create_cells_and_populate_gridlayout(String gridstring) {
		//Log.d(this.toString(), "DEBUG: Entering Puzzle.create_cells_and_populate_gridlayout()...");
		LayoutInflater inflater = crossword_activity.getLayoutInflater();
		for ( int i = 0 ; i < rows ; i++ ) {
			TableRow tr = (TableRow) inflater.inflate(R.layout.eink_xwordrow, null);
			for (int j = 0; j < cols; j++) {
				char c = gridstring.charAt( j + (i * cols) ) ;
				Cell cell = new Cell(crossword_activity, this, i, j, c, false);
				cellgrid[i][j] = cell;
				tr.addView(cell.getEinkView());

			}
			gridlayout.addView(tr);
		}
		//Log.d(this.toString(), "DEBUG: Leaving Puzzle.create_cells_and_populate_gridlayout().");
	} // create_cells_and_populate_gridlayout

	

	private void populate_eink_clues_page() {
		//Log.d(this.toString(), "DEBUG: Entering Puzzle.populate_eink_clues_page()");
		String s = "";
		if ( (notes != null) && (! notes.equals("")) ) {
			s = s + notes + "\n\n" ;
		}
		s = s + "Across\n\n";
		for (Clue clue : clues) {
			if (clue.dir == CrossWord.ACROSS) {
				s = s + clue.num + ". " + clue.text + " (" + (clue.numcells ) + ")" + "\n";
			}
		}
		s = s + "\n\n";
		s = s + "Down\n\n";
		for (Clue clue : clues) {
			if (clue.dir == CrossWord.DOWN) {
				s = s + clue.num + ". " + clue.text + " (" + (clue.numcells) + ")" + "\n";
			}
		}
		einkcluespage.setText(s);
		//Log.d(this.toString(), "DEBUG: Leaving Puzzle.populate_eink_clues_page()");
	} // populate_eink_clues_page

	// These two methods return the "best" clue for a given grid location.
	// For example, for a typical crossword puzzle, the "best" clues for
	// row=0,col=2 are probably across:1,down:3.
	Clue getBestAcrossClue(int i, int j) {
		for (int c = j; c >= 0; c--) {
			Clue clue;
			int n = getCell(i, c).number ;
			if (n != 0) {
				clue = getClue(CrossWord.ACROSS, n);
				if (clue != null)
					return (clue);
			}
		}
		return (null); // Should never happen
	} // getBestAcrossClue
	Clue getBestDownClue(int i, int j) {
		for (int c = i; c >= 0; c--) {
			Clue clue;
			int n = getCell(c, j).number ;
			if (n != 0) {
				clue = getClue(CrossWord.DOWN, n);
				if (clue != null)
					return (clue);
			}
		}
		return (null); // Should never happen
	} // getBestDownClue


	private void synchronizeCellsAndClues() {
		//Log.d( this.toString(), "DEBUG: Entering synchronizeCellsAndClues()..." );
		//  For each clue, find the cell it starts at, and set that cell's number
		//  to the clue number:
		for ( Clue clue : clues ) {
			Cell cell = getCell( clue.row, clue.col );
			if ( cell != null ) {
				cell.number = clue.num ;
				cell.drawCellNumber();
			}
		}
		for ( Clue clue: clues ) {
			clue.cells = getCellListForClue(clue) ;
			clue.numcells = clue.cells.size();
		}
		//  For each cell, find the clue it "belongs" to, and set its two
		//  clue variables to point to this clue:
		for ( int i = 0 ; i < rows ; i++ ) {
			for ( int j = 0 ; j < cols ; j++ ) {
				Cell cell = getCell(i,j);
				Clue acrossclue = getBestAcrossClue(i,j);
				Clue downclue = getBestDownClue(i,j);
				cell.acrossclue = acrossclue ;
				cell.downclue = downclue ;
			}
		}
		//Log.d( this.toString(), "DEBUG: Leaving synchronizeCellsAndClues()." );
	} // Puzzle.synchronizeCellsAndClues
	
	//  For a clue, return the list of cells which its answer go into
	ArrayList<Cell> getCellListForClue( Clue clue ) {
		ArrayList<Cell> celllist = new ArrayList<Cell>() ;
		int i = clue.row;
		int j = clue.col;
		int dir = clue.dir;
		while( positionIsValid(i,j) && (! getCell(i,j).isBlockedOut()) ) {
			celllist.add( getCell(i,j) );
			if ( dir == CrossWord.ACROSS ) {
				j++ ;
			} else if ( dir == CrossWord.DOWN ) {
				i++ ;
			}
		}
		return( celllist );
	} // Puzzle.getCellListForClue


	void setUserGridString(String s) {
		//Log.d( this.toString(), "DEBUG: Entering setUserGridString()..." );
		for ( int i = 0 ; i < rows ; i++ ) {
			for ( int j = 0 ; j < cols ; j++ ) {
				(cellgrid[i][j]).setUserText( "" + s.charAt( j + (i * cols) ) );
			}
		}
		//Log.d( this.toString(), "DEBUG: Leaving setUserGridString()." );
	} // setUserGridString
		
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////

	
	// Returns the View of the main puzzle page:
	View getMainEinkView() {
		return (grid_and_selected_clues);
	}

	// Returns the View of the clues page
	View getEinkCluesView() {
		return ((View) einkcluespage);
	}
	
	//  Returns the View of the touchscreen clues
	View getTouchScreenCluesView() {
		return( (View) touchscreenclues.getView() );
	}
	
	// Whether or not the given row and column refer to a cell in this
	// puzzle:
	boolean positionIsValid(int rownum, int colnum) {
		if (rownum < 0)
			return (false);
		if (colnum < 0)
			return (false);
		if (rownum >= rows)
			return (false);
		if (colnum >= cols)
			return (false);
		return (true);
	} // positionIsValid

	
	private void set_Cursor_Start_Position() {
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (positionIsValid(i, j) && (!cellIsBlockedOut(i, j))) {
					setCursorPos(i,j);
					return;
				}
			}
		}
		Log.e(this.toString(), "Error: could not find any valid cells to start at");
	} // set_Cursor_Start_Position()

	boolean cellIsBlockedOut(int i, int j) {
		Cell cell = getCell(i, j);
		if (cell == null) {
			Log.e(this.toString(), "Error: could not find cell at " + i + "," + j + "." );
			return (true); // an invalid cell is blocked out, I guess
		}
		return (cell.isBlockedOut());
	} // cellIsBlockedOut

	int getCursorRow() {
		return (cursor_row);
	} // getCursorRow

	int getCursorCol() {
		return (cursor_col);
	} // getCursorCol

	
	void setCursorPos(int i, int j) {
		if (!positionIsValid(i, j)) {
			Log.e( this.toString(), "Error: position is invalid: " + i + "," + j );
			return;
		}
		Cell oldcell = getCell(cursor_row, cursor_col);
		Cell newcell = getCell(i, j);
		if (newcell.isBlockedOut()) {
			Log.e( this.toString(), "Error: position is blocked out: " + i + "," + j );
			return;
		}
		// OK, it's valid.  Go there:
		cursor_row = i; cursor_col = j;
		oldcell.lostCursor(); newcell.gotCursor();
		ping_CurrentCluesUpdaterThread();
	} // setCursorPos

	// Move the cursor, adjusting as necessary so that we skip over blocked
	// out cells, and we don't go off the edge of the puzzle.
	// Then update our Clue views as necessary.
	void moveCursor(int dir, int amount) {
		int new_cursor_row ;
		int new_cursor_col ;
		int delta_one ;
		if ( amount > 0 ) {
			delta_one = 1 ;
		} else if ( amount < 0 ) {
			delta_one = -1 ;
		} else {
			return ;  // amount==0 means STAYPUT
		}
		//  Do the requested move:
		new_cursor_row = getCursorRow(); new_cursor_col = getCursorCol();
		if (dir == CrossWord.ACROSS) {
			new_cursor_col += amount ;
		} else if (dir == CrossWord.DOWN) {
			new_cursor_row += amount;
		} else {
			return;  // i.e. dir == STAYPUT; do nothing
		}
		//  Now, if we've landed on an invalid square, skip over it:
		while (positionIsValid(new_cursor_row, new_cursor_col)
				&& getCell(new_cursor_row, new_cursor_col).isBlockedOut()) {
			if (dir == CrossWord.ACROSS) {
				new_cursor_col += delta_one ;
			} else if (dir == CrossWord.DOWN) {
				new_cursor_row += delta_one ;
			}
		}
		if (positionIsValid(new_cursor_row, new_cursor_col)) {
			setCursorPos(new_cursor_row, new_cursor_col);
			scroll_TouchscreenClues_to_Cursor();
		}
	} // moveCursor
	// After typing a letter, move the cursor once cell right or down as
	// appropriate:
	void incrementCursor() {
		moveCursor(direction, +1);
	} // incrementCursor
	void decrementCursor() {
		moveCursor(direction, -1);
	} // decrementCursor

	void scroll_TouchscreenClues_to_Cursor() {
		// TODO
	} // scroll_TouchscreenClues_to_Cursor

	//  Gets the clue for this cell, or null if we're not on a numbered cell:
	Clue getClue(int dir, int num) {
		for (Clue clue : clues) {
			if ((clue.dir == dir) && (clue.num == num)) {
				return (clue);
			}
		}
		return (null);
	} // Puzzle.getClue

	String getClueText(int dir, int num) {
		Clue c = getClue(dir, num);
		if (c == null) {
			return ("");
		}
		return ( c.text );
	} // Puzzle.getClueText
	


	Cell getCell(int i, int j) {
		if (cellgrid == null) {
			return (null);
		}
		try {
			return (cellgrid[i][j]);
		} catch (Exception ex) {
			Log.e(this.toString(), "Out of bounds clue: row=" + i + " col=" + j);
			return (null);
		}
	} // getCell

	
	void setDirection(int d) {
		direction = d ;

		//  Re-draw the cursor:
		cellgrid[cursor_row][cursor_col].drawBackground() ;

	} // setDirection
	
	int getDirection() {
		return(direction);
	} // getDirection

	/////////////////////////////////////////////////////////////////////////////////////////////////////

	//  We used to just call displayCurrentClues() every time the cursor moved.
	//  The trouble is, because e-ink draws slowly, this meant that cursor
	//  navigation felt sluggish.
	//  So, we force those requests through a background thread, which does a
	//  little sleep first, so the user can get in several cursor moves before
	//  the main thread has to pause to update the clues text on the e-ink.
	
	private synchronized void currentCluesUpdaterThread() {
		while(true) {
			//  wait() until we're interrupted by the main thread:
			try {
				wait();
			} catch(Exception ex) {
				Log.e(this.toString(), "Exception in wait(): " + ex );
			}
			//  now sleep a moment before drawing the current cell:
			/*
			try {
				Thread.sleep(150); // maybe they'll move a few more cells while we sleep
			} catch(Exception ex) {
				Log.e(this.toString(), "Exception in sleep(): " + ex );
				continue ;
			}
			*/
			
			current_clues_handler.post(new Runnable() {
				public void run() {
					displayCurrentClues();
				}
			});
		}
	} // currentCluesUpdaterThread
	
	//  The cursor has moved; notify the background thread
	private synchronized void ping_CurrentCluesUpdaterThread() {
		//Log.d( this.toString(), "About to notify()..." );
		notify();
		//cluesupdaterthread.interrupt();
		//Log.d( this.toString(), "Notified it." );
	}

	// Displays the two clues in the small TextView just below the crossword
	// grid, depending on the cursor location
	void displayCurrentClues() {
		Cell cell = getCell( cursor_row, cursor_col );
		Clue ca = cell.acrossclue ;
		Clue cd = cell.downclue ;
		StringBuilder sa = new StringBuilder("Across: ");
		StringBuilder sd = new StringBuilder("Down: ");
		if (ca != null) {
			sa.append( ca.num );
			sa.append( ". " );
			sa.append( ca.text );
		}
		if (cd != null) {
			sd.append( cd.num );
			sd.append( ". " );
			sd.append( cd.text );
		}
		selected_clue_a.setText(sa);
		selected_clue_d.setText(sd);
	} // displayCurrentClues
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	public void prepareTouchScreenClues(int dir) {
		touchscreenclues.prepare(dir);
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	//  A hint (cheat), to wipe out all incorrect answers:
	int eraseWrongAnswers() {
		int r = 0 ;  // count of how many wrong answers we erased
		for ( int i = 0 ; i < rows ; i++ ) {
			for ( int j = 0 ; j < cols ; j++ ) {
				Cell cell = getCell(i,j);
				if ( (! cell.usertext.equals(" ")) && (! cell.hasCorrectAnswer() )) {
					//  This cell is wrong; erase it:
					//cell.setUserText(" ");
					setUserText(i,j, " ");
					r++ ;
				}
			}
		}
		return(r);
	} // eraseWrongAnswers
	
	//  A cheat to solve the puzzle for the user, when they give up
	void justSolveTheWholeThing() {
		for ( int i = 0 ; i < rows ; i++ ) {
			for ( int j = 0 ; j < cols ; j++ ) {
				Cell cell = getCell(i,j);
				//cell.setUserText( cell.answertext );
				setUserText(i,j, cell.answertext);
			}
		}
	} // justSolveTheWholeThing
	
	void clearAllAnswers() {
		for ( int i = 0 ; i < rows ; i++ ) {
			for ( int j = 0 ; j < cols ; j++ ) {
				Cell cell = getCell(i,j);
				if ( ! cell.isBlockedOut() ) {
					//cell.setUserText( " " );
					setUserText(i,j, " ");
				}
			}
		}
	} // clearAllAnswers
	
	//  Returns a string describing this puzzle (title, author, etc)
	String aboutThisPuzzle() {
		String s;
		if (title==null || title.equals("")) {
			s = "CROSSWORD PUZZLE";
		} else {
			s = title.toUpperCase() + "\n\n" ;
		}
		if ((author != null) && (! author.equals(""))) {
			s = s + "Author: " + author + "\n" ;
		}
		if ((editor != null) && (! editor.equals(""))) {
			s = s + "Editor: " + editor + "\n" ;
		}
		if ((publisher != null) && (! publisher.equals(""))) {
			s = s + "Publisher: " + publisher + "\n" ;
		}
		if ( (copyright != null) && (! copyright.equals(""))) {
			s = s + copyright + "\n" ;
		}
		if ((date != null) && (! date.equals(""))) {
			s = s + date + "\n" ;
		}
		if ((notes != null) && (! notes.equals(""))) {
			s = s + "\n" + notes + "\n" ;
		}
		return(s);			
	} // aboutThisPuzzle
	
	
	void setUserText(int i, int j, String s) {
		
		try {
			Cell cell = getCell(i, j);
			cell.setUserText(s);
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception setting user text: " +ex );
			return ;
		}
		
		try {
			touchscreenclues.setUserText(i,j,s);
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception setting user text: " +ex );
			return ;
		}
		
	} // setUserText

	void setUserText(String s) {
		setUserText(cursor_row, cursor_col, s);
	} // setUserText
	
	String getUserGridString() {
		String s = "" ;
		for ( int i = 0 ; i < rows ; i++ ) {
			for ( int j = 0 ; j < cols ; j++ ) {
				s = s + (cellgrid[i][j]).usertext;
			}
		}
		return(s);
	} // getUserGridString
	
	boolean hasRebusEntries() {
		return( has_rebus_entries );
	} // hasRebusEntries
	
	boolean isSolved() {				
		return( gridstring.equals( getUserGridString() ) );
	} // isSolved
	
	String getFileName() {
		return( filename );
	} // getFileName
	
	String getPuzzleTitle() {
		return( title );
	} // getPuzzleTitle
	
}  // Puzzle class

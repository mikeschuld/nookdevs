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


public class Puzzle {
	CrossWord crossword_activity = null ;
	int direction = CrossWord.ACROSS ;
	private Cell[][] cellgrid = null;
	int rows = 0;
	int cols = 0;
	ArrayList<Clue> acrossClues ;
	ArrayList<Clue> downClues ;
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
	private boolean formatSupportsRebus ;
    ArrayList<int[]> mRebusCells;
    ArrayList<String> mRebusValues;
	SizeDependent sizedependent = null ;
	View grid_and_selected_clues = null; // the main eink view
	TableLayout gridlayout; // contained in grid_and_selected_clues View
	TextView selected_clue_a; // contained in grid_and_selected_clues View
	TextView selected_clue_d; // contained in grid_and_selected_clues View
	TextView einkcluespage = null;
	TouchScreenClues touchscreenclues ;
	
	//  TODO: the build_Views and populate_* stuff should be merged more
	
	Puzzle(CrossWord xw, String fn, int tmprows, int tmpcols, String gridstring, ArrayList<int[]> circles,
			ArrayList<int[]> shades, boolean isrebussupported, ArrayList<int[]> mRebusCells, ArrayList<String> mRebusValues,
			ArrayList<Clue> tmpclues, String tmptitle, String tmpauthor,
			String tmpcopyright, String tmpdate, String tmpeditor, String tmppublisher, String tmpnotes ) {
		//Log.d(this.toString(), "DEBUG: Entering Puzzle()...");
		crossword_activity = xw ;
		filename = fn ;
		rows = tmprows ; cols = tmpcols ;
		formatSupportsRebus = isrebussupported ;
		//  Store the metadata:
		if (title != null) title = tmptitle ;
		if (author != null) author = tmpauthor ;
		if (copyright != null) copyright = tmpcopyright ;
		if (date != null) date = tmpdate ;
		if (editor != null) editor = tmpeditor ;
		if (publisher != null) publisher = tmppublisher ;
		if (notes != null) notes = tmpnotes ;		
		
		
		//  Populate a grid of cells:
		cellgrid = new Cell[rows][cols] ;
		create_cells(gridstring);
		setCircleCells(circles);		
		setShadedCells(shades);
		if ( mRebusCells != null && mRebusValues != null ) {
			for ( int c = 0 ; c < mRebusCells.size() ; c++ ) {
				int row = mRebusCells.get(c)[0] ; int col = mRebusCells.get(c)[1];
				cellgrid[row][col].answertext = mRebusValues.get(c);
			}
		}
		//  Process the clues:
		acrossClues = new ArrayList<Clue>() ;
		downClues = new ArrayList<Clue>() ;
		for ( Clue tmpclue : tmpclues ) {
			if ( tmpclue.dir == CrossWord.ACROSS ) {
				acrossClues.add(tmpclue);
			} else if ( tmpclue.dir == CrossWord.DOWN ) {
				downClues.add(tmpclue);
			}
		}
		Collections.sort(acrossClues);  // they have to be in order!
		Collections.sort(downClues);  // they have to be in order!
		//  Match cells with clues and vice versa:
		synchronizeCellsAndClues();

		//  Set parameters which depend on the puzzle size:
		sizedependent = new SizeDependent(rows, cols);

		//  Build our eink Views:
		build_puzzle_views();

		//  Prepare the touchscreen clues data structures:
		touchscreenclues = new TouchScreenClues(this);
		
		//  Launch our background thread:
		cluesupdaterthread = new Thread() {
			public void run() {
				currentCluesUpdaterThread();
			}
		};
		cluesupdaterthread.setDaemon(true); // automatically exit if the main thread exits
		current_clues_handler = new Handler();
		cluesupdaterthread.start();

		//  Set the initial cursor position:
		set_Cursor_Start_Position();
		displayCurrentClues();
		
		//Log.d(this.toString(), "DEBUG: Leaving Puzzle().");
	} // Puzzle constructor
	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//  These functions are called when the puzzle is initially loaded.
	//
	//  They populate our data structures for Cells and Clues.
	//
	
	//  Based on the grid definition string, create our cells and put them in the cellgrid
	//  (note that they don't have views yet):
	private void create_cells(String gridstring) {
		for ( int i = 0 ; i < rows ; i++ ) {
			for (int j = 0; j < cols; j++) {
				boolean is_blocked_out ;
				char c = gridstring.charAt( j + (i * cols) ) ;
		        if (c == '.' || c == '~' ) {
		        	// This is a filled-in blocked-out cell:
		        	is_blocked_out = true ;
		        } else {
		        	is_blocked_out = false ;
		        }
				Cell cell = new Cell(crossword_activity, this, i, j, (c + ""), is_blocked_out, false);
				cellgrid[i][j] = cell;
			}
		}
	} // create_cells
	
	private void setCircleCells(ArrayList<int[]> circles) {
		if ( circles == null ) return ;
		for( int[] position : circles ) {
			int i = position[0];
			int j = position[1];
			getCell(i,j).is_a_circle = true ;
		}
	} // Puzzle.markCircleCells
	
	private void setShadedCells(ArrayList<int[]> shades) {
		if (shades == null) return ;
		for ( int shade[] : shades ) {
			int i = shade[0];
			int j = shade[1];
			int color = shade[2];
			getCell(i,j).setCellShade( color );	
		}
	} // markShadedCells
	

	private void synchronizeCellsAndClues() {
		//Log.d( this.toString(), "DEBUG: Entering synchronizeCellsAndClues()..." );
		for( int c = 0 ; c < 2 ; c++ ) {
			ArrayList<Clue> clues ;
			if ( c == 0 ) {
				clues = acrossClues ;
			} else {
				clues = downClues ;
			}
			for ( Clue clue : clues ) {
				//  For each clue, find the cell it starts at, and set that cell's number
				//  to the clue number:
				Cell cell = getCell( clue.row, clue.col );
				if ( cell != null ) {
					cell.number = clue.num ;
				}
				//  For each clue, figure out which cells it "owns":
				assignCellListForClue(clue) ;
			}
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

	
	// These two methods return the "best" clue for a given grid location.
	// For example, for a typical crossword puzzle, the "best" clues for
	// row=0,col=2 are probably across:1,down:3.
	private Clue getBestAcrossClue(int i, int j) {
		for (int c = j; c >= 0; c--) {
			Clue clue;
			int n = getCell(i, c).number ;
			if (n != 0) {
				clue = getClue(CrossWord.ACROSS, n);
				if (clue != null)
					return (clue);
			}
		}
		return (null); // Only happens on non-standard puzzles
	} // getBestAcrossClue
	private Clue getBestDownClue(int i, int j) {
		for (int c = i; c >= 0; c--) {
			Clue clue;
			int n = getCell(c, j).number ;
			if (n != 0) {
				clue = getClue(CrossWord.DOWN, n);
				if (clue != null)
					return (clue);
			}
		}
		return (null); // Only happens on non-standard puzzles
	} // getBestDownClue
	
	//  For a clue, assign the list of cells which its answer goes into
	private void assignCellListForClue( Clue clue ) {
		ArrayList<Cell> celllist = new ArrayList<Cell>() ;
		int i = clue.row;
		int j = clue.col;
		int dir = clue.dir;
		while( positionIsValid(i,j) && (! getCell(i,j).is_blocked_out) ) {
			celllist.add( getCell(i,j) );
			if ( dir == CrossWord.ACROSS ) {
				j++ ;
			} else if ( dir == CrossWord.DOWN ) {
				i++ ;
			}
		}
		clue.cells = celllist ;
		clue.numcells = clue.cells.size();				
	} // Puzzle.assignCellListForClue

	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////

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
		//  Build the eink table:
		for ( int i = 0 ; i < rows ; i++ ) {
			TableRow tr = (TableRow) inflater.inflate(R.layout.eink_xwordrow, null);
			for (int j = 0; j < cols; j++) {
				Cell cell = cellgrid[i][j];
				tr.addView(cell.getEinkView());
			}
			gridlayout.addView(tr);
		}
		//  The e-ink clues page:
		populate_eink_clues_page();
		//Log.d(this.toString(), "DEBUG: Leaving Puzzle.build_Views().");
	} // Puzzle.build_puzzle_views		

	private void populate_eink_clues_page() {
		//Log.d(this.toString(), "DEBUG: Entering Puzzle.populate_eink_clues_page()");
		StringBuilder s = new StringBuilder( "" );
		if ( (notes != null) && (! notes.equals("")) ) {
			s.append( notes );
			s.append( "\n\n" );
		}
		s.append( "Across\n\n" );
		for (Clue clue : acrossClues) {
			s.append( clue.num + ". " );
			s.append( clue.text );
			s.append( " (" + clue.numcells );
			s.append( ")\n" );
		}
		s.append(  "\n\nDown\n\n" ) ;
		for (Clue clue : downClues) {
			s.append( clue.num + ". " );
			s.append( clue.text );
			s.append( " (" + clue.numcells );
			s.append( ")\n" );
		}
		einkcluespage.setText(s);
		//Log.d(this.toString(), "DEBUG: Leaving Puzzle.populate_eink_clues_page()");
	} // populate_eink_clues_page

	
	
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
				if (positionIsValid(i, j) && (! getCell(i,j).is_blocked_out )) {
					setCursorPos(i,j);
					return;
				}
			}
		}
		Log.e(this.toString(), "Error: could not find any valid cells to start at");
	} // set_Cursor_Start_Position()


	int getCursorRow() {
		return (cursor_row);
	} // getCursorRow

	int getCursorCol() {
		return (cursor_col);
	} // getCursorCol

	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	void setCursorPos(int i, int j) {
		if (!positionIsValid(i, j)) {
			Log.e( this.toString(), "Error: position is invalid: " + i + "," + j );
			return;
		}
		Cell oldcell = getCell(cursor_row, cursor_col);
		Cell newcell = getCell(i, j);
		if (newcell.is_blocked_out) {
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
	private void moveCursor(int dir, int plus_or_minus) {
		int new_cursor_row ;
		int new_cursor_col ;
		int delta_one ;
		
		//  Figure out what we're doing:
		if ( plus_or_minus > 0 ) {
			delta_one = 1 ;
		} else if ( plus_or_minus < 0 ) {
			delta_one = -1 ;
		} else {
			return ;  // 0 means STAYPUT
		}
		
		//  Figure out where we are now:
		new_cursor_row = getCursorRow(); new_cursor_col = getCursorCol();
		//  Naively add/subtract 1 in that direction, and see what happens:
		if (dir == CrossWord.ACROSS) {
			new_cursor_col += delta_one ;  // ACROSS
		} else {
			new_cursor_row += delta_one ;  // DOWN
		}
		
		//  What to do if we land on an invalid square (off the edge, or blocked out):
		if ( (! positionIsValid(new_cursor_row, new_cursor_col)) || getCell(new_cursor_row, new_cursor_col).is_blocked_out ) {
			if ( crossword_activity.cursor_next_clue ) {
				Clue curClue ;
				if ( dir == CrossWord.ACROSS ) {
					curClue = (cellgrid[cursor_row][cursor_col]).acrossclue ;
				} else if ( dir == CrossWord.DOWN ) {
					curClue = (cellgrid[cursor_row][cursor_col]).downclue ;
				} else {
					return ;
				}
				if ( plus_or_minus > 0 ) {
					// go to the first cell of this clue:
					Clue nextClue =	getNextClue(curClue);
					if ( nextClue != null ) {
						new_cursor_row = nextClue.row ; new_cursor_col = nextClue.col ;
					}
				} else {
					// go to the last cell in this clue:
					Clue nextClue = getPreviousClue( curClue );
					if ( nextClue != null ) {
						int index = nextClue.cells.size() - 1 ;
						Cell nextCell = nextClue.cells.get(index) ;
						new_cursor_row = nextCell.row ; new_cursor_col = nextCell.col ;
					}
				}
			} else {

				// Move through any blocked-out cells:
				while (positionIsValid(new_cursor_row, new_cursor_col)
						&& (cellgrid[new_cursor_row][new_cursor_col]).is_blocked_out ) {
					if (dir == CrossWord.ACROSS) {
						new_cursor_col += delta_one ;
					} else if (dir == CrossWord.DOWN) {
						new_cursor_row += delta_one ;
					}
				}
				
				// Have we gone off the edge of the screen?
				if ( crossword_activity.cursor_wraps ) {
					if ( plus_or_minus > 0 ) {
						if ( new_cursor_col >= cols ) {
							new_cursor_col = 0 ; new_cursor_row++ ;
							if ( new_cursor_row >= rows ) new_cursor_row = 0 ;
						}
						else if ( new_cursor_row >= rows ) {
							new_cursor_row = 0 ;
							new_cursor_col++ ;
							if ( new_cursor_col >= cols ) new_cursor_col = 0 ;
						}
					} else {
						if ( new_cursor_col < 0 ) {
							new_cursor_col = cols-1 ; new_cursor_row-- ;
							if ( new_cursor_row < 0 ) new_cursor_row = rows-1 ;
						} else if ( new_cursor_row < 0 ) {
							new_cursor_row = rows-1 ; new_cursor_col-- ;
							if ( new_cursor_col < 0 ) new_cursor_col = cols-1 ;							
						}
					}
					// Now that we've jumped, again move through any blocked-out cells:
					while (positionIsValid(new_cursor_row, new_cursor_col)
							&& (cellgrid[new_cursor_row][new_cursor_col]).is_blocked_out ) {
						if (dir == CrossWord.ACROSS) {
							new_cursor_col += delta_one ;
						} else if (dir == CrossWord.DOWN) {
							new_cursor_row += delta_one ;
						}
					}

				}
			}
		}
		
		//  If we've figured out a valid destination, go there:
		if (positionIsValid(new_cursor_row, new_cursor_col)) {
            setCursorPos(new_cursor_row, new_cursor_col);
            scroll_TouchscreenClues_to_Cursor();
		}

		//  If there's nothing we can do, that's OK.  We're probably in
		//  simple navigation mode at the edge of the screen.
		
	} // moveCursor
	

	// Move the cursor one cell right or down as appropriate (e.g. after
	// typing a letter):
	public void incrementCursor(int dir) {
		moveCursor(dir, +1);
	} // incrementCursor
	public void incrementCursor() {
		moveCursor(direction, +1);
	} // incrementCursor
	public void decrementCursor(int dir) {
		moveCursor(dir, -1);
	} // decrementCursor
	public void decrementCursor() {
		moveCursor(direction, -1);
	} // decrementCursor

	////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private Clue getPreviousClue(Clue thisClue) {		
		if (thisClue == null) return(null);
		ArrayList<Clue> clues ;
		if ( thisClue.dir == CrossWord.ACROSS ) {
			clues = acrossClues ;
		} else {
			clues = downClues ;
		}
		int index ;
		index = clues.indexOf(thisClue);
		if ( index < 0 ) return( thisClue ); // shouldn't happen
		if ( index > 0 ) {
			// Return the previous clue in the list:
			return( clues.get(index - 1) );
		} else {
			// We were the first clue, so go to the end of the list:
			return( clues.get( clues.size() - 1 ) );
		}
	} // getPreviousClue
	
	private Clue getNextClue(Clue thisClue) {
		if (thisClue == null) return(null);
		ArrayList<Clue> clues ;
		if ( thisClue.dir == CrossWord.ACROSS ) {
			clues = acrossClues ;
		} else {
			clues = downClues ;
		}
		int index ;
		index = clues.indexOf(thisClue);
		if ( index < 0 ) return( thisClue ); // shouldn't happen
		if ( index < clues.size() ) {
			// Return the next clue in the list:
			return( clues.get(index + 1) );
		} else {
			// We were the last clue, so go back to the beginning:
			return( clues.get(0) );
		}
	} // getNextClue
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////

	void scroll_TouchscreenClues_to_Cursor() {
		// TODO?
	} // scroll_TouchscreenClues_to_Cursor

	//  Look up a clue by clue number:
	public Clue getClue(int dir, int num) {
		ArrayList<Clue> clues ;
		if ( dir == CrossWord.ACROSS ) {
			clues = acrossClues ;
		} else {
			clues = downClues ;
		}
		for (Clue clue : clues) {
			if ( clue.num == num ) {
				return (clue);
			}
		}
		return (null);
	} // Puzzle.getClue

	public String getClueText(int dir, int num) {
		Clue c = getClue(dir, num);
		if (c == null) {
			return ("");
		}
		return ( c.text );
	} // Puzzle.getClueText
	


	public Cell getCell(int i, int j) {
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
	
	
	public String getCurrentCellUserText() {
		Cell cell = getCell(cursor_row, cursor_col);
		return( cell.usertext ) ;
	} // getCurrentCellUserText

	
	public void setDirection(int d) {
		direction = d ;
		//  Re-draw the cursor:
		cellgrid[cursor_row][cursor_col].drawBackground() ;
	} // setDirection
	
	public int getDirection() {
		return(direction);
	} // getDirection

	/////////////////////////////////////////////////////////////////////////////////////////////////////

	//  We used to just call displayCurrentClues() every time the cursor moved.
	//  The trouble is, because e-ink draws slowly, this meant that cursor
	//  navigation was very sluggish.
	//  So, we force those requests through a background thread.
	//  The command to draw the clues is run with a delay, so that if the user
	//  presses the arrow key several times in a row, they can get in several
	//  cursor moves before the main thread has to pause to update the clues
	//  text on the e-ink.
	
	private boolean currentCluesUpdaterThreadTimeToDie = false ;  // set true when thread should exit
	
	private synchronized void currentCluesUpdaterThread() {
		Runnable cmd = new Runnable() {
			public void run() {
				displayCurrentClues();
			}
		} ;
		while(true) {
			//  wait() until we're interrupted by the main thread:
			try {
				wait();
			} catch(Exception ex) {
				Log.e(this.toString(), "Exception in wait(): " + ex );
			}
			if ( currentCluesUpdaterThreadTimeToDie ) {
				return ;
			}
			current_clues_handler.removeCallbacks(cmd);
			current_clues_handler.postDelayed(cmd, 300);
		}
	} // currentCluesUpdaterThread
	
	//  The cursor has moved; notify the background thread
	private synchronized void ping_CurrentCluesUpdaterThread() {
		//Log.d( this.toString(), "About to notify()..." );
		notify();
		//cluesupdaterthread.interrupt();
		//Log.d( this.toString(), "Notified it." );
	}
	
	public synchronized void killChildThreads() {
		currentCluesUpdaterThreadTimeToDie = true ;
		cluesupdaterthread.interrupt();
	} // killChildThreads


	// Displays the two clues in the small TextView just below the crossword
	// grid, depending on the cursor location
	void displayCurrentClues() {
		Cell cell = getCell( cursor_row, cursor_col );
		Clue ca = cell.acrossclue ;
		Clue cd = cell.downclue ;
		StringBuilder sa = new StringBuilder( crossword_activity.getString(R.string.eink_across_label) + " " );
		StringBuilder sd = new StringBuilder( crossword_activity.getString(R.string.eink_down_label) + " " );
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
				if ( (! cell.is_blocked_out) && (! cell.usertext.equals(" ")) && cell.hasWrongAnswer() ) {
					//  This cell is wrong; erase it:
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
				setUserText( i, j, getCell(i,j).answertext );
			}
		}
	} // justSolveTheWholeThing
	
	void clearAllAnswers() {
		for ( int i = 0 ; i < rows ; i++ ) {
			for ( int j = 0 ; j < cols ; j++ ) {
				Cell cell = getCell(i,j);
				if ( ! cell.is_blocked_out ) {
					setUserTextUnconditionally(cell, " ");
				}
			}
		}
	} // clearAllAnswers
	
	//  Returns a string describing this puzzle (title, author, etc)
	public String aboutThisPuzzle() {
		String s;
		if (title==null || title.equals("")) {
			s = filename.replaceAll( ".*/", "");
		} else {
			//s = title.toUpperCase() + "\n\n" ;
			s = title + "\n\n" ;
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
	
	
	//  TODO: should this filter for allowed characters?
	private void setUserTextUnconditionally(Cell cell, String s) {
		if ( s == null ) {
			s = " " ;
		} else if ( s.equals("") ) {
			s = " ";
		}
		//s = s.toUpperCase();
		try {
			cell.setUserText(s);
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception setting user text: " +ex );
			return ;
		}
		try {
			touchscreenclues.setUserText(cell.row, cell.col, s);
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception setting user text: " +ex );
			return ;
		}
	} // setUserTextUnconditionally

	private void setUserText(int i, int j, String s) {
		Cell cell = getCell(i, j);
    	if ( crossword_activity.freeze_right_answers ) {
    		if ( cell.hasCorrectAnswer() ) return ;
    	}
    	setUserTextUnconditionally(cell,s);
	} // setUserText

	public void setUserText(String s) {
		setUserText(cursor_row, cursor_col, s);
	} // setUserText

	//  Used to notify the puzzle that the user has changed settings:
	public void settingsHaveChanged() {
		// In case they've changed the setting to mark wrong answers, we should
		// re-draw any cells with wrong answers:
		for ( int i = 0 ; i < rows ; i++ ) {
			for ( int j = 0 ; j < cols ; j++ ) {
				if ( (cellgrid[i][j]).hasWrongAnswer() ) {
					(cellgrid[i][j]).drawBackground() ;
				}
			}
		}
	}
	
	
	//  Checks the user's answers to determine if the puzzle is solved.
	public boolean isSolved() {
		for ( int i = 0 ; i < rows ; i++ ) {
			for ( int j = 0 ; j < cols ; j++ ) {
				Cell cell = cellgrid[i][j] ;
				if ( cell.is_blocked_out ) continue ;
				if (formatSupportsRebus ) {
					if ( ! cell.usertext.equals(cell.answertext) ) {
						return(false);
					}
				} else {
					//  Old .puz formats don't support rebus entries, although the
					//  puzzle solution might require them.  So, we have to hack it.
					//  We only look at the user's first letter, and we translate
					//  symbols to a corresponding letter.  (This is what AcrossLite
					//  does, too.)
					char cuser, cuseralt, canswer;
					cuser = cell.usertext.charAt(0); cuseralt = cuser;
					canswer = cell.answertext.charAt(0);
					switch(cuser) {
					case '0': cuseralt='Z'; break;
					case '1': cuseralt='O'; break;
					case '2': cuseralt='T'; break;
					case '3': cuseralt='T'; break;
					case '4': cuseralt='F'; break;
					case '5': cuseralt='F'; break;
					case '6': cuseralt='S'; break;
					case '7': cuseralt='S'; break;
					case '8': cuseralt='E'; break;
					case '9': cuseralt='N'; break;
					case '@': cuseralt='A'; break;
					case '#': cuseralt='H'; break;
					case '$': cuseralt='D'; break;
					case '%': cuseralt='P'; break;
					case '&': cuseralt='A'; break;
					case '+': cuseralt='P'; break;
					case '?': cuseralt='Q'; break;
					}
					if ( (cuser != canswer) && (cuseralt != canswer) ) {
						return(false);
					}
				}
			}
		}
		return(true);
	} // isSolved
	
	public String getFileName() {
		return( filename );
	} // getFileName
	
	public String getPuzzleTitle() {
		return( title );
	} // getPuzzleTitle
	
}  // Puzzle class

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

import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import java.util.ArrayList;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;

//  cluepages_? are ArrayLists of ArrayLists, representing a list of clues for each page.
//
//  The buttongrid[][] arrays are mostly full of nulls.
//  They contain only the currently visible buttons at a given time.
//
//  When the user types a letter into a crossword cell, we
//  are called, and we check this grid in order to update
//  the button text, but only if it's displayed at the time.
//
// 		touchscreen_clues_scroller.requestChildFocus( touchscreenclues, button ) ;




public class TouchScreenClues {
	Puzzle puzzle ;
	ScrollView touchscreen_clues_scroller ;
	//LinearLayout layout ;
    TableLayout table ;
    HorizontalScrollView top_navbuttons ;
    HorizontalScrollView bottom_navbuttons ;
    Button buttongrid_a[][] ;
    Button buttongrid_d[][] ;
    private static final int MAX_CLUES_PER_PAGE = 10 ;
    ArrayList<ArrayList<Clue>> cluespages_a ;
    ArrayList<ArrayList<Clue>> cluespages_d ;
    int num_pages_a ;
    int num_pages_d ;

    TouchScreenClues(Puzzle pp) {
		puzzle = pp ;
		LayoutInflater inflater = puzzle.crossword_activity.getLayoutInflater();
		touchscreen_clues_scroller = (ScrollView) inflater.inflate(R.layout.touchscreenclues, null);
		//layout = (LinearLayout) touchscreen_clues_scroller.findViewById(R.id.layout);
	    table = (TableLayout) touchscreen_clues_scroller.findViewById(R.id.table);
	    top_navbuttons = (HorizontalScrollView) touchscreen_clues_scroller.findViewById(R.id.top_navbuttons);
	    bottom_navbuttons = (HorizontalScrollView) touchscreen_clues_scroller.findViewById(R.id.bottom_navbuttons);
	    buttongrid_a = new Button[puzzle.rows][puzzle.cols] ;
	    buttongrid_d = new Button[puzzle.rows][puzzle.cols] ;
	    cluespages_a = new ArrayList<ArrayList<Clue>>();
	    cluespages_d = new ArrayList<ArrayList<Clue>>();
	    num_pages_a = build_cluelist( CrossWord.ACROSS, cluespages_a );
	    num_pages_d = build_cluelist( CrossWord.DOWN, cluespages_d );
	}
    

    //  Figure out how many clues pages we'll need and which clues go on
    //  which page.
    private int build_cluelist(int dir, ArrayList<ArrayList<Clue>> cluelist) {
    	int pagenum = 0 ;
    	cluelist.add( new ArrayList<Clue>() ) ;
    	int cluesonthispage = 0 ;
    	
		ArrayList<Clue> clues ;
		if ( dir == CrossWord.ACROSS ) {
			clues = puzzle.acrossClues ;
		} else {
			clues = puzzle.downClues ;
		}

    	for ( Clue clue: clues ) {
    		if ( cluesonthispage > MAX_CLUES_PER_PAGE ) {
    			pagenum++ ; cluesonthispage=0 ;
    			cluelist.add( new ArrayList<Clue>() ) ;
    		}
    		cluelist.get(pagenum).add(clue);
    		cluesonthispage++ ;
    	}
    	return( pagenum + 1 ) ;  // The number of pages
    } // build_cluelist
    
    //////////////////////////////////////////////////////////////////////////////////////
    //
    //  These prepare() methods are called right before our view is displayed
    //  to the user.  We use them to populate the list of clues the user will
    //  see.
    //
    
    public void prepare() {
    	int row = puzzle.cursor_row ;
    	int col = puzzle.cursor_col ;
    	int dir = puzzle.direction;
    	prepare(row, col, dir);
    }
    
    public void prepare(int dir) {
    	int row = puzzle.cursor_row ;
    	int col = puzzle.cursor_col ;
    	prepare(row, col, dir);
    }
    
    public void prepare(int row, int col, int dir) {
    	int pagenum = 0 ;
    	int num_pages ;
    	Clue targetclue ;
        ArrayList<ArrayList<Clue>> cluelist ;

    	//  Figure out which clue page we should display:
    	boolean gotit=false ;
    	if ( dir == CrossWord.ACROSS ) {
    		num_pages = num_pages_a ;
    		targetclue = puzzle.getCell(row,col).acrossclue ;
    		cluelist = cluespages_a ;
    	} else {
    		num_pages = num_pages_d ;
    		targetclue = puzzle.getCell(row,col).downclue ;
    		cluelist = cluespages_d ;
    	}
        try {
        	for( int n = 0 ; (n < num_pages) && (! gotit) ; n++ ) {
        		for ( Clue clue : cluelist.get(n) ) {
        			if ( clue == targetclue ) {
        				pagenum = n ; gotit=true ; break ;
        			}
        		}
        	}
        } catch (Exception ex ) {
        	Log.e( this.toString(), "Unexpected exception: " + ex ) ;
        }
        
    	if ( gotit ) {
    		prepare_page(dir, pagenum);
    	} else {
    		Log.e( this.toString(), "Error: could not find clue") ;
    		return ;
    	}
    	
    } // prepare
    
    private void prepare_page(int dir, int pagenum) {
    	ArrayList<ArrayList<Clue>> cluelist ;
    	//  Erase everything:
    	for ( int i = 0 ; i < puzzle.rows ; i++ ) {
    		for ( int j = 0 ; j < puzzle.cols ; j++ ) {
    			buttongrid_a[i][j] = null ;
    			buttongrid_d[i][j] = null ;
    		}
    	}
    	table.removeAllViews();
    	top_navbuttons.removeAllViews();
    	bottom_navbuttons.removeAllViews();
    	//  Now populate everything:
    	if ( dir == CrossWord.ACROSS ) {
    		cluelist = cluespages_a ;
    	} else {
    		cluelist = cluespages_d ;
    	}
    	for ( Clue clue : cluelist.get(pagenum) ) {
	    	addClueToTable(clue, table);
    	}
	    top_navbuttons.addView( buildNavButtons(dir, pagenum) );
	    bottom_navbuttons.addView( buildNavButtons(dir, pagenum) );
	    //  Scroll to (almost) the top:
	    try {
	    	touchscreen_clues_scroller.scrollTo(0,40);
	    } catch (Exception ex) { }
    } // prepare_page
    
    //////////////////////////////////////////////////////////////////////////////////////
    

    void addClueToTable(Clue clue, TableLayout table) {
    	LayoutInflater inflater = puzzle.crossword_activity.getLayoutInflater();

    	TableRow tablerow_text = (TableRow) inflater.inflate(R.layout.touchscreenclues_row_text, null);

        TextView cluenumber = (TextView) tablerow_text.findViewById(R.id.cluenumber);
        TextView cluetext = (TextView) tablerow_text.findViewById(R.id.cluetext);
        cluenumber.setText( "" + clue.num + ". " ) ;
        cluetext.setText( clue.text ) ;
        table.addView(tablerow_text);
        
        TableRow tablerow_cells = (TableRow) inflater.inflate(R.layout.touchscreenclues_row_cells, null);
        LinearLayout dirind = (LinearLayout) tablerow_cells.findViewById(R.id.direction_indicator);
        if (clue.dir == CrossWord.ACROSS ) {
                dirind.setBackgroundResource(R.drawable.blue_across_arrow);
        } else {
                dirind.setBackgroundResource(R.drawable.blue_down_arrow);
        }
        LinearLayout cellslayout = (LinearLayout) tablerow_cells.findViewById(R.id.cellslayout);
        int cellcount = 0 ;
        for ( Cell cell : clue.cells  ) {
        	cellcount++ ;
            LinearLayout cellbg = (LinearLayout) inflater.inflate(R.layout.touchscreenclues_cell, null);
            cellbg.setBackgroundColor( cell.shade );
            Button cellbutton = (Button) cellbg.findViewById(R.id.cellbutton);
            cellbutton.setTextSize(TypedValue.COMPLEX_UNIT_PX, puzzle.sizedependent.getTouchScreenCellTextSize(cell.usertext.length()) );
            cellbutton.setText( cell.usertext ) ;
            cellbutton.setWidth( puzzle.sizedependent.touchscreenCellWidth );
            cellbutton.setHeight( puzzle.sizedependent.touchscreenCellHeight );
            //  Call on the Cell class to draw our background based on the cell's contents:
            cell.drawBackgroundIcon_TouchScreen( (View) cellbutton );

            int i[] = new int[3] ;
            i[0] = cell.row; i[1] = cell.col; i[2] = clue.dir;
            cellbutton.setTag( i );
            cellbutton.setOnClickListener(new OnClickListener() {
            	public void onClick(View v) {
            		try {
                		int[] i = (int []) v.getTag();
                		int row = i[0] ; int col = i[1]; int dir = i[2];
                        puzzle.setCursorPos( row, col );
                        puzzle.crossword_activity.setDirection_with_Toast( dir );
                        puzzle.crossword_activity.bringUpKeyboard();
            		} catch ( Exception ex ) {
            			Log.d( this.toString(), "Unexpected exception: " + ex );
            		}
                }
            });
            if ( clue.dir == CrossWord.ACROSS ) {
            	buttongrid_a[cell.row][cell.col] = cellbutton ;
            } else {
            	buttongrid_d[cell.row][cell.col] = cellbutton ;
            }
            cellslayout.addView( cellbg );
            
            //  If there are too many cells to fit on the screen, start a new row:
        	if ( cellcount >= SizeDependent.max_cells_on_touchscreen_clues_scroller ) {
        		table.addView(tablerow_cells);
        		tablerow_cells = (TableRow) inflater.inflate(R.layout.touchscreenclues_row_cells_nextline, null);
                cellslayout = (LinearLayout) tablerow_cells.findViewById(R.id.cellslayout);
        		cellcount = 0 ;
        	}
        }
        table.addView(tablerow_cells);
    } // addClueToTable
    
    
    void setUserText(int i, int j, String s) {
    	
    	try {
			if ( buttongrid_a[i][j] != null ) {
				(buttongrid_a[i][j]).setTextSize(TypedValue.COMPLEX_UNIT_PX, puzzle.sizedependent.getTouchScreenCellTextSize(s.length()) );
				(buttongrid_a[i][j]).setText(s);
				puzzle.getCell(i,j).drawBackgroundIcon_TouchScreen( buttongrid_a[i][j] );
			}
			if ( buttongrid_d[i][j] != null ) {
				(buttongrid_d[i][j]).setTextSize(TypedValue.COMPLEX_UNIT_PX, puzzle.sizedependent.getTouchScreenCellTextSize(s.length()) );
				(buttongrid_d[i][j]).setText(s);
				puzzle.getCell(i,j).drawBackgroundIcon_TouchScreen( buttongrid_d[i][j] );
			}
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: unexpected exception adding text to touchscreen clues: " + ex );
		}
    	
    } // setUserText
    
    //////////////////////////////////////////////////////////////////////////////////////

    View buildNavButtons(int dir, int mypagenum) {
    	ArrayList<ArrayList<Clue>> cluespages ;
    	int num_pages ;
    	if ( dir == CrossWord.ACROSS ) {
        	cluespages = cluespages_a ;
        	num_pages = num_pages_a ;
        	dir = CrossWord.ACROSS ;
    	} else {
        	cluespages = cluespages_d ;
        	num_pages = num_pages_d ;
        	dir = CrossWord.DOWN ;
    	}
    	LayoutInflater inflater = puzzle.crossword_activity.getLayoutInflater();
    	LinearLayout navlayout = (LinearLayout) inflater.inflate(R.layout.touchscreenclues_navbuttons, null);
    	navlayout.removeAllViews();
    	for ( int n = 0 ; n < num_pages ; n++ ) {
    		ArrayList<Clue> clueslist = cluespages.get(n);
    		Button b = (Button) inflater.inflate(R.layout.touchscreenclues_navbutton, null);
    		int startnum = clueslist.get(0).num ;
    		int endnum = clueslist.get( clueslist.size() - 1 ).num ;
    		if ( startnum == endnum ) {
    			b.setText( "" + startnum );
    		} else {
    			b.setText( startnum + "-" + endnum );
    		}
    		if ( n == mypagenum ) {
    			b.setTypeface(Typeface.DEFAULT_BOLD) ;
    		}
    		int i[] = new int[2] ;
    		i[0] = dir; i[1] = n ;
    		b.setTag( i );
            b.setOnClickListener(new OnClickListener() {
            	public void onClick(View v) {
            		int i[] = (int []) v.getTag();
            		int dir = i[0] ; int pagenum = i[1] ;
            		prepare_page( dir, pagenum );
                }
            });
            navlayout.addView( b );
    	}
    	return( (View)navlayout );
    } // buildNavButtons
    
    //////////////////////////////////////////////////////////////////////////////////////

	View getView() {
		return( (View)touchscreen_clues_scroller);
	}

}

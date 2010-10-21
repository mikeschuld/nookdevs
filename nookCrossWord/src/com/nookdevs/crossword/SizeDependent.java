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

//
//  Depending on the number of cells in the crossword, we need to set
//  the text size differently.  Also, for smaller numbers of cells
//  (larger cells), we'd like a higher-resolution background than
//  for larger numbers of cells.  This class abstracts this sort of
//  consideration from the rest of the program.
//
//  Note that 25x25 is about the limit of what's usable on the
//  nook's screen.
//

//import android.util.Log;


public class SizeDependent {
	  // Hard-coded, based on nook hardware:
	  static final int EINK_WINDOW_HEIGHT = 760 ;  // the size of the eink window (minus the titlebar)
	  static final int PUZZLE_WINDOW_HEIGHT = 660 ;  // the max puzzle height, leaving room for the two textclues
	  static final int PUZZLE_WINDOW_WIDTH = 600 ;
	  static final int PUZZLE_WINDOW_DIMENSION = 600 ;  // the smaller of the two dimensions
	  static final int max_cells_on_touchscreen_clues_scroller = 8 ;
	  //  Determined dynamically, based on puzzle dimensions:
	  float cell_textsize ;
	  float cellnum_textsize ;
	  int cellHeight ;  // How many pixels high we expect each cell to be
	  int cellWidth ;   // How many pixels wide we expect each cell to be
	  int cell_icon_normal ;
	  int cell_icon_circle ;
	  int cell_icon_normal_cursor ;
	  int cell_icon_normal_cursor_across ;
	  int cell_icon_normal_cursor_down ;
	  int cell_icon_circle_cursor ;
	  int cell_icon_circle_cursor_across ;
	  int cell_icon_circle_cursor_down ;
	  int cell_icon_normal_wrong ;
	  int cell_icon_circle_wrong ;
	  int cell_icon_normal_cursor_wrong ;
	  int cell_icon_normal_cursor_across_wrong ;
	  int cell_icon_normal_cursor_down_wrong ;
	  int cell_icon_circle_cursor_wrong ;
	  int cell_icon_circle_cursor_across_wrong ;
	  int cell_icon_circle_cursor_down_wrong ;
	  
	  
	  SizeDependent(int i, int j) {
		  int numcells ;
		  // Most puzzles are square, but if not, use the larger dimension:
		  if ( i > j ) {
			  numcells = i ;
		  } else {
			  numcells = j ;
		  }
		  cellHeight = PUZZLE_WINDOW_HEIGHT / numcells ;
		  cellWidth = PUZZLE_WINDOW_WIDTH / numcells ;
		  calculate_textsize(numcells);
		  pick_iconset(numcells);
	  }  // SizeDependent constructor

	  private void calculate_textsize(int n) {
		  Double dt, dtn;
		  dt = (PUZZLE_WINDOW_DIMENSION * .80) / n;  // * .73 allows for a square cell
		  cell_textsize = dt.floatValue();
		  dtn = dt * .35 ;
		  cellnum_textsize = dtn.floatValue() ;
	  } // calculate_textsize

	  //  Returns the cell text size, based on the number of letters in the cell:
	  float getCellTextSize( int numLetters ) {
		  if ( numLetters <= 1 ) {
			  return( cell_textsize );
		  } else if ( numLetters <= 4 ) {
			  return( cell_textsize / 2 );
		  } else {
			  return( cell_textsize / 4 );
		  }
	  } // getCellTextSize
	  
	  // 1x1 to 15x15:
	  private int[] iconset40 = { 	R.drawable.cell_40,
			  						R.drawable.cell_circle_40,
              						R.drawable.cell_cursor_40,
              						R.drawable.cell_cursor_across_40,
              						R.drawable.cell_cursor_down_40,
              						R.drawable.cell_circle_cursor_40,
              						R.drawable.cell_circle_cursor_across_40,
              						R.drawable.cell_circle_cursor_down_40,
              					    R.drawable.cell_wrong_40,
        			  				R.drawable.cell_circle_wrong_40,
                      				R.drawable.cell_cursor_wrong_40,
                      				R.drawable.cell_cursor_across_wrong_40,
                      				R.drawable.cell_cursor_down_wrong_40,
                      				R.drawable.cell_circle_cursor_wrong_40,
                      				R.drawable.cell_circle_cursor_across_wrong_40,
                      				R.drawable.cell_circle_cursor_down_wrong_40,
              						} ;
	  // 16x16 to 25x25:
	  private int[] iconset24 = { 	R.drawable.cell_24,
			  						R.drawable.cell_circle_24,
									R.drawable.cell_cursor_24,
									R.drawable.cell_cursor_across_24,
									R.drawable.cell_cursor_down_24,
									R.drawable.cell_circle_cursor_24,
									R.drawable.cell_circle_cursor_across_24,
									R.drawable.cell_circle_cursor_down_24,
									R.drawable.cell_wrong_24,                           
									R.drawable.cell_circle_wrong_24,
									R.drawable.cell_cursor_wrong_24,                
									R.drawable.cell_cursor_across_wrong_24,
									R.drawable.cell_cursor_down_wrong_24,
									R.drawable.cell_circle_cursor_wrong_24,
									R.drawable.cell_circle_cursor_across_wrong_24,
									R.drawable.cell_circle_cursor_down_wrong_24,
									} ;
	  // 26x26 and above:
	  private int[] iconset10 = { 	R.drawable.cell_10,
									R.drawable.cell_circle_10,
									R.drawable.cell_cursor_10,
									R.drawable.cell_cursor_across_10,
									R.drawable.cell_cursor_down_10,
									R.drawable.cell_circle_cursor_10,
									R.drawable.cell_circle_cursor_across_10,
									R.drawable.cell_circle_cursor_down_10,
									R.drawable.cell_wrong_10,                           
									R.drawable.cell_circle_wrong_10,
									R.drawable.cell_cursor_wrong_10,                
									R.drawable.cell_cursor_across_wrong_10,
									R.drawable.cell_cursor_down_wrong_10,
									R.drawable.cell_circle_cursor_wrong_10,
									R.drawable.cell_circle_cursor_across_wrong_10,
									R.drawable.cell_circle_cursor_down_wrong_10,
									} ;


	  private void pick_iconset(int n) {
		int[] iconset ;

		Double d = ( (PUZZLE_WINDOW_DIMENSION * 1.0) / n ) ;
	    float iconmax = d.floatValue() ;
	    if ( 40 <= iconmax ) {
	    	iconset = iconset40 ;
	    } else if ( 24 <= iconmax ) {
	      iconset = iconset24 ;
	    } else {
	    	iconset = iconset10 ;
	    }
		cell_icon_normal = iconset[0] ;
		cell_icon_circle = iconset[1] ;
		cell_icon_normal_cursor = iconset[2] ;
		cell_icon_normal_cursor_across = iconset[3] ;
		cell_icon_normal_cursor_down = iconset[4] ;
		cell_icon_circle_cursor = iconset[5] ;
		cell_icon_circle_cursor_across = iconset[6] ;
		cell_icon_circle_cursor_down = iconset[7] ;
		cell_icon_normal_wrong = iconset[8] ;
		cell_icon_circle_wrong = iconset[9] ;
		cell_icon_normal_cursor_wrong = iconset[10] ;
		cell_icon_normal_cursor_across_wrong = iconset[11] ;
		cell_icon_normal_cursor_down_wrong = iconset[12] ;
		cell_icon_circle_cursor_wrong = iconset[13] ;
		cell_icon_circle_cursor_across_wrong = iconset[14] ;
		cell_icon_circle_cursor_down_wrong = iconset[15] ;
	  } // pick_iconset
	
} // SizeDependent class

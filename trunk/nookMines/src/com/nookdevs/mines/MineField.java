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
 * 
 * Written by Kevin Vajk
 */


package com.nookdevs.mines;

import java.util.Random;
import java.util.ArrayList;
import android.util.Log;

public class MineField {
	  public static final int MINEFIELDSTATE_PLAYING=1;
	  public static final int MINEFIELDSTATE_WON=2;
	  public static final int MINEFIELDSTATE_LOST=3;
	  public int minefield_state = MINEFIELDSTATE_PLAYING ;
	  public int rows ; public int cols; public int num_mines ;
	  private Cell[][] cellgrid ;
	  public MineFieldViews mineFieldViews;
	  public MinesPlayActivity activity ;
	  public MineField mineField ;
	  int planted_flags ;
	  int cleared_cells ;
	  
	  MineField( MinesPlayActivity a, int r, int c, int n ) {
		activity = a ;
	    rows = r; cols = c; num_mines = n;
	    cellgrid = new Cell[rows][cols] ;
	    for ( int i = 0 ; i < rows ; i++ ) {
	    	for ( int j = 0 ; j < cols ; j++ ) {
	    		cellgrid[i][j] = new Cell(i,j);
	    	}
	    }
	    assignMines(num_mines);
	    assignCellNumbers();
	    // call this last, after the minefield is fully created:
	    mineFieldViews = new MineFieldViews( this );
	    planted_flags = 0 ;
	    cleared_cells = 0 ;
	  }

///////////////////////////////////////////////////////////////////////

	  ///////////////////////////////////////////////////////////////////////
	  //
	  //  Functions called by our constructor:
	  //

	  private void assignMines(int n) {
	    if ( n >= (rows * cols) ) {
	      Log.e(this.toString(), "Internal error: too many mines for grid size");
	      activity.finish();
	    }
	    Random generator = new Random();
	    for ( int i=0 ; i<n ; i++ ) {
	      while( true ) {
	        int randomR = generator.nextInt( rows );
	        int randomC = generator.nextInt( cols );
	        if ( ! (cellgrid[randomR][randomC]).has_a_mine ) {
	          (cellgrid[randomR][randomC]).has_a_mine = true ;
	          break ;
	        }
	      }
	    }
	  } // assignMines

	  //  Call this after we've created mines:
	  private void assignCellNumbers() {
		  int r,c;
		  for( r = 0 ; r < rows ; r++ ) {
			  for( c = 0 ; c < cols ; c++ ) {
				  ArrayList<Cell> neighbors = getNeighbors( cellgrid[r][c] );
				  for( Cell neighbor : neighbors ) {
					  if ( neighbor.has_a_mine ) {
						  (cellgrid[r][c]).num ++ ;
					  }
				  }
			  }
		  }
	  } // assignCellNumbers

	  //  Returns a list of all the cells next to the cell
	  //  e.g. cell 0,0 has 3 neighbors, cell 0,1 has 5 neighbors,
	  //       cell 1,1, has 8 neighbors.
	  private ArrayList<Cell> getNeighbors(Cell cell) {
		  ArrayList<Cell> neighbors = new ArrayList<Cell>();
		  int r,c;
		  for( r = (cell.row - 1) ; r <= (cell.row + 1) ; r++ ) {
			  for( c = (cell.col - 1) ; c <= (cell.col + 1) ; c++ ) {
				  if ( (r >= 0) && (r < rows) && (c >= 0) && (c < cols) ) {
					  if ( (r != cell.row) || (c != cell.col) ) {
						  neighbors.add( cellgrid[r][c] );
					  }
				  }
			  }
		  }
		  return neighbors;
	  } // getNeighbors()

	  /////////////////////////////////////////////////////////////////////////////
	  
	  public boolean cellIsFlagged(int r, int c) {
		  if ( cellgrid[r][c].is_flagged ) {
			  return(true);
		  } else {
			  return(false);
		  }
	  }
	  
	  public boolean cellIsCleared(int r, int c) {
		  if ( cellgrid[r][c].is_cleared ) {
			  return(true);
		  } else {
			  return(false);
		  }
	  }
	  
	  public int getCellNum(int r, int c) {
		  return( cellgrid[r][c].num ) ;
	  }
	  
	  public boolean cellHasExplosion(int r, int c) {
		  if ( cellgrid[r][c].has_explosion ) {
			  return(true);
		  } else {
			  return(false);
		  }
	  }

	  public boolean cellHasAMine(int r, int c) {
		  if ( cellgrid[r][c].has_a_mine ) {
			  return(true);
		  } else {
			  return(false);
		  }
	  }

	  /////////////////////////////////////////////////////////////////////////////

	  //  They clicked on a mine:
	  private void gameLost(int r, int c) {
	    minefield_state = MINEFIELDSTATE_LOST ;
	    (cellgrid[r][c]).has_explosion = true ;
	    mineFieldViews.gameLost();
	  }  // gameLost
	  
	  private void gameWon() {
		  minefield_state = MINEFIELDSTATE_WON ;
		  mineFieldViews.gameWon();
	  } // gameWon

	  private void have_we_won_yet() {
		  if ( planted_flags + cleared_cells == (rows * cols) ) {
			  gameWon();
		  }
	  } // private_void_have_we_won_yet
	  
	  public void toggleCellFlag( int row, int col ) {
		  if ( minefield_state !=  MINEFIELDSTATE_PLAYING ) return ;
		  if ( (cellgrid[row][col]).is_cleared ) return ;
		  if ( (cellgrid[row][col]).is_flagged ) {
			  (cellgrid[row][col]).removeFlag();
			  planted_flags-- ;
		  } else {
			  (cellgrid[row][col]).plantFlag();
			  planted_flags++ ;
		  }
		  mineFieldViews.cellHasChanged(row,col);
		  have_we_won_yet();
	  } // flagCell
	  
	  public void clearCell(int row, int col) {
		  if ( minefield_state !=  MINEFIELDSTATE_PLAYING ) return ;  
		  if ( (cellgrid[row][col]).is_cleared ) return ;		  
		  if ( (cellgrid[row][col]).has_a_mine ) {
			  gameLost(row,col);
			  return ;
		  } else {
			  (cellgrid[row][col]).clear() ;
			  mineFieldViews.cellHasChanged(row,col);
			  cleared_cells++ ;
		  }
		  
		  //  If the we've cleared a cell, and its number is zero, then all adjacent
		  //  cells should be cleared as well.  We iterate multiple times because those
		  //  cells we've cleared might be zero, too.  Not the most efficient algorithm,
		  //  but it'll do.
		  if ( (cellgrid[row][col]).num == 0 ) {
			  boolean cleared_anything ;
			  while( true ) {
				  cleared_anything = false ;
				  for( int r = 0 ; r < rows ; r++ ) {
					  for ( int c = 0 ; c < cols ; c++ ) {
						  Cell cell = cellgrid[r][c];
						  if ( (cell.num == 0) && cell.is_cleared ) {
							  for ( Cell neighbor : getNeighbors(cell) ) {
								  if ( ! neighbor.is_cleared ) {
									  neighbor.clear();
									  cleared_cells++ ;
									  mineFieldViews.cellHasChanged(neighbor.row, neighbor.col);
									  cleared_anything = true ;
								  }
							  }
						  }
					  }
				  }
				  if ( ! cleared_anything ) break ;
			  }
		  }
		  have_we_won_yet();
	  } // clearCell
	  
}

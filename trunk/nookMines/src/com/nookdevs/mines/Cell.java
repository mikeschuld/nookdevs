package com.nookdevs.mines;

public class Cell {
	public int row;
	public int col;
	public int num;
	public boolean has_a_mine;
	public boolean is_cleared;
	public boolean is_flagged;
	public boolean has_explosion;
	
	Cell(int r, int c) {
		row = r ;
		col = c ;
		num = 0 ;  // for now
		has_a_mine = false ; // for now
		is_cleared = false ;
		is_flagged = false ;
		has_explosion = false ;
	}
	
	public void clear() {
		is_cleared = true ;
		if ( has_a_mine ) {
			has_explosion = true ;
		}
	}
	
	public void plantFlag() {
		if ( is_cleared ) return ;
		is_flagged = true ;
	}
	
	public void removeFlag() {
		if ( is_cleared ) return ;
		is_flagged = false ;
	}

}

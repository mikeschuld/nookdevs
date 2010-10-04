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

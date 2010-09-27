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
import java.util.ArrayList;
//import android.util.Log;



public class Clue implements Comparable<Clue> {
	String text;
	int dir;
	int num;  // Note: unlike row,col, this starts at 1, not zero
	int row;
	int col;
	ArrayList<Cell> cells ;
	int numcells = 0 ;
	
	Clue(String t, int d, int n, int r, int c) {
		text = t ;
		dir = d ;
		num = n ;
		row = r ;
		col = c ;
	}
	
	//  For debugging:
	@Override
	public String toString() {
		String s = "";
		if ( dir == CrossWord.ACROSS ) {
			s = s + "ACROSS " ;
		} else if ( dir == CrossWord.DOWN ) {
			s = s + "DOWN " ;
		} else { 
			s = s + "INVALID " ;
		}
		s = s + num + " (" + row + "," + col + "): " + "\"" + text + "\"" ;
		return(s);
	} // toString
	
	// Used to sort a Collection of Clues:
	public int compareTo(Clue that) {
		// ACROSS before DOWN:
		if ((this.dir == CrossWord.ACROSS) && (that.dir == CrossWord.DOWN))
			return (-1);
		if ((this.dir == CrossWord.DOWN) && (that.dir == CrossWord.ACROSS))
			return (1);
		// low numbers before high numbers:
		if (this.num < that.num)
			return (-1);
		return (1);
	} // compareTo
		
} // Clue class

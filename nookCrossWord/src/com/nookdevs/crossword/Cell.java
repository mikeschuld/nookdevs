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


import android.graphics.Color;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.util.TypedValue;
import android.util.Log;

//  This class keeps track of what's in a cell.
//  It also takes care of the eink view of this cell.
//  The touchscreen views are managed in TouchScreenClues, with some help from methods here
//  TODO: we're creating circled cells with is_a_circle=false, then changing
//        it to true with setCircle(true).

public class Cell {
	static CrossWord crossword_activity = null ; // our parent activity
    static Puzzle puzzle = null; // our parent puzzle
    // The Views for displaying the cell on the e-ink screen:
    private LinearLayout bg = null;  // For shaded cells, the View behind the cell where we set the shade
    private RelativeLayout cv = null; // The View for the cell, e.g. for changing the icon
    private TextView tv = null; // The TextView where the letter is drawn
    private TextView numtv = null; // The little TextView where the clue number goes
    public View view = null;  // The View we return to the main program, to draw this cell
    // The crossword stuff:
    String answertext = ""; // the answer (or "."/"~" for a blocked-out cell)
    String usertext = " "; // the answer entered by the user (or " " if unset)
    int row;
    int col;
    int number = 0;  // if non-zero, the clue number
    boolean is_a_circle = false;
    boolean is_blocked_out = false ;
    boolean is_shaded = false;
    int shade = Color.WHITE ;
    // The clues we are a part of (even cells with no number will normally belong to two clues):
    Clue acrossclue = null ;
    Clue downclue = null ;

    
    Cell(CrossWord xw, Puzzle parent, int myrow, int mycol, String answer, boolean isBlock, boolean circ) {    	
    	crossword_activity = xw ;
    	row = myrow;
        col = mycol;
        puzzle = parent;
        is_blocked_out = isBlock ;
        answertext = answer ;
        usertext = " ";
        is_a_circle = circ ;
    } // Cell constructor
    

    //  This needs to be called before the views have been created
    public void setCellShade( int color ) {
    	shade = color ;
    	if ( color != Color.WHITE ) {
    		is_shaded = true ;
    	}
    } // setCellShade

    
    /////////////////////////////////////////////////////////////////////////////////////////
    
    
    //  This is called once; it builds the eink views that we draw into,
    //  which the main program will attach to views shown on the screen
    //  This method only creates the views; the contents of the views (e.g.
    //  the letters in each cell) are drawn elsewhere (e.g. in reDraw())
    public void buildEInkViews() {
    	//Log.d(this.toString(), "DEBUG: Entering Cell.buildViews()...");
    	
    	LayoutInflater inflater = crossword_activity.getLayoutInflater();
    	
    	if ( is_shaded ) {
    		bg = (LinearLayout) inflater.inflate(R.layout.eink_cell_shaded, null);
    		cv = (RelativeLayout) bg.findViewById(R.id.cellview);
    		view = bg ;
    	} else { 
    		bg = null ;
    		cv = (RelativeLayout) inflater.inflate(R.layout.eink_cell, null);
    		view = cv ;
    	}
    	
    	view.setMinimumHeight(puzzle.sizedependent.cellHeight);
    	view.setMinimumWidth(puzzle.sizedependent.cellWidth);
    	
    	tv = (TextView) view.findViewById(R.id.celltext);
    	tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, puzzle.sizedependent.cell_textsize );
    	tv.setHeight( puzzle.sizedependent.cellHeight );
    	tv.setWidth( puzzle.sizedependent.cellWidth );
    	
    	if ( number != 0 ) buildNumberView();

    	drawEverything();
    	//Log.d(this.toString(), "DEBUG: Leaving Cell.buildViews().");
    } // buildEInkViews
    
    // This should only be called once, and only if this cell has a number:
    private void buildNumberView() {
        LayoutInflater inflater = crossword_activity.getLayoutInflater();
        numtv = (TextView) inflater.inflate(R.layout.eink_cell_number, null);
        numtv.setTextSize(TypedValue.COMPLEX_UNIT_PX, puzzle.sizedependent.cellnum_textsize );
        // Hack: If we're in the left-most column, move the number a little to the right:
        if ( col == 0 ) {
        	numtv.setPadding(2,0,0,0);
        }
        
        cv.addView(numtv);

    } // buildNumberView
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    
    private void drawCellNumber() {
    	/*
    	if ( number == 0 ) return ;
    	if ( numtv == null ) {
    		// the first time we're called
    		buildNumberView();
    	}
    	*/
    	if ((number != 0) && (numtv != null)) {
    		numtv.setText((number + ""));
    	}
    } // drawCellNumber
    
    private void drawCellText() {
        try {
			if (tv != null) {
				tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, puzzle.sizedependent.getEinkCellTextSize(usertext.length()) );
			    tv.setText(usertext);
			}
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception drawing cell text: " + ex );
		}
    } // drawCellText
    
    private void drawEverything() {
        //Log.d(this.toString(), "DEBUG: Entering Cell.drawEverything()...");
    	drawCellNumber();
    	drawCellText();
    	//drawTouchScreenText();
    	if ( is_shaded ) drawBackgroundShade();
        drawBackground();
        //Log.d(this.toString(), "DEBUG: Leaving Cell.drawEverything().");
    } // drawEverything

    //  This only needs to be called once
    private void drawBackgroundShade() {
		if (bg != null) {
			bg.setBackgroundColor( shade );
		}
    } // drawBackgroundShade

    //////////////////////////////////////////////////////////////////////////////////////////////////

    //  Called whenever they enter text into this cell:
    public void setUserText(String s) {
    	if ( (s == null) || s.length() == 0 ) {
    		//  This should never happen!
    		Log.e( this.toString(), "WARNING: setUserText() called incorrectly" ) ;
    		if ( ! is_blocked_out ) {
    			usertext = " " ;
    			drawCellText();
    			drawBackground();
    		}
    		return;
    	}
        usertext = s;
        drawCellText();
        if ( crossword_activity.mark_wrong_answers ) drawBackground();
    } // setUserText
    public void setUserText(char c) {
        setUserText( c + "" );
    } // setUserText    


    // Set the cell background image, depending on the type of cell, whether
    // or not it contains a cursor, etc
    // This function is called repeatedly during game play, whenever the
    // user's cursor moves onto or off of this cell
    public void drawBackground() {
        //Log.d(this.toString(), "DEBUG: Entering Cell.drawBackground()...");
    	boolean mark_answer_wrong = false ;
    	if ( crossword_activity.mark_wrong_answers ) {
    		mark_answer_wrong = hasWrongAnswer();
    	}
        try {
			if (is_blocked_out) {
				cv.setBackgroundColor(Color.BLACK);
			    return ;
			}
			if ((puzzle.getCursorRow() == row) && (puzzle.getCursorCol() == col)) {
				if ( is_a_circle ) {
					if ( puzzle.direction == CrossWord.ACROSS ) {
						if ( mark_answer_wrong ) {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_cursor_across_wrong );
						} else {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_cursor_across );
						}
					} else if ( puzzle.direction == CrossWord.DOWN ) {
						if ( mark_answer_wrong ) {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_cursor_down_wrong );
						} else {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_cursor_down );
						}
					} else {
						if ( mark_answer_wrong ) {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_cursor_wrong );
						} else {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_cursor );
						}
					}
			    } else {
					if ( puzzle.direction == CrossWord.ACROSS ) {
						if ( mark_answer_wrong ) {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_cursor_across_wrong );
						} else {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_cursor_across );
						}
					} else if ( puzzle.direction == CrossWord.DOWN ) {
						if ( mark_answer_wrong ) {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_cursor_down_wrong );
						} else {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_cursor_down );
						}
					} else {
						if ( mark_answer_wrong ) {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_cursor_wrong );
						} else {
							cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_cursor );
						}
					}
			    }
			} else {
				if ( is_a_circle ) {
					if ( mark_answer_wrong ) {
						cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_wrong );
					} else {
						cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle );
					}
			    } else {
			    	if ( mark_answer_wrong ) {
			    		cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_wrong );
			    	} else {
			    		cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal );
			    	}
			    }
			}
		} catch (Exception ex) {
			Log.e(this.toString(), "Error: excepting drawing cell background: " + ex );
		}
        //Log.d(this.toString(), "DEBUG: Leaving Cell.drawBackground().");
    } // drawBackground

    //  Similar to the above, but called by TouchScreenClues:
    public void drawBackgroundIcon_TouchScreen(View v) {
    	boolean mark_answer_wrong = false ;
        if ( crossword_activity.mark_wrong_answers ) {
            mark_answer_wrong = hasWrongAnswer();
        }
	    if ( is_a_circle ) {
	    	if ( mark_answer_wrong ) {
	    		v.setBackgroundResource( R.drawable.touchscreencell_circle_wrong_selector );
	    	} else {
	    		v.setBackgroundResource( R.drawable.touchscreencell_circle_selector );
	    	}
	    } else {
	    	if ( mark_answer_wrong ) {
	    		v.setBackgroundResource( R.drawable.touchscreencell_wrong_selector );
	    	} else {
	    		v.setBackgroundResource( R.drawable.touchscreencell_selector );
	    	}
	    }
    } // drawBackgroundIcon_TouchScreen

    /////////////////////////////////////////////////////////////////////////////////////////
    //
    // These are called by the Puzzle class:
    //
    
    public View getEinkView() {
    	if ( view == null ) {
    		// First time we're called:
    		buildEInkViews();
    	}
        return (view);
    } // getEinkView
    
    
    //  Called when the cursor moves off of us:
    public void lostCursor() {
    	drawBackground();
    } // lostCursor

    //  Called when the cursor moves onto us:
    public void gotCursor() {
    	drawBackground();
    } //


    // Whether or not the letter the user has entered in this cell is
    // correct.  Note that this is not quite the opposite of the
    // hasWrongAnswer() function; an unanswered cell is considered
    // neither wrong nor correct, just incomplete.
    public boolean hasCorrectAnswer() {
        if ( is_blocked_out ) {
        	return(true);
        }
        return (answertext.equals(usertext));
    } // hasCorrectAnswer
    
    // Whether or not the letter the user has entered in this cell is
    // wrong.  Note that this is not quite the opposite of the
    // hasCorrectAnswer() function; an unanswered cell is considered
    // neither wrong nor correct, just incomplete.
    // We use this function to flag or erase wrong answers.
    public boolean hasWrongAnswer() {
        if (usertext.equals("") || usertext.equals(" ")) {
            return (false); // it's not wrong, it just doesn't exist
        }
        if ( is_blocked_out ) {
        	return(false);
        }
        return (! answertext.equals(usertext));
    } // hasWrongAnswer

} // class Cell

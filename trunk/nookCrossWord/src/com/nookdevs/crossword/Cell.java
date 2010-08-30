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
import android.graphics.Color;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.util.TypedValue;
import android.util.Log;

//  TODO: we're creating circled cells with is_a_circle=false, then changing
//        it to true with setCircle(true).


public class Cell extends Activity {
	static CrossWord crossword_activity = null ; // our parent activity
    static Puzzle puzzle = null; // our parent puzzle
    /*
    // The Views for displaying the cell in the touchscreen Clues menus, both Across and Down:
    LinearLayout tscellbg_a = null ;  // to change the bg color
    LinearLayout tscellbg_d = null ;  // to change the bg color
    private Button tscellbutton_a = null ;   // to set the cell letter
    private Button tscellbutton_d = null ;   // to set the cell letter
    */
    // The Views for displaying the cell on the e-ink screen:
    private LinearLayout bg = null;  // The View behind the cell, for changing the bg shade
    private RelativeLayout cv = null; // The View for the cell, e.g. for changing the bg image
    private TextView tv = null; // The TextView where the letter is drawn
    private TextView numtv = null; // The little TextView where the clue number goes
    // The crossword stuff:
    String answertext = ""; // the answer (or "."/"~" for a blocked-out cell)
    String usertext = " "; // the answer entered by the user (or " " if unset)
    int row;
    int col;
    int number = 0;  // if non-zero, the clue number
    boolean is_a_circle ;
    int shade = Color.WHITE ;
    // The clues we are a part of (even cells with no number will belong to two clues):
    Clue acrossclue = null ;
    Clue downclue = null ;

    
    Cell(CrossWord xw, Puzzle parent, int myrow, int mycol, char c, boolean circ) {
    	crossword_activity = xw ;
    	row = myrow;
        col = mycol;
        puzzle = parent;
        answertext = (c + "");
        usertext = " ";
        if (answertext.equals(".") || answertext.equals("~")) {
        	// This is a filled-in blocked-out cell:
            usertext = answertext;
        }
        is_a_circle = circ ;
        buildViews();
    } // Cell constructor
    
    /////////////////////////////////////////////////////////////////////////////////////////
    
    
    //  This is called once; it builds the "views" that our table draws into,
    //  which the main program will attach to views shown on the screen
    //  This method only creates the views; the contents of the views (e.g.
    //  the letters in each cell) are drawn elsewhere (e.g. in reDraw())
    private void buildViews() {
            //Log.d(this.toString(), "DEBUG: Entering Cell.buildViews()...");
            LayoutInflater inflater = crossword_activity.getLayoutInflater();
            //  The e-ink Views for this cell:
            bg = (LinearLayout) inflater.inflate(R.layout.eink_cell, null);
            cv = (RelativeLayout) bg.findViewById(R.id.cellview);
            
            tv = (TextView) bg.findViewById(R.id.celltext);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, puzzle.sizedependent.cell_textsize );
            
            drawEverything();
            //Log.d(this.toString(), "DEBUG: Leaving Cell.buildViews().");
    } // buildViews
    
    // This should only be called once:
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
    
    void drawCellNumber() {
    	if ( number == 0 ) return ;
    	if ( numtv == null ) {
    		// the first time we're called
    		buildNumberView();
    	}
    	if ((number != 0) && (numtv != null)) {
    		numtv.setText((number + ""));
    	}
    } // drawCellNumber
    
    void drawCellText() {
        try {
			if (tv != null) {
			    tv.setText(usertext);
			}
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception drawing cell text: " + ex );
		}
    } // drawCellText
    
    /*
    void drawTouchScreenText() {
        if (tscellbutton_a != null) {
            tscellbutton_a.setText(usertext);    
        }
        if (tscellbutton_d != null) {        
        	tscellbutton_d.setText(usertext);
        }
    } // drawTouchScreenText
    */
    
    
    void drawEverything() {
        //Log.d(this.toString(), "DEBUG: Entering Cell.drawEverything()...");
    	drawCellNumber();
    	drawCellText();
    	//drawTouchScreenText();
        drawBackground();
        //Log.d(this.toString(), "DEBUG: Leaving Cell.drawEverything().");
    } // reDraw

    
    // TODO: maybe this should filter for allowed characters?
    void setUserText(String s) {
        usertext = s;
        drawCellText();
        //drawTouchScreenText();
    } // setUserText
    

    
    // Set the cell background image, depending on the type of cell, whether
    // or not it contains a cursor, etc
    // This function is called repeatedly during game play, whenever the
    // user's cursor moves onto or off of this cell
    void drawBackground() {
        //Log.d(this.toString(), "DEBUG: Entering Cell.drawBackground()...");
    	
        try {
			if (isBlockedOut()) {
				cv.setBackgroundColor(Color.BLACK);
			    bg.setBackgroundColor(Color.BLACK);
			    return ;
			}
			if ((puzzle.getCursorRow() == row) && (puzzle.getCursorCol() == col)) {
				if ( is_a_circle ) {
					if ( puzzle.direction == CrossWord.ACROSS ) {
						cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_cursor_across );
					} else if ( puzzle.direction == CrossWord.DOWN ) {
						cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_cursor_down );
					} else {
						cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle_cursor );
					}
			    } else {
					if ( puzzle.direction == CrossWord.ACROSS ) {
						cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_cursor_across );
					} else if ( puzzle.direction == CrossWord.DOWN ) {
						cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_cursor_down );
					} else {
						cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal_cursor );
					}
			    }
			} else {
				if ( is_a_circle ) {
					cv.setBackgroundResource( puzzle.sizedependent.cell_icon_circle );
			    } else {
			        cv.setBackgroundResource( puzzle.sizedependent.cell_icon_normal );
			    }
			}
			bg.setBackgroundColor( shade );
			
			//tscellbg_a.setBackgroundColor( shade );
			//tscellbg_d.setBackgroundColor( shade );
		} catch (Exception ex) {
			Log.e(this.toString(), "Error: excepting drawing cell background: " + ex );
		}
        //Log.d(this.toString(), "DEBUG: Leaving Cell.drawBackground().");
    } // drawBackground
    

    public void setCellShade( int color ) {
    	shade = color ;
    	drawBackground();
    } // setCellShade

    /////////////////////////////////////////////////////////////////////////////////////////

    
    
    // These are called by the Puzzle class:
    View getEinkView() {
            return (bg);
    } // getEinkView
    
    /*
    View getTouchScreenView(int dir) {
    	if ( dir == CrossWord.ACROSS ) {
            return (tscellbg_a);
    	} else {
    		return (tscellbg_d);
    	}
    } // getTouchScreenView
    */
    
    //  Called when the cursor moves off of us:
    void lostCursor() {
    	drawBackground();
    } // lostCursor

    //  Called when the cursor moves onto us:
    void gotCursor() {
    	drawBackground();
    } //

    boolean isBlockedOut() {
    	if (answertext.equals(".") || answertext.equals("~")) {
    		return(true);
    	}
    	return(false);  // normal cell
    } // isBlockedOut


    // Whether or not the letter the user has entered in this cell is
    // correct:
    boolean hasCorrectAnswer() {
        if (answertext.equals("")) {
                return (false); // we're uninitialized; shouldn't happen
        }
        if ( isBlockedOut() ) {
        	return(true);
        }
        return (answertext.equals(usertext));
    } // hasCorrectAnswer

} // class Cell

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

import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.util.Log;


public class MinesPlayActivity extends MinesActivity {
	private static final int CLICKMODE_NORMAL = 0 ;
	private static final int CLICKMODE_FLAG = 1 ;
	int clickmode = CLICKMODE_NORMAL ;
	private static final int ACTIVITY_SETTINGS = 1 ;
	
	
    MineField mineField = null ;
	Button newgameButton;
	Button settingsButton;
	Button flagModeButton;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play);
        
        // Touchscreen back button:
        Button back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    goBack();
                }
        });
        
		// The three buttons on the right of the touchscreen:
        
        // New game:
		newgameButton = (Button) findViewById(R.id.newgamebutton);
		newgameButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( mineField == null ) {
					newGame();
				} else if ( mineField.minefield_state != MineField.MINEFIELDSTATE_PLAYING ) {
					newGame();
				} else {
					AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder( v.getContext() );
					alertdialogbuilder
					.setTitle( R.string.new_game_areyousure_title )
					.setMessage( R.string.new_game_areyousure_msg )
					.setCancelable(false)
					.setPositiveButton( R.string.new_game_areyousure_yes , new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							newGame();
						}
					})
					.setNegativeButton( R.string.new_game_areyousure_no , new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					alertdialogbuilder.show();
				}
				
			}
		});
		// Flag:
		flagModeButton = (Button) findViewById(R.id.flagmodebutton);
		flagModeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleClickMode();
			}
		});
		// Settings:
		settingsButton = (Button) findViewById(R.id.settingsbutton);
		settingsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( mineField != null ) {
					if ( mineField.minefield_state == MineField.MINEFIELDSTATE_PLAYING ) {
						mineField.stopTimer();
					}
				}
		    	startActivityForResult( new Intent(MinesPlayActivity.this, SettingsActivity.class), ACTIVITY_SETTINGS );
			}
		});

        // Start a game:
        newGame();
        
    } // onCreate
    
    @Override
    public void onResume() {
        super.onResume();
        if ( mineField == null ) {
        	newGame();
        } else if ( mineField.minefield_state == MineField.MINEFIELDSTATE_PLAYING ) {
        	mineField.startTimer();
        }
    }  // onResume

    @Override
    public void onPause() {
        super.onPause();
        if ( mineField != null ) {
        	 if ( mineField.minefield_state == MineField.MINEFIELDSTATE_PLAYING ) {
        		 mineField.stopTimer();
        	 }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	//Bundle extras = intent.getExtras();
    	switch(requestCode) {
    		case ACTIVITY_SETTINGS:
    			if ( mineField == null ) {
    				newGame();
    			} else if ( mineField.minefield_state == MineField.MINEFIELDSTATE_PLAYING ) {
    				mineField.startTimer();
    			} else if (mineField.minefield_state != MineField.MINEFIELDSTATE_PLAYING ) {
    				// determine whether or not they changed the game size.  If so, and
    				// they're not currently playing, show them a new grid with the new size:
    				int[] rcn = getMineFieldSizePrefs();
    				int rows = rcn[0]; int cols = rcn[1]; int num_mines = rcn[2];
    				if ( (mineField.rows != rows) || (mineField.cols != cols) || (mineField.num_mines != num_mines) ) {
    					newGame();
    				}
    			}
    		    break;
    	}

    } // onActivityResult
    
    ////////////////////////////////////////////////////////////////////////
    
    //  returns {rows,cols,num_mines} based on users's stored preferences
    private int[] getMineFieldSizePrefs() {
    	int rows, cols, num_mines ;
    	if ( mSettings.contains(MINES_PREFERENCES_ROWS) &&
    	     mSettings.contains(MINES_PREFERENCES_COLS) &&
    	     mSettings.contains(MINES_PREFERENCES_NUM_MINES) ) {
    	    rows = mSettings.getInt(MINES_PREFERENCES_ROWS, DEFAULT_ROWS);
    	    cols = mSettings.getInt(MINES_PREFERENCES_COLS, DEFAULT_COLS);
    	    num_mines = mSettings.getInt(MINES_PREFERENCES_NUM_MINES,DEFAULT_NUM_MINES);
    	} else {
    	    rows=DEFAULT_ROWS ; cols=DEFAULT_COLS ; num_mines=DEFAULT_NUM_MINES ;
    	}
    	int r[] = { rows, cols, num_mines } ;
    	return( r );
    } // getMineFieldSizePrefs
    
    ////////////////////////////////////////////////////////////////////////


    //  Whether or not clicking on a cell clears it or plants a flag:
    private void clickModeNormal() {
		clickmode = CLICKMODE_NORMAL ;
		flagModeButton.setBackgroundResource( R.drawable.flagmode_off );
    } // clickModeNormal
    private void clickModeFlag() {
		clickmode = CLICKMODE_FLAG ;
		flagModeButton.setBackgroundResource( R.drawable.flagmode_on );
    } // clickModeFlag
    private void toggleClickMode() {
    	if ( clickmode == CLICKMODE_NORMAL ) {
    		clickModeFlag();
    	} else {
    		clickModeNormal();
    	}
    } // toggleClickMode

    
    //  Start a new game:
    public void newGame() {
    	
    	clickModeNormal();
    	
    	//  Clear the e-ink screen of artifacts:
    	try {
			if ( mineField != null ) mineField.mineFieldViews.removeAll();
			mineField = null ;
		} catch (Exception e) {
			e.printStackTrace();
		}
    	startActivity( new Intent(MinesPlayActivity.this, FlashEinkScreenActivity.class));

        int[] rcn = getMineFieldSizePrefs();
        int rows = rcn[0]; int cols = rcn[1]; int num_mines = rcn[2];
        
        mineField = new MineField( this, rows, cols, num_mines );
    } // newGame

    public void clickedCell(int r,int c) {
    	// flag mode:
    	if ( clickmode == CLICKMODE_FLAG ) {
    		mineField.toggleCellFlag(r,c);
    		// back to normal mode:
    		toggleClickMode();
    		return ;
    	}
    	// normal mode:
    	if ( mineField.cellIsFlagged(r,c) ) {
    		// ignore clicks on flagged cell
    		return ;
    	}
    	if ( mineField.cellIsCleared(r,c) ) {
    		// ignore clicks on already-cleared cell
    		return ;
    	}

        mineField.clearCell(r,c);
        if ( mineField.minefield_state == MineField.MINEFIELDSTATE_LOST ) {
        	gameOver();
        }
    } // clickedCell

    public void longClickedCell(int r,int c) {
    	mineField.toggleCellFlag(r,c);
    }

    private void gameOver() {
    } // gameOver
    
}

/*
 * nookCrossWord
 * 
 * Copyright 2010 nookDevs
 * 
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
 * Written by Kevin Vajk and Hariharan Swaminathan
 */
package com.nookdevs.crossword;
import android.app.Activity;
import android.os.Bundle;
import android.database.Cursor;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.os.PowerManager;
import android.net.Uri;
import android.widget.Button;
import android.widget.LinearLayout;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.widget.ViewAnimator;
import android.widget.ScrollView;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.content.SharedPreferences.Editor;
import java.util.ArrayList;
import java.util.Collections;
import java.io.*;


// TODO: support rebus entries
// TODO: this code is vulnerable to null pointer exceptions
// TODO: when adding a Clue, make sure it's not already there
// TODO: handle multiple puzzles in an XPF puzzle file
// TODO: when building/loading a puzzle, I'm using exponential-time algorithms all over the place
// TODO: get rid of methods like addClue()
// TODO: consolidate the build/populate functions; it's very confusing
// TODO: support entering symbols, too, and maybe numbers
// TODO: What to put in "About this puzzle" when the puzzle has no metadata?
// TODO: Don't save user answers if they're all blank
// TODO: load time is *way* too slow
// TODO: hide the keyboard when exiting
// TODO: should I be using onKeyDown, or onKeyUp?
// TODO: thread problem
// TODO: <merge>?
// TODO: hard-coded sizes in xml files (e.g. e-ink screen size) should maybe be
//       set in SizeDependent.java.
// TODO: can we handle accented characters in clues?
// TODO: JNotes


public class CrossWord extends Activity {

	protected static String TITLE = "Crossword";

	ViewAnimator einkanimator; // The container for all Views on the e-ink screen
	LinearLayout eink_xword_page;  // The main e-ink screen, contains the crossword grid, etc
	ScrollView eink_cluespagescroller; // The Clues View on the e-ink screen (NOOK_PAGE_DOWN)
	ViewAnimator touchscreenanimator; // The container for menus and submenus on the touchscreen
	ScrollView touchscreen_submenu_play ; // The sub-menu for picking which direction clues to view on the touchscreen	
	LinearLayout touchscreen_clues_container; // the touchscreen clues (populated dynamically)
	LinearLayout puzzlelist;              // TODO: move this to another activity, on the e-ink screen
	
	static final int STAYPUT = 0;
	static final int ACROSS = 1;
	static final int DOWN = 2;

	// Yuck:
	static final int MAIN_MENU_VIEWNUM = 0 ;
	static final int PLAY_SUBMENU_VIEWNUM   = 1 ;
	static final int CLUESSCROLLERS_SUBMENU_VIEWNUM  = 2 ;
	static final int HINTS_SUBMENU_VIEWNUM  = 3 ;
	static final int PUZZLES_SUBMENU_VIEWNUM  = 4 ;
	static final int PUZZLE_LIST_SUBMENU_VIEWNUM = 5 ;

	static final String CROSSWORD_PREFERENCES = "nookCrossWordPreferences" ;
	static final String CROSSWORD_PREFERENCES_CURRENT_PUZZLE = "CURRENTPUZZLE" ;

	InputMethodManager keyboardim; // the pop-up keyboard

	PowerManager.WakeLock screenLock = null;
	long m_ScreenSaverDelay = 600000; // default value; will be replaced with
										// Nook setting
	SharedPreferences mSettings ;


	Puzzle activePuzzle = null ;

	public static final String EXTERNAL_SD_FOLDER="/sdcard/my crosswords";
	public static final String INTERNAL_SD_FOLDER="/system/media/sdcard/my crosswords";
	private String m_folder;

	///////////////////////////////////////////////////////////////////////////////

	
	// onCreate:
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Button b;

		// android power management screen stuff:
		initializeScreenLock();

		//  The soft keyboard:
		keyboardim = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		
		//  To save/restore state and settings:
		mSettings = getSharedPreferences( CROSSWORD_PREFERENCES, Context.MODE_PRIVATE ) ;
		
		//  Our Views:
		einkanimator = (ViewAnimator) findViewById(R.id.eink_animator);
		// einkanimator switches between these views:
		eink_xword_page = (LinearLayout) findViewById(R.id.eink_xword_page);
		eink_cluespagescroller = (ScrollView) findViewById(R.id.eink_cluespagescroller);
		eink_cluespagescroller.setSmoothScrollingEnabled(false);  // smooth scrolling is awful on e-ink
		//
		touchscreenanimator = (ViewAnimator) findViewById(R.id.touchscreen_animator);
		touchscreenanimator.setInAnimation(this, R.anim.fromright);
		touchscreen_submenu_play = (ScrollView) findViewById(R.id.touchscreen_submenu_play);
		touchscreen_clues_container = (LinearLayout) findViewById(R.id.touchscreen_clues_container);
		puzzlelist = (LinearLayout) findViewById(R.id.puzzlelist);
		
		
		// Touchscreen back button:
		b = (Button) findViewById(R.id.back);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromleft);
			  	if ( touchscreenanimator.getDisplayedChild() == MAIN_MENU_VIEWNUM ) {
				    goBack();
			  	} else if ( touchscreenanimator.getDisplayedChild() == CLUESSCROLLERS_SUBMENU_VIEWNUM ) {
			  		touchscreenanimator.setDisplayedChild( PLAY_SUBMENU_VIEWNUM );
			  	} else if ( touchscreenanimator.getDisplayedChild() == HINTS_SUBMENU_VIEWNUM ) {
			  		touchscreenanimator.setDisplayedChild( PLAY_SUBMENU_VIEWNUM );
			  	} else if ( touchscreenanimator.getDisplayedChild() == PUZZLE_LIST_SUBMENU_VIEWNUM ) {
			  		touchscreenanimator.setDisplayedChild( PUZZLES_SUBMENU_VIEWNUM );
			  	} else {
			  		touchscreenanimator.setDisplayedChild( MAIN_MENU_VIEWNUM );
			  	}
			  	touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromright);
			}
		});

		// Play button:
		b = (Button) findViewById(R.id.play_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (activePuzzle == null) {
					showToast_No_Active_Puzzle();
					return;
				}
				touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromright);
		  		touchscreenanimator.setDisplayedChild(PLAY_SUBMENU_VIEWNUM);
			}
		});
		
		
		// Across clues button:
		b = (Button) findViewById(R.id.go_to_touchscreen_across_clues);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (activePuzzle == null) {
					showToast_No_Active_Puzzle();
					return;
				}
				activePuzzle.prepareTouchScreenClues( ACROSS );
				touchscreenanimator.setInAnimation(v.getContext(), R.anim.noanim);
				touchscreenanimator.setDisplayedChild(CLUESSCROLLERS_SUBMENU_VIEWNUM);
			}
		});
		// Down clues button:
		b = (Button) findViewById(R.id.go_to_touchscreen_down_clues);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (activePuzzle == null) {
					showToast_No_Active_Puzzle();
					return;
				}
				activePuzzle.prepareTouchScreenClues( DOWN );
				touchscreenanimator.setInAnimation(v.getContext(), R.anim.noanim);
				touchscreenanimator.setDisplayedChild(CLUESSCROLLERS_SUBMENU_VIEWNUM);
			}
		});
		// Button to launch the keyboard:
		b = (Button) findViewById(R.id.launchkeyboardbutton);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				bringUpKeyboard();
			}
		});
		// Check answer button:
		b = (Button) findViewById(R.id.check_solution_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}
				if ( activePuzzle.isSolved() ) {
					showLongToast( getString(R.string.you_solved_it) );
				} else {
					showShortToast( getString(R.string.not_solved_yet) );
				}
			}
		});


		// About this puzzle button:
		b = (Button) findViewById(R.id.aboutthispuzzle_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (activePuzzle == null) {
					showToast_No_Active_Puzzle();
					return;
				}
				LayoutInflater inflater = getLayoutInflater();
				TextView dialogtextview = (TextView) inflater.inflate( R.layout.dialogtext, null );
				dialogtextview.setText( activePuzzle.aboutThisPuzzle() );
				AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder( v.getContext() );
		        alertdialogbuilder.setCustomTitle( dialogtextview )
		        .setCancelable(false)
		        .setNegativeButton( getString(R.string.ok), new DialogInterface.OnClickListener() {
		                        public void onClick(DialogInterface dialog, int id) {
		                        	dialog.cancel();
		                        }

		        });
                alertdialogbuilder.show();
			}
		});

		// Hints button:
		b = (Button) findViewById(R.id.hints_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (activePuzzle == null) {
					showToast_No_Active_Puzzle();
					return;
				}
				touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromright);
				touchscreenanimator.setDisplayedChild( HINTS_SUBMENU_VIEWNUM );
			}
		});
		
		// The current letter hint button:
		b = (Button) findViewById(R.id.show_current_letter_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}
				//  Briefly flash the answer for the selected cell:
				showSingleLetterToast(
		                 activePuzzle.getCell( activePuzzle.getCursorRow(),
				                   activePuzzle.getCursorCol()  ).answertext );
			}
		});
		// A hint/cheat which erases all the user's incorrect answers:
		b = (Button) findViewById(R.id.erase_wrong_answers_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}				
				int r = activePuzzle.eraseWrongAnswers();
			    if ( r == 0 ) {
			        showShortToast("No wrong answers");
			    } else {
			        String s = "Erased " + r + " wrong answer" ;
			        if ( r > 1 ) s = s + "s" ;
			        showShortToast(s);
			    }
			}
		});
		// The hint/cheat button to just solve the entire puzzle:
		b = (Button) findViewById(R.id.i_give_up_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}
				LayoutInflater inflater = getLayoutInflater();
				TextView dialogtextview = (TextView) inflater.inflate( R.layout.dialogtext, null );
				dialogtextview.setText( R.string.giveup_areyousure);
				AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder( v.getContext() );
				alertdialogbuilder
					.setCancelable(false)
					.setCustomTitle( dialogtextview )
					.setPositiveButton( R.string.giveup_yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							activePuzzle.justSolveTheWholeThing();
							showLongToast( getString(R.string.giveup_wedidit) );
						}
				})
				.setNegativeButton( R.string.giveup_no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				alertdialogbuilder.show();
			}
		});
		

		// The Puzzles ("File") button:
		b = (Button) findViewById(R.id.file_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromright);
				touchscreenanimator.setDisplayedChild( PUZZLES_SUBMENU_VIEWNUM );
			}
		});
		
		// Open puzzle button:
		b = (Button) findViewById(R.id.open_puzzle_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				my_crosswords_file_menu() ;
			}
		});
		
		// Resume current puzzle button:
		b = (Button) findViewById(R.id.resume_current_puzzle_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					resumeCurrentPuzzle() ;
					if ( activePuzzle != null ) {
						touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromleft);
						touchscreenanimator.setDisplayedChild( MAIN_MENU_VIEWNUM  );
						touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromright);
					}
				} catch (Exception ex) {
					Log.e( this.toString(), "Error: unexpected exception: " + ex );
				}
			}
		});

		// Clear puzzle button:
		b = (Button) findViewById(R.id.clear_puzzle_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}
				LayoutInflater inflater = getLayoutInflater();
				TextView dialogtextview = (TextView) inflater.inflate( R.layout.dialogtext, null );
				dialogtextview.setText( R.string.clear_puzzle_areyousure );
						
				AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder( v.getContext() );
				alertdialogbuilder.setCustomTitle( dialogtextview )
				  .setCancelable(false)
				  .setPositiveButton( R.string.clear_puzzle_yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							activePuzzle.clearAllAnswers();
						}
				})
				  .setNegativeButton( R.string.clear_puzzle_no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				alertdialogbuilder.show();
			}
		});
		
		// Puzzles Help button:
		b = (Button) findViewById(R.id.puzzles_help_button);
		b.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				LayoutInflater inflater = getLayoutInflater();
				TextView dialogtextview = (TextView) inflater.inflate( R.layout.dialogtext, null );
				dialogtextview.setText( R.string.puzzles_help_dialog_text );
				AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder( v.getContext() );
				alertdialogbuilder.setCustomTitle( dialogtextview )
				.setCancelable(false)
				.setNegativeButton( getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				alertdialogbuilder.show();
			}
		});
		
		
		updateTitle(TITLE);
		
        File f = new File( EXTERNAL_SD_FOLDER);
	    if( !f.exists()) {
	        f = new File(INTERNAL_SD_FOLDER);
	        if( !f.exists()) {
	            f.mkdir();
	        }
	        m_folder = INTERNAL_SD_FOLDER;
	    } else {
	        m_folder = EXTERNAL_SD_FOLDER;
	    }
	} // onCreate

	/////////////////////////////////////////////////


	// onResume:
	@Override
	public void onResume() {
		super.onResume();
		acquireScreenLock();
		
		try {
			updateTitle(TITLE);
			if ( activePuzzle != null ) {
				String title = activePuzzle.getPuzzleTitle();
				if ( title != null ) {
					if ( ! title.equals("") ) {
						updateTitle(title);
					}
				}
			}
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: exception on onResume() trying to set title: " + ex );
		}
		
	} // onResume
	
	@Override
	public void onPause() {
		super.onPause();
		
		try {
			if ( activePuzzle != null ) {
				PuzzleIO puzzleio = new PuzzleIO(this);
				puzzleio.savePuzzleWIP( activePuzzle.getUserGridString(), activePuzzle.getFileName() );
			}
		} catch (Exception ex) {
			Log.e(this.toString(), "Error: exception saving user work: " + ex );
		}
				
		releaseScreenLock();
	} // onPause

	@Override
	public void onUserInteraction() {
		super.onUserInteraction();
		acquireScreenLock();
	} // onUserInteraction
	
	
	///////////////////////////////////////////////////////////////////////////////

	void destroyActivePuzzle() {
		einkanimator.setDisplayedChild( 0 );
		activePuzzle = null ;
		eink_xword_page.removeAllViews();
		eink_cluespagescroller.removeAllViews();
		touchscreen_clues_container.removeAllViews();
		System.gc();
		updateTitle(TITLE); // Remove the title of the previous puzzle
	} // destroyActivePuzzle
	
	void attachPuzzle(Puzzle puzzle) {
		//Log.d(this.toString(), "DEBUG: Entering attachPuzzle()...");
		destroyActivePuzzle();
		activePuzzle = puzzle;
		eink_xword_page.addView( puzzle.getMainEinkView() );
		eink_cluespagescroller.addView( puzzle.getEinkCluesView() );
		touchscreen_clues_container.addView( puzzle.getTouchScreenCluesView() );
		//  Puzzle title at the top of the screen:
		String title = activePuzzle.getPuzzleTitle();
		if ( title != null ) {
			if ( ! title.equals("") ) {
				updateTitle(title);
			}
		}
		//Log.d(this.toString(), "DEBUG: Leaving attachPuzzle().");
	} // attachPuzzle
	
	///////////////////////////////////////////////////////////////////////////////

	
	@Override 
	protected void onActivityResult(int requestCode,int resultCode,Intent data) 
	{
	    if( resultCode == RESULT_OK && data != null) {
	        Bundle b = data.getExtras();
	        if( b != null) {
	            String file = b.getString("FILE");
	            if( file != null) {
	                loadCrossWord(file);
	                if ( activePuzzle != null ) {
	                	touchscreenanimator.setInAnimation(this, R.anim.fromleft);
	                	touchscreenanimator.setDisplayedChild( MAIN_MENU_VIEWNUM );
	                	touchscreenanimator.setInAnimation(this, R.anim.fromright);
	                }
	            }
	        }
	    }
	}

	boolean launchFileSelector() {
	    try {
	        Intent intent = new Intent(Intent.ACTION_MAIN);
    	    intent.addCategory(Intent.ACTION_DEFAULT);
    	    intent.setComponent(new ComponentName("com.nookdevs.fileselector", "com.nookdevs.fileselector.FileSelector"));
    	    intent.putExtra("ROOT",m_folder);
    	    String[] filters = {".*\\.puz", ".*\\.xml", ".*\\.xpf"};
    	    intent.putExtra("FILTER", filters);
    	    intent.putExtra("TITLE", getString(R.string.my_puzzles));
    	    startActivityForResult(intent, 0);
    	    return true;
	    } catch(Exception ex) {
	        return false;
	    }
	    
	}
		
	void my_crosswords_file_menu() {
	    if( launchFileSelector()) {
	        return;
	    }
	    
		//  This code below is not normally used.  It's a fall-back if the
		//  com.nookdevs.fileselector intent can't be found.
		//  When they click on the button to open a puzzle, we dynamically
		//  populate the menu (by listing the files in "my crosswords"):
	    showLongToast("FYI: This will work better if you install the nookdevs FileSelector package.");

		Button b ;
		LayoutInflater inflater = getLayoutInflater();
		ArrayList<String> filelist = new ArrayList<String>();
		
		puzzlelist.removeAllViews() ;
		
		File xworddir = new File(m_folder);
        for ( String fname : xworddir.list() ) {
            filelist.add( fname );
    
        }
        Collections.sort(filelist);

		for ( String fname : filelist ) {
			File f = new File(xworddir, fname);
			if ( f.isFile() && ( (f.getName().toLowerCase()).endsWith(".puz") ||
							(f.getName().toLowerCase()).endsWith(".xpf") ||
							(f.getName().toLowerCase()).endsWith(".xml") )
						) {
				b = (Button) inflater.inflate( R.layout.filename_button, null );
				b.setText( f.getName() );
				b.setTag( f.getAbsolutePath() );
				b.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						loadCrossWord( (String) v.getTag() );
						touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromleft);
						touchscreenanimator.setDisplayedChild( MAIN_MENU_VIEWNUM  );
						touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromright);
					}
				});
				puzzlelist.addView( b );
			}
		}
		touchscreenanimator.setInAnimation(this, R.anim.fromright);
		touchscreenanimator.setDisplayedChild( PUZZLE_LIST_SUBMENU_VIEWNUM );
	} // my_crosswords_file_menu
	
	
	private void loadCrossWord( String puzzlefilename ) {
		PuzzleIO puzzleio = new PuzzleIO(this);
		Puzzle puzzle = null;
		String usergridstring = null ;
		
		//  If another puzzle was already loaded, save our work before loading a new puzzle:
		if ( activePuzzle != null ) {
			puzzleio.savePuzzleWIP( activePuzzle.getUserGridString(), activePuzzle.getFileName() );
		}
		//  Wipe out the current puzzle to free up some memory:
		destroyActivePuzzle();
		
		//  Load the puzzle:
		//showShortToast("Loading puzzle...");
	    puzzle = puzzleio.loadPuzzle( puzzlefilename );
		if (puzzle == null) {
			showLongToast( getString(R.string.error_could_not_open_puzzle) );
			return;
		}
		
		//  Load any work they user has already done:
		usergridstring = puzzleio.loadPuzzleWIP( puzzlefilename ) ;
		if ( usergridstring != null ) {
			puzzle.setUserGridString( usergridstring );
		}
		
		//  Attach it to our views:
		attachPuzzle(puzzle);
		
		//  Pop up some info about this puzzle:
		showLongToast( activePuzzle.aboutThisPuzzle() );

		//  Warn about unsupported features:
		if ( puzzle.hasRebusEntries() ) {
			showLongToast( getString(R.string.warning_rebus_not_supported) );
		}
		
		//  Set this as the current puzzle:
		setAsCurrentPuzzle( puzzlefilename );

	} // loadCrossWord

	void resumeCurrentPuzzle() {
		String fname ;
		try {
			if (! mSettings.contains(CROSSWORD_PREFERENCES_CURRENT_PUZZLE  ) ) {
				showLongToast( "Cannot determine latest puzzle.  Open a new puzzle." );
				return ;
			}	
			fname = mSettings.getString( CROSSWORD_PREFERENCES_CURRENT_PUZZLE, "" );
			if ( (fname == null) || (fname.equals("")) ) {
				showLongToast( "Cannot determine latest puzzle.  Open a new puzzle." );
				return ;
			}
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception loading preferences: " + ex );
			return ;
		}
		try {
			loadCrossWord(fname);
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception loading puzzle: " + ex );
		}
	} // resumeCurrentPuzzle
	
	void setAsCurrentPuzzle( String fname ) {
		try {
			Editor editor = mSettings.edit();
			editor.putString( CROSSWORD_PREFERENCES_CURRENT_PUZZLE, fname );
			editor.commit();
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception saving preferences: " + ex );
		}
	} // setAsCurrentPuzzle
	

	// /////////////////////////////////////////////////////////////////////////////

	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		// We don't handle any keys when no puzzle is loaded.
		if (activePuzzle == null)
			return (false);

		try {
			switch (keyCode) {

			case NOOK_PAGE_UP_KEY_LEFT:
			case NOOK_PAGE_UP_KEY_RIGHT:
			case NOOK_PAGE_UP_SWIPE:
				try {
					int y = ((ScrollView) einkanimator.getCurrentView())
							.getScrollY();
					if (y == 0) {
						einkanimator.showPrevious();
					} else {
						((ScrollView) einkanimator.getCurrentView())
								.pageScroll(View.FOCUS_UP);
					}
				} catch (Exception ex) {
					//einkanimator.showPrevious();
				}
				return (true);
			case NOOK_PAGE_DOWN_KEY_LEFT:
			case NOOK_PAGE_DOWN_KEY_RIGHT:
			case NOOK_PAGE_DOWN_SWIPE:
				try {
					// If the current view is the main puzzle page, which is not
					// a ScrollView, then this will throw an exception:
				    ((ScrollView) einkanimator.getCurrentView())
								.pageScroll(View.FOCUS_DOWN);
				} catch (Exception ex) {
					einkanimator.showNext();
				}
				return (true);
			case SOFT_KEYBOARD_CLEAR:
				activePuzzle.setUserText(" "); // clear the cell
				return (true);
			case KeyEvent.KEYCODE_DEL:
				activePuzzle.decrementCursor();
				activePuzzle.setUserText(" "); // clear the cell
				return (true);
			case KeyEvent.KEYCODE_SPACE:
				// Spacebar switches typing direction. Non-intuitive, I know; maybe
				// we should have a custom keyboard
				toggleDirection_with_Toast();
				return (true);
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				activePuzzle.moveCursor(ACROSS, +1);
				activePuzzle.scroll_TouchscreenClues_to_Cursor();
				return (true);
			case KeyEvent.KEYCODE_DPAD_LEFT:
				activePuzzle.moveCursor(ACROSS, -1);
				activePuzzle.scroll_TouchscreenClues_to_Cursor();
				return (true);
			case KeyEvent.KEYCODE_DPAD_UP:
				activePuzzle.moveCursor(DOWN, -1);
				activePuzzle.scroll_TouchscreenClues_to_Cursor();
				return (true);
			case KeyEvent.KEYCODE_DPAD_DOWN:
				activePuzzle.moveCursor(DOWN, +1);
				activePuzzle.scroll_TouchscreenClues_to_Cursor();
				return (true);
			default:
				char c;
				// TODO:
				c = event.getMatch("ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray());
				// c = event.getDisplayLabel();
				if (c != '\0') {
					activePuzzle.setUserText((c + ""));
					activePuzzle.incrementCursor();
					return (true);
				}
				/*
				 * //String s = (event.getDisplayLabel() + "").toUpperCase();
				 * KeyData keydata = new KeyData(); if ( event.getKeyData( keydata)
				 * ) { activePuzzle.setUserText( 1, 1, s ); }
				 */
				break;
			}
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: unexpected exception in onKeyDown(): " + ex );
		}
		
		return (false);
	} // onKeyDown
	
	///////////////////////////////////////////////////////////////////////////////


	void bringUpKeyboard() {
		
		try {
			keyboardim.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: Exception trying to bring up keyboard: " + ex  );
			return ;
		}
		
		// if we were looking at clues on the eink page, go back to the puzzle
		// so they can see what they're typing:
		try {
			einkanimator.setDisplayedChild(0);
		} catch ( Exception ex ) { }
	} // bringUpKeyboard
	
	///////////////////////////////////////////////////////////////////////////////

	
	void setDirection_with_Toast(int dir) {
		String msg = "";
		if ( activePuzzle == null) return ;
		if (activePuzzle.getDirection() == dir)
			return;
		
		try {
			activePuzzle.setDirection(dir);
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception while trying to set puzzle direction: " + ex );
			return ;
		}
		
		if (dir == ACROSS) {
			msg = "ACROSS" ;
		} else if (dir == DOWN) {
			msg = "DOWN" ;
		} else {
			msg = "Direction: NONE" ;
		}
		try {
			showShortToast( msg );
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception while trying to pop up a toast: " + ex );
			return ;
		}
				
	} // setDirection_With_Toast

	void toggleDirection_with_Toast() {
		if ( activePuzzle == null) return ;
		if (activePuzzle.getDirection() == ACROSS) {
			setDirection_with_Toast(DOWN);
			return ;
		} else if (activePuzzle.getDirection() == DOWN) {
			setDirection_with_Toast(ACROSS);
			return ;
		}
	} // toggleDirection


	///////////////////////////////////////////////////////////////////////////////

	void showSingleLetterToast(String s) {
		try {
			LayoutInflater inflater = getLayoutInflater();
			View layout = inflater.inflate(R.layout.toast_layout_oneletter,
			                               (ViewGroup) findViewById(R.id.toast_layout_root));
			TextView toast_text = (TextView)layout ;
			toast_text.setText(s);
			Toast toast = new Toast(getApplicationContext());
			toast.setView(layout);
			toast.setDuration(Toast.LENGTH_LONG);		
			toast.setGravity(Gravity.CENTER, 0, 0); // grav, x-off, y-off
			toast.show();
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: exception in showSingleLetterToast(): " + ex );
		}
	} // showToast

	
	void showToast(String s, int duration) {
		try {
			LayoutInflater inflater = getLayoutInflater();
			View layout = inflater.inflate(R.layout.toast_layout,
			                               (ViewGroup) findViewById(R.id.toast_layout_root));
			TextView toast_text = (TextView)layout ;
			toast_text.setText(s);
			Toast toast = new Toast(getApplicationContext());
			toast.setView(layout);
			toast.setDuration(duration);		
			toast.setGravity(Gravity.CENTER, 0, 0); // grav, x-off, y-off
			toast.show();
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: exception in showToast(): " + ex );
		}
	} // showToast

	void showShortToast(String s) {
		showToast(s, Toast.LENGTH_SHORT);
	} // showShortToast

	void showLongToast(String s) {
		showToast(s, Toast.LENGTH_LONG);
	} // showLongToast

	void showToast_No_Active_Puzzle() {
		showLongToast( getString(R.string.error_noactivepuzzle) );
	}
	
		
	// /////////////////////////////////////////////////////////////////////////////

	
	public static final int SOFT_KEYBOARD_CLEAR = -13;
	public static final int SOFT_KEYBOARD_SUBMIT = -8;
	public static final int SOFT_KEYBOARD_CANCEL = -3;
	public static final int SOFT_KEYBOARD_DOWN_KEY = 20;
	public static final int SOFT_KEYBOARD_UP_KEY = 19;
	protected static final int NOOK_PAGE_UP_KEY_RIGHT = 98;
	protected static final int NOOK_PAGE_DOWN_KEY_RIGHT = 97;
	protected static final int NOOK_PAGE_UP_KEY_LEFT = 96;
	protected static final int NOOK_PAGE_DOWN_KEY_LEFT = 95;
	protected static final int NOOK_PAGE_DOWN_SWIPE = 100;
	protected static final int NOOK_PAGE_UP_SWIPE = 101;

	// Android was designed for phones with back-lit screens; it doesn't know
	// that the Nook's e-ink display doesn't use power when displaying a static
	// image.
	// So, we want to prevent android from blanking the e-ink display on us.
	// Called from onCreate:
	// Adapted from nookDevs code:
	private void initializeScreenLock() {
		PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
		screenLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"nookactivity" + hashCode());
		screenLock.setReferenceCounted(false);
		String[] values = { "value" };
		String[] fields = { "bnScreensaverDelay" };
		Cursor c = getContentResolver().query(
				Uri.parse("content://settings/system"), values, "name=?",
				fields, "name");
		if (c != null) {
			c.moveToFirst();
			long lvalue = c.getLong(0);
			if (lvalue > 0) {
				m_ScreenSaverDelay = lvalue;
			}
		}
	} // initializeScreenLock

	// Called from onPause:
	private void releaseScreenLock() {
		try {
			if (screenLock != null) {
				screenLock.release();
			}
		} catch (Exception ex) {
			Log.e(this.toString(), "exception releasing screenLock - ", ex);
			finish();
		}
	} // releaseScreenLock

	// Called from onResume and onUserInteraction:
	private void acquireScreenLock() {
		if (screenLock != null) {
			screenLock.acquire(m_ScreenSaverDelay);
		}
	} // acquireScreenLock

	// Update the title bar:
	// Taken from nookDevs common:
	public final static String UPDATE_TITLE = "com.bravo.intent.UPDATE_TITLE";

	protected void updateTitle(String title) {
		try {
			Intent intent = new Intent(UPDATE_TITLE);
			String key = "apptitle";
			intent.putExtra(key, title);
			sendBroadcast(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // updateTitle

	// Taken from nookDevs common:
	protected void goHome() {
		String action = "android.intent.action.MAIN";
		String category = "android.intent.category.HOME";
		Intent intent = new Intent();
		intent.setAction(action);
		intent.addCategory(category);
		startActivity(intent);
	} // goHome

	// Taken from nookDevs common:
	protected void goBack() {
		try {
			Intent intent = new Intent();
			if (getCallingActivity() != null) {
				intent.setComponent(getCallingActivity());
				startActivity(intent);
			} else {
				goHome();
			}
		} catch (Exception ex) {
			goHome();
		}
	} // goBack


	
} // CrossWord class
	
	

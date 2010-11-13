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

import android.os.Bundle;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
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
// TODO: consolidate the build/populate functions; it's very confusing
// TODO: load time is *way* too slow
// TODO: hide the keyboard when exiting
// TODO: JNotes?


public class CrossWord extends BaseActivity {

	protected static String TITLE = "Crossword";

	ViewAnimator einkanimator; // The container for all Views on the e-ink screen
	LinearLayout eink_xword_page;  // The main e-ink screen, contains the crossword grid, etc
	ScrollView eink_cluespagescroller; // The Clues View on the e-ink screen (NOOK_PAGE_DOWN)
	ViewAnimator touchscreenanimator; // The container for menus and submenus on the touchscreen
	LinearLayout touchscreen_clues_container; // the touchscreen clues (populated dynamically)
	ScrollView play_submenu_scroller;
	LinearLayout puzzlelist;              // TODO: move this to another activity, on the e-ink screen
	LinearLayout puzzles_submenu;
	//  The buttons that (conditionally) appear in the Puzzles sub-menu:
	Button open_puzzle_button;
	Button resume_current_puzzle_button;
	Button clear_puzzle_button; 
	Button delete_puzzle_button;
	Button puzzles_help_button;
	//  Settings (loaded from saved settings at startup):
	public boolean mark_wrong_answers ;
	public boolean freeze_right_answers ;
	public boolean cursor_wraps ;
	public boolean cursor_next_clue ;

	static final int STAYPUT = 0;
	static final int ACROSS = 1;
	static final int DOWN = 2;

	static final int EINK_PUZZLE_VIEWNUM = 0 ;
	static final int EINK_CLUES_VIEWNUM = 1 ;
	
	static final int MAIN_MENU_VIEWNUM = 0 ;
	static final int PLAY_SUBMENU_VIEWNUM = 1 ;
	static final int CLUESSCROLLERS_SUBMENU_VIEWNUM = 2 ;
	static final int HINTS_SUBMENU_VIEWNUM = 3 ;
	static final int PUZZLES_SUBMENU_VIEWNUM = 4 ;
	static final int PUZZLE_LIST_SUBMENU_VIEWNUM = 5 ;
	
	static final int ACTIVITY_OPEN_PUZZLE = 0 ;
	static final int ACTIVITY_SETTINGS = 1 ;
	static final int ACTIVITY_INSERT_REBUS = 2 ;

	Puzzle activePuzzle = null ;


	///////////////////////////////////////////////////////////////////////////////

	
	// onCreate:
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//  Load the user's preferences:
		loadGameSettings();

		//  Our Views:
		einkanimator = (ViewAnimator) findViewById(R.id.eink_animator);
		// einkanimator switches between these views:
		eink_xword_page = (LinearLayout) findViewById(R.id.eink_xword_page);
		eink_cluespagescroller = (ScrollView) findViewById(R.id.eink_cluespagescroller);
		eink_cluespagescroller.setSmoothScrollingEnabled(false);  // smooth scrolling is awful on e-ink
		//
		touchscreenanimator = (ViewAnimator) findViewById(R.id.touchscreen_animator);
		touchscreenanimator.setInAnimation(this, R.anim.fromright);
		touchscreen_clues_container = (LinearLayout) findViewById(R.id.touchscreen_clues_container);
		puzzlelist = (LinearLayout) findViewById(R.id.puzzlelist);
		puzzles_submenu = (LinearLayout) findViewById(R.id.puzzles_submenu );
		play_submenu_scroller = (ScrollView) findViewById(R.id.play_submenu_scroller);
		//
		
		
		// Touchscreen back button:
		((Button) findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromleft);
			  	if ( touchscreenanimator.getDisplayedChild() == MAIN_MENU_VIEWNUM ) {
				    goBack();
			  	} else if ( touchscreenanimator.getDisplayedChild() == CLUESSCROLLERS_SUBMENU_VIEWNUM ) {
			  		touchscreenanimator.setDisplayedChild( PLAY_SUBMENU_VIEWNUM );
			  	} else if ( touchscreenanimator.getDisplayedChild() == HINTS_SUBMENU_VIEWNUM ) {
			  		touchscreenanimator.setDisplayedChild( PLAY_SUBMENU_VIEWNUM );
			  		play_submenu_scroller.fullScroll( ScrollView.FOCUS_UP );
			  	} else if ( touchscreenanimator.getDisplayedChild() == PUZZLE_LIST_SUBMENU_VIEWNUM ) {
			  		touchscreenanimator.setDisplayedChild( PUZZLES_SUBMENU_VIEWNUM );
			  	} else {
			  		touchscreenanimator.setDisplayedChild( MAIN_MENU_VIEWNUM );
			  	}
			  	touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromright);
			}
		});

		// Play button:
		((Button) findViewById(R.id.play_button)).setOnClickListener(new OnClickListener() {
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
		((Button) findViewById(R.id.go_to_touchscreen_across_clues)).setOnClickListener(new OnClickListener() {
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
		((Button) findViewById(R.id.go_to_touchscreen_down_clues)).setOnClickListener(new OnClickListener() {
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
		((Button) findViewById(R.id.launchkeyboardbutton)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				einkanimator.setDisplayedChild( EINK_PUZZLE_VIEWNUM );
				bringUpKeyboard();
			}
		});
		// Check answer button:
		((Button) findViewById(R.id.check_solution_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}
				if ( ! activePuzzle.isSolved() ) {
					showShortToast( getString(R.string.not_solved_yet) );
				} else {
					//showLongToast( getString(R.string.you_solved_it) );
					try {
						LayoutInflater inflater = getLayoutInflater();
						TextView dialogtextview = (TextView) inflater.inflate( R.layout.dialogtext, null );
						dialogtextview.setText( R.string.you_solved_it);
						AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder( v.getContext() );
						alertdialogbuilder
							.setCancelable(false)
							.setCustomTitle( dialogtextview )
							.setPositiveButton( R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
								}
						});
						alertdialogbuilder.show();
						touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromleft);
						touchscreenanimator.setDisplayedChild( MAIN_MENU_VIEWNUM );
						play_submenu_scroller.fullScroll( ScrollView.FOCUS_UP );
					} catch (Exception ex) {
						Log.e( this.toString(), "Error: Exception trying to show dialog: " + ex );
						ex.printStackTrace();
					}
				}
			}
		});


		// About this puzzle button:
		((Button) findViewById(R.id.aboutthispuzzle_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (activePuzzle == null) {
					showToast_No_Active_Puzzle();
					return;
				}
				try {
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
				} catch (Exception ex) {
					Log.e( this.toString(), "Error: Exception trying to show dialog: " + ex );
					ex.printStackTrace();
				}
			}
		});
		
		// Rebus entry button:
		((Button) findViewById(R.id.rebus_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (activePuzzle == null) {
					showToast_No_Active_Puzzle();
					return;
				}
				Intent intent = new Intent(CrossWord.this, InsertRebusActivity.class) ;
				intent.putExtra(REBUSTEXTLABEL, activePuzzle.getCurrentCellUserText() );
				startActivityForResult( intent, ACTIVITY_INSERT_REBUS );
			}
		});


		// Hints button:
		((Button) findViewById(R.id.hints_button)).setOnClickListener(new OnClickListener() {
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
		((Button) findViewById(R.id.show_current_letter_button)).setOnClickListener(new OnClickListener() {
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
		((Button) findViewById(R.id.erase_wrong_answers_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}				
				int r = activePuzzle.eraseWrongAnswers();
			    if ( r == 0 ) {
			        showShortToast( getString(R.string.no_wrong_answers) );
			    } else {
			    	String s;
			    	if ( r == 1 ) {
			    		s = getString(R.string.erased_one_wrong_answer) ;
			    	} else {
			    		s = String.format( getString(R.string.erased_x_wrong_answers_fmt), r) ;
			    	}
			        showShortToast(s);
			    }
			}
		});
		// The hint/cheat button to just solve the entire puzzle:
		((Button) findViewById(R.id.i_give_up_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}
				try {
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
				} catch (Exception ex) {
					Log.e( this.toString(), "Error: Exception trying to show dialog: " + ex );
					ex.printStackTrace();
				}
			}
		});
		

		// The Puzzles ("File") button:
		((Button) findViewById(R.id.file_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				preparePuzzleMenu();
				touchscreenanimator.setInAnimation(v.getContext(), R.anim.fromright);
				touchscreenanimator.setDisplayedChild( PUZZLES_SUBMENU_VIEWNUM );
			}
		});
		
		// Open puzzle button:
		open_puzzle_button = (Button) findViewById(R.id.open_puzzle_button);
		open_puzzle_button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				my_crosswords_file_menu() ;
			}
		});
		
		// Resume current puzzle button:
		resume_current_puzzle_button = (Button) findViewById(R.id.resume_current_puzzle_button);
		resume_current_puzzle_button.setOnClickListener(new OnClickListener() {
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
		clear_puzzle_button = (Button) findViewById(R.id.clear_puzzle_button);
		clear_puzzle_button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}
				try {
					LayoutInflater inflater = getLayoutInflater();
					TextView dialogtextview = (TextView) inflater.inflate( R.layout.dialogtext, null );
					dialogtextview.setText( R.string.clear_puzzle_areyousure );
					AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder( v.getContext() );
					alertdialogbuilder.setCustomTitle( dialogtextview )
					  .setCancelable(false)
					  .setPositiveButton( R.string.clear_puzzle_yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								activePuzzle.clearAllAnswers();
								preparePuzzleMenu();
							}
					})
					  .setNegativeButton( R.string.clear_puzzle_no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					alertdialogbuilder.show();
				} catch (Exception ex) {
					Log.e( this.toString(), "Error: Exception trying to show dialog: " + ex );
					ex.printStackTrace();
				}
			}
		});
		
		// Delete puzzle button:
		delete_puzzle_button = (Button) findViewById(R.id.delete_puzzle_button);
		delete_puzzle_button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( activePuzzle == null ) {
					showToast_No_Active_Puzzle();
					return;
				}
				try {
					LayoutInflater inflater = getLayoutInflater();
					TextView dialogtextview = (TextView) inflater.inflate( R.layout.dialogtext, null );
					dialogtextview.setText( R.string.delete_puzzle_areyousure );
					AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder( v.getContext() );
					alertdialogbuilder.setCustomTitle( dialogtextview )
					  .setCancelable(false)
					  .setPositiveButton( R.string.delete_puzzle_yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteActivePuzzle();
								preparePuzzleMenu();
							}
					})
					  .setNegativeButton( R.string.delete_puzzle_no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					alertdialogbuilder.show();
				} catch (Exception ex) {
					Log.e( this.toString(), "Error: Exception trying to show dialog: " + ex );
					ex.printStackTrace();
				}
			}
		});
		
		// Puzzles Help button:
		puzzles_help_button = (Button) findViewById(R.id.puzzles_help_button);
		puzzles_help_button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
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
				} catch (Exception ex) {
					Log.e( this.toString(), "Error: Exception trying to show dialog: " + ex );
					ex.printStackTrace();
				}
			}
		});
		
		// The Settings button:
		((Button) findViewById(R.id.settings_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
		    	startActivityForResult( new Intent(CrossWord.this, SettingsActivity.class), ACTIVITY_SETTINGS );
			}
		});
		
	} // onCreate

	/////////////////////////////////////////////////

	// onResume:
	@Override
	public void onResume() {
		super.onResume();
		
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
		
		//  Save our work:
		try {
			if ( activePuzzle != null ) {
				PuzzleIO puzzleio = new PuzzleIO(this);
				puzzleio.savePuzzleWIP( activePuzzle );
			}
		} catch (Exception ex) {
			Log.e(this.toString(), "Error: exception saving user work: " + ex );
		}
				
	} // onPause
	
	///////////////////////////////////////////////////////////////////////////////

	//  This function dynamically generates the "Puzzles" submenu
	void preparePuzzleMenu() {
		puzzles_submenu.removeAllViews();
		//  Open:
		puzzles_submenu.addView(open_puzzle_button);
		//  Resume latest:
		if ( (activePuzzle == null) && mSettings.contains(CROSSWORD_PREFERENCES_CURRENT_PUZZLE  ) ) {
			puzzles_submenu.addView(resume_current_puzzle_button);
		}
		//  Clear puzzle:
		if ( activePuzzle != null ) {
			puzzles_submenu.addView(clear_puzzle_button);
		}
		//  Delete puzzle:
		if ( activePuzzle != null ) {
			if ( activePuzzle.isSolved() ) {
				puzzles_submenu.addView(delete_puzzle_button);
			}
		}
		//  Help:
		puzzles_submenu.addView(puzzles_help_button);
	} // preparePuzzleMenu
	

	///////////////////////////////////////////////////////////////////////////////

	private void loadGameSettings() {
		mark_wrong_answers = mSettings.getBoolean( CrossWord.CROSSWORD_PREFERENCES_MARK_WRONG_ANSWERS, CROSSWORD_PREFERENCES_MARK_WRONG_ANSWERS_DEFAULT) ;
		freeze_right_answers = mSettings.getBoolean( CrossWord.CROSSWORD_PREFERENCES_FREEZE_RIGHT_ANSWERS, CROSSWORD_PREFERENCES_FREEZE_RIGHT_ANSWERS_DEFAULT) ;
		cursor_next_clue = mSettings.getBoolean( CrossWord.CROSSWORD_PREFERENCES_CURSOR_NEXT_CLUE, CROSSWORD_PREFERENCES_CURSOR_NEXT_CLUE_DEFAULT) ;
		cursor_wraps = mSettings.getBoolean( CrossWord.CROSSWORD_PREFERENCES_CURSOR_WRAPS, CROSSWORD_PREFERENCES_CURSOR_WRAPS_DEFAULT) ;
	} // loadGameSettings
	
	///////////////////////////////////////////////////////////////////////////////

	
	//  This function is called after we've saved our work (if applicable), when
	//  we want to wipe out the existing puzzle:
	void destroyActivePuzzle() {
		einkanimator.setDisplayedChild( EINK_PUZZLE_VIEWNUM );
		if ( activePuzzle != null ) activePuzzle.killChildThreads();
		eink_xword_page.removeAllViews();
		eink_cluespagescroller.removeAllViews();
		touchscreen_clues_container.removeAllViews();
		activePuzzle = null ;
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
	
	private void deleteActivePuzzle() {
		if ( activePuzzle == null ) return ;
		String f1 = activePuzzle.getFileName() ;
		String f2 = f1 + ".wip" ;
		destroyActivePuzzle();

		Editor editor = mSettings.edit();
		editor.remove( CROSSWORD_PREFERENCES_CURRENT_PUZZLE );
		editor.commit();        

		File f;		
		try {
			f = new File(f1);
			f.delete();
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception deleting puzzle: " + ex );
		}
		try {
			f = new File(f2);
			f.delete();
		} catch (Exception ex) {
			Log.e( this.toString(), "Exception deleting puzzle WIP: " + ex );
		}
		
	} // deleteActivePuzzle
	
	
	///////////////////////////////////////////////////////////////////////////////

	@Override 
	protected void onActivityResult(int requestCode,int resultCode,Intent data) 
	{
		switch(requestCode) {
		case ACTIVITY_SETTINGS:
			loadGameSettings();
			if ( activePuzzle != null ) activePuzzle.settingsHaveChanged();
			break;
		case ACTIVITY_OPEN_PUZZLE:
			if( resultCode == RESULT_OK && data != null) {
				Bundle b = data.getExtras();
				if( b != null) {
					String file = b.getString(FILE);
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
			break ;
		case ACTIVITY_INSERT_REBUS:
			if ( activePuzzle == null ) return;
			if( resultCode == RESULT_OK && data != null) {
				Bundle b = data.getExtras();
				if( b != null) {
					String s = b.getString(REBUSTEXTLABEL).toUpperCase();
					activePuzzle.setUserText(s);
					activePuzzle.incrementCursor();
				}
			}
			break ;
		}
	} // onActivityResult

	boolean launchFileSelector(String m_folder) {
	    try {
	        Intent intent = new Intent(Intent.ACTION_MAIN);
    	    intent.addCategory(Intent.ACTION_DEFAULT);
    	    intent.setComponent(new ComponentName("com.nookdevs.fileselector", "com.nookdevs.fileselector.FileSelector"));
    	    intent.putExtra("ROOT",m_folder);
    	    String[] filters = {".*\\.puz", ".*\\.xml", ".*\\.xpf"};
    	    intent.putExtra("FILTER", filters);
    	    intent.putExtra("TITLE", getString(R.string.my_puzzles));
    	    startActivityForResult(intent, ACTIVITY_OPEN_PUZZLE);
    	    return true;
	    } catch(Exception ex) {
	        return false;
	    }
	    
	}
		
	void my_crosswords_file_menu() {
		String m_folder;
		//  Figure out where "my puzzles" lives: 
		File f = new File( EXTERNAL_SD_FOLDER);
		if( !f.exists()) {
		        m_folder = INTERNAL_SD_FOLDER;
		} else {    
		        m_folder = EXTERNAL_SD_FOLDER;
		}           
		
	    if( launchFileSelector(m_folder)) {
	        return;
	    }
	    
		//  This code below is not normally used.  It's a fall-back if the
		//  com.nookdevs.fileselector intent can't be found.
		//  When they click on the button to open a puzzle, we dynamically
		//  populate the menu (by listing the files in "my crosswords"):
	    showLongToast( getString(R.string.install_fileselector) );

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
			f = new File(xworddir, fname);
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
		
		//  If another puzzle was already loaded, save our work before loading a new puzzle:
		if ( activePuzzle != null ) {
			puzzleio.savePuzzleWIP( activePuzzle );
		}
		//  Wipe out the current puzzle to free up some memory:
		destroyActivePuzzle();
		
		//  Load the puzzle:
	    puzzle = puzzleio.loadPuzzle( puzzlefilename );
		if (puzzle == null) {
			showLongToast( getString(R.string.error_could_not_open_puzzle) );
			return;
		}
		
		//  Attach it to our views:
		attachPuzzle(puzzle);
		
		//  Pop up some info about this puzzle:
		showShortToast( activePuzzle.getFileName().replaceAll(".*/", "") );
		showLongToast( activePuzzle.aboutThisPuzzle() );

		//  Warn about unsupported features:
		//if ( puzzle.hasUnsupportedFeatures() ) {
		//	showLongToast( getString(R.string.warning_rebus_not_supported) );
		//}
		
		//  Set this as the current puzzle:
		setAsCurrentPuzzle( puzzlefilename );

	} // loadCrossWord

	void resumeCurrentPuzzle() {
		String fname ;
		try {
			if (! mSettings.contains(CROSSWORD_PREFERENCES_CURRENT_PUZZLE  ) ) {
				showLongToast( getString(R.string.error_cannot_determine_latest_puzzle) );
				return ;
			}	
			fname = mSettings.getString( CROSSWORD_PREFERENCES_CURRENT_PUZZLE, "" );
			if ( (fname == null) || (fname.equals("")) ) {
				showLongToast( getString(R.string.error_cannot_determine_latest_puzzle) );
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
		
		/*
		Log.d( this.toString(), "DEBUG: KeyEvent = " + event.toString() );
		Log.d( this.toString(), "DEBUG: Key number: " + event.getNumber() );
		if ( event.isShiftPressed() ) {
		    Log.d( this.toString(),  "DEBUG: SHIFT" );
		} else {
		    Log.d( this.toString(),  "DEBUG: Not SHIFTed" );
		}
		*/

		// We don't handle any keys when no puzzle is loaded.
		if (activePuzzle == null) return (false);

		try {
			switch (keyCode) {

			case NOOK_PAGE_UP_KEY_LEFT:
			case NOOK_PAGE_UP_KEY_RIGHT:
			case NOOK_PAGE_UP_SWIPE:
				if ( einkanimator.getDisplayedChild() == EINK_CLUES_VIEWNUM ) {
					try {
						int y = ((ScrollView) einkanimator.getCurrentView()).getScrollY();
						if (y == 0) {
							einkanimator.showPrevious();
						} else {
							//((ScrollView) einkanimator.getCurrentView()).pageScroll(View.FOCUS_UP);
							((ScrollView) einkanimator.getCurrentView()).scrollBy(0, -(SizeDependent.EINK_WINDOW_HEIGHT - 30) );
						}
					} catch (Exception ex) {
						//einkanimator.showPrevious();
					}
				}
				return (true);
			case NOOK_PAGE_DOWN_KEY_LEFT:
			case NOOK_PAGE_DOWN_KEY_RIGHT:
			case NOOK_PAGE_DOWN_SWIPE:
				if ( einkanimator.getDisplayedChild() == EINK_PUZZLE_VIEWNUM ) {
					einkanimator.showNext();
				} else {
					try {
						//((ScrollView) einkanimator.getCurrentView()).pageScroll(View.FOCUS_DOWN);
						((ScrollView) einkanimator.getCurrentView()).scrollBy(0, (SizeDependent.EINK_WINDOW_HEIGHT - 30) );
					} catch (Exception ex) {
						//einkanimator.showNext();
					}
				}
				return (true);
			case SOFT_KEYBOARD_CLEAR:
			case KeyEvent.KEYCODE_CLEAR:
				if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
				activePuzzle.setUserText(" "); // clear the cell
				return (true);
			case KeyEvent.KEYCODE_DEL:
				if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
				activePuzzle.decrementCursor();
				activePuzzle.setUserText(" "); // clear the cell
				return (true);
			case KeyEvent.KEYCODE_SPACE:
				// Spacebar switches typing direction. Non-intuitive, I know; maybe
				// we should have a custom keyboard
				toggleDirection_with_Toast();
				return (true);
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
				activePuzzle.incrementCursor(ACROSS);
				activePuzzle.scroll_TouchscreenClues_to_Cursor();
				return (true);
			case KeyEvent.KEYCODE_DPAD_LEFT:
				if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
				activePuzzle.decrementCursor(ACROSS);
				activePuzzle.scroll_TouchscreenClues_to_Cursor();
				return (true);
			case KeyEvent.KEYCODE_DPAD_UP:
				if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
				activePuzzle.decrementCursor(DOWN);
				activePuzzle.scroll_TouchscreenClues_to_Cursor();
				return (true);
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
				activePuzzle.incrementCursor(DOWN);
				activePuzzle.scroll_TouchscreenClues_to_Cursor();
				return (true);
			case KeyEvent.KEYCODE_AT:
				if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
				activePuzzle.setUserText("@");
				activePuzzle.incrementCursor();
				return (true);
			case KeyEvent.KEYCODE_PERIOD:
				if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
				activePuzzle.setUserText(".");
				activePuzzle.incrementCursor();
				return (true);
			default:
				//  It's annoying that I have to jump through these hoops.  Isn't there a cleaner way?
				if ( ! event.isPrintingKey() ) break ;
				char c;
				//  Regular letters:
				c = event.getMatch("ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray());
				if (c != '\0') {
					if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
					activePuzzle.setUserText((c + ""));
					activePuzzle.incrementCursor();
					return (true);
				}
				//  Numbers and their symbols:
				c = event.getNumber();
				if ( c != '\0' ) {
					String s;
					s = "!@#$%^&*()" ;
					if ( s.contains( "" + c) ) {
						if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
						activePuzzle.setUserText((c + ""));
						activePuzzle.incrementCursor();
						return(true);
					}
					if ( event.isShiftPressed() ) {
						if ( c == '1' ) c = '!' ;
						else if ( c == '2' ) c = '@' ;
						else if ( c == '3' ) c = '#' ;
						else if ( c == '4' ) c = '$' ;
						else if ( c == '5' ) c = '%' ;
						else if ( c == '6' ) c = '^' ;
						else if ( c == '7' ) c = '&' ;
						else if ( c == '8' ) c = '*' ;
						else if ( c == '9' ) c = '(' ;
						else if ( c == '0' ) c = ')' ;
						else if ( c == '\'' ) c = '"' ;
						else if ( c == '/' ) c = '?' ;
						else if ( c == '.' ) c = ':' ;  // Huh?!!!
					}
					s = "1234567890!@#$%^&*()?/,:;'\"+=-" ;  // for now, no . or ~
					if ( s.contains( "" + c) ) {
						if (einkanimator.getDisplayedChild()==EINK_CLUES_VIEWNUM) einkanimator.setDisplayedChild(EINK_PUZZLE_VIEWNUM);
						activePuzzle.setUserText((c + ""));
						activePuzzle.incrementCursor();
						return(true);
					}

				}
				break;
			}
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: unexpected exception in onKeyDown(): " + ex );
		}
		
		return (false);
	} // onKeyDown
	
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
			msg = getString( R.string.dir_is_across_toast ) ;
		} else if (dir == DOWN) {
			msg = getString( R.string.dir_is_down_toast ) ;
		} else {
			msg = getString( R.string.dir_is_none_toast ) ;
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
	
		
} // CrossWord class
	
	

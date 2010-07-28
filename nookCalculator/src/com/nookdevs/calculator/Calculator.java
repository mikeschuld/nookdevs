
/* 
 * Nook Calculator,  Copyright 2010 nookDevs
 * 
 * Written by Kevin Vajk
 * 
 * A basic calculator for the Barnes & Noble "nook" e-book reader.
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
 */
/*
 * Nook utility functions at the bottom of this file were adapted from
 * code written and copyrighted by the nookDevs team, under the same
 * Apache License, Version 2.0.  Scroll down for details.
 */
/*
 * Note:
 * This is my first Java program ever, as well as my first program on the
 * android platform.  I apologize if the code is awful, but I've got to
 * start somewhere.  No warranty implied, obviously.
 */

package com.nookdevs.calculator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.TextView;
import android.os.PowerManager;
import android.util.Log;
import android.database.Cursor;
import android.net.Uri;
import java.math.BigDecimal;
import java.math.MathContext;

//  TODO: History buffer so we can page back to look at old calculations
//  TODO: When doing iterated calculations, we're losing accuracy by converting to/from a String
//  TODO: Allow user to enter negative numbers with - button.  (Can cause confusion, though.)
//  TODO: If the user enters a ridiculous number of digits, the output looks funny.
//  TODO: I wish I had screen space for a "settings" button, but I don't want to waste the space
//        on something most people don't need.

public class Calculator extends Activity {
	
    protected static String LOGTAG = "Calculator" ;
	protected static String NAME = "calculator" ;
	protected static String TITLE = "Calculator" ;
	private TextView touchscreentext ;
	private TextView einktext ;
	
    PowerManager.WakeLock screenLock = null;
    long m_ScreenSaverDelay = 600000;  // default value; will be replaced with Nook setting

    private String arg1 = "" ;
    private String op = "" ;
    private String arg2 = "" ;
    private String result = "" ;
    
    ///////////////////////////////////////////////////////////////////////////////
    

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // android power management screen stuff (see comments down below):
        initializeScreenLock();

        // The views we'll be displaying our output in:
        touchscreentext = (TextView) findViewById( R.id.touchscreentext);
        einktext = (TextView) findViewById( R.id.einktext);

        //  Back button handler:
        Button back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                goBack();
            }
        });

        //  Calculator button handlers:
        setUpCalcNumKey(R.id.one);
        setUpCalcNumKey(R.id.two);
        setUpCalcNumKey(R.id.three);
        setUpCalcNumKey(R.id.four);
        setUpCalcNumKey(R.id.five);
        setUpCalcNumKey(R.id.six);
        setUpCalcNumKey(R.id.seven);
        setUpCalcNumKey(R.id.eight);
        setUpCalcNumKey(R.id.nine);
        setUpCalcNumKey(R.id.zero);
        setUpCalcPointKey(R.id.point);
        setUpCalcOpKey(R.id.add);
        setUpCalcOpKey(R.id.subtract);
        setUpCalcOpKey(R.id.multiply);
        setUpCalcOpKey(R.id.divide);
        setUpCalcEqualsKey(R.id.equals);
        setUpCalcClearKey(R.id.clear);
        setUpCalcClearEntryKey(R.id.clearentry);
        
    } //onCreate
    
    
    @Override
    public void onPause() {
        super.onPause();
        releaseScreenLock();
    } // onPause
    
    @Override
    public void onResume() {
        super.onResume();
        acquireScreenLock();
        updateTitle(TITLE);
        updateScreens();
    } // onResume
    
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        acquireScreenLock();
    } // onUserInteraction
    
    
    ///////////////////////////////////////////////////////////////////////////////
    
    
    //  Handle the number keys:
    private void setUpCalcNumKey( int id ) {
        Button b;
        b = (Button) findViewById(id);        
        b.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		String key = ((String) ((Button) v).getText()).trim() ;
        		
        		//  If we haven't done a calculation yet, or if we've just finished
        		//  one, pressing a number key starts a new calculation:
        		if ( arg1.equals("") || (! result.equals("")) ) {
        			arg1 = key ;
        			op = "" ;
        			arg2 = "" ;
        			result = "" ;
        		} else if ( op.equals("") ) {
        			//  No operation specified yet, which means we're entering arg1 digits:
        			arg1 = arg1 + key ;
        		} else {
        			//  An operation has already been specified, so we must be entering arg2 digits:
        			arg2 = arg2 + key ;
        		}
        		
        		updateScreens();
        	}
        });
    } //  setUpCalcNumKey
    
    
    //  Handle the decimal point button
    private void setUpCalcPointKey( int id ) {
        Button b;
        b = (Button) findViewById(id);        
        b.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		
        		if (! result.equals("")) {
        			//  We've just finished a calculation, so this starts a new one:
        			arg1 = "0." ;
        			op = "" ;
        			arg2 = "" ;
        			result = "" ;
        		} else if ( op.equals("") ) {
        			//  No operation specified yet, which means we're still entering arg1 digits:
        			if ( arg1.equals("") ) {
        				//  We're starting a new calculation, so clear everything else:
        				arg1 = "0." ;
        				op = "" ;
        				arg2 = "" ;
        				result = "" ;
        				// If they start a number with a decimal point, prepend a zero:
        				// (e.g. turn ".2" into "0.2")
        				arg1 = "0." ;
        			} else if ( arg1.equals("-") ) {
        				// If they start a negative number with a minus and then a decimal point, include a zero
        				// (e.g. turn "-.5" into "-0.5"):
        				arg1 = "-0." ;
        			} else if ( ! arg1.contains(".") ) {
        				arg1 = arg1 + ".";
        			} else {
        			    //  Ignore this: they mistyped.
        				//  They're trying to type something like "3.01.", which makes no sense.
        			}
        		} else if ( ! op.equals("") ) {
        			//  An operation has already been specified, so we must be entering arg2 digits:
        			if ( arg2.equals("") ) {
        				// If they start a number with a decimal point, prepend a zero:
        				// (e.g. turn ".2" into "0.2")
        				arg2 = "0." ;
        			} else if ( arg2.equals("-") ) {
        				// If they start a negative number with a minus and then a decimal point, include a zero
        				// (e.g. turn "-.5" into "-0.5"):
        				arg2 = "-0." ;
        			} else if ( ! arg2.contains(".") ) {
        				arg2 = arg2 + ".";
        			} else {
        			    //  Ignore this: they mistyped.
        				//  They're trying to type something like "3.01.", which makes no sense.
        			}
        		}

        		updateScreens();
        	}
        });
    } //  setUpCalcPointKey
    

    //  The C (clear) button:
    private void setUpCalcClearKey( int id ) {
        Button b;
        b = (Button) findViewById(R.id.clear);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                arg1 = "" ;
                op = "" ;
                arg2 = "" ;
                result = "" ;
                updateScreens();
            }
        });
    } // setUpCalcClearKey
    
    
    //  The CE (clear entry) button:
    private void setUpCalcClearEntryKey( int id ) {
    	Button b;
        b = (Button) findViewById(R.id.clearentry);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	if ( (! arg1.equals("")) && op.equals("") ) {
            		// we were in the middle of entering arg1:
            		arg1 = "" ;
            	} else if ( (! op.equals("")) && arg2.equals("") ) {
            		// an operation has been specified, but no second number yet,
            		// so clear the op:
            		op = "" ;
            	} else if ( (! op.equals("")) && (! arg2.equals("")) && result.equals("") ) {
            		// we were entering arg2:
            		arg2 = "" ;
            	} else if (! result.equals("")) {
            		// the calculation was done, but they want to change something:
            		result = "" ;
            		arg2 = "" ;
            	}
                updateScreens();
            }
        });
    } // setUpCalcClearEntryKey
    
    
    //  Handle the +,-,/,* keys:
    private void setUpCalcOpKey( int id ) {
        Button b;
        b = (Button) findViewById(id);        
        b.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		String key = ((String) ((Button) v).getText()).trim() ;
        		        		
        		if ( (! arg1.equals("")) && (! arg1.equals("-")) && op.equals("") ) {
        			//  The normal case:
        			op = key ;
        		} else if ( (! arg1.equals("")) && (! op.equals("")) && (! arg2.equals("")) ) {
        			// They typed in something like: "2" "+" "3" "*" (or maybe "2" "+" "3" "=" "*")
        			// So, do the first operation (e.g. 2+3), and put its result (e.g. 5) into arg1
        			doCalculation();
        			arg1 = result ; arg2 = "" ; result = "" ; op = key ;
        		} else {
        			//  They pressed a +-*/ key when it doesn't make sense, so ignore it
        		}
        		updateScreens();
        	}
        });
    } //  setUpCalcOpKey

    
    //  Handle the = key:
    private void setUpCalcEqualsKey( int id ) {
        Button b;
        b = (Button) findViewById(id);        
        b.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		if ( (! arg1.equals("")) && (! op.equals("")) && (! arg2.equals("")) ) {
        			doCalculation();
        		}
        		updateScreens();
        	}
        });
    } //  setUpCalcEqualsKey
    
    
    ///////////////////////////////////////////////////////////////////////////////


    //  If I were to re-write this, I'd probably just have the buttons send
    //  their operation as a tag, rather than parsing their text label.
    private boolean opIsPlus(String op) {
    	return( op.equals("+") );
    } // opIsPlus
    private boolean opIsMinus(String op) {
    	// Note that there is a difference between a dash, a minus sign, an emdash, and an em-dash:
    	return( op.equals("−") || op.equals("-") || op.equals("–") || op.equals("—") );
    } // opIsMinux
    private boolean opIsTimes(String op) {
    	// Note that there is a difference between an x and a times sign
    	return( op.equals("×") || op.equals("x") || op.equals("X") || op.equals("*") );
    } // opIsTimes
    private boolean opIsDivision(String op) {
    	return( op.equals("÷") || op.equals("/") );
    } // opIsDivision
    
    
    private void doCalculation() {
    	BigDecimal a1 = new BigDecimal(arg1) ;
    	BigDecimal a2 = new BigDecimal(arg2) ;
    	BigDecimal r ;
    	MathContext mc = MathContext.DECIMAL64 ;
    	try {
    	    if ( opIsPlus(op) ) {
    		    r = a1.add(a2, mc) ;
    	    } else if ( opIsMinus(op) ) {
    		    r = a1.subtract(a2, mc) ;
    	    } else if ( opIsTimes(op) ) {
    	    	r = a1.multiply(a2, mc);
    	    } else if ( opIsDivision(op) ) {
        		r = a1.divide(a2, mc);
        	} else {
			    Log.e(LOGTAG, "Internal error: unknown operation");
			    finish();
			    return ; // not reached
    	    }
    	    result = String.valueOf(r);
    	} catch (Exception ex) {
            Log.e(LOGTAG, "exception doing calculation - ", ex);
            finish();
        }
    } // doCalculation

    
    //  Based on the current calculator state, draw the displays:
    private void updateScreens() {
        touchscreentext.setText("");
    	einktext.setText("");
    	einktext.invalidate();  // force a re-draw

        if ( arg1.equals("") ) {
        	// everything blank:
        	touchscreentext.setText( "" ) ;
        	einktext.setText( "" );
        } else if ( (! arg1.equals("")) && op.equals("") ) {
        	//  entering the first argument:
        	touchscreentext.setText( arg1 ) ;
        	einktext.setText( arg1 );
        } else if ( (! arg1.equals("")) && (! op.equals("")) && (result.equals("")) ) {
        	// entering the second argument (it might still be blank, though):
        	String padded_op = op + " " ;
            while ( (padded_op.length() + arg2.length()) < (arg1.length() + 2) ) {
            	padded_op = padded_op + " " ;
            }
            einktext.setText( arg1 + "\n" + padded_op + arg2 );
            
        	if ( arg2.equals("") ) {
        		touchscreentext.setText( "\n" + op + "  ");
        	} else {
        	    touchscreentext.setText( "\n" + arg2);
        	}
        } else if ( (! arg1.equals("")) && (! op.equals("")) && (! arg2.equals("")) && (! result.equals("")) ) {
        	//  the calculation has been done, so display it all:
        	String padded_op = op + " " ;
        	String separator = "" ;
        	String padded_equal = "=" + " ";
        	
            while ( ( (padded_op.length() + arg2.length()) < (arg1.length() + 2) ) ||
            		( (padded_op.length() + arg2.length()) < (result.length() + 2) )  ) {
            	padded_op = padded_op + " " ;
            }
            while ( (separator.length() < arg1.length()) ||
            		(separator.length() < arg2.length()) ||
            		(separator.length() < result.length()) ) {
            	separator = separator + "_" ;
            }
            while( padded_equal.length() + result.length() < (separator.length() + 2) ) {
            	padded_equal = padded_equal + " ";
            }
            
        	//touchscreentext.setText("");
            touchscreentext.setText(result);
        	einktext.setText( arg1 + "\n" + padded_op + arg2 + "\n" + separator + "\n\n" + padded_equal + result );

        } else {
        	touchscreentext.setText("");
        	einktext.setText("");
        }
        
        //einktext.append("\n\n\n\n\nDEBUG:\n" + "arg1 = " + arg1 + "\n" + "op = " + op + "\n" + "arg2 = " + arg2 + "\n" + "result = " + result + "\n" );
    } // updateScreens
    
    
    ///////////////////////////////////////////////////////////////////////////////
    
    
    //
    //  The code below was adapted from nookDevs code.  Their license is:
    //
    
    /* 
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
     */

    
    //  Android was designed for phones with back-lit screens; it doesn't know
    //  that the Nook's e-ink display doesn't use power when displaying a static
    //  image.
    //  So, we want to prevent android from blanking the e-ink display on us.
    //  Called from onCreate:
    //  Adapted from nookDevs code:
    private void initializeScreenLock() {
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        screenLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "nookactivity" + hashCode());
        screenLock.setReferenceCounted(false);
        String[] values = {
                "value"
        };
        String[] fields = {
        		"bnScreensaverDelay"
        } ;
        Cursor c = getContentResolver().query(Uri.parse("content://settings/system"), values, "name=?", fields, "name");
        if (c != null) {
            c.moveToFirst();
            long lvalue = c.getLong(0);
            if (lvalue > 0) {
                m_ScreenSaverDelay = lvalue;
            }
        }
    }  // initializeScreenLock
    // Called from onPause:
    private void releaseScreenLock() {
        try {
            if (screenLock != null) {
                screenLock.release();
            }
        } catch (Exception ex) {
            Log.e(LOGTAG, "exception releasing screenLock - ", ex);
            finish();
        }
    } // releaseScreenLock
    //  Called from onResume and onUserInteraction:
    private void acquireScreenLock() {
        if (screenLock != null) {
            screenLock.acquire(m_ScreenSaverDelay);
        }
    }  // acquireScreenLock
    
    //  Update the title bar:
    //  Taken from nookDevs common:
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
    
    //  Taken from nookDevs common:
    protected void goHome() {
        String action = "android.intent.action.MAIN";
        String category = "android.intent.category.HOME";
        Intent intent = new Intent();
        intent.setAction(action);
        intent.addCategory(category);
        startActivity(intent);
    } // goHome
    //  Taken from nookDevs common:
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
    
    
}  // Calculator Activity

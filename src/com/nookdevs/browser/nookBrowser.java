/**
 *     This file is part of nookBrowser.

    nookBrowser is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    nookBrowser is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with nookBrowser.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.nookdevs.browser;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.nookdevs.common.IconArrayAdapter;
import com.nookdevs.common.nookBaseActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemClickListener;

public class nookBrowser extends nookBaseActivity 
			 implements OnClickListener , OnLongClickListener , OnItemClickListener, OnKeyListener 
{

    private static final String LOGTAG = "browser";
    
    private static final int NOOK_PAGE_UP_KEY_RIGHT = 98;
    private static final int NOOK_PAGE_DOWN_KEY_RIGHT = 97;
    private static final int NOOK_PAGE_UP_KEY_LEFT = 96;
    private static final int NOOK_PAGE_DOWN_KEY_LEFT = 95;
	private WebView webview;
	private ListView lview, sublist;
	public static ConnectivityManager.WakeLock lock=null;
	private Button goButton,backButton,upButton,downButton, rightButton,leftButton;
	private boolean m_Processing=false;
	protected static final String DEFAULT_HOME_PAGE="http://books.google.com/m";
	// stores the last url navigated
	private String lastNavigatedUrl=null;
	private int m_Cmd=-1;
	private TextListener m_TextListener = new TextListener(this);
	private Dialog m_Dialog;
	private DownloadManager m_DownloadManager = new DownloadManager(this);
	protected static final int LOAD_URL=0;
	protected static final int SETTINGS=1;
	protected static final int GO_HOME=2;
	protected static final int SOFT_KEYBOARD=3;
	protected static final int TEXT_SIZE=4;
	protected static final int FIND_STRING=5;
	protected static final int SAVE_PAGE=6;
	protected static final int CLOSE=7;
	protected  static final int SOFT_KEYBOARD_CLEAR=-13;
	protected static final int SOFT_KEYBOARD_SUBMIT=-8;
	protected static final int SOFT_KEYBOARD_CANCEL=-3;
	private static final int WEB_SCROLL_PX = 700;
	public static final int CONNECTION_TIMEOUT=120000; // lose connection if no n/w action for 2 minutes.
	private ViewAnimator m_ViewAnimator;
	private String m_HomePage;
	public static final String APP_TITLE="Web";
	private ProgressBar m_ProgressBar ;
	Handler m_Handler = new Handler();
	int [] icons = { -1,-1,-1,-1,R.drawable.submenu,-1,-1,-1};
	int [] subicons = { -1,-1,-1,-1,-1,-1,-1,-1};
		
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);
        lview = (ListView) findViewById(R.id.list);
        CharSequence[] menuitems = getResources().getTextArray(R.array.webmenu);
        List<CharSequence> menuitemsList = Arrays.asList(menuitems);
        IconArrayAdapter<CharSequence> adapter = 
        	new IconArrayAdapter(lview.getContext(), R.layout.listitem,menuitemsList,icons); 
        adapter.setImageField(R.id.ListImageView);
        adapter.setTextField(R.id.ListTextView);
        lview.setAdapter(adapter);
        lview.setOnItemClickListener(this);
        
        sublist = (ListView) findViewById(R.id.sublist);
        menuitems = getResources().getTextArray(R.array.submenu1);
        menuitemsList = Arrays.asList(menuitems);
        adapter = new IconArrayAdapter<CharSequence>( this, R.layout.listitem, menuitemsList, subicons);
        adapter.setImageField(R.id.ListImageView);
        adapter.setTextField(R.id.ListTextView);
        sublist.setAdapter(adapter);
        sublist.setOnItemClickListener(this);
        webview = (WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setClickable(false);
        m_ViewAnimator = (ViewAnimator)findViewById(R.id.listviewanim);
        m_ViewAnimator.setInAnimation(this, R.anim.fromright);
	    m_ViewAnimator.setAnimateFirstView(true);
        ConnectivityManager cmgr = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        lock = cmgr.newWakeLock(1,"nookBrowser" + hashCode());
        lock.setReferenceCounted(false);
       	webview.setWebViewClient(new HelloWebViewClient(this));
       	m_ProgressBar = (ProgressBar) findViewById(R.id.progress_horizontal);
       	m_ProgressBar.setVisibility(View.INVISIBLE);
       	final Activity activity = this;
           webview.setWebChromeClient(new WebChromeClient() {
             public void onProgressChanged(WebView view, int progress) {
               m_ProgressBar.setProgress(progress);
               if( progress ==100) {
            	   m_ProgressBar.setVisibility(View.INVISIBLE);
            	   m_ProgressBar.setProgress(0);
               } else {
            	   m_ProgressBar.setVisibility(View.VISIBLE);
            	   m_ProgressBar.setProgress(progress);
               }
             }
        });
        goButton = (Button)findViewById(R.id.go);
        backButton = (Button)findViewById(R.id.back);
        upButton = (Button)findViewById(R.id.up);
        downButton = (Button)findViewById(R.id.down);
        rightButton = (Button)findViewById(R.id.right);
        leftButton = (Button)findViewById(R.id.left);
        addListeners();

        Uri data = getIntent().getData();
        if( data != null)
        	m_HomePage = data.toString();
        else
        	m_HomePage=null;
    }

    private void addListeners() {
        goButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        upButton.setOnClickListener(this);
        downButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);
        leftButton.setOnClickListener(this);
        goButton.setOnLongClickListener(this);
        backButton.setOnLongClickListener(this);
        upButton.setOnLongClickListener(this);
        downButton.setOnLongClickListener(this);
        rightButton.setOnLongClickListener(this);
        leftButton.setOnLongClickListener(this);
        webview.setDownloadListener(m_DownloadManager);
        webview.setOnKeyListener(this);
    }
   
    // Helper method for getting the top window.
    public WebView getTopWindow() {
        return webview;
    }
 
    protected void readSettings() {
        try {
        	if( m_HomePage ==null)
        		m_HomePage = getPreferences(MODE_PRIVATE).getString("HOME_PAGE", DEFAULT_HOME_PAGE);
        } catch(Exception ex) {
        	Log.e(this.LOGTAG, "preference exception: ", ex);
        	m_HomePage = DEFAULT_HOME_PAGE;
        }
        super.readSettings();
    }
    @Override
    public void onPause() {
    	super.onPause();
    	try {
    		if( lock != null) {
    			lock.release();
    		}
    	} catch(Exception ex) {
    		
    	}
    }

    @Override
    public void onResume() {
    	super.onResume();
    	m_ProgressBar.setVisibility(View.INVISIBLE);
        updateTitle(APP_TITLE);
        m_Processing=false;
        try {
        	if( m_Dialog != null) m_Dialog.dismiss();
        } catch(Exception ex) {};
        CookieManager.getInstance().removeExpiredCookie();
    	try {
    		if( lock != null) {
    			lock.acquire(CONNECTION_TIMEOUT);
    			if(waitForConnection()) {
    				if( lastNavigatedUrl != null && !lastNavigatedUrl.equals(""))
    					webview.loadUrl(lastNavigatedUrl);
    				else if( m_HomePage != null) 
    					webview.loadUrl(m_HomePage);
    			}
    			else {
    					// load a local file with error. - later
    			}
    		}
    	} catch(Exception ex) {
    		Log.e(this.LOGTAG, "exception on Resume " , ex);
    	}
     }
    
    private boolean waitForConnection() {
 
    	try {
    		if( getAirplaneMode()) {
    			return false;
    		}
    		ConnectivityManager cmgr = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    		NetworkInfo info = cmgr.getActiveNetworkInfo();
            boolean connection =(info ==null)?false:info.isConnected();
            int attempts=0;
            while ( !connection &&  attempts < 10) {
            	Thread.sleep(2000);
            	info = cmgr.getActiveNetworkInfo();
            	connection =(info ==null)?false:info.isConnected();
            	attempts++;
            }
            return connection;
    	} catch(Exception ex) {
    		Log.e(this.LOGTAG, "Exception while checking the connection " , ex);
    	}
    	return false;
    }
    protected void connectionLock() {
    	if( lock != null) {
    		lock.acquire(CONNECTION_TIMEOUT);
    		m_Handler.post(new Runnable() {
    			public void run() {
    				try {
    					m_ProgressBar.setProgress(5);
    					m_ProgressBar.setVisibility(View.VISIBLE);
    				} catch(Exception ex) {
    					Log.e(LOGTAG, "Exception in connectionLock ", ex);
    				}
    				}
    		});
    		waitForConnection();
    	}
    }
    public boolean onLongClick(View v) {
   		processKey(v);
   		processKey(v);
    	return false;
    }
    
	public void onClick(View v) {
		processKey(v);
		
	};
	private void updateTextSize(int size) {
        WebSettings.TextSize textSize;
        int fontsize=8;
        switch(size) {
        //smallest to largest
        	case 0:
        		textSize = WebSettings.TextSize.SMALLEST;
        		fontsize=8;
        		break;
        	case 1:
        		textSize = WebSettings.TextSize.SMALLER;
        		fontsize=10;
        		break;
        	case 2:
        		textSize = WebSettings.TextSize.NORMAL;
        		fontsize=12;
        		break;
        	case 3:
        		textSize = WebSettings.TextSize.LARGER;
        		fontsize=14;
        		break;
        	case 4:
        		textSize = WebSettings.TextSize.LARGEST;
        		fontsize=16;
        		break;
        	default:
        		textSize = WebSettings.TextSize.NORMAL;

        }
        
        webview.getSettings().setTextSize(textSize);
        WebSettings settings = webview.getSettings();
        settings.setTextSize(textSize);
        m_ViewAnimator.showPrevious();
      //  sublist.setVisibility(View.INVISIBLE);
      //  lview.setVisibility(View.VISIBLE);
	}
	
	private void processKey(View v) {
		KeyEvent event;
		if( v.equals(goButton)) {
			event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
			webview.onKeyDown(event.KEYCODE_DPAD_CENTER, event);
			event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER);
			webview.onKeyUp(event.KEYCODE_DPAD_CENTER, event);
			webview.dispatchKeyEvent(event);
		} else if( v.equals(backButton)) {
			if( m_ViewAnimator.getCurrentView().equals(sublist)){
				//sublist.setVisibility(View.INVISIBLE);
				//lview.setVisibility(View.VISIBLE);
				m_ViewAnimator.showPrevious();
			} else if( webview.getProgress() !=100) {
				webview.stopLoading();
				m_ProgressBar.setVisibility(View.INVISIBLE);
			}
			else if( webview.canGoBack()) {
				Runnable run1 = new Runnable() {
					public void run() {
						connectionLock();
						webview.goBack();
					}
				};
				(new Thread(run1)).start();
			} else {
				goBack();
				
			}
		} else if( v.equals(upButton)) {
			event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);
			webview.onKeyDown(event.KEYCODE_DPAD_UP, event);
			event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP);
			webview.onKeyUp(event.KEYCODE_DPAD_UP, event);
		} else if( v.equals(downButton)) {
			event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
			webview.onKeyDown(event.KEYCODE_DPAD_DOWN,event);
			event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN);
			webview.onKeyUp(event.KEYCODE_DPAD_DOWN,event);
		
		} else if( v.equals(rightButton)) {
			event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
			webview.onKeyDown(event.KEYCODE_DPAD_RIGHT, event);
			event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT);
			webview.onKeyUp(event.KEYCODE_DPAD_RIGHT, event);
			
		} else if( v.equals(leftButton)) {
			event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
			webview.onKeyDown(event.KEYCODE_DPAD_LEFT,event);
			event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT);
			webview.onKeyUp(event.KEYCODE_DPAD_LEFT,event);
		}
	}
	
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    	Log.i(this.LOGTAG, "onKeyDown: key: " + keyCode);
        boolean handled =  false;
        if (!handled) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_SPACE:
                    if (event.isShiftPressed()) {
                        pageUp();
                    } else {
                        pageDown();
                    }
                    handled = true;
                    break;

                default:
                    break;
            }
        }
        
        return handled || super.onKeyDown(keyCode, event);
    }
	
	protected void  displayDialog(int cmd) {
		if( m_Dialog == null)
			m_Dialog = new Dialog(this,android.R.style.Theme_Panel);
		m_Dialog.setContentView(R.layout.textinput);
		m_Dialog.setCancelable(true);
		//m_Dialog.setCanceledOnTouchOutside(true);
		TextView txt = (TextView)m_Dialog.findViewById(R.id.TextView01);
		EditText url = (EditText)m_Dialog.findViewById(R.id.EditText01);
		if( cmd == LOAD_URL) {
			txt.setText(R.string.url);
			if (this.lastNavigatedUrl != null && !this.lastNavigatedUrl.equalsIgnoreCase("")) {
				url.setText(lastNavigatedUrl);
			}
			else {
				url.setText("http://");
			}		
		} else if( cmd == FIND_STRING ){ 
			txt.setText(R.string.find);
			webview.findAll("");
			url.setText("");
		} else if( cmd == SETTINGS) {
			txt.setText(R.string.homepage);
			url.setText(m_HomePage);
		}
		url.requestFocus();
		url.setOnKeyListener( m_TextListener);
		m_Cmd=cmd;
		// This hack for displaying soft keyboard - not working if I use dialog/url. - don't know why
		InputMethodManager imm =
        	(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(webview,InputMethodManager.SHOW_FORCED);
		m_Dialog.show();
		
	}
	protected void processCmd(final String text) { 
		try {
			m_Dialog.dismiss();
			if( text == null) return;
			if( m_Cmd ==LOAD_URL) {
				Runnable run1 = new Runnable() {
					public void run() {
						connectionLock();
						webview.loadUrl(text);
					}
				};
				(new Thread(run1)).start();
				this.lastNavigatedUrl = text;
			} else if( m_Cmd == FIND_STRING){
				webview.findAll(text);
			} else if( m_Cmd == SETTINGS) {
				Editor e = getPreferences(this.MODE_PRIVATE).edit();
				e.putString("HOME_PAGE", text);
				e.commit();
				m_HomePage=text;
			}
		} catch(Exception ex) {
			Log.e(this.LOGTAG, "process Cmd exception", ex);
		} finally {
			m_Processing=false;
		}
	}
	
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		Log.i(LOGTAG, "onItemClick: item click: " + position);
		if( parent.equals(sublist)) {
			updateTextSize(position);
			return;
		}
		if (position >= LOAD_URL & !m_Processing) {            
			m_Processing=true;
			if( position ==GO_HOME) {
				lastNavigatedUrl = m_HomePage;
				Runnable run1 = new Runnable() {
					public void run() {
						connectionLock();
						webview.loadUrl(m_HomePage);
					}
				};
				(new Thread(run1)).start();
				m_Processing=false;
			} else if( position == SOFT_KEYBOARD){
				InputMethodManager imm =
			        	(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
			   imm.showSoftInput(webview,InputMethodManager.SHOW_FORCED);
			   m_Processing=false;
			} else if( position == CLOSE) {
				m_Processing=false;
				goBack();
			} else if( position == SAVE_PAGE) {
				downloadLink(webview.getUrl());
				m_Processing=false;
			} else if( position == TEXT_SIZE) {
				//sublist.setVisibility(View.VISIBLE);
				//lview.setVisibility(View.INVISIBLE);
				m_ViewAnimator.showNext();
				m_Processing=false;
			} else {
				displayDialog(position);
			}
		} 
		
	}
	public void downloadLink(final String url) {
		Runnable thrd = new Runnable() {
			public void run() {
				m_DownloadManager.onDownloadStartNoStream(url,null,null,null,0);
			}
		};
		(new Thread(thrd)).start();
	}
	// from kbs - trook.projectsource code.
	private final void pageUp()
	{
		if (webview != null) {
		int cury = webview.getScrollY();
		if (cury == 0) { return; }
		int newy = cury - WEB_SCROLL_PX;
		if (newy < 0) { newy = 0; }
			webview.scrollTo(0, newy);
		}
	}
	private final void pageDown()
	{
		if (webview != null) {
			int cury = webview.getScrollY();
			int hmax = webview.getContentHeight() - WEB_SCROLL_PX;
			if (hmax < 0) { hmax = 0; }
			int newy = cury + WEB_SCROLL_PX;
			if (newy > hmax) { newy = hmax; }
			if (cury != newy) {
				webview.scrollTo(0, newy);
			}
		}
	}

	public boolean onKey(View view, int keyCode, KeyEvent event) {
        boolean handled =  false;
        if ( event.getAction() == KeyEvent.ACTION_DOWN)
        {
        	Log.i(LOGTAG, "key code: " + keyCode);
            switch (keyCode) {
	        	case KeyEvent.KEYCODE_B:
	                getTopWindow().zoomIn();
	                handled = true;
	                break;
	                
	            case KeyEvent.KEYCODE_S:
	                getTopWindow().zoomOut();
	                handled = true;
	                break;
            	case NOOK_PAGE_UP_KEY_LEFT:
            	case NOOK_PAGE_UP_KEY_RIGHT:
                    pageUp();
                    handled = true;
                    break;
                    
                case NOOK_PAGE_DOWN_KEY_LEFT:
                case NOOK_PAGE_DOWN_KEY_RIGHT:
                    pageDown();
                    handled = true;
                    break;

                default:
                    break;
            }
        } // ACTION down
        
        return handled ;
	}
}
class HelloWebViewClient extends WebViewClient {
	nookBrowser browser;
	public HelloWebViewClient(nookBrowser browser) {
		super();
		this.browser = browser;
	}
	@Override    
	public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
		
		if( url ==null || (url.indexOf("/") == -1 && url.indexOf(".") ==-1)) {
			return false;
		}
		Runnable run1 = new Runnable() {
			public void run() {
				browser.connectionLock();
				view.loadUrl(url);
			}
		};
		(new Thread(run1)).start();
//		browser.displayAlert("Loading", browser.getString(R.string.please_wait),1,
//					new AlertDialog.OnClickListener() {
//
//						public void onClick(DialogInterface dialog, int which) {
//							view.stopLoading();
//							browser.closeAlert();
//						}
//		},-1);
//		view.loadUrl(url);
		return true;
	}
}

class TextListener implements OnKeyListener {
	private nookBrowser browser;
	
	public TextListener(nookBrowser browser) {
		this.browser = browser;
	}
	public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
		if( keyEvent.getAction() == KeyEvent.ACTION_UP) {
			if( view instanceof EditText) {
				EditText editTxt = (EditText) view;
				//- no idea why - this is what is returned in the emulator when I press these keys
				if(keyCode == nookBrowser.SOFT_KEYBOARD_CLEAR ) { // Clear button?
					editTxt.setText("");
				} else if( keyCode ==  nookBrowser.SOFT_KEYBOARD_SUBMIT) { //Submit button?
					String text =editTxt.getText().toString();
					browser.processCmd(text);
				} else if( keyCode ==  nookBrowser.SOFT_KEYBOARD_CANCEL) { //Cancel button? 
					browser.processCmd(null);
				} 
			}
		}
		return false;
	}
}



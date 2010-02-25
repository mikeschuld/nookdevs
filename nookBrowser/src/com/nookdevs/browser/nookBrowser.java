/* 
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.nookdevs.browser;

import java.util.Arrays;
import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.nookdevs.common.IconArrayAdapter;
import com.nookdevs.common.nookBaseActivity;

public class nookBrowser extends nookBaseActivity implements OnClickListener, OnLongClickListener, OnItemClickListener,
    OnKeyListener, OnItemLongClickListener {
    
    protected WebView webview;
    private ListView lview, sublist;
    private ConnectivityManager.WakeLock lock = null;
    private Button goButton, backButton, upButton, downButton, rightButton, leftButton;
    private boolean m_Processing = false;
    protected static final String DEFAULT_HOME_PAGE = "http://m.npr.org";
    // stores the last url navigated
    private String lastNavigatedUrl = null;
    private int m_Cmd = -1;
    private TextListener m_TextListener = new TextListener(this);
    private Dialog m_Dialog;
    private DownloadManager m_DownloadManager = new DownloadManager(this);
    protected static final int LOAD_URL = 0;
    protected static final int SETTINGS = 1;
    protected static final int GO_HOME = 4;
    protected static final int FAVS = 2;
    protected static final int ADDFAVS = 10;
    protected static final int SOFT_KEYBOARD = 3;
    protected static final int MEDIA = 5;
    protected static final int FIND_STRING = 6;
    protected static final int SAVE_PAGE = 7;
    protected static final int CLOSE = 8;
    protected static final int READABILITY = 0;
    protected static final int TEXT_SIZE = 1;
    protected static final int ZOOM_IN = 2;
    protected static final int ZOOM_OUT = 3;
    protected static final int HOME_PAGE = 4;
    protected static final int USER_AGENT = 5;
    protected static final int SCREEN = 6;
    protected static final int ZOOM = 7;
    protected static final int OFFLINE = 8;
    private static final int WEB_SCROLL_PX = 750;
    public static final int CONNECTION_TIMEOUT = 600000;
    private ViewAnimator m_ViewAnimator;
    private String m_HomePage;
    private ProgressBar m_ProgressBar;
    Handler m_Handler = new Handler();
    String m_UserAgentStr = null;
    private static final String DESKTOP_USER_AGENT = "Mozilla/6.0";
    private static String m_DefaultUserAgentStr = "";
    private boolean m_OfflineBrowsing = false;
    int[] icons = {
        -1, R.drawable.submenu, R.drawable.submenu, -1, -1, -1, -1, -1, -1, -1, -1
    };
    int[] subicons = {
        -1, R.drawable.submenu, -1, -1, -1, R.drawable.submenu, R.drawable.submenu, -1, -1, -1
    };
    int[] subicons2 = {
        -1, -1, -1, -1, -1, -1, -1
    };
    int[] subicons3 = {
        -1, -1, -1
    };
    IconArrayAdapter<CharSequence> m_SubListAdapter1 = null;
    IconArrayAdapter<CharSequence> m_SubListAdapter2 = null;
    ArrayAdapter<CharSequence> m_SubListAdapter3 = null;
    IconArrayAdapter<CharSequence> m_SubListAdapter4 = null;
    IconArrayAdapter<CharSequence> m_SubListAdapter5 = null;
    
    boolean m_BuiltInZoom = true;
    int m_SubMenuType = 0;
    CharSequence[] m_TextSizes = null;
    int m_TextSize = 3; // larger
    FavsDB m_FavsDB = null;
    VideoView m_Player;
    MediaListener m_MediaListener = null;
    Button closeBtn;
    Button volumeUp;
    Button volumeDown;
    private boolean m_paramPassed = false;
    private WebView webview_eink;
    private WebView webview_touchscreen;
    private int m_ScreenChoice = 1;
    private ViewAnimator m_WebViewAnimator;
    Button switchView;
    Button switchData;
    boolean m_einkToTouchscreen = false;
    boolean m_Readability = false;
    private String m_ReadabilityUrl =
        "javascript:(function(){readStyle='style-ebook';readSize='size-medium';readMargin='margin-x-narrow';_readability_script=document.createElement('SCRIPT');_readability_script.type='text/javascript';_readability_script.src='http://lab.arc90.com/experiments/readability/js/readability.js?x='+(Math.random());document.getElementsByTagName('head')[0].appendChild(_readability_script);_readability_css=document.createElement('LINK');_readability_css.rel='stylesheet';_readability_css.href='http://lab.arc90.com/experiments/readability/css/readability.css';_readability_css.type='text/css';_readability_css.media='all';document.getElementsByTagName('head')[0].appendChild(_readability_css);_readability_print_css=document.createElement('LINK');_readability_print_css.rel='stylesheet';_readability_print_css.href='http://lab.arc90.com/experiments/readability/css/readability-print.css';_readability_print_css.media='print';_readability_print_css.type='text/css';document.getElementsByTagName('head')[0].appendChild(_readability_print_css);})();";
    private String m_ReadabilityOrgUrl = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "nookBrowser";
        NAME = "Web";
        
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);
        lview = (ListView) findViewById(R.id.list);
        CharSequence[] menuitems = getResources().getTextArray(R.array.webmenu);
        List<CharSequence> menuitemsList = Arrays.asList(menuitems);
        IconArrayAdapter<CharSequence> adapter =
            new IconArrayAdapter<CharSequence>(lview.getContext(), R.layout.listitem, menuitemsList, icons);
        adapter.setImageField(R.id.ListImageView);
        adapter.setTextField(R.id.ListTextView);
        lview.setAdapter(adapter);
        lview.setOnItemClickListener(this);
        
        sublist = (ListView) findViewById(R.id.sublist);
        sublist.setOnItemClickListener(this);
        webview_touchscreen = (WebView) findViewById(R.id.webview2);
        webview = (WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setClickable(false);
        webview_touchscreen.setClickable(true);
        webview_touchscreen.getSettings().setJavaScriptEnabled(true);
        webview_touchscreen.getSettings().setBuiltInZoomControls(m_BuiltInZoom);
        if (m_OfflineBrowsing) {
            webview_touchscreen.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
            webview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        }
        menuitems = getResources().getTextArray(R.array.submenu1);
        menuitemsList = Arrays.asList(menuitems);
        m_SubListAdapter1 = new IconArrayAdapter<CharSequence>(this, R.layout.listitem, menuitemsList, subicons);
        m_SubListAdapter1.setImageField(R.id.ListImageView);
        m_SubListAdapter1.setTextField(R.id.ListTextView);
        m_SubListAdapter1.setSubTextField(R.id.ListSubTextView);
        sublist.setAdapter(m_SubListAdapter1);
        
        m_TextSizes = getResources().getTextArray(R.array.submenu2);
        menuitemsList = Arrays.asList(m_TextSizes);
        m_SubListAdapter2 = new IconArrayAdapter<CharSequence>(this, R.layout.listitem, menuitemsList, subicons2);
        m_SubListAdapter2.setImageField(R.id.ListImageView);
        m_SubListAdapter2.setTextField(R.id.ListTextView);
        
        m_SubListAdapter3 = new ArrayAdapter<CharSequence>(this, R.layout.listitem2, m_FavsDB.getNames());
        menuitems = getResources().getTextArray(R.array.useragent);
        menuitemsList = Arrays.asList(menuitems);
        m_SubListAdapter4 = new IconArrayAdapter<CharSequence>(this, R.layout.listitem, menuitemsList, subicons2);
        m_SubListAdapter4.setImageField(R.id.ListImageView);
        m_SubListAdapter4.setTextField(R.id.ListTextView);
        sublist.setOnItemLongClickListener(this);
        
        menuitems = getResources().getTextArray(R.array.screen);
        menuitemsList = Arrays.asList(menuitems);
        m_SubListAdapter5 = new IconArrayAdapter<CharSequence>(this, R.layout.listitem, menuitemsList, subicons3);
        m_SubListAdapter5.setImageField(R.id.ListImageView);
        m_SubListAdapter5.setTextField(R.id.ListTextView);
        
        m_ViewAnimator = (ViewAnimator) findViewById(R.id.listviewanim);
        m_ViewAnimator.setInAnimation(this, R.anim.fromright);
        m_ViewAnimator.setAnimateFirstView(true);
        ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        lock = cmgr.newWakeLock(1, "nookBrowser" + hashCode());
        webview.setWebViewClient(new HelloWebViewClient(this));
        m_ProgressBar = (ProgressBar) findViewById(R.id.progress_horizontal);
        m_ProgressBar.setVisibility(View.INVISIBLE);
        m_Player = (VideoView) findViewById(R.id.surface);
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                m_ProgressBar.setProgress(progress);
                if (progress == 100) {
                    m_ProgressBar.setVisibility(View.INVISIBLE);
                    m_ProgressBar.setProgress(0);
                } else {
                    m_ProgressBar.setVisibility(View.VISIBLE);
                    m_ProgressBar.setProgress(progress);
                }
            }
        });
        webview_touchscreen.setWebViewClient(new HelloWebViewClient(this));
        webview_touchscreen.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                m_ProgressBar.setProgress(progress);
                if (progress == 100) {
                    m_ProgressBar.setVisibility(View.INVISIBLE);
                    m_ProgressBar.setProgress(0);
                } else {
                    m_ProgressBar.setVisibility(View.VISIBLE);
                    m_ProgressBar.setProgress(progress);
                }
            }
            
        });
        m_Player.setMediaController(new MediaController(this, true));
        m_DefaultUserAgentStr = webview.getSettings().getUserAgentString();
        if (m_UserAgentStr == null) {
            m_UserAgentStr = m_DefaultUserAgentStr;
        }
        webview.getSettings().setUserAgentString(m_UserAgentStr);
        m_MediaListener = new MediaListener();
        m_Player.setOnErrorListener(m_MediaListener);
        m_Player.setOnCompletionListener(m_MediaListener);
        goButton = (Button) findViewById(R.id.go);
        backButton = (Button) findViewById(R.id.back);
        upButton = (Button) findViewById(R.id.up);
        downButton = (Button) findViewById(R.id.down);
        rightButton = (Button) findViewById(R.id.right);
        leftButton = (Button) findViewById(R.id.left);
        closeBtn = (Button) findViewById(R.id.closemedia);
        volumeUp = (Button) findViewById(R.id.volumeup);
        volumeDown = (Button) findViewById(R.id.volumedown);
        switchView = (Button) findViewById(R.id.switchview);
        switchData = (Button) findViewById(R.id.switchdata);
        webview_eink = webview;
        addListeners();
        updateTextSize(m_TextSize, true);
        Uri data = getIntent().getData();
        m_WebViewAnimator = (ViewAnimator) findViewById(R.id.webviewanim);
        
        if (data != null) {
            lastNavigatedUrl = data.toString();
            m_paramPassed = true;
        }
        if (m_paramPassed) {
            m_ScreenChoice = 0;
        }
        if (m_ScreenChoice >= 1) {
            webview = webview_touchscreen;
            m_WebViewAnimator.showNext();
        }
        try {
            String url = null;
            if (lastNavigatedUrl != null && !lastNavigatedUrl.equals("")) {
                url = lastNavigatedUrl;
            } else {
                url = m_HomePage;
            }
            waitForConnection(url);
        } catch (Exception ex) {
            Log.e(LOGTAG, "exception on Resume ", ex);
        }
        
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
        closeBtn.setOnClickListener(this);
        volumeUp.setOnClickListener(this);
        volumeDown.setOnClickListener(this);
        switchData.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (m_WebViewAnimator.getCurrentView().equals(webview_touchscreen)) {
                    if (webview_touchscreen.getUrl() != null) {
                        m_einkToTouchscreen = false;
                        waitForConnection("sync");
                        // webview_eink.loadUrl(webview_touchscreen.getUrl());
                    } else {
                        webview_eink.clearView();
                    }
                    
                } else {
                    if (webview_eink.getUrl() != null) {
                        m_einkToTouchscreen = true;
                        waitForConnection("sync");
                        // webview_touchscreen.loadUrl(webview_eink.getUrl());
                    } else {
                        webview_touchscreen.clearView();
                    }
                    m_WebViewAnimator.showNext();
                }
                
            }
        });
        switchView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                m_WebViewAnimator.showNext();
            }
        });
        webview_eink.setDownloadListener(m_DownloadManager);
        webview_touchscreen.setDownloadListener(m_DownloadManager);
        webview.setOnKeyListener(this);
    }
    
    // Helper method for getting the top window.
    public WebView getTopWindow() {
        return webview;
    }
    
    @Override
    protected void readSettings() {
        try {
            if (!m_FirstTime) { return; }
            SharedPreferences p = getPreferences(MODE_PRIVATE);
            m_TextSize = p.getInt("TEXT_SIZE", m_TextSize);
            m_UserAgentStr = p.getString("USER_AGENT", null);
            m_ScreenChoice = p.getInt("SCREEN", m_ScreenChoice);
            m_BuiltInZoom = p.getBoolean("ZOOM", m_BuiltInZoom);
            m_OfflineBrowsing = p.getBoolean("OFFLINE", m_OfflineBrowsing);
            if (m_HomePage == null) {
                m_HomePage = p.getString("HOME_PAGE", DEFAULT_HOME_PAGE);
            }
        } catch (Exception ex) {
            Log.e(LOGTAG, "preference exception: ", ex);
            m_HomePage = DEFAULT_HOME_PAGE;
        }
        if (m_FavsDB == null) {
            m_FavsDB = new FavsDB(this, null, 1);
        }
        super.readSettings();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        m_FavsDB.close();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        try {
            if (lock != null) {
                lock.release();
            }
        } catch (Exception ex) {
            
        }
    }
    
    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        webview.restoreState(bundle);
    }
    
    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        webview.saveState(bundle);
    }
    
    boolean m_CloseButtonDisplayed = false;
    private boolean m_UserClicked = false;
    
    @Override
    public String getPackageName() {
        // This is done to determine if a javascript listbox alert is displayed.
        // we don't have a callback from webview for this in 1.5 and the nook
        // specific
        // alert dialog code blocks touchscreen if there are no buttons.
        // I'm exiting the app when this happens. This will have to do until I
        // figure out a
        // way to close that alert dialog.
        if (!m_UserClicked
            && (m_WebViewAnimator == null || !m_WebViewAnimator.getCurrentView().equals(webview_touchscreen))) { return super
            .getPackageName(); }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (StackTraceElement item : trace) {
            String classname = item.getClassName();
            if (classname != null && classname.contains("InvokeListBox")) {
                m_UserClicked = false;
                m_Handler.post(new Runnable() {
                    public void run() {
                        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface v, int which) {
                                nookBrowser.this.finish();
                            }
                        };
                        displayAlert(getString(R.string.app_name), getString(R.string.not_supported), 2, listener, -1);
                    }
                });
                break;
            }
        }
        return super.getPackageName();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        m_ProgressBar.setVisibility(View.INVISIBLE);
        m_Player.bringToFront();
        m_Processing = false;
        try {
            if (m_Dialog != null) {
                m_Dialog.dismiss();
            }
            CookieManager.getInstance().removeExpiredCookie();
        } catch (Exception ex) {
            Log.e(LOGTAG, "exception in resume ...", ex);
        };
    }
    
    protected void waitForConnection(String url) {
        
        try {
            if (webview.equals(webview_eink)) {
                m_Readability = false;
                m_SubListAdapter1.setSubText(READABILITY, "Off");
            }
            if (url != null && url.startsWith("file://")) {
                if (m_ScreenChoice == 2) {
                    webview_eink.loadUrl(url);
                    webview_touchscreen.loadUrl(url);
                    m_Readability = false;
                    m_SubListAdapter1.setSubText(READABILITY, "Off");
                } else {
                    webview.loadUrl(url);
                }
                return;
            }
            if (getAirplaneMode()) {
                if (url == null) {
                    if (m_ScreenChoice == 2) {
                        webview_eink.goBack();
                        webview_touchscreen.goBack();
                    } else {
                        webview.goBack();
                    }
                }
                return;
            }
            ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (lock != null) {
                if (!m_OfflineBrowsing) {
                    lock.acquire(CONNECTION_TIMEOUT);
                }
            }
            // NetworkInfo info =
            // cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo info = cmgr.getActiveNetworkInfo();
            boolean connection = (info == null) ? false : info.isConnected();
            if (!m_OfflineBrowsing && !connection) {
                WifiTask wifi = new WifiTask();
                wifi.execute(url);
            } else {
                if (url == null) {
                    if (m_ScreenChoice == 2) {
                        webview_eink.goBack();
                        webview_touchscreen.goBack();
                    } else {
                        webview.goBack();
                    }
                } else if (url.equals("sync")) {
                    if (m_einkToTouchscreen) {
                        webview_touchscreen.loadUrl(webview_eink.getUrl());
                    } else {
                        webview_eink.loadUrl(webview_touchscreen.getUrl());
                        m_Readability = false;
                        m_SubListAdapter1.setSubText(READABILITY, "Off");
                    }
                } else if (url.startsWith("rtsp://") || url.startsWith("RTSP://") || url.endsWith(".mp3")
                    || url.endsWith(".mp4")) {
                    m_MediaListener.playMedia(url);
                } else {
                    if (m_ScreenChoice == 2) {
                        webview_eink.loadUrl(url);
                        webview_touchscreen.loadUrl(url);
                    } else {
                        webview.loadUrl(url);
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(LOGTAG, "Exception while checking the connection ", ex);
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
    
    private void updateTextSize(int size, boolean init) {
        WebSettings.TextSize textSize;
        if (!init && size == m_TextSize) {
            sublist.setAdapter(m_SubListAdapter1);
            m_ViewAnimator.showPrevious();
            m_ViewAnimator.showNext();
            m_SubMenuType = 1;
            return;
        }
        switch (size) {
            // smallest to largest
            case 0:
                textSize = WebSettings.TextSize.SMALLEST;
                break;
            case 1:
                textSize = WebSettings.TextSize.SMALLER;
                break;
            case 2:
                textSize = WebSettings.TextSize.NORMAL;
                break;
            case 3:
                textSize = WebSettings.TextSize.LARGER;
                break;
            case 4:
                textSize = WebSettings.TextSize.LARGEST;
                break;
            default:
                textSize = WebSettings.TextSize.NORMAL;
                
        }
        webview_eink.getSettings().setTextSize(textSize);
        webview_touchscreen.getSettings().setTextSize(textSize);
        if (init) { return; }
        subicons2[m_TextSize] = -1;
        m_TextSize = size;
        Editor e = getPreferences(MODE_PRIVATE).edit();
        e.putInt("TEXT_SIZE", m_TextSize);
        e.commit();
        m_SubListAdapter1.setSubText(TEXT_SIZE, m_TextSizes[m_TextSize].toString());
        m_SubListAdapter1.setSubText(ZOOM, m_BuiltInZoom ? "On" : "Off");
        m_SubListAdapter1.setSubText(READABILITY, m_Readability ? "On" : "Off");
        m_SubListAdapter1.setSubText(OFFLINE, m_OfflineBrowsing ? "Enabled" : "Disabled");
        sublist.setAdapter(m_SubListAdapter1);
        m_ViewAnimator.showPrevious();
        m_ViewAnimator.showNext();
        m_SubMenuType = 1;
        // sublist.setVisibility(View.INVISIBLE);
        // lview.setVisibility(View.VISIBLE);
    }
    
    private void processKey(View v) {
        KeyEvent event;
        m_UserClicked = false;
        if (v.equals(closeBtn)) {
            m_MediaListener.stop();
            m_ViewAnimator.showNext();
            m_SubMenuType = 0;
            return;
        }
        if (v.equals(volumeUp)) {
            AudioManager amgr = (AudioManager) getSystemService(AUDIO_SERVICE);
            amgr.adjustVolume(AudioManager.ADJUST_RAISE, 0);
        }
        if (v.equals(volumeDown)) {
            AudioManager amgr = (AudioManager) getSystemService(AUDIO_SERVICE);
            amgr.adjustVolume(AudioManager.ADJUST_LOWER, 0);
        }
        if (v.equals(goButton)) {
            m_UserClicked = true;
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
            webview_eink.onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, event);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER);
            webview_eink.onKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, event);
            webview_eink.dispatchKeyEvent(event);
        } else if (v.equals(backButton)) {
            if (m_ViewAnimator.getCurrentView().equals(sublist)) {
                m_ViewAnimator.showPrevious();
            } else if (m_SubMenuType == 4 && !m_WebViewAnimator.getCurrentView().equals(webview_touchscreen)) {
                m_ViewAnimator.showNext();
                m_SubMenuType = 1;
            } else if (webview.getProgress() != 100) {
                webview.stopLoading();
                m_ProgressBar.setVisibility(View.INVISIBLE);
            } else if (webview.canGoBack()) {
                waitForConnection(null);
            } else if (m_WebViewAnimator.getCurrentView().equals(webview_touchscreen)) {
                m_WebViewAnimator.showNext();
            } else if (m_SubMenuType == 4) {
                m_ViewAnimator.showNext();
                m_SubMenuType = 1;
            } else {
                if (m_paramPassed) {
                    finish();
                } else {
                    goHome();
                }
            }
        } else if (v.equals(upButton)) {
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);
            webview_eink.onKeyDown(KeyEvent.KEYCODE_DPAD_UP, event);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP);
            webview_eink.onKeyUp(KeyEvent.KEYCODE_DPAD_UP, event);
        } else if (v.equals(downButton)) {
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
            webview_eink.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, event);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN);
            webview_eink.onKeyUp(KeyEvent.KEYCODE_DPAD_DOWN, event);
            
        } else if (v.equals(rightButton)) {
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
            webview_eink.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, event);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT);
            webview_eink.onKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT, event);
            
        } else if (v.equals(leftButton)) {
            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
            webview_eink.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, event);
            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT);
            webview_eink.onKeyUp(KeyEvent.KEYCODE_DPAD_LEFT, event);
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
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
    
    protected void displayDialog(int cmd) {
        if (m_Dialog == null) {
            m_Dialog = new Dialog(this, android.R.style.Theme_Panel);
        }
        m_Dialog.setContentView(R.layout.textinput);
        m_Dialog.setCancelable(true);
        // m_Dialog.setCanceledOnTouchOutside(true);
        TextView txt = (TextView) m_Dialog.findViewById(R.id.TextView01);
        EditText url = (EditText) m_Dialog.findViewById(R.id.EditText01);
        if (cmd == LOAD_URL) {
            txt.setText(R.string.url);
            if (lastNavigatedUrl != null && !lastNavigatedUrl.equalsIgnoreCase("")) {
                url.setText(lastNavigatedUrl);
            } else {
                url.setText("");
            }
        } else if (cmd == FIND_STRING) {
            txt.setText(R.string.find);
            webview.findAll("");
            url.setText("");
        } else if (cmd == SETTINGS) {
            txt.setText(R.string.homepage);
            url.setText(m_HomePage);
        }
        url.requestFocus();
        url.setOnKeyListener(m_TextListener);
        m_Cmd = cmd;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        m_Dialog.show();
    }
    
    protected void processCmd(String text) {
        try {
            m_Dialog.dismiss();
            if (text == null) { return; }
            if (m_Cmd == LOAD_URL) {
                if (text.indexOf("://") == -1) {
                    text = URLUtil.guessUrl(text);
                }
                waitForConnection(text);
                lastNavigatedUrl = text;
                if (webview.equals(webview_touchscreen)) {
                    m_WebViewAnimator.showNext();
                }
            } else if (m_Cmd == FIND_STRING) {
                webview_eink.findAll(text);
            } else if (m_Cmd == SETTINGS) {
                if (text.indexOf("://") == -1 && !text.trim().equals("")) {
                    text = URLUtil.guessUrl(text);
                }
                Editor e = getPreferences(MODE_PRIVATE).edit();
                e.putString("HOME_PAGE", text);
                e.commit();
                m_HomePage = text;
            }
        } catch (Exception ex) {
            Log.e(LOGTAG, "process Cmd exception", ex);
        } finally {
            m_Processing = false;
        }
    }
    
    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
        if (parent.equals(sublist) && m_SubMenuType == 3) {
            try {
                if (position > 0) {
                    m_FavsDB.deleteFav(position);
                }
            } catch (Exception ex) {
            }
            m_SubListAdapter3 = new ArrayAdapter<CharSequence>(this, R.layout.listitem2, m_FavsDB.getNames());
            m_SubMenuType = 1;
            m_ViewAnimator.showPrevious();
        }
        return true;
    }
    
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        m_UserClicked = false;
        if (parent.equals(sublist)) {
            if (m_SubMenuType == 6) {
                subicons3[m_ScreenChoice] = -1;
                if (m_ScreenChoice != position) {
                    m_ScreenChoice = position;
                    Editor e = getPreferences(MODE_PRIVATE).edit();
                    e.putInt("SCREEN", m_ScreenChoice);
                    e.commit();
                    if (m_ScreenChoice == 0) {
                        webview = webview_eink;
                    } else {
                        webview = webview_touchscreen;
                    }
                }
                sublist.setAdapter(m_SubListAdapter1);
                m_ViewAnimator.showPrevious();
                m_ViewAnimator.showNext();
                m_SubMenuType = 1;
                return;
                
            }
            if (m_SubMenuType == 5) {
                if (position == 0) {
                    webview.getSettings().setUserAgentString(m_DefaultUserAgentStr);
                    m_UserAgentStr = m_DefaultUserAgentStr;
                    
                } else {
                    webview.getSettings().setUserAgentString(DESKTOP_USER_AGENT);
                    m_UserAgentStr = DESKTOP_USER_AGENT;
                }
                subicons2[0] = -1;
                subicons2[1] = -1;
                Editor e = getPreferences(MODE_PRIVATE).edit();
                e.putString("USER_AGENT", m_UserAgentStr);
                e.commit();
                sublist.setAdapter(m_SubListAdapter1);
                m_ViewAnimator.showPrevious();
                m_ViewAnimator.showNext();
                m_SubMenuType = 1;
                return;
                
            }
            if (m_SubMenuType == 2) {
                updateTextSize(position, false);
                m_SubMenuType = 1;
                return;
            } else if (m_SubMenuType == 3) { // FAVS
                if (position == 0) { // Add
                    if (webview.getUrl() != null && !webview.getUrl().trim().equals("")) {
                        m_FavsDB.addFav(webview.getTitle(), webview.getUrl());
                        m_SubListAdapter3 =
                            new ArrayAdapter<CharSequence>(this, R.layout.listitem2, m_FavsDB.getNames());
                    }
                } else {
                    String url = (String) m_FavsDB.getValues().get(position - 1);
                    waitForConnection(url);
                    if (webview.equals(webview_touchscreen)) {
                        m_WebViewAnimator.showNext();
                    }
                }
                m_SubMenuType = 1;
                m_ViewAnimator.showPrevious();
                return;
            }
            // check submenu items
            switch (position) {
                case TEXT_SIZE:
                    subicons2[0] = -1;
                    subicons2[1] = -1;
                    subicons2[m_TextSize] = R.drawable.check;
                    sublist.setAdapter(m_SubListAdapter2);
                    m_ViewAnimator.showPrevious();
                    m_ViewAnimator.showNext();
                    m_SubMenuType = 2;
                    break;
                case ZOOM_IN:
                    webview_eink.zoomIn();
                    webview_touchscreen.zoomIn();
                    break;
                case ZOOM_OUT:
                    webview_eink.zoomOut();
                    webview_touchscreen.zoomOut();
                    break;
                case HOME_PAGE:
                    displayDialog(SETTINGS);
                    break;
                case USER_AGENT:
                    if (m_UserAgentStr == null || m_UserAgentStr.equals(m_DefaultUserAgentStr)) {
                        subicons2[0] = R.drawable.check;
                    } else {
                        subicons2[1] = R.drawable.check;
                    }
                    sublist.setAdapter(m_SubListAdapter4);
                    m_ViewAnimator.showPrevious();
                    m_ViewAnimator.showNext();
                    m_SubMenuType = 5;
                    break;
                case SCREEN:
                    subicons3[m_ScreenChoice] = R.drawable.check;
                    sublist.setAdapter(m_SubListAdapter5);
                    m_ViewAnimator.showPrevious();
                    m_ViewAnimator.showNext();
                    m_SubMenuType = 6;
                case ZOOM:
                    m_BuiltInZoom = !m_BuiltInZoom;
                    m_SubListAdapter1.setSubText(ZOOM, m_BuiltInZoom ? "On" : "Off");
                    webview_touchscreen.getSettings().setBuiltInZoomControls(m_BuiltInZoom);
                    Editor e = getPreferences(MODE_PRIVATE).edit();
                    e.putBoolean("ZOOM", m_BuiltInZoom);
                    e.commit();
                    break;
                case READABILITY:
                    m_Readability = !m_Readability;
                    m_SubListAdapter1.setSubText(READABILITY, m_Readability ? "On" : "Off");
                    if (!m_Readability) {
                        if (m_ReadabilityOrgUrl != null) {
                            webview_eink.loadUrl(m_ReadabilityOrgUrl);
                        }
                    } else {
                        m_ReadabilityOrgUrl = webview_eink.getUrl();
                        webview_eink.loadUrl(m_ReadabilityUrl);
                    }
                    // webview_eink.loadUrl("javascript:readable()");
                    break;
                case OFFLINE:
                    m_OfflineBrowsing = !m_OfflineBrowsing;
                    m_SubListAdapter1.setSubText(OFFLINE, m_OfflineBrowsing ? "Enabled" : "Disabled");
                    if (m_OfflineBrowsing) {
                        webview_touchscreen.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                        webview_eink.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                    } else {
                        webview_touchscreen.getSettings().setCacheMode(WebSettings.LOAD_NORMAL);
                        webview_eink.getSettings().setCacheMode(WebSettings.LOAD_NORMAL);
                    }
                    e = getPreferences(MODE_PRIVATE).edit();
                    e.putBoolean("OFFLINE", m_OfflineBrowsing);
                    e.commit();
            }
            return;
        }
        if (position == MEDIA) {
            m_ViewAnimator.showPrevious();
            m_SubMenuType = 4;
            return;
        }
        if (position >= LOAD_URL & !m_Processing) {
            m_Processing = true;
            if (position == GO_HOME) {
                lastNavigatedUrl = m_HomePage;
                waitForConnection(m_HomePage);
                if (webview.equals(webview_touchscreen)) {
                    m_WebViewAnimator.showNext();
                }
                m_Processing = false;
            } else if (position == SOFT_KEYBOARD) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                m_Processing = false;
            } else if (position == CLOSE) {
                m_Processing = false;
                finish();
            } else if (position == SAVE_PAGE) {
                downloadLink(webview.getUrl());
                m_Processing = false;
            } else if (position == SETTINGS) {
                sublist.setAdapter(m_SubListAdapter1);
                m_SubListAdapter1.setSubText(TEXT_SIZE, m_TextSizes[m_TextSize].toString());
                m_SubListAdapter1.setSubText(ZOOM, m_BuiltInZoom ? "On" : "Off");
                m_SubListAdapter1.setSubText(OFFLINE, m_OfflineBrowsing ? "Enabled" : "Disabled");
                m_SubListAdapter1.setSubText(READABILITY, m_Readability ? "On" : "Off");
                m_ViewAnimator.showNext();
                m_SubMenuType = 1;
                m_Processing = false;
            } else if (position == FAVS) {
                sublist.setAdapter(m_SubListAdapter3);
                m_ViewAnimator.showNext();
                m_SubMenuType = 3;
                m_Processing = false;
            } else {
                displayDialog(position);
            }
        }
        
    }
    
    public void downloadLink(final String url) {
        Runnable thrd = new Runnable() {
            public void run() {
                m_DownloadManager.onDownloadStartNoStream(url, null, null, null, 0);
            }
        };
        (new Thread(thrd)).start();
    }
    
    public void playMedia(String url) {
        if (url != null) {
            m_MediaListener.playMedia(url);
        }
    }
    
    // from kbs - trook.projectsource code.
    private final void pageUp() {
        if (webview_eink != null) {
            int cury = webview_eink.getScrollY();
            if (cury == 0) { return; }
            int newy = cury - WEB_SCROLL_PX;
            if (newy < 0) {
                newy = 0;
            }
            webview_eink.scrollTo(0, newy);
        }
    }
    
    private final void pageDown() {
        if (webview_eink != null) {
            int cury = webview_eink.getScrollY();
            int hmax = webview_eink.getContentHeight() - 200;
            if (hmax < 0) {
                hmax = 0;
            }
            int newy = cury + WEB_SCROLL_PX;
            if (newy > hmax) {
                newy = hmax;
            }
            if (cury != newy) {
                webview_eink.scrollTo(0, newy);
            }
        }
    }
    
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case NOOK_PAGE_UP_KEY_LEFT:
                case NOOK_PAGE_UP_KEY_RIGHT:
                case NOOK_PAGE_UP_SWIPE:
                    pageUp();
                    handled = true;
                    break;
                
                case NOOK_PAGE_DOWN_KEY_LEFT:
                case NOOK_PAGE_DOWN_KEY_RIGHT:
                case NOOK_PAGE_DOWN_SWIPE:
                    pageDown();
                    handled = true;
                    break;
                
                default:
                    break;
            }
        }
        return handled;
    }
    
    class WifiTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            displayAlert(getString(R.string.start_wifi), getString(R.string.please_wait), 1, null, -1);
        }
        
        @Override
        protected String doInBackground(String... params) {
            ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            // NetworkInfo info =
            // cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo info = cmgr.getActiveNetworkInfo();
            boolean connection = (info == null) ? false : info.isConnected();
            int attempts = 1;
            while (!connection && attempts < 10) {
                try {
                    Thread.sleep(3000);
                } catch (Exception ex) {
                    
                }
                // info = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                info = cmgr.getActiveNetworkInfo();
                connection = (info == null) ? false : info.isConnected();
                attempts++;
            }
            return params[0];
        }
        
        @Override
        protected void onPostExecute(String result) {
            closeAlert();
            if (result == null) {
                if (m_ScreenChoice == 2) {
                    webview_eink.goBack();
                    webview_touchscreen.goBack();
                    m_Readability = false;
                    m_SubListAdapter1.setSubText(READABILITY, "Off");
                } else {
                    webview.goBack();
                }
            } else if (result.equals("")) {
                if (m_einkToTouchscreen) {
                    webview_touchscreen.loadUrl(webview_eink.getUrl());
                } else {
                    webview_eink.loadUrl(webview_touchscreen.getUrl());
                    m_Readability = false;
                    m_SubListAdapter1.setSubText(READABILITY, "Off");
                }
                return;
            } else {
                if (result.startsWith("rtsp://") || result.startsWith("RTSP://") || result.endsWith(".mp3")
                    || result.endsWith("mp4")) {
                    m_MediaListener.playMedia(result);
                    return;
                }
                if (m_ScreenChoice == 2) {
                    webview_eink.loadUrl(result);
                    webview_touchscreen.loadUrl(result);
                    m_Readability = false;
                    m_SubListAdapter1.setSubText(READABILITY, "Off");
                } else {
                    webview.loadUrl(result);
                }
            }
        }
    }
    
    class MediaListener implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(LOGTAG, "Error playing Media - " + " what = " + what + "  extra =" + extra);
            // m_Player.stopPlayback();
            Toast.makeText(nookBrowser.this, R.string.playback_error, Toast.LENGTH_LONG);
            onCompletion(mp);
            return true;
        }
        
        public void onCompletion(MediaPlayer mp) {
            if (m_SubMenuType == 4) {
                m_ViewAnimator.showNext();
                m_SubMenuType = 1;
                webview.requestFocus();
            }
            AudioManager amgr = (AudioManager) getSystemService(AUDIO_SERVICE);
            amgr.setStreamSolo(AudioManager.STREAM_MUSIC, false);
            try {
                if (lock.isHeld()) {
                    lock.release();
                }
                if (!m_OfflineBrowsing) {
                    lock.acquire(nookBrowser.CONNECTION_TIMEOUT);
                }
            } catch (Exception ex) {
                Log.e(LOGTAG, "Exception in onCompletion - Media", ex);
            }
        }
        
        protected void stop() {
            if (m_Player.isPlaying()) {
                m_Player.stopPlayback();
                if (lock.isHeld()) {
                    lock.release();
                }
                AudioManager amgr = (AudioManager) getSystemService(AUDIO_SERVICE);
                amgr.setStreamSolo(AudioManager.STREAM_MUSIC, false);
                if (!m_OfflineBrowsing) {
                    lock.acquire(CONNECTION_TIMEOUT);
                }
            }
        }
        
        protected void playMedia(String url) {
            if (m_WebViewAnimator.getCurrentView().equals(webview_touchscreen)) {
                m_WebViewAnimator.showNext();
            }
            if (m_ViewAnimator.getCurrentView().equals(sublist)) {
                m_ViewAnimator.showNext();
            } else if (m_SubMenuType != 4) {
                m_ViewAnimator.showPrevious();
            }
            m_SubMenuType = 4;
            m_Player.bringToFront();
            if (m_Player.isPlaying()) {
                m_Player.stopPlayback();
            }
            if (!m_OfflineBrowsing) {
                lock.acquire();
            }
            m_Player.setVideoPath(url);
            AudioManager amgr = (AudioManager) getSystemService(AUDIO_SERVICE);
            amgr.setStreamSolo(AudioManager.STREAM_MUSIC, true);
            m_Player.start();
        }
        
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
        if (url == null || (url.indexOf("//") == -1 && url.indexOf("javascript") == -1)) { return false; }
        browser.webview = view;
        browser.waitForConnection(url);
        return true;
    }
}

class TextListener implements OnKeyListener {
    private nookBrowser browser;
    
    public TextListener(nookBrowser browser) {
        this.browser = browser;
    }
    
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
            if (view instanceof EditText) {
                EditText editTxt = (EditText) view;
                // - no idea why - this is what is returned in the emulator when
                // I press these keys
                if (keyCode == nookBaseActivity.SOFT_KEYBOARD_CLEAR) { // Clear
                    // button?
                    editTxt.setText("");
                } else if (keyCode == nookBaseActivity.SOFT_KEYBOARD_SUBMIT) { // Submit
                    // button?
                    String text = editTxt.getText().toString();
                    browser.processCmd(text);
                } else if (keyCode == nookBaseActivity.SOFT_KEYBOARD_CANCEL) { // Cancel
                    // button?
                    browser.processCmd(null);
                }
            }
        }
        return false;
    }
}

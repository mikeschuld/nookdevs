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
package com.nookdevs.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.bravo.ecmscannerservice.IECMScannerService;
import com.bravo.ecmscannerservice.IECMScannerServiceCallback;
import com.bravo.ecmscannerservice.ScannedFile;
import com.nookdevs.common.CustomGallery;
import com.nookdevs.common.IconArrayAdapter;
import com.nookdevs.common.ImageAdapter;
import com.nookdevs.common.nookBaseActivity;

public class NookLibrary extends nookBaseActivity implements OnItemClickListener, OnLongClickListener, OnClickListener {
    private List<ScannedFile> m_Files = new ArrayList<ScannedFile>(200);
    public static final int MAX_FILES_PER_BATCH = 99999;
    private boolean m_SearchView = false;
    
    private Button backButton, upButton, downButton;
    private ImageButton goButton;
    protected static final int VIEW_DETAILS = 0;
    protected static final int SORT_BY = 1;
    protected static final int SEARCH = 2;
    protected static final int SHOW_COVERS = 3;
    protected static final int SHOW = 4;
    protected static final int AUTHORS = 5;
    protected static final int REFRESH = 6;
    protected static final int SHOW_ARCHIVED = 7;
    protected static final int CLOSE = 8;
    protected static final int LOCAL_BOOKS = 0;
    protected static final int BN_BOOKS = 1;
    protected static final int ALL_BOOKS = -1;
    protected static final int ADD_LIB_MENU = 10;
    protected int m_AddLibIndex = 2;
    protected static final int FICTIONWISE_BOOKS = 0;
    protected static final int SMASHWORDS = 1;
    private ConditionVariable m_LocalScanDone = new ConditionVariable();
    private ConditionVariable m_BNBooksLock = new ConditionVariable();
    private ConditionVariable m_FictionwiseLock = new ConditionVariable();
    private ConditionVariable m_SmashwordsLock = new ConditionVariable();
    private static final int WEB_SCROLL_PX = 750;
    private Toast m_Toast = null;
    private boolean m_ArchiveView = false;
    public static final String PREF_FILE = "NookLibrary";
    private int[] icons =
        {
            -1, R.drawable.submenu, R.drawable.search, R.drawable.covers, R.drawable.submenu, R.drawable.submenu,
            R.drawable.submenu, -1, -1, -1, -1, -1, -1, -1, -1
        };
    
    private int[] subicons = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
    };
    private int[] refreshicons = {
        -1, -1, -1
    };
    private ListView lview;
    private ListView submenu;
    private ViewAnimator animator;
    private int m_SubMenuType = -1;
    private PageViewHelper pageViewHelper;
    private Handler m_Handler = new Handler();
    private ImageAdapter m_IconAdapter = null;
    private CustomGallery m_IconGallery = null;
    private Button m_CloseBtn = null;
    private Button m_Archive = null;
    IconArrayAdapter<CharSequence> m_ListAdapter = null;
    IconArrayAdapter<CharSequence> m_SortAdapter = null;
    IconArrayAdapter<CharSequence> m_RefreshAdapter = null;
    IconArrayAdapter<CharSequence> m_LibsAdapter = null;
    ArrayAdapter<CharSequence> m_ShowAdapter = null;
    ArrayAdapter<String> m_AuthorAdapter = null;
    List<CharSequence> m_SortMenuValues = null;
    ConnectivityManager.WakeLock m_Lock;
    protected List<CharSequence> m_ShowValues = null;
    protected List<String> m_AuthorValues = null;
    int m_ShowIndex = 0;
    int m_AuthorIndex = 0;
    ImageButton m_CoverBtn = null;
    TextView m_Details = null;
    ScrollView m_DetailsScroll = null;
    ViewAnimator m_PageViewAnimator = null;
    TextView m_DetailsPage = null;
    private boolean m_ScanInProgress = false;
    private boolean m_RemoteScanDone = false;
    private OtherBooks m_OtherBooks;
    private BNBooks m_BNBooks;
    private FictionwiseBooks m_FictionwiseBooks = null;
    private Smashwords m_Smashwords = null;
    
    public Handler getHandler() {
        return m_Handler;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "nookLibrary";
        NAME = "My Books";
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        goButton = (ImageButton) findViewById(R.id.go);
        backButton = (Button) findViewById(R.id.back);
        upButton = (Button) findViewById(R.id.up);
        downButton = (Button) findViewById(R.id.down);
        m_Archive = (Button) findViewById(R.id.archive);
        m_Archive.setOnClickListener(new OnClickListener() {
            
            public void onClick(View arg0) {
                ScannedFile file = pageViewHelper.getCurrent();
                if (file.getStatus() == null || !file.getStatus().equals(BNBooks.ARCHIVED)) {
                    // archive book
                    m_Files.remove(file);
                    if (file.matchSubject(getString(R.string.fictionwise))) {
                        m_FictionwiseBooks.archiveBook(file, true);
                    } else if (file.matchSubject("B&N")) {
                        m_BNBooks.archiveBook(file, true);
                    } else if (file.matchSubject(getString(R.string.smashwords))) {
                        m_Smashwords.archiveBook(file, true);
                    } else {
                        // local
                        m_OtherBooks.archiveBook(file, true);
                    }
                } else {
                    m_Files.remove(file);
                    if (file.matchSubject(getString(R.string.fictionwise))) {
                        m_FictionwiseBooks.archiveBook(file, false);
                    } else if (file.matchSubject("B&N")) {
                        // unarchive B&N book
                        m_BNBooks.archiveBook(file, false);
                        
                    } else if (file.matchSubject(getString(R.string.smashwords))) {
                        m_Smashwords.archiveBook(file, false);
                    } else {
                        m_OtherBooks.archiveBook(file, false);
                    }
                }
                pageViewHelper.setFiles(m_Files);
                backButton.performClick();
            }
        });
        lview = (ListView) findViewById(R.id.list);
        submenu = (ListView) findViewById(R.id.sublist);
        animator = (ViewAnimator) findViewById(R.id.listviewanim);
        submenu.setOnItemClickListener(this);
        submenu.setOnItemLongClickListener(new OnItemLongClickListener() {
            
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (m_SubMenuType == REFRESH && arg2 >= 2 && arg2 != m_AddLibIndex) {
                    backButton.performClick();
                    CharSequence name = m_RefreshAdapter.getItem(arg2);
                    if (name.equals(getString(R.string.fictionwise))) {
                        m_FictionwiseBooks.deleteAll();
                    } else if (name.equals(getString(R.string.smashwords))) {
                        m_Smashwords.deleteAll();
                    }
                    m_LibsAdapter.add(name);
                    m_RefreshAdapter.remove(name);
                    m_AddLibIndex--;
                    queryFolders(LOCAL_BOOKS);
                    return true;
                }
                return false;
            }
            
        });
        CharSequence[] menuitems = getResources().getTextArray(R.array.mainmenu);
        List<CharSequence> menuitemsList = Arrays.asList(menuitems);
        m_ListAdapter = new IconArrayAdapter<CharSequence>(lview.getContext(), R.layout.listitem, menuitemsList, icons);
        m_ListAdapter.setImageField(R.id.ListImageView);
        m_ListAdapter.setTextField(R.id.ListTextView);
        m_ListAdapter.setSubTextField(R.id.ListSubTextView);
        menuitems = getResources().getTextArray(R.array.sortmenu);
        m_SortMenuValues = Arrays.asList(menuitems);
        m_SortAdapter =
            new IconArrayAdapter<CharSequence>(lview.getContext(), R.layout.listitem, m_SortMenuValues, subicons);
        m_SortAdapter.setImageField(R.id.ListImageView);
        m_SortAdapter.setTextField(R.id.ListTextView);
        menuitems = getResources().getTextArray(R.array.refreshmenu);
        m_FictionwiseBooks = new FictionwiseBooks(this);
        m_Smashwords = new Smashwords(this);
        ArrayList<CharSequence> list = new ArrayList<CharSequence>(5);
        list.addAll(Arrays.asList(menuitems));
        list.addAll(getAdditionalLibraries());
        m_RefreshAdapter =
            new IconArrayAdapter<CharSequence>(lview.getContext(), R.layout.listitem, list, refreshicons);
        m_RefreshAdapter.setImageField(R.id.ListImageView);
        m_RefreshAdapter.setTextField(R.id.ListTextView);
        WebView web = new WebView(this);
        String usera = web.getSettings().getUserAgentString();
        System.out.println("User agent String =" + usera);
        
        lview.setAdapter(m_ListAdapter);
        lview.setOnItemClickListener(this);
        goButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        upButton.setOnClickListener(this);
        downButton.setOnClickListener(this);
        upButton.setOnLongClickListener(this);
        downButton.setOnLongClickListener(this);
        m_IconGallery = ((CustomGallery) findViewById(R.id.icongallery));
        TypedArray a = this.obtainStyledAttributes(R.styleable.default_gallery);
        int backid = a.getResourceId(R.styleable.default_gallery_android_galleryItemBackground, 0);
        a.recycle();
        m_IconGallery.setVisibility(View.INVISIBLE);
        GalleryClickListener galleryListener = new GalleryClickListener();
        m_IconGallery.setOnItemClickListener(galleryListener);
        m_IconGallery.setAlwaysDrawnWithCacheEnabled(true);
        m_IconGallery.setCallbackDuringFling(false);
        m_CloseBtn = (Button) (findViewById(R.id.closeButton));
        m_CloseBtn.setVisibility(View.INVISIBLE);
        m_CloseBtn.setOnClickListener(this);
        m_IconAdapter = new ImageAdapter(this, null, null);
        m_IconAdapter.setBackgroundStyle(backid);
        m_IconAdapter.setDefault(R.drawable.no_cover);
        m_IconGallery.setOnItemSelectedListener(galleryListener);
        m_CoverBtn = (ImageButton) findViewById(R.id.cover);
        m_Details = (TextView) findViewById(R.id.details);
        m_DetailsScroll = (ScrollView) findViewById(R.id.detailsscroll);
        m_CoverBtn.setVisibility(View.INVISIBLE);
        m_CoverBtn.setOnClickListener(this);
        m_Details.setVisibility(View.INVISIBLE);
        m_DetailsScroll.setVisibility(View.INVISIBLE);
        m_PageViewAnimator = (ViewAnimator) findViewById(R.id.pageview);
        m_DetailsPage = (TextView) findViewById(R.id.pageview2);
        m_OtherBooks = new OtherBooks(this);
        m_BNBooks = new BNBooks(this);
        m_Archive.setVisibility(View.INVISIBLE);
        ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        m_Lock = cmgr.newWakeLock(1, "NookLibrary.scanTask" + hashCode());
        queryFolders();
    }
    
    private List<CharSequence> getAdditionalLibraries() {
        ArrayList<CharSequence> list = new ArrayList<CharSequence>(2);
        String fictionwise = getString(R.string.fictionwise);
        String smashwords = getString(R.string.smashwords);
        if (m_FictionwiseBooks.getUser()) {
            list.add(fictionwise);
        }
        if (m_Smashwords.getUser()) {
            list.add(smashwords);
        }
        list.add(getString(R.string.add_lib));
        m_AddLibIndex = list.size() + 1;
        ArrayList<CharSequence> libsList = new ArrayList<CharSequence>(2);
        if (!list.contains(fictionwise)) {
            libsList.add(fictionwise);
        }
        if (!list.contains(smashwords)) {
            libsList.add(smashwords);
        }
        m_LibsAdapter = new IconArrayAdapter<CharSequence>(lview.getContext(), R.layout.listitem, libsList, subicons);
        m_LibsAdapter.setImageField(R.id.ListImageView);
        m_LibsAdapter.setTextField(R.id.ListTextView);
        return list;
        
    }
    
    private List<ScannedFile> searchFiles(String keyword, List<ScannedFile> list) {
        List<ScannedFile> results = new ArrayList<ScannedFile>(m_Files.size());
        String key = keyword.toLowerCase();
        for (ScannedFile file : list) {
            if (file.getData().contains(key)) {
                results.add(file);
            }
        }
        return results;
    }
    
    private List<ScannedFile> filterFiles(String keyword, List<ScannedFile> list) {
        List<ScannedFile> results = new ArrayList<ScannedFile>(m_Files.size());
        for (ScannedFile file : list) {
            if (file.matchSubject(keyword)) {
                results.add(file);
            }
        }
        return results;
    }
    
    @Override
    public void readSettings() {
        int sortType = ScannedFile.SORT_BY_NAME;
        try {
            sortType = getSharedPreferences(PREF_FILE, MODE_PRIVATE).getInt("SORT_BY", sortType);
        } catch (Exception ex) {
            Log.e(LOGTAG, "preference exception: ", ex);
            
        }
        ScannedFile.setSortType(sortType);
        super.readSettings();
    }
    
    @Override
    public void onResume() {
        if (!m_FirstTime && ScannedFile.getSortType() == ScannedFile.SORT_BY_LATEST) {
            SortTask task = new SortTask(true);
            task.execute(ScannedFile.SORT_BY_LATEST);
        }
        super.onResume();
    }
    
    void updatePageView(List<ScannedFile> files) {
        if (files != null && files.size() > 0) {
            synchronized (m_Files) {
                m_Files.addAll(files);
            }
            updatePageView(false);
        }
    }
    
    private void updatePageView(boolean last) {
        List<String> tmpList = null;
        if (last) {
            Collections.sort(m_Files);
            tmpList = ScannedFile.getAvailableKeywords();
            m_ShowIndex = 0;
            m_AuthorIndex = 0;
            List<String> authList = ScannedFile.getAuthors();
            Comparator<String> c = new Comparator<String>() {
                public int compare(String object1, String object2) {
                    if (object1 != null) {
                        return object1.compareToIgnoreCase(object2);
                    } else {
                        return 1;
                    }
                }
            };
            Collections.sort(tmpList, c);
            Collections.sort(authList, c);
            m_ShowValues = new ArrayList<CharSequence>(tmpList.size() + 1);
            m_AuthorValues = new ArrayList<String>(authList.size() + 1);
        } else {
            m_ShowValues = new ArrayList<CharSequence>(1);
            m_AuthorValues = new ArrayList<String>(1);
        }
        m_ShowIndex = 0;
        m_AuthorIndex = 0;
        m_ShowValues.add("All");
        m_AuthorValues.add("All");
        if (ScannedFile.m_StandardKeywords != null) {
            m_ShowValues.addAll(ScannedFile.m_StandardKeywords);
        }
        if (last) {
            m_ShowValues.addAll(tmpList);
        }
        m_ShowAdapter = new ArrayAdapter<CharSequence>(lview.getContext(), R.layout.listitem2, m_ShowValues);
        m_AuthorAdapter = new ArrayAdapter<String>(lview.getContext(), R.layout.listitem2, m_AuthorValues);
        Runnable thrd = new Runnable() {
            public void run() {
                closeAlert();
                LinearLayout pageview = (LinearLayout) NookLibrary.this.findViewById(R.id.pageview1);
                if (pageViewHelper == null) {
                    pageViewHelper = new PageViewHelper(NookLibrary.this, pageview, m_Files);
                } else {
                    pageViewHelper.setFiles(m_Files);
                }
                m_ListAdapter.setSubText(SORT_BY, m_SortMenuValues.get(ScannedFile.getSortType()).toString());
                m_ListAdapter.setSubText(SHOW, m_ShowValues.get(0).toString());
                m_ListAdapter.setSubText(AUTHORS, m_AuthorValues.get(0).toString());
                m_ListAdapter.setSubText(SEARCH, " ");
                m_ListAdapter.setSubText(SHOW_ARCHIVED, "off");
            }
        };
        m_Handler.post(thrd);
        
    }
    
    private IECMScannerService m_Service = null;
    private IECMScannerServiceCallback m_Callback = new IECMScannerServiceCallback.Stub() {
        
        public void appendFiles(List<ScannedFile> files) throws RemoteException {
            Log.i(LOGTAG, "appendFiles called ...");
        }
        
        public void getBatchList(final List<ScannedFile> files) throws RemoteException {
            m_RemoteScanDone = true;
            updatePageView(files);
            unbindService(m_Conn);
            m_RemoteScanDone = false;
            m_BNBooksLock.block();
            m_LocalScanDone.block();
            m_FictionwiseLock.block();
            m_SmashwordsLock.block();
            updatePageView(true);
            Runnable thrd1 = new Runnable() {
                public void run() {
                    loadCovers();
                }
            };
            (new Thread(thrd1)).start();
            
        }
        
        public void getFileFound(ScannedFile file) throws RemoteException {
            Log.i(LOGTAG, "getFileFound called ...");
            if (file != null) {
                Log.w(LOGTAG, file.toString());
            }
        }
        
        public void getList(List<ScannedFile> list) throws RemoteException {
            Log.i(LOGTAG, "getList called ...");
        }
        
        public void setTotalSize(int size) throws RemoteException {
        }
    };
    
    private void loadBookData() {
        try {
            String[] folders = {
                "my documents", "Digital Editions", "mydownloads", "my downloads"
            };
            String[] exts = {
                "pdb"
            };
            m_Service.scanDirectoriesBatch(1, folders, exts, MAX_FILES_PER_BATCH, m_Callback);
        } catch (Exception ex) {
            Log.e(LOGTAG, "Exception calling scanDirectoriesBatch ...", ex);
            try {
                displayAlert(getString(R.string.scanning), getString(R.string.scan_error), 2, null, -1);
                m_Callback.getBatchList(null);
            } catch (Exception ex1) {
                
            }
        }
        
    }
    
    private ServiceConnection m_Conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(LOGTAG, "scanner service connected ..." + name);
            m_Service = IECMScannerService.Stub.asInterface(service);
            try {
                m_Service.registerCallback(m_Callback);
                Runnable thrd = new Runnable() {
                    public void run() {
                        loadBookData();
                    }
                };
                (new Thread(thrd)).start();
            } catch (Exception ex) {
                Log.e(LOGTAG, "Exception calling registercallback ...", ex);
            }
        }
        
        public void onServiceDisconnected(ComponentName name) {
            Log.i(LOGTAG, "service disconnected ..." + name);
        }
    };
    
    private void queryFolders() {
        queryFolders(LOCAL_BOOKS);
    }
    
    private void queryFolders(final int type) {
        if (m_ScanInProgress) { return; }
        m_ScanInProgress = true;
        ScannedFile.loadStandardKeywords();
        displayAlert(getString(R.string.scanning), getString(R.string.please_wait), 1, null, -1);
        m_ListAdapter.setSubText(REFRESH, getString(R.string.in_progress));
        m_Files.clear();
        m_Lock.acquire();
        bindService(new Intent("SCAN_ECM"), m_Conn, Context.BIND_AUTO_CREATE);
        m_LocalScanDone.close();
        Runnable thrd1 = new Runnable() {
            public void run() {
                m_BNBooksLock.close();
                List<ScannedFile> files = m_BNBooks.getBooks(type == BN_BOOKS || type == ALL_BOOKS);
                updatePageView(files);
                m_BNBooksLock.open();
            }
        };
        (new Thread(thrd1)).start();
        Runnable thrd = new Runnable() {
            public void run() {
                m_OtherBooks.getOtherBooks();
                m_LocalScanDone.open();
            }
        };
        final CharSequence val = m_RefreshAdapter.getItem(type);
        (new Thread(thrd)).start();
        if (m_FictionwiseBooks.getUser()) {
            Runnable thrd2 = new Runnable() {
                public void run() {
                    m_FictionwiseLock.close();
                    m_FictionwiseBooks.getBooks(val.equals(getString(R.string.fictionwise)) || type == ALL_BOOKS);
                    m_FictionwiseLock.open();
                }
            };
            (new Thread(thrd2)).start();
        } else {
            m_FictionwiseLock.open();
        }
        if (m_Smashwords.getUser()) {
            Runnable thrd3 = new Runnable() {
                public void run() {
                    m_SmashwordsLock.close();
                    m_Smashwords.getBooks(val.equals(getString(R.string.smashwords)) || type == ALL_BOOKS);
                    m_SmashwordsLock.open();
                }
            };
            (new Thread(thrd3)).start();
        } else {
            m_SmashwordsLock.open();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    private Dialog m_Dialog = null;
    private OnKeyListener m_TextListener = new OnKeyListener() {
        
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                if (view instanceof EditText) {
                    EditText editTxt = (EditText) view;
                    if (keyCode == SOFT_KEYBOARD_CLEAR) {
                        editTxt.setText("");
                    } else if (keyCode == SOFT_KEYBOARD_SUBMIT) {
                        String text = editTxt.getText().toString();
                        m_Dialog.cancel();
                        if (!text.trim().equals("")) {
                            SearchTask task = new SearchTask();
                            task.execute(text);
                        }
                    } else if (keyCode == SOFT_KEYBOARD_CANCEL) {
                        m_Dialog.cancel();
                    }
                }
            }
            return false;
        }
        
    };
    
    private class AddLibListener implements OnKeyListener {
        EditText m_User;
        EditText m_Pass;
        private int m_Type;
        
        public AddLibListener(int type, EditText user, EditText pass) {
            m_Type = type;
            m_User = user;
            m_Pass = pass;
        }
        
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                if (view instanceof EditText) {
                    EditText editTxt = (EditText) view;
                    if (keyCode == SOFT_KEYBOARD_CLEAR) {
                        editTxt.setText("");
                    } else if (keyCode == SOFT_KEYBOARD_SUBMIT) {
                        String login = m_User.getText().toString();
                        String pass = m_Pass.getText().toString();
                        if (login != null && pass != null) {
                            m_Dialog.cancel();
                            if (m_Type == FICTIONWISE_BOOKS) {
                                if (!m_FictionwiseBooks.addUser(login, pass)) {
                                    displayAlert(getString(R.string.add_lib), getString(R.string.add_lib_error), 2,
                                        null, -1);
                                } else {
                                    String name = getString(R.string.fictionwise);
                                    m_LibsAdapter.remove(name);
                                    m_RefreshAdapter.insert(name, m_AddLibIndex);
                                    queryFolders(m_AddLibIndex);
                                    m_AddLibIndex++;
                                    
                                }
                            } else if (m_Type == SMASHWORDS) {
                                if (!m_Smashwords.addUser(login, pass)) {
                                    displayAlert(getString(R.string.add_lib), getString(R.string.add_lib_error), 2,
                                        null, -1);
                                } else {
                                    String name = getString(R.string.smashwords);
                                    m_LibsAdapter.remove(name);
                                    m_RefreshAdapter.insert(name, m_AddLibIndex);
                                    queryFolders(m_AddLibIndex);
                                    m_AddLibIndex++;
                                    
                                }
                            }
                        } else {
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        }
                    } else if (keyCode == SOFT_KEYBOARD_CANCEL) {
                        m_Dialog.cancel();
                    }
                }
            }
            return false;
        }
        
    };
    
    protected void displayDialog(int cmd) {
        if (m_Dialog == null) {
            m_Dialog = new Dialog(this, android.R.style.Theme_Panel);
        }
        m_Dialog.setCancelable(true);
        m_Dialog.setCanceledOnTouchOutside(true);
        if (cmd == SEARCH) {
            m_Dialog.setContentView(R.layout.textinput);
            TextView txt = (TextView) m_Dialog.findViewById(R.id.TextView01);
            EditText keyword = (EditText) m_Dialog.findViewById(R.id.EditText01);
            txt.setText(R.string.search_lib);
            keyword.setText("");
            keyword.requestFocus();
            keyword.setOnKeyListener(m_TextListener);
        } else {
            m_Dialog.setContentView(R.layout.library_input);
            TextView txt = (TextView) m_Dialog.findViewById(R.id.libtext01);
            txt.setText(m_LibsAdapter.getItem(cmd));
            EditText user = (EditText) m_Dialog.findViewById(R.id.libedittext02);
            user.requestFocus();
            EditText pass = (EditText) m_Dialog.findViewById(R.id.libedittext03);
            AddLibListener l = new AddLibListener(cmd, user, pass);
            user.setOnKeyListener(l);
            pass.setOnKeyListener(l);
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        m_Dialog.show();
        
    }
    
    private void loadCovers() {
        List<ScannedFile> list = m_Files;
        for (ScannedFile file : list) {
            file.loadCover();
        }
        m_Lock.release();
        for (ScannedFile file : list) {
            if (!file.getBookInDB() && "pdf".equals(file.getType())) {
                file.updateMetaData(false);
            }
            if (!file.getBookInDB()) {
                String fictionwise = getString(R.string.fictionwise);
                if (file.getBookID() != null) {
                    if (file.matchSubject(fictionwise)) {
                        m_FictionwiseBooks.addBookToDB(file);
                    } else {
                        m_Smashwords.addBookToDB(file);
                    }
                } else {
                    m_OtherBooks.addBookToDB(file);
                }
                file.setBookInDB(true);
            }
        }
        m_FictionwiseBooks.close();
        final List<String> tmpList = ScannedFile.getAvailableKeywords();
        final List<String> authList = ScannedFile.getAuthors();
        Comparator<String> c = new Comparator<String>() {
            public int compare(String object1, String object2) {
                if (object1 != null) {
                    return object1.compareToIgnoreCase(object2);
                } else {
                    return 1;
                }
            }
            
        };
        Collections.sort(tmpList, c);
        Collections.sort(authList, c);
        Runnable thrd = new Runnable() {
            public void run() {
                m_ShowValues = new ArrayList<CharSequence>(tmpList.size() + 1);
                m_AuthorValues = new ArrayList<String>(authList.size() + 1);
                m_AuthorValues.add("All");
                m_AuthorValues.addAll(authList);
                m_ShowValues.add("All");
                if (ScannedFile.m_StandardKeywords != null) {
                    m_ShowValues.addAll(ScannedFile.m_StandardKeywords);
                }
                m_ShowValues.addAll(tmpList);
                m_ShowAdapter = new ArrayAdapter<CharSequence>(lview.getContext(), R.layout.listitem2, m_ShowValues);
                m_ListAdapter.setSubText(REFRESH, " ");
                m_AuthorAdapter = new ArrayAdapter<String>(lview.getContext(), R.layout.listitem2, m_AuthorValues);
                m_ScanInProgress = false;
            }
        };
        m_Handler.post(thrd);
    }
    
    private Vector<String> loadImages() {
        Vector<String> result = new Vector<String>();
        List<ScannedFile> list = pageViewHelper.getFiles();
        for (ScannedFile file : list) {
            result.add(file.getCover());
        }
        return result;
    }
    
    public void onItemClick(AdapterView<?> v, View parent, int position, long id) {
        if (v.equals(submenu)) {
            if (m_SubMenuType == SORT_BY) {
                int currvalue = ScannedFile.getSortType();
                subicons[currvalue] = -1;
                if (currvalue != position) {
                    m_ListAdapter.setSubText(SORT_BY, m_SortMenuValues.get(position).toString());
                    SortTask task = new SortTask();
                    task.execute(position);
                    Editor e = getSharedPreferences(PREF_FILE, MODE_PRIVATE).edit();
                    e.putInt("SORT_BY", position);
                    e.commit();
                }
            } else if (m_SubMenuType == SHOW) {
                if (m_ShowIndex != position) {
                    m_ListAdapter.setSubText(SHOW, (String) m_ShowValues.get(position));
                    ShowTask task = new ShowTask();
                    m_ShowIndex = position;
                    task.execute((String) m_ShowValues.get(position));
                }
            } else if (m_SubMenuType == AUTHORS) {
                if (m_AuthorIndex != position) {
                    m_ListAdapter.setSubText(AUTHORS, m_AuthorValues.get(position));
                    ShowTask task = new ShowTask();
                    m_AuthorIndex = position;
                    task.execute((String) m_ShowValues.get(m_ShowIndex));
                }
            } else if (m_SubMenuType == REFRESH) {
                if (position == m_AddLibIndex) {
                    submenu.setAdapter(m_LibsAdapter);
                    animator.setInAnimation(this, R.anim.fromleft);
                    animator.showNext();
                    animator.setInAnimation(this, R.anim.fromright);
                    animator.showNext();
                    m_SubMenuType = ADD_LIB_MENU;
                    return;
                } else {
                    queryFolders(position);
                }
            } else if (m_SubMenuType == ADD_LIB_MENU) {
                displayDialog(position);
            }
            animator.setInAnimation(this, R.anim.fromleft);
            animator.showNext();
            m_SubMenuType = -1;
            return;
        }
        // main menu actions here.
        switch (position) {
            case VIEW_DETAILS:
                m_PageViewAnimator.showNext();
                m_Details.setVisibility(View.VISIBLE);
                m_DetailsScroll.setVisibility(View.VISIBLE);
                m_CoverBtn.setVisibility(View.VISIBLE);
                ScannedFile file = pageViewHelper.getCurrent();
                String tmp = file.getCover();
                if (tmp == null) {
                    m_CoverBtn.setImageResource(R.drawable.no_cover);
                } else {
                    m_CoverBtn.setImageURI(Uri.parse(tmp));
                }
                if (!file.matchSubject("B&N")) {
                    m_Archive.setVisibility(View.VISIBLE);
                }
                Spanned txt = Html.fromHtml(file.getDetails());
                m_Details.setText(txt);
                m_DetailsPage.setText(txt);
                lview.setVisibility(View.INVISIBLE);
                upButton.setVisibility(View.INVISIBLE);
                downButton.setVisibility(View.INVISIBLE);
                goButton.setVisibility(View.INVISIBLE);
                m_SubMenuType = VIEW_DETAILS;
                break;
            case SEARCH:
                displayDialog(SEARCH);
                break;
            case SORT_BY:
                int currvalue = ScannedFile.getSortType();
                subicons[currvalue] = R.drawable.check;
                m_SortAdapter.setIcons(subicons);
                submenu.setAdapter(m_SortAdapter);
                animator.setInAnimation(this, R.anim.fromright);
                animator.showNext();
                m_SubMenuType = SORT_BY;
                break;
            case SHOW:
                submenu.setAdapter(m_ShowAdapter);
                animator.setInAnimation(this, R.anim.fromright);
                animator.showNext();
                m_SubMenuType = SHOW;
                break;
            case AUTHORS:
                submenu.setAdapter(m_AuthorAdapter);
                animator.setInAnimation(this, R.anim.fromright);
                animator.showNext();
                m_SubMenuType = AUTHORS;
                break;
            case SHOW_ARCHIVED:
                if (m_ArchiveView) {
                    m_ArchiveView = false;
                    pageViewHelper.setTitle(R.string.my_documents);
                    queryFolders();
                    m_ListAdapter.setSubText(SHOW_ARCHIVED, "off");
                    break;
                }
                m_ListAdapter.setSubText(SHOW_ARCHIVED, "on");
                m_ArchiveView = true;
                pageViewHelper.setTitle(R.string.archived_books);
                m_Files.clear();
                m_Files.addAll(m_OtherBooks.getArchived());
                m_Files.addAll(m_FictionwiseBooks.getArchived());
                m_Files.addAll(m_Smashwords.getArchived());
                m_Files.addAll(m_BNBooks.getArchived());
                m_ShowIndex = 0;
                m_AuthorIndex = 0;
                m_SearchView = false;
                m_ListAdapter.setSubText(AUTHORS, m_AuthorValues.get(0));
                m_ListAdapter.setSubText(SHOW, (String) m_ShowValues.get(0));
                m_ListAdapter.setSubText(SEARCH, " ");
                SortTask task = new SortTask();
                task.execute(ScannedFile.getSortType());
                break;
            case CLOSE:
                goHome();
                break;
            case REFRESH:
                if (m_ScanInProgress || m_ArchiveView) { return; }
                submenu.setAdapter(m_RefreshAdapter);
                animator.setInAnimation(this, R.anim.fromright);
                animator.showNext();
                m_SubMenuType = REFRESH;
                break;
            case SHOW_COVERS:
                Vector<String> images = loadImages();
                m_IconAdapter.setImageUrls(images);
                m_IconGallery.setAdapter(m_IconAdapter);
                m_IconGallery.setVisibility(View.VISIBLE);
                m_CloseBtn.setVisibility(View.VISIBLE);
                lview.setVisibility(View.INVISIBLE);
                backButton.setVisibility(View.INVISIBLE);
                upButton.setVisibility(View.INVISIBLE);
                downButton.setVisibility(View.INVISIBLE);
                goButton.setVisibility(View.INVISIBLE);
                m_IconGallery.setSelection(pageViewHelper.getCurrentIndex() - 1, true);
                
                break;
        }
        
    }
    
    public void onClick(View button) {
        if (button.equals(m_CloseBtn)) {
            if (m_Toast != null) {
                m_Toast.cancel();
                m_Toast.getView().setVisibility(View.INVISIBLE);
            }
            m_CloseBtn.setVisibility(View.INVISIBLE);
            m_IconGallery.setVisibility(View.INVISIBLE);
            lview.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.VISIBLE);
            upButton.setVisibility(View.VISIBLE);
            downButton.setVisibility(View.VISIBLE);
            goButton.setVisibility(View.VISIBLE);
        }
        if (button.equals(backButton)) {
            if (m_SubMenuType == VIEW_DETAILS) {
                lview.setVisibility(View.VISIBLE);
                backButton.setVisibility(View.VISIBLE);
                upButton.setVisibility(View.VISIBLE);
                downButton.setVisibility(View.VISIBLE);
                goButton.setVisibility(View.VISIBLE);
                m_CoverBtn.setVisibility(View.INVISIBLE);
                m_Details.setVisibility(View.INVISIBLE);
                m_DetailsScroll.setVisibility(View.INVISIBLE);
                m_Archive.setVisibility(View.INVISIBLE);
                m_PageViewAnimator.showNext();
                m_SubMenuType = -1;
                return;
            } else if (m_SubMenuType >= 0) {
                animator.setInAnimation(this, R.anim.fromleft);
                animator.showNext();
                m_SubMenuType = -1;
                return;
            } else if (m_SearchView) {
                m_SearchView = false;
                pageViewHelper.setTitle(R.string.my_documents);
                m_ListAdapter.setSubText(SEARCH, " ");
                if (m_ShowIndex != 0) {
                    ShowTask task = new ShowTask();
                    task.execute(m_ShowValues.get(m_ShowIndex).toString());
                } else {
                    pageViewHelper.setFiles(m_Files);
                }
                return;
            } else if (m_ArchiveView) {
                m_ArchiveView = false;
                pageViewHelper.setTitle(R.string.my_documents);
                queryFolders();
                m_ListAdapter.setSubText(SHOW_ARCHIVED, "off");
                return;
            }
            goHome();
        } else if (button.equals(upButton)) {
            pageViewHelper.selectPrev();
        } else if (button.equals(downButton)) {
            pageViewHelper.selectNext();
        } else if (button.equals(goButton) || button.equals(m_CoverBtn)) {
            final ScannedFile file = pageViewHelper.getCurrent();
            if (m_ArchiveView) { return; }
            if (file.getStatus() != null && !file.getStatus().equals(BNBooks.BORROWED)
                && !file.getStatus().equals(BNBooks.SAMPLE)) {
                int msgId;
                if (file.getStatus().startsWith(BNBooks.DOWNLOAD_IN_PROGRESS)) {
                    msgId = R.string.download_in_progress;
                }
                if (file.getStatus().contains(BNBooks.DOWNLOAD)) {
                    file.setStatus(BNBooks.DOWNLOAD_IN_PROGRESS);
                    Runnable thrd = new Runnable() {
                        public void run() {
                            if (file.getBookID() != null) {
                                if (file.matchSubject(getString(R.string.fictionwise))) {
                                    m_FictionwiseBooks.downloadBook(file);
                                } else {
                                    m_Smashwords.downloadBook(file);
                                }
                            } else {
                                m_BNBooks.getBook(file);
                            }
                            m_Handler.post(new Runnable() {
                                public void run() {
                                    pageViewHelper.update();
                                }
                            });
                        }
                    };
                    (new Thread(thrd)).start();
                    msgId = R.string.download_started;
                    pageViewHelper.update();
                } else {
                    msgId = R.string.on_loan;
                }
                Toast.makeText(this, msgId, Toast.LENGTH_SHORT).show();
                return;
            }
            String path = file.getPathName();
            if (path == null) { return; }
            Intent intent = new Intent("com.bravo.intent.action.VIEW");
            
            String mimetype = "application/";
            int idx = path.lastIndexOf('.');
            // File file1 = new File(path);
            // file1.setLastModified(System.currentTimeMillis());
            String ext = path.substring(idx + 1);
            if ("txt".equals(ext) || "html".equals(ext) || "htm".equals(ext)) {
                // try nookBrowser first
                try {
                    File tfile = new File(path);
                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.ACTION_DEFAULT);
                    intent.setComponent(new ComponentName("com.nookdevs.browser", "com.nookdevs.browser.nookBrowser"));
                    intent.setData(Uri.fromFile(tfile));
                    startActivity(intent);
                    return;
                } catch (Exception ex) {
                    intent = new Intent("com.bravo.intent.action.VIEW");
                }
            }
            mimetype += ext;
            path = "file://" + path;
            if (file.getEan() != null && !file.getEan().trim().equals("")) {
                path += "?EAN=" + file.getEan();
            }
            intent.setDataAndType(Uri.parse(path), mimetype);
            updateReadingNow(intent);
            try {
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException ex) {
                Log.i(LOGTAG, "Error while attempting to start reader App", ex);
                Toast.makeText(this, R.string.reader_not_found, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    // from kbs - trook.projectsource code.
    private final void pageUp() {
        if (m_DetailsPage != null) {
            int cury = m_DetailsPage.getScrollY();
            if (cury == 0) { return; }
            int newy = cury - WEB_SCROLL_PX;
            if (newy < 0) {
                newy = 0;
            }
            m_DetailsPage.scrollTo(0, newy);
        }
    }
    
    private final void pageDown() {
        if (m_DetailsPage != null) {
            int cury = m_DetailsPage.getScrollY();
            int hmax = m_DetailsPage.getMeasuredHeight() - 100;
            if (hmax < 0) {
                hmax = 0;
            }
            int newy = cury + WEB_SCROLL_PX;
            if (newy > hmax) { return;// newy = hmax;
            }
            if (cury != newy) {
                m_DetailsPage.scrollTo(0, newy);
            }
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case NOOK_PAGE_UP_KEY_LEFT:
            case NOOK_PAGE_UP_KEY_RIGHT:
            case NOOK_PAGE_UP_SWIPE:
                if (m_SubMenuType == VIEW_DETAILS) {
                    pageUp();
                } else {
                    pageViewHelper.pageUp();
                }
                handled = true;
                break;
            
            case NOOK_PAGE_DOWN_KEY_LEFT:
            case NOOK_PAGE_DOWN_KEY_RIGHT:
            case NOOK_PAGE_DOWN_SWIPE:
                if (m_SubMenuType == VIEW_DETAILS) {
                    pageDown();
                } else {
                    pageViewHelper.pageDown();
                }
                handled = true;
                break;
            
            default:
                break;
        }
        if (handled) {
            if (m_CloseBtn.getVisibility() == View.VISIBLE) {
                m_IconGallery.setSelection(pageViewHelper.getCurrentIndex() - 1, true);
            }
        }
        return handled;
    }
    
    class ShowTask extends AsyncTask<String, Integer, List<ScannedFile>> {
        @Override
        protected void onPreExecute() {
            m_ListAdapter.setSubText(SHOW, getString(R.string.in_progress));
        }
        
        @Override
        protected List<ScannedFile> doInBackground(String... keyword) {
            try {
                String text = keyword[0];
                List<ScannedFile> list = m_Files;
                if ("All".equals(text)) {
                    return list;
                } else {
                    return filterFiles(text, list);
                }
            } catch (Exception ex) {
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<ScannedFile> result) {
            m_ListAdapter.setSubText(SHOW, m_ShowValues.get(m_ShowIndex).toString());
            if (m_AuthorIndex != 0) {
                SearchTask task = new SearchTask(result);
                task.setType(AUTHORS);
                String txt = m_AuthorValues.get(m_AuthorIndex);
                task.execute(txt);
            } else if (m_SearchView) {
                SearchTask task = new SearchTask(result);
                String txt = m_ListAdapter.getSubText(SEARCH);
                m_ListAdapter.setSubText(SEARCH, " ");
                task.execute(txt);
            } else if (result != null) {
                pageViewHelper.setFiles(result);
            }
        }
        
    }
    
    class SearchTask extends AsyncTask<String, Integer, List<ScannedFile>> {
        private String m_Text;
        List<ScannedFile> m_SubList;
        private int m_Type = SEARCH;
        
        public SearchTask() {
            super();
            m_SubList = null;
        }
        
        public SearchTask(List<ScannedFile> list) {
            super();
            m_SubList = list;
        }
        
        public void setType(int type) {
            m_Type = type;
        }
        
        @Override
        protected void onPreExecute() {
            if (m_SearchView) {
                m_Text = m_ListAdapter.getSubText(SEARCH);
            } else {
                m_Text = null;
            }
            m_ListAdapter.setSubText(m_Type, getString(R.string.in_progress));
        }
        
        @Override
        protected List<ScannedFile> doInBackground(String... keyword) {
            try {
                String text = keyword[0];
                List<ScannedFile> list;
                if (m_SubList != null) {
                    list = m_SubList;
                } else {
                    list = pageViewHelper.getFiles();
                }
                if (m_Text == null || m_Text.trim().equals("")) {
                    m_Text = text;
                } else {
                    if (m_Type == AUTHORS) {
                        list = searchFiles(m_Text, list);
                        m_Text = text;
                    } else {
                        m_Text = text + " & " + m_Text;
                    }
                }
                if (m_Type == AUTHORS && text.equals("All")) { return list; }
                return searchFiles(text, list);
            } catch (Exception ex) {
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<ScannedFile> result) {
            m_ListAdapter.setSubText(m_Type, m_Text);
            if (result != null) {
                if (m_Type == SEARCH) {
                    pageViewHelper.setTitle(getString(R.string.search_results));
                    m_SearchView = true;
                }
                pageViewHelper.setFiles(result);
            }
        }
        
    }
    
    class SortTask extends AsyncTask<Integer, Integer, List<ScannedFile>> {
        private boolean m_Refresh;
        
        public SortTask() {
            this(false);
        }
        
        public SortTask(boolean refresh) {
            m_Refresh = refresh;
        }
        
        @Override
        protected void onPreExecute() {
            m_ListAdapter.setSubText(SORT_BY, getString(R.string.in_progress));
        }
        
        @Override
        protected List<ScannedFile> doInBackground(Integer... params) {
            int type = params[0];
            ScannedFile.setSortType(type);
            try {
                if (m_Refresh) {
                    for (ScannedFile file : m_Files) {
                        File f = new File(file.getPathName());
                        file.setLastAccessedDate(new Date(f.lastModified()));
                    }
                }
                List<ScannedFile> list = pageViewHelper.getFiles();
                Collections.sort(list);
                if (m_SearchView || m_ShowIndex != 0 || m_AuthorIndex != 0) {
                    Collections.sort(m_Files);
                }
                return list;
            } catch (Exception ex) {
                Log.e(LOGTAG, "Sorting failed...");
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<ScannedFile> result) {
            m_ListAdapter.setSubText(SORT_BY, m_SortMenuValues.get(ScannedFile.getSortType()).toString());
            if (result != null) {
                if (!m_SearchView && m_ShowIndex == 0) {
                    m_Files = result;
                }
                pageViewHelper.setFiles(result);
            }
        }
    }
    
    class GalleryClickListener implements OnItemClickListener, OnItemSelectedListener {
        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
            View selected = ((Gallery) arg0).getSelectedView();
            if (arg1.equals(selected)) {
                onClick(goButton);
            }
        }
        
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
            if (m_IconGallery.getVisibility() == View.VISIBLE) {
                pageViewHelper.gotoItem(position + 1);
                if (m_Toast != null) {
                    m_Toast.cancel();
                    m_Toast.getView().setVisibility(View.INVISIBLE);
                }
                m_Toast = Toast.makeText(NookLibrary.this, pageViewHelper.getCurrent().getTitle(), Toast.LENGTH_SHORT);
                m_Toast.show();
            }
        }
        
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
            
        }
    }
    
    public boolean onLongClick(View view) {
        if (view.equals(upButton)) {
            pageViewHelper.gotoTop();
        } else {
            pageViewHelper.gotoBottom();
        }
        return true;
    }
}
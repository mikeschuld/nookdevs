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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.bravo.ecmscannerservice.IECMScannerService;
import com.bravo.ecmscannerservice.IECMScannerServiceCallback;
import com.bravo.ecmscannerservice.ScannedFile;
import com.nookdevs.common.CustomGallery;
import com.nookdevs.common.IconArrayAdapter;
import com.nookdevs.common.ImageAdapter;
import com.nookdevs.common.nookBaseActivity;

public class NookLibrary extends nookBaseActivity implements OnItemClickListener, OnClickListener {
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
    protected static final int SCAN_FOLDERS = 5;
    protected static final int CLOSE = 6;
    private ConditionVariable m_LocalScanDone = new ConditionVariable();
    private static final int WEB_SCROLL_PX = 750;
    private Toast m_Toast=null;
    private int[] icons =
        {
            -1, R.drawable.submenu, R.drawable.search, R.drawable.covers, R.drawable.submenu, -1, -1, -1, -1, -1, -1,
            -1, -1, -1
        };
    
    private int[] subicons = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
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
    private ImageView divider = null;
    IconArrayAdapter<CharSequence> m_ListAdapter = null;
    IconArrayAdapter<CharSequence> m_SortAdapter = null;
    ArrayAdapter<CharSequence> m_ShowAdapter = null;
    List<CharSequence> m_SortMenuValues = null;
    // ConnectivityManager.WakeLock m_Lock;
    protected List<CharSequence> m_ShowValues = null;
    int m_ShowIndex = 0;
    ImageButton m_CoverBtn = null;
    TextView m_Details = null;
    ScrollView m_DetailsScroll = null;
    ViewAnimator m_PageViewAnimator = null;
    TextView m_DetailsPage = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        LOGTAG = "nookLibrary";
        
        goButton = (ImageButton) findViewById(R.id.go);
        backButton = (Button) findViewById(R.id.back);
        upButton = (Button) findViewById(R.id.up);
        downButton = (Button) findViewById(R.id.down);
        lview = (ListView) findViewById(R.id.list);
        submenu = (ListView) findViewById(R.id.sublist);
        animator = (ViewAnimator) findViewById(R.id.listviewanim);
        submenu.setOnItemClickListener(this);
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
        
        lview.setAdapter(m_ListAdapter);
        lview.setOnItemClickListener(this);
        goButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
        upButton.setOnClickListener(this);
        downButton.setOnClickListener(this);
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
        divider = (ImageView) findViewById(R.id.divider);
        m_CoverBtn = (ImageButton) findViewById(R.id.cover);
        m_Details = (TextView) findViewById(R.id.details);
        m_DetailsScroll = (ScrollView) findViewById(R.id.detailsscroll);
        m_CoverBtn.setVisibility(View.INVISIBLE);
        m_CoverBtn.setOnClickListener(this);
        m_Details.setVisibility(View.INVISIBLE);
        m_DetailsScroll.setVisibility(View.INVISIBLE);
        m_PageViewAnimator = (ViewAnimator) findViewById(R.id.pageview);
        m_DetailsPage = (TextView) findViewById(R.id.pageview2);
        queryFolders();
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
            sortType = getPreferences(MODE_PRIVATE).getInt("SORT_BY", sortType);
            m_ListAdapter.setSubText(SORT_BY, m_SortMenuValues.get(sortType).toString());
        } catch (Exception ex) {
            Log.e(LOGTAG, "preference exception: ", ex);
            
        }
        ScannedFile.setSortType(sortType);
        super.readSettings();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateTitle("my books");
        getSystemService(CONNECTIVITY_SERVICE);
    }
    
    private void getOtherFiles() {
        try {
            File file = new File(SDFOLDER);
            File external = new File(EXTERNAL_SDFOLDER);
            FileFilter filter = new FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) { return true; }
                    String extension = f.getName().toLowerCase();
                    if (extension.endsWith("epub") || extension.endsWith("htm") || extension.endsWith("txt")
                        || extension.endsWith("html")) {
                        return true;
                    } else {
                        return false;
                    }
                }
                
            };
            retrieveFiles(file, filter);
            retrieveFiles(external, filter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        m_LocalScanDone.open();
    }
    
    private void retrieveFiles(File base, FileFilter filter) {
        File[] files = base.listFiles(filter);
        if (files == null) { return; }
        for (File file : files) {
            if (file.isDirectory()) {
                retrieveFiles(file, filter);
            } else {
                ScannedFile file1 = new ScannedFile(file.getAbsolutePath());
                file1.setLastAccessedDate(new Date(file.lastModified()));
                m_Files.add(file1);
            }
        }
        return;
    }
    
    private IECMScannerService m_Service = null;
    private IECMScannerServiceCallback m_Callback = new IECMScannerServiceCallback.Stub() {
        
        public void appendFiles(List<ScannedFile> files) throws RemoteException {
            Log.i(LOGTAG, "appendFiles called ...");
        }
        
        public void getBatchList(final List<ScannedFile> files) throws RemoteException {
            m_LocalScanDone.block();
            m_Files.addAll(files);
            unbindService(m_Conn);
            Collections.sort(m_Files);
            List<String> tmpList = ScannedFile.getAvailableKeywords();
            m_ShowIndex = 0;
            Collections.sort(tmpList);
            m_ShowValues = new ArrayList<CharSequence>(tmpList.size() + 1);
            m_ShowValues.add("All");
            m_ShowValues.addAll(tmpList);
            m_ShowAdapter = new ArrayAdapter<CharSequence>(lview.getContext(), R.layout.listitem2, m_ShowValues);
            Runnable thrd = new Runnable() {
                public void run() {
                    closeAlert();
                    LinearLayout pageview = (LinearLayout) NookLibrary.this.findViewById(R.id.pageview1);
                    pageViewHelper = new PageViewHelper(NookLibrary.this, pageview, m_Files);
                    m_ListAdapter.setSubText(SORT_BY, m_SortMenuValues.get(ScannedFile.getSortType()).toString());
                    m_ListAdapter.setSubText(SHOW, m_ShowValues.get(m_ShowIndex).toString());
                }
            };
            m_Handler.post(thrd);
            Runnable thrd1 = new Runnable() {
                public void run() {
                    loadCovers();
                }
            };
            (new Thread(thrd1)).start();
        }
        
        public void getFileFound(ScannedFile file) throws RemoteException {
            Log.i(LOGTAG, "getFileFound called ...");
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
                "my documents", "Digital Editions", "mydownloads", "my downloads", "my B&N Downloads"
            };
            String[] exts = {
                "pdf", "pdb"
            };
            m_Service.scanDirectoriesBatch(1, folders, exts, MAX_FILES_PER_BATCH, m_Callback);
        } catch (Exception ex) {
            Log.e(LOGTAG, "Exception calling scanDirectoriesBatch ...", ex);
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
        displayAlert(getString(R.string.scanning), getString(R.string.please_wait), 1, null, -1);
        m_Files.clear();
        bindService(new Intent("SCAN_ECM"), m_Conn, Context.BIND_AUTO_CREATE);
        m_LocalScanDone.close();
        Runnable thrd = new Runnable() {
            public void run() {
                getOtherFiles();
            }
        };
        (new Thread(thrd)).start();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // m_Lock.release();
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
                            m_ListAdapter.setSubText(SEARCH, text);
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
    
    protected void displayDialog(int cmd) {
        if (m_Dialog == null) {
            m_Dialog = new Dialog(this, android.R.style.Theme_Panel);
        }
        m_Dialog.setContentView(R.layout.textinput);
        m_Dialog.setCancelable(true);
        m_Dialog.setCanceledOnTouchOutside(true);
        TextView txt = (TextView) m_Dialog.findViewById(R.id.TextView01);
        EditText keyword = (EditText) m_Dialog.findViewById(R.id.EditText01);
        if (cmd == SEARCH) {
            txt.setText(R.string.search_lib);
            keyword.setText("");
        }
        keyword.requestFocus();
        keyword.setOnKeyListener(m_TextListener);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(lview, InputMethodManager.SHOW_FORCED);
        m_Dialog.show();
        
    }
    
    private void loadCovers() {
        List<ScannedFile> list = m_Files;
        for (ScannedFile file : list) {
            file.loadCover();
        }
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
                    Editor e = getPreferences(MODE_PRIVATE).edit();
                    e.putInt("SORT_BY", position);
                    e.commit();
                }
            } else if (m_SubMenuType == SHOW) {
                if (m_ShowIndex != position) {
                    m_ListAdapter.setSubText(SHOW, (String) m_ShowValues.get(position));
                    ShowTask task = new ShowTask();
                    task.execute((String) m_ShowValues.get(position));
                    m_ShowIndex = position;
                }
            }
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
                Spanned txt = Html.fromHtml(file.getDetails());
                m_Details.setText(txt);
                m_DetailsPage.setText(txt);
                lview.setVisibility(View.INVISIBLE);
                upButton.setVisibility(View.INVISIBLE);
                downButton.setVisibility(View.INVISIBLE);
                goButton.setVisibility(View.INVISIBLE);
                divider.setVisibility(View.INVISIBLE);
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
                animator.showNext();
                m_SubMenuType = SORT_BY;
                break;
            case SHOW:
                submenu.setAdapter(m_ShowAdapter);
                animator.showNext();
                m_SubMenuType = SHOW;
                break;
            case CLOSE:
                goHome();
                break;
            case SCAN_FOLDERS:
                queryFolders();
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
                divider.setVisibility(View.INVISIBLE);
                m_IconGallery.setSelection(pageViewHelper.getCurrentIndex() - 1, true);
                
                break;
        }
        
    }
    
    public void onClick(View button) {
        if (button.equals(m_CloseBtn)) {
            if( m_Toast != null) {
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
            divider.setVisibility(View.VISIBLE);
        }
        if (button.equals(backButton)) {
            if (m_SubMenuType == VIEW_DETAILS) {
                lview.setVisibility(View.VISIBLE);
                backButton.setVisibility(View.VISIBLE);
                upButton.setVisibility(View.VISIBLE);
                downButton.setVisibility(View.VISIBLE);
                goButton.setVisibility(View.VISIBLE);
                divider.setVisibility(View.VISIBLE);
                m_CoverBtn.setVisibility(View.INVISIBLE);
                m_Details.setVisibility(View.INVISIBLE);
                m_DetailsScroll.setVisibility(View.INVISIBLE);
                m_PageViewAnimator.showNext();
                m_SubMenuType = -1;
                return;
            } else if (m_SearchView) {
                pageViewHelper.setFiles(m_Files);
                m_SearchView = false;
                pageViewHelper.setTitle(R.string.my_documents);
                m_ListAdapter.setSubText(SEARCH, " ");
                return;
            } else if (m_SubMenuType >= 0) {
                animator.showNext();
                m_SubMenuType = -1;
                return;
            }
            goHome();
        } else if (button.equals(upButton)) {
            pageViewHelper.selectPrev();
        } else if (button.equals(downButton)) {
            pageViewHelper.selectNext();
        } else if (button.equals(goButton) || button.equals(m_CoverBtn)) {
            ScannedFile file = pageViewHelper.getCurrent();
            String path = file.getPathName();
            Intent intent = new Intent("com.bravo.intent.action.VIEW");
            
            String mimetype = "application/";
            int idx = path.lastIndexOf('.');
            // File file1 = new File(path);
            // file1.setLastModified(System.currentTimeMillis());
            String ext = path.substring(idx + 1);
            if ("txt".equals(ext) || "html".equals(ext) || "htm".equals(ext)) {
                // try nookBrowser first
                try {
                    File tfile = new File( path);
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
            Log.i(LOGTAG, "mimetype = " + mimetype);
            Log.i(LOGTAG, "URI =" + path);
            intent.setDataAndType(Uri.parse(path), mimetype);
            updateReadingNow(intent);
            try {
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException ex) {
                Log.i(LOGTAG, "Error while attempting to start reader App", ex);
                Toast.makeText(this,R.string.reader_not_found, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void updateReadingNow(Intent intent) {
        try {
            ContentValues values = new ContentValues();
            ByteArrayOutputStream aout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(aout);
            dout.writeUTF(intent.getAction());
            dout.writeUTF(intent.getDataString());
            dout.writeUTF(intent.getType());
            dout.writeByte(0);
            byte[] data = aout.toByteArray();
            dout.close();
            values.put("data", data);
            getContentResolver().insert(Uri.parse(READING_NOW_URL), values);
        } catch (Exception ex) {
            Log.e(LOGTAG, "Exception while updating reading now data - ", ex);
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
                if (m_SubMenuType == VIEW_DETAILS) {
                    pageUp();
                } else {
                    pageViewHelper.pageUp();
                }
                handled = true;
                break;
            
            case NOOK_PAGE_DOWN_KEY_LEFT:
            case NOOK_PAGE_DOWN_KEY_RIGHT:
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
            displayAlert(getString(R.string.searching), getString(R.string.please_wait), 1, null, -1);
        }
        
        @Override
        protected List<ScannedFile> doInBackground(String... keyword) {
            try {
                String text = keyword[0];
                if ("All".equals(text)) {
                    return m_Files;
                } else {
                    List<ScannedFile> list = m_Files;
                    return filterFiles(text, list);
                }
            } catch (Exception ex) {
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<ScannedFile> result) {
            closeAlert();
            if (result != null) {
                // pageViewHelper.setTitle(getString(R.string.search_results));
                pageViewHelper.setFiles(result);
            }
        }
        
    }
    
    class SearchTask extends AsyncTask<String, Integer, List<ScannedFile>> {
        @Override
        protected void onPreExecute() {
            displayAlert(getString(R.string.searching), getString(R.string.please_wait), 1, null, -1);
        }
        
        @Override
        protected List<ScannedFile> doInBackground(String... keyword) {
            try {
                String text = keyword[0];
                List<ScannedFile> list = m_Files;
                return searchFiles(text, list);
            } catch (Exception ex) {
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<ScannedFile> result) {
            closeAlert();
            if (result != null) {
                pageViewHelper.setTitle(getString(R.string.search_results));
                pageViewHelper.setFiles(result);
                m_SearchView = true;
            }
        }
        
    }
    
    class SortTask extends AsyncTask<Integer, Integer, List<ScannedFile>> {
        @Override
        protected void onPreExecute() {
            displayAlert(getString(R.string.sorting), getString(R.string.please_wait), 1, null, -1);
        }
        
        @Override
        protected List<ScannedFile> doInBackground(Integer... params) {
            int type = params[0];
            ScannedFile.setSortType(type);
            try {
                List<ScannedFile> list = pageViewHelper.getFiles();
                Collections.sort(list);
                return list;
            } catch (Exception ex) {
                Log.e(LOGTAG, "Sorting failed...", ex);
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<ScannedFile> result) {
            closeAlert();
            if (result != null) {
                if (!m_SearchView) {
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
            if( m_IconGallery.getVisibility() == View.VISIBLE) {
                pageViewHelper.gotoItem(position + 1);
                if( m_Toast != null) {
                    m_Toast.cancel();
                    m_Toast.getView().setVisibility(View.INVISIBLE);
                }
                m_Toast =Toast.makeText(NookLibrary.this, pageViewHelper.getCurrent().getTitle(), Toast.LENGTH_SHORT);
                m_Toast.show();
            }
        }
        
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub

        }
    }
}
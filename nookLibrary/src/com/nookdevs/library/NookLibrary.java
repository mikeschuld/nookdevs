/**
 *     This file is part of nookLibrary

    This is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    nookLibrary is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License.
    If not, see <http://www.gnu.org/licenses/>.

 */
package com.nookdevs.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
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
    private List<ScannedFile> m_Files = null;
    public static final int MAX_FILES_PER_BATCH = 99999;
    private boolean m_SearchView = false;
    
    private Button backButton, upButton, downButton;
    private ImageButton goButton;
    
    protected static final int SORT_BY = 0;
    protected static final int SEARCH = 1;
    protected static final int SHOW_COVERS = 2;
    protected static final int QUICK_BROWSE = 3;
    protected static final int SCAN_FOLDERS = 3;
    protected static final int CLOSE = 4;
    protected static final int SOFT_KEYBOARD_CLEAR = -13;
    protected static final int SOFT_KEYBOARD_SUBMIT = -8;
    protected static final int SOFT_KEYBOARD_CANCEL = -3;
    
    private int[] icons = {
        R.drawable.submenu, R.drawable.search, R.drawable.covers, -1, -1, -1, -1, -1, -1, -1, -1, -1
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
        IconArrayAdapter<CharSequence> adapter = new IconArrayAdapter<CharSequence>(lview.getContext(),
            R.layout.listitem, menuitemsList, icons);
        adapter.setImageField(R.id.ListImageView);
        adapter.setTextField(R.id.ListTextView);
        lview.setAdapter(adapter);
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
        
        queryFolders();
    }
    
    private List<ScannedFile> searchFiles(String keyword, List<ScannedFile> list) {
        List<ScannedFile> results = new ArrayList<ScannedFile>();
        String key = keyword.toLowerCase();
        for (ScannedFile file : list) {
            if (file.getData().contains(key)) {
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
    }
    
    private IECMScannerService m_Service = null;
    private IECMScannerServiceCallback m_Callback = new IECMScannerServiceCallback.Stub() {
        
        public void appendFiles(List<ScannedFile> files) throws RemoteException {
            Log.i(LOGTAG, "appendFiles called ...");
        }
        
        public void getBatchList(final List<ScannedFile> files) throws RemoteException {
            m_Files = files;
            unbindService(m_Conn);
            Runnable thrd = new Runnable() {
                public void run() {
                    Collections.sort(m_Files);
                    closeAlert();
                    LinearLayout pageview = (LinearLayout) NookLibrary.this.findViewById(R.id.pageview);
                    pageViewHelper = new PageViewHelper(NookLibrary.this, pageview, m_Files);
                }
            };
            m_Handler.post(thrd);
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
                "my documents", "Digital Editions", "mydownloads", "my downloads"
            };
            String[] exts = {
                "epub", "pdf", "pdb", "html", "htm", "txt"
            };
            m_Service.scanDirectoriesBatch(1, folders, exts, MAX_FILES_PER_BATCH, m_Callback);
            Log.w(LOGTAG, "Called scanDirectoriesBatch from thread...");
            
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
        displayAlert(getString(R.string.scanning), getString(R.string.please_wait), 5, null, -1);
        bindService(new Intent("SCAN_ECM"), m_Conn, Context.BIND_AUTO_CREATE);
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
                        SearchTask task = new SearchTask();
                        task.execute(text);
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
    
    private Vector<String> loadImages() {
        Vector<String> result = new Vector<String>();
        List<ScannedFile> list = pageViewHelper.getFiles();
        for (ScannedFile file : list) {
            String path = file.getPathName();
            int idx = path.lastIndexOf('.');
            String path1 = path.substring(0, idx);
            // check for image files
            String[] fileTypes = {
                ".jpg", ".png", ".jpeg"
            };
            boolean found = false;
            for (String s : fileTypes) {
                File f = new File(path1 + s);
                if (f.exists()) {
                    result.add(path1 + s);
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(null);
            }
        }
        return result;
    }
    
    public void onItemClick(AdapterView<?> v, View parent, int position, long id) {
        if (v.equals(submenu)) {
            if (m_SubMenuType == SORT_BY) {
                int currvalue = ScannedFile.getSortType();
                subicons[currvalue] = -1;
                if (currvalue != position) {
                    SortTask task = new SortTask();
                    task.execute(position);
                    Editor e = getPreferences(MODE_PRIVATE).edit();
                    e.putInt("SORT_BY", position);
                    e.commit();
                }
                animator.setInAnimation(this, R.anim.fromright);
                animator.showNext();
                m_SubMenuType = -1;
                return;
            }
            return;
        }
        // main menu actions here.
        switch (position) {
            case SEARCH:
                displayDialog(SEARCH);
                break;
            case SORT_BY:
                int currvalue = ScannedFile.getSortType();
                CharSequence[] menuitems = getResources().getTextArray(R.array.sortmenu);
                List<CharSequence> menuitemsList = Arrays.asList(menuitems);
                subicons[currvalue] = R.drawable.check;
                IconArrayAdapter<CharSequence> adapter = new IconArrayAdapter<CharSequence>(lview.getContext(),
                    R.layout.listitem, menuitemsList, subicons);
                adapter.setImageField(R.id.ListImageView);
                adapter.setTextField(R.id.ListTextView);
                submenu.setAdapter(adapter);
                animator.setInAnimation(this, R.anim.fromright);
                animator.showNext();
                m_SubMenuType = SORT_BY;
                break;
            case CLOSE:
                try {
                    goBack();
                } catch (Exception ex) {
                    goHome();
                }
                break;
            case SCAN_FOLDERS:
                queryFolders();
                break;
            case SHOW_COVERS:
                // displayAlert(getString(R.string.error),
                // getString(R.string.not_implemented)
                // , 2, null, -1);
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
            // case QUICK_BROWSE:
            // displayAlert(getString(R.string.error),
            // getString(R.string.not_implemented)
            // , 2, null, -1);
            // break;
            
        }
        
    }
    
    public void onClick(View button) {
        if (button.equals(m_CloseBtn)) {
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
            try {
                // check for submenus
                // check for search results display
                if (m_SearchView) {
                    pageViewHelper.setFiles(m_Files);
                    m_SearchView = false;
                    pageViewHelper.setTitle(R.string.my_documents);
                    return;
                } else if (m_SubMenuType >= 0) {
                    m_SubMenuType = -1;
                    // animator.setInAnimation(this, R.anim.fromright);
                    animator.showNext();
                    return;
                }
                goBack();
            } catch (Exception ex) {
                goHome();
            }
        } else if (button.equals(upButton)) {
            pageViewHelper.selectPrev();
        } else if (button.equals(downButton)) {
            pageViewHelper.selectNext();
        } else if (button.equals(goButton)) {
            ScannedFile file = pageViewHelper.getCurrent();
            String path = file.getPathName();
            Intent intent = new Intent("com.bravo.intent.action.VIEW");
            
            String mimetype = "application/";
            int idx = path.lastIndexOf('.');
            String ext = path.substring(idx + 1);
            if ("txt".equals(ext) || "html".equals(ext) || "htm".equals(ext)) {
                // try nookBrowser first
                try {
                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.ACTION_DEFAULT);
                    intent.setComponent(new ComponentName("com.nookdevs.browser", "com.nookdevs.browser.nookBrowser"));
                    intent.setData(Uri.parse("file://" + path));
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
            try {
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException ex) {
                Log.i(LOGTAG, "Error while attempting to start reader App", ex);
            }
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        Log.i(LOGTAG, "key code: " + keyCode);
        switch (keyCode) {
            case NOOK_PAGE_UP_KEY_LEFT:
            case NOOK_PAGE_UP_KEY_RIGHT:
                pageViewHelper.pageUp();
                handled = true;
                break;
            
            case NOOK_PAGE_DOWN_KEY_LEFT:
            case NOOK_PAGE_DOWN_KEY_RIGHT:
                pageViewHelper.pageDown();
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
    
    class SearchTask extends AsyncTask<String, Integer, List<ScannedFile>> {
        @Override
        protected void onPreExecute() {
            displayAlert(getString(R.string.searching), getString(R.string.please_wait), 5, null, -1);
        }
        
        @Override
        protected List<ScannedFile> doInBackground(String... keyword) {
            try {
                String text = keyword[0];
                List<ScannedFile> list = pageViewHelper.getFiles();
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
            displayAlert(getString(R.string.sorting), getString(R.string.please_wait), 5, null, -1);
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
            Log.i(LOGTAG, "onItemClick - Item " + position + " moved to select position");
            if (arg1.equals(selected)) {
                onClick(goButton);
            }
        }
        
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
            Log.i(LOGTAG, "onItemSelected - Item " + position + " moved to select position");
            pageViewHelper.gotoItem(position + 1);
            // display title
        }
        
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    }
}
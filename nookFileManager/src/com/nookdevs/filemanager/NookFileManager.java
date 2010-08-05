/* 
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.nookdevs.filemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.nookdevs.common.IconArrayAdapter;
import com.nookdevs.common.nookBaseActivity;

public class NookFileManager extends nookBaseActivity implements OnItemClickListener {
    
    private String[] m_StartFolders = {
        SDFOLDER, EXTERNAL_SDFOLDER
    };
    LinearLayout m_Header;
    LinearLayout m_Content;
    public static final int BROWSE = 1;
    public static final int OPEN = 2;
    public static final int SAVE = 3;
    public static final int MAX_FILES_PER_VIEW = 50;
    
    private int m_Type = BROWSE;
    private FileSelectListener m_FileSelectListener = new FileSelectListener();
    private Button m_Back;
    private TextView m_Title;
    private Button m_Add;
    private boolean m_AtRoot = true;
    private Dialog m_Dialog = null;
    private String m_CurrentFolder = null;
    private TextListener m_TextListener = new TextListener();
    private ViewAnimator m_ViewAnimator = null;
    private ListView m_List = null;
    private IconArrayAdapter<CharSequence> m_LocalAdapter = null;
    private IconArrayAdapter<CharSequence> m_RemoteAdapter = null;
    private File m_CurrentTarget=null;
    private static final int CUT = 0;
    private static final int COPY = 1;
    private static final int DELETE = 2;
    private static final int RENAME = 3;
    private static final int SET_AS_TARGET = 4;
    private static final int RCOPY = 0;
    private static final int DOWNLOAD = 1;
    int[] icons = {
        R.drawable.cut, R.drawable.copy, R.drawable.delete, -1,-1
    };
    int[] remoteicons = {
        R.drawable.copy, R.drawable.download
    };
    ImageButton m_FileIcon = null;
    ArrayList<RemotePC> m_Nodes = new ArrayList<RemotePC>(4);
    private boolean m_Local = true;
    private File m_Current;
    private SmbFile m_CurrentRemote;
    ImageButton m_PasteButton = null;
    private List<File> m_CopyFile = new ArrayList<File>(5);
    private List<SmbFile> m_RemoteCopy = new ArrayList<SmbFile>(5);
    private HashMap<String,Boolean> m_CutOperation = new HashMap<String,Boolean>();
    Handler m_Handler = new Handler();
    private boolean m_Rename = false;
    private int m_CurrentNode = 0;
    private File m_externalMyDownloads = new File(EXTERNAL_SDFOLDER + "/my downloads");
    private File m_MyDownloads = new File(SDFOLDER + "/my downloads");
    private ConnectivityManager.WakeLock m_Lock = null;
    private StatusUpdater m_StatusUpdater = null;
    ImageView m_ImageView = null;
    SmbFile[] m_CurrentSmbFiles;
    File[] m_CurrentFiles;
    ArrayList<CharSequence> m_RemoteMenuItems = new ArrayList<CharSequence>(4);
    ListView m_PasteMenu = null;
    ArrayAdapter m_PasteAdapter = null;
    ArrayList<String> m_PasteMenuItems = new ArrayList<String>(10);
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "nookFileManager";
        NAME = "File Manager";
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        LOGTAG = "nookFileManager";
        m_Header = (LinearLayout) findViewById(R.id.header);
        m_Content = (LinearLayout) findViewById(R.id.files);
        m_Back = (Button) findViewById(R.id.back);
        m_Title = (TextView) findViewById(R.id.title);
        m_Add = (Button) findViewById(R.id.add);
        m_List = (ListView) findViewById(R.id.list);
        m_ViewAnimator = (ViewAnimator) findViewById(R.id.fileanim);
        CharSequence[] menuitems = getResources().getTextArray(R.array.localmenu);
        List<CharSequence> menuitemsList = Arrays.asList(menuitems);
        m_LocalAdapter =
            new IconArrayAdapter<CharSequence>(m_List.getContext(), R.layout.listitem, menuitemsList, icons);
        m_LocalAdapter.setImageField(R.id.ListImageView);
        m_LocalAdapter.setTextField(R.id.ListTextView);
        m_LocalAdapter.setSubTextField(R.id.ListSubTextView);
        menuitems = getResources().getTextArray(R.array.remotemenu);
        menuitemsList = Arrays.asList(menuitems);
        m_RemoteMenuItems.addAll(menuitemsList);
        m_RemoteAdapter =
            new IconArrayAdapter<CharSequence>(m_List.getContext(), R.layout.listitem,m_RemoteMenuItems, remoteicons);
        m_RemoteAdapter.setImageField(R.id.ListImageView);
        m_RemoteAdapter.setTextField(R.id.ListTextView);
        m_FileIcon = (ImageButton) findViewById(R.id.fileicon);
        m_ImageView = (ImageView) findViewById(R.id.mainimage);
        m_FileIcon.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (m_Current != null && !m_Current.isDirectory()) {
                    startViewer(m_Current);
                }
            }
        });
        m_List.setOnItemClickListener(this);
        m_PasteButton = (ImageButton) findViewById(R.id.paste);
        m_PasteMenu = (ListView) findViewById(R.id.pastemenu);
        m_PasteButton.setVisibility(View.INVISIBLE);
        m_PasteMenu.setVisibility(View.INVISIBLE);
        m_PasteMenu.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if( position ==0) {
                    for(int i=0; i< m_CopyFile.size();i++) {
                        CopyTask task = new CopyTask( m_CurrentFolder, m_CopyFile.get(i));
                        task.execute();
                    }
                    for(int i=0; i< m_RemoteCopy.size();i++) {
                        RemoteCopyTask task = new RemoteCopyTask( m_CurrentFolder, m_RemoteCopy.get(i));
                        task.execute();
                    }
                    m_CopyFile.clear();
                    m_RemoteCopy.clear();
                } else if( position ==1) {
                    m_CopyFile.clear();
                    m_RemoteCopy.clear();
                } else {
                    int val = position-2;
                    if( val < m_CopyFile.size()) {
                        //local copy
                        CopyTask task = new CopyTask( m_CurrentFolder, m_CopyFile.get(val));
                        task.execute();
                        m_CopyFile.remove(val);
                    } else {
                        //remote copy
                        val -= m_CopyFile.size();
                        RemoteCopyTask task = new RemoteCopyTask( m_CurrentFolder, m_RemoteCopy.get(val));
                        task.execute();
                        m_RemoteCopy.remove(val);
                    }
                }
                if( m_CopyFile.size() ==0 && m_RemoteCopy.size() ==0) {
                    m_PasteButton.setVisibility(View.INVISIBLE);
                }
                m_PasteMenu.setVisibility(View.INVISIBLE);
                m_Back.setVisibility(View.VISIBLE);
                m_ViewAnimator.setVisibility(View.VISIBLE);
                m_Add.setVisibility(View.VISIBLE);
                m_PasteMenuItems.clear();
                m_StatusView=false;
            }
        });
        m_PasteMenu.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if( position <=1) {
                    return false;
                } else {
                    int val = position-2;
                    if( val < m_CopyFile.size()) {
                        m_CopyFile.remove(val);
                    } else {
                        val -= m_CopyFile.size();
                        m_RemoteCopy.remove(val);
                    }
                    if( m_CopyFile.size() ==0 && m_RemoteCopy.size() ==0) {
                        m_PasteButton.setVisibility(View.INVISIBLE);
                        m_PasteMenu.setVisibility(View.INVISIBLE);
                        m_Back.setVisibility(View.VISIBLE);
                        m_ViewAnimator.setVisibility(View.VISIBLE);
                        m_Add.setVisibility(View.VISIBLE);
                        m_PasteMenuItems.clear();
                        m_StatusView=false;
                    } else {
                        m_PasteMenuItems.remove( position);
                        m_PasteAdapter = new ArrayAdapter<String>(NookFileManager.this, R.layout.listitem2, m_PasteMenuItems);
                        m_PasteMenu.setAdapter(m_PasteAdapter);
                    }
                    return true;
                }
            }
           
        });
        
        m_PasteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if( m_StatusView) {
                    m_PasteMenu.setVisibility(View.INVISIBLE);
                    m_Back.setVisibility(View.VISIBLE);
                    m_ViewAnimator.setVisibility(View.VISIBLE);
                    m_Add.setVisibility(View.VISIBLE);
                    m_PasteMenuItems.clear();
                    m_StatusView=false;
                    return;
                }
                m_PasteMenuItems.add(getString(R.string.paste_all));
                m_PasteMenuItems.add(getString(R.string.clear_all));
                for( File file:m_CopyFile) {
                    m_PasteMenuItems.add(file.getAbsolutePath());
                }
                for( SmbFile file:m_RemoteCopy) {
                    m_PasteMenuItems.add(file.getCanonicalPath());
                }
                m_PasteAdapter = new ArrayAdapter<String>(NookFileManager.this, R.layout.listitem2, m_PasteMenuItems);
                m_Add.setVisibility(View.INVISIBLE);
                m_ViewAnimator.setVisibility(View.INVISIBLE);
                m_StatusView = true;
                m_PasteMenu.setAdapter(m_PasteAdapter);
                m_Back.setVisibility(View.INVISIBLE);
                m_PasteMenu.setVisibility(View.VISIBLE);
            }
        });
        m_Add.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                m_Dialog = new Dialog(NookFileManager.this, android.R.style.Theme_Panel);
                if (m_AtRoot == true) {
                    m_Dialog.setContentView(R.layout.textinput);
                    EditText txt = (EditText) m_Dialog.findViewById(R.id.EditText01);
                    txt.setOnKeyListener(m_TextListener);
                    txt = (EditText) m_Dialog.findViewById(R.id.EditText02);
                    txt.setOnKeyListener(m_TextListener);
                    txt = (EditText) m_Dialog.findViewById(R.id.EditText03);
                    txt.setOnKeyListener(m_TextListener);
                } else {
                    m_Dialog.setContentView(R.layout.folderinput);
                    EditText txt = (EditText) m_Dialog.findViewById(R.id.EditText04);
                    txt.setOnKeyListener(m_TextListener);
                }
                m_Dialog.setCancelable(true);
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                m_Dialog.show();
                
            }
            
        });
        if (m_externalMyDownloads.exists()) {
            m_CurrentTarget = m_externalMyDownloads;
        } else {
            m_CurrentTarget = m_MyDownloads;
            if (!m_MyDownloads.exists()) {
                m_MyDownloads.mkdir();
            }
        }
        m_Add.setText(R.string.add_pc);
        m_Back.setText(R.string.back);
        if (m_Type == BROWSE) {
            m_Title.setText("");
        }
        m_Back.setOnClickListener(m_FileSelectListener);
        m_Back.setOnLongClickListener(m_FileSelectListener);
        loadFolders(m_StartFolders, true);
        loadNodes();
        ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        m_Lock = cmgr.newWakeLock(1, "nookBrowser" + hashCode());
        loadNetwork(null);
        loadStatus();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        if (m_Lock != null && !m_Lock.isHeld()) {
            m_Lock.acquire();
        }
        if (m_StatusUpdater != null) {
            m_StatusUpdater.removeCompleted();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        try {
            if (m_Lock != null && m_Lock.isHeld()) {
                m_Lock.release();
            }
        } catch (Exception ex) {
            
        }
    }
    
    private void startViewer(File file) {
        String path = file.getAbsolutePath();
        Intent intent;
        
        int idx = path.lastIndexOf('.');
        String ext = path.substring(idx + 1);
        MimeTypeMap mimemap = MimeTypeMap.getSingleton();
        String mimetype = mimemap.getMimeTypeFromExtension(ext);
        if( mimetype == null) {
            if ("apk".equals(ext)) {
                mimetype = "application/vnd.android.package-archive";
            } else {
                mimetype="application/" +ext;
            }
        }
        if ("txt".equals(ext) || "html".equals(ext) || "htm".equals(ext)) {
            try {
                intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.ACTION_DEFAULT);
                intent.setComponent(new ComponentName("com.nookdevs.browser", "com.nookdevs.browser.nookBrowser"));
                intent.setData(Uri.fromFile(file));
                startActivity(intent);
                return;
            } catch (Exception ex) {
            }
        }
        path = "file://" + path;
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(path), mimetype);
        try {
            if ("epub".equals(ext) || "pdf".equals(ext) || "pdb".equals(ext)) {
                updateReadingNow(intent);
            }
            startActivity(intent);
            return;
        } catch (ActivityNotFoundException ex) {
        }
        intent = new Intent("com.bravo.intent.action.VIEW");
        intent.setDataAndType(Uri.fromFile(file), mimetype);
        try {
            startActivity(intent);
        } catch (Exception ex) {
            int id = getResource(ext);
            if (id == -1) {
                try {
                    Bitmap bMap = BitmapFactory.decodeFile(path.substring(7));
                    m_ImageView.setImageBitmap(bMap);
                } catch(Throwable ex1) {
                }
                
            } else {
                Toast.makeText(this, R.string.no_viewer, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void loadStatus() {
        LayoutInflater inflater = getLayoutInflater();
        RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
        ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
        TextView text = (TextView) filedetails.findViewById(R.id.text);
        String name = getString(R.string.status);
        text.setText(name);
        icon.setImageResource(R.drawable.info);
        if (m_StatusUpdater == null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.status);
            m_StatusUpdater = new StatusUpdater(this, layout, R.layout.statustxt);
        }
        icon.setTag(m_StatusUpdater);
        icon.setOnClickListener(m_FileSelectListener);
        text.setTag(icon);
        text.setOnClickListener(m_FileSelectListener);
        m_Content.addView(filedetails);
    }
    
    private void loadNodes() {
        m_Nodes.clear();
        SharedPreferences p = getPreferences(MODE_PRIVATE);
        int count = p.getInt("NODES", 0);
        if (count == 0) { return; }
        boolean holes = false;
        for (int i = 1; i <= count; i++) {
            String ip = p.getString("IP_ADDRESS" + i, "");
            if (ip.trim().equals("")) {
                holes = true;
                continue;
            }
            RemotePC pc = new RemotePC();
            pc.ip = ip;
            pc.user = p.getString("USER" + i, "");
            pc.pass = p.getString("PASS" + i, "");
            pc.idx = i;
            m_Nodes.add(pc);
        }
        if (holes) {
            Editor e = getPreferences(MODE_PRIVATE).edit();
            e.putInt("NODES", m_Nodes.size());
            for (int i = 0; i < m_Nodes.size(); i++) {
                RemotePC pc = m_Nodes.get(i);
                e.putString("IP_ADDRESS" + (i + 1), pc.ip);
                e.putString("USER" + (i + 1), pc.user);
                e.putString("PASS" + (i + 1), pc.pass);
                e.commit();
                pc.idx = i + 1;
            }
        }
    }
    
    private void loadNetwork(SmbFile folder) {
        try {
            LayoutInflater inflater = getLayoutInflater();
            SmbFile smb = null;
            if (folder == null) {
                for (RemotePC pc : m_Nodes) {
                    RelativeLayout filedetails =
                        (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
                    ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
                    TextView text = (TextView) filedetails.findViewById(R.id.text);
                    text.setText(pc.ip);
                    icon.setImageResource(R.drawable.network);
                    icon.setTag(pc);
                    icon.setOnClickListener(m_FileSelectListener);
                    icon.setOnLongClickListener(m_FileSelectListener);
                    text.setTag(icon);
                    text.setOnClickListener(m_FileSelectListener);
                    icon.setOnLongClickListener(m_FileSelectListener);
                    m_Content.addView(filedetails);
                }
                return;
            }
            smb = folder;
            m_Content.removeAllViews();
            SmbFile[] files = smb.listFiles();
            Arrays.sort(files, new Comparator<SmbFile>() {
                public int compare(SmbFile object1, SmbFile object2) {
                    return object1.getName().compareTo(object2.getName());
                }
            });
            m_CurrentSmbFiles = files;
            loadNetworkFiles(0);
        } catch (Exception ex) {
            Log.e(LOGTAG, "Exception in loadNetwork", ex);
        }
    }
    
    private void loadNetworkFiles(int index) {
        try {
            m_Content.removeAllViews();
            LayoutInflater inflater = getLayoutInflater();
            int i;
            if (index > 0) {
                // add prev
                RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
                ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
                TextView text = (TextView) filedetails.findViewById(R.id.text);
                text.setText(R.string.prev);
                icon.setImageResource(R.drawable.prev);
                icon.setTag(new Integer(index - MAX_FILES_PER_VIEW));
                icon.setOnClickListener(m_FileSelectListener);
                text.setTag(icon);
                text.setOnClickListener(m_FileSelectListener);
                
                m_Content.addView(filedetails);
            }
            for (i = index; i < m_CurrentSmbFiles.length && i < index + MAX_FILES_PER_VIEW; i++) {
                SmbFile f = m_CurrentSmbFiles[i];
                RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
                ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
                TextView text = (TextView) filedetails.findViewById(R.id.text);
                String name = f.getName();
                String type = f.isDirectory() ? "dir" : name.substring(name.lastIndexOf('.') + 1);
                if (!f.isDirectory()) {
                    name += "\nSize: " + (((int) (f.length() / 1024.0 * 100)) / 100.0) + "K";
                }
                text.setText(name);
                int id = getResource(type);
                if (id != -1) {
                    icon.setImageResource(id);
                } else {
                    icon.setImageResource(R.drawable.image);
                }
                icon.setTag(f);
                icon.setOnClickListener(m_FileSelectListener);
                text.setTag(icon);
                text.setOnClickListener(m_FileSelectListener);
                
                if (f.isDirectory()) {
                    icon.setOnLongClickListener(m_FileSelectListener);
                    
                }
                m_Content.addView(filedetails);
            }
            if (i == m_CurrentSmbFiles.length) { return; }
            // add More
            RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
            ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
            TextView text = (TextView) filedetails.findViewById(R.id.text);
            text.setText(R.string.next);
            icon.setImageResource(R.drawable.next);
            icon.setTag(new Integer(index + MAX_FILES_PER_VIEW));
            icon.setOnClickListener(m_FileSelectListener);
            m_Content.addView(filedetails);
            text.setTag(icon);
            text.setOnClickListener(m_FileSelectListener);
        } catch (Exception ex) {
            
        }
    }
    
    private void loadLocalFiles(int index) {
        try {
            m_Content.removeAllViews();
            LayoutInflater inflater = getLayoutInflater();
            int i;
            if (index > 0) {
                // add prev
                RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
                ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
                TextView text = (TextView) filedetails.findViewById(R.id.text);
                text.setText(R.string.prev);
                icon.setImageResource(R.drawable.prev);
                icon.setTag(new Integer(index - MAX_FILES_PER_VIEW));
                icon.setOnClickListener(m_FileSelectListener);
                text.setTag(icon);
                text.setOnClickListener(m_FileSelectListener);
                m_Content.addView(filedetails);
            }
            for (i = index; i < m_CurrentFiles.length && i < index + MAX_FILES_PER_VIEW; i++) {
                File f = m_CurrentFiles[i];
                RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
                ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
                TextView text = (TextView) filedetails.findViewById(R.id.text);
                String name = f.getName();
                String type = f.isDirectory() ? "dir" : name.substring(name.lastIndexOf('.') + 1);
                if (!f.isDirectory()) {
                    name += "\nSize: " + (((int) (f.length() / 1024.0 * 100)) / 100.0) + "K";
                }
                text.setText(name);
                int id = getResource(type);
                if (id != -1) {
                    icon.setImageResource(id);
                } else {
                    try {
                        icon.setImageURI(Uri.parse(f.getAbsolutePath()));
                    } catch(Throwable err) {
                        icon.setImageResource(R.drawable.image);
                    }
                }
                icon.setTag(f);
                icon.setOnClickListener(m_FileSelectListener);
                text.setTag(icon);
                text.setOnClickListener(m_FileSelectListener);
                
                if (f.isDirectory()) {
                    icon.setOnLongClickListener(m_FileSelectListener);
                    
                }
                m_Content.addView(filedetails);
            }
            if (i == m_CurrentFiles.length) { return; }
            // add More
            RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
            ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
            TextView text = (TextView) filedetails.findViewById(R.id.text);
            text.setText(R.string.next);
            icon.setImageResource(R.drawable.next);
            icon.setTag(new Integer(index + MAX_FILES_PER_VIEW));
            icon.setOnClickListener(m_FileSelectListener);
            text.setTag(icon);
            text.setOnClickListener(m_FileSelectListener);
            m_Content.addView(filedetails);
        } catch (Exception ex) {
            
        }
    }
    
    private void loadFolders(String[] folders, boolean base) {
        m_Content.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        if (base) {
            for (String f2 : folders) {
                RelativeLayout filedetails = (RelativeLayout) inflater.inflate(R.layout.filedetail, m_Content, false);
                ImageButton icon = (ImageButton) filedetails.findViewById(R.id.icon);
                TextView text = (TextView) filedetails.findViewById(R.id.text);
                File f1 = new File(f2);
                String name = f1.getName();
                if (f1.getParent().equals("/")) {
                    name += " External";
                }
                text.setText(name);
                String type = "dir";
                icon.setImageResource(getResource(type));
                icon.setTag(f1);
                icon.setOnClickListener(m_FileSelectListener);
                text.setTag(icon);
                text.setOnClickListener(m_FileSelectListener);
                if (f1.isDirectory()) {
                    icon.setOnLongClickListener(m_FileSelectListener);
                    
                }
                m_Content.addView(filedetails);
            }
            return;
        }
        String folder = folders[0];
        File[] files = (new File(folder)).listFiles();
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File object1, File object2) {
                return object1.getName().compareTo(object2.getName());
            }
        });
        m_CurrentFiles = files;
        loadLocalFiles(0);
    }
    
    private int getResource(String type) {
        if ("ndir".equalsIgnoreCase(type)) { return R.drawable.network; }
        if ("dir".equalsIgnoreCase(type)) { return R.drawable.folder; }
        if ("pdb".equalsIgnoreCase(type)) { return R.drawable.pdb; }
        if ("pdf".equalsIgnoreCase(type)) { return R.drawable.pdf; }
        if ("gif".equalsIgnoreCase(type)) { return -1; }
        if ("jpg".equalsIgnoreCase(type)) { return -1; }
        if ("jpeg".equalsIgnoreCase(type)) { return -1; }
        if ("png".equalsIgnoreCase(type)) { return -1; }
        if ("epub".equalsIgnoreCase(type)) { return R.drawable.epub; }
        if ("xml".equalsIgnoreCase(type)) { return R.drawable.xml; }
        if ("txt".equalsIgnoreCase(type)) { return R.drawable.txt; }
        if ("htm".equalsIgnoreCase(type)) { return R.drawable.html; }
        if ("html".equalsIgnoreCase(type)) { return R.drawable.html; }
        if ("apk".equalsIgnoreCase(type)) { return R.drawable.icon; }
        if ("mp3".equalsIgnoreCase(type)) { return R.drawable.mp3; }
        if ("mp4".equalsIgnoreCase(type)) { return R.drawable.video; }
        return R.drawable.generic;
    }
    
    class TextListener implements OnKeyListener {
        
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                if (view instanceof EditText) {
                    EditText editTxt = (EditText) view;
                    if (keyCode == nookBaseActivity.SOFT_KEYBOARD_CLEAR) {
                        editTxt.setText("");
                    } else if (keyCode == nookBaseActivity.SOFT_KEYBOARD_SUBMIT) {
                        if (m_Rename) {
                            String name = editTxt.getText().toString();
                            if (!m_Current.renameTo(new File(m_Current.getParent() + "/" + name))) {
                                displayError(R.string.rename_error);
                            }
                            m_Dialog.cancel();
                            m_Rename = false;
                            clickAction(m_Back);
                            return false;
                        }
                        if (m_AtRoot) {
                            RemotePC pc = new RemotePC();
                            pc.ip = ((EditText) (m_Dialog.findViewById(R.id.EditText01))).getText().toString();
                            pc.user = ((EditText) (m_Dialog.findViewById(R.id.EditText02))).getText().toString();
                            pc.pass = ((EditText) (m_Dialog.findViewById(R.id.EditText03))).getText().toString();
                            // add pc
                            int count = getPreferences(MODE_PRIVATE).getInt("NODES", 0);
                            Editor e = getPreferences(MODE_PRIVATE).edit();
                            count++;
                            pc.idx = count;
                            e.putString("IP_ADDRESS" + count, pc.ip);
                            e.putString("USER" + count, pc.user);
                            e.putString("PASS" + count, pc.pass);
                            e.putInt("NODES", count);
                            e.commit();
                            m_Nodes.add(pc);
                            loadFolders(m_StartFolders, true);
                            loadNetwork(null);
                            loadStatus();
                        } else {
                            // create folder.
                            String foldername = editTxt.getText().toString();
                            File f = new File(m_CurrentFolder + "/" + foldername + "/");
                            f.mkdir();
                            String[] subfolder = {
                                m_CurrentFolder
                            };
                            loadFolders(subfolder, false);
                        }
                        m_Dialog.cancel();
                        
                    } else if (keyCode == nookBaseActivity.SOFT_KEYBOARD_CANCEL) {
                        if (m_Rename) {
                            m_Rename = false;
                            clickAction(m_Back);
                        }
                        m_Dialog.cancel();
                    }
                }
            }
            return false;
        }
    }
    
    private boolean m_FileView = false;
    private boolean m_DirDetails = false;
    private boolean m_StatusView = false;
    
    private void clickAction(View v) {
        if (v.getTag() == null) {
            if (m_DirDetails) {
                m_Lock.release();
                finish();
                throw new Error("Dummy exception to force stop the app.");
            } else {
                goBack();
            }
            return;
        }
        // hack to get the image 
        if( v instanceof TextView && v.getTag() instanceof ImageButton) 
            v = (View)v.getTag();
        m_ImageView.setImageBitmap(null);
        m_PasteButton.setVisibility(View.INVISIBLE);
        if (v.getTag() instanceof Integer) {
            int idx = (Integer) v.getTag();
            if (m_CurrentFolder != null) {
                loadLocalFiles(idx);
            } else {
                loadNetworkFiles(idx);
            }
            return;
        }
        m_Current = null;
        m_CurrentRemote = null;
        
        if (v.getTag() instanceof StatusUpdater) {
            m_Back.setText(" < ");
            m_Back.setTag(new File("/"));
            m_Add.setVisibility(View.INVISIBLE);
            m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromright);
            m_StatusView = true;
            m_Title.setText(R.string.status);
            m_Back.setText("<");
            m_ViewAnimator.showPrevious();
            return;
        } else if (m_StatusView) {
            m_StatusView = false;
            m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromleft);
            m_ViewAnimator.showNext();
        }
        if (m_FileView) {
            m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromleft);
            m_ViewAnimator.showPrevious();
        }
        if (v.getTag() instanceof RemotePC) {
            try {
                m_Back.setText(" < ");
                m_Back.setTag(new File("/"));
                m_Add.setVisibility(View.INVISIBLE);
                RemotePC pc = (RemotePC) v.getTag();
                m_Title.setText(pc.ip);
                if (m_DirDetails) { // file details.
                    m_List.setAdapter(m_LocalAdapter);
                    m_FileIcon.setImageDrawable(((ImageButton) v).getDrawable());
                    m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromright);
                    m_ViewAnimator.showNext();
                    m_DirDetails = false;
                    m_FileView = true;
                    m_Local = true;
                    m_CurrentNode = pc.idx;
                    return;
                }
                ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo info = cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                boolean connection = (info == null) ? false : info.isConnected();
                if( !connection) {
                    displayAlert(getString(R.string.wifi_error), getString(R.string.wait_for_wifi), 2, null, -1);
                    return;
                }
                String smUrl = "smb://";
                if (pc.user != null && !pc.user.trim().equals("")) {
                    System.setProperty("jcifs.smb.client.password", pc.pass);
                    System.setProperty("jcifs.smb.client.user", pc.user);
                    smUrl += pc.user;
                    smUrl += ":";
                    smUrl += pc.pass + "@";
                }
                SmbFile sf = new SmbFile(smUrl + pc.ip + "/");
                loadNetwork(sf);
                return;
            } catch (Exception ex) {
                Toast.makeText(NookFileManager.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(LOGTAG, "Exception connecting to remote PC - ", ex);
                return;
            }
        }
        if (v.getTag() instanceof SmbFile) {
            SmbFile sf = (SmbFile) v.getTag();
            m_Add.setText(R.string.add_folder);
            m_Add.setVisibility(View.INVISIBLE);
            m_AtRoot = false;
            try {
                String parent = sf.getParent();
                if (parent.endsWith("/")) {
                    parent = parent.substring(0, parent.length() - 1);
                }
                int idx = parent.lastIndexOf('/');
                parent = parent.substring(idx + 1);
                m_Back.setText(" < " + parent);
                if (parent.trim().equals("")) {
                    m_Back.setTag(new File("/"));
                } else {
                    m_Back.setTag(new SmbFile(sf.getParent()));
                }
                m_Title.setText(sf.getName());
                if (!m_DirDetails && sf.isDirectory()) {
                    loadNetwork(sf);
                    m_FileView = false;
                } else {
                    // file details.
                    m_FileView = true;
                    m_List.setAdapter(m_RemoteAdapter);
                    m_FileIcon.setImageDrawable(((ImageButton) v).getDrawable());
                    m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromright);
                    m_ViewAnimator.showNext();
                    m_DirDetails = false;
                    m_Local = false;
                    m_FileView = true;
                    m_CurrentRemote = sf;
                }
            } catch (Exception e) {
                Log.e(LOGTAG, "error in listener ", e);
            }
            return;
        }
        File f = (File) v.getTag();
        m_CurrentFolder = null;
        if (f.getParent() == null) {
            m_Back.setText(R.string.back);
            m_Back.setTag(null);
            if (m_Type == BROWSE) {
                m_Title.setText("");
            }
            m_Add.setText(R.string.add_pc);
            m_AtRoot = true;
            if (f.isDirectory()) {
                m_Add.setVisibility(View.VISIBLE);
                loadFolders(m_StartFolders, true);
                loadNetwork(null);
                loadStatus();
                m_FileView = false;
            }
            return;
        } else {
            m_Add.setText(R.string.add_folder);
            m_AtRoot = false;
            String tmp = f.getParent();
            m_CurrentFolder = f.getAbsolutePath();
            boolean valid = false;
            for (String t : m_StartFolders) {
                if (tmp.contains(t)) {
                    valid = true;
                    break;
                }
            }
            if (valid) {
                m_Back.setTag(f.getParentFile());
                m_Back.setText(" < " + f.getParentFile().getName());
            } else {
                m_Back.setTag(new File("/"));
                m_Back.setText(" < " + f.getParentFile().getName());
            }
        }
        String[] subfolder = {
            f.getAbsolutePath()
        };
        m_Title.setText(f.getName());
        if (f.getName().equals("sdcard")) {
            m_DirDetails = false;
        }
        if (!m_DirDetails && f.isDirectory()) {
            m_Add.setVisibility(View.VISIBLE);
            loadFolders(subfolder, false);
            m_FileView = false;
            if (m_CopyFile.size() >0 || m_RemoteCopy.size()>0) {
                m_PasteButton.setVisibility(View.VISIBLE);
            }
        } else {
            if( f.isDirectory()) {
                m_LocalAdapter.setEnabled(SET_AS_TARGET, true);
            } else {
                m_LocalAdapter.setEnabled(SET_AS_TARGET, false);
            }
            // file details.
            m_Add.setVisibility(View.INVISIBLE);
            m_FileIcon.setImageDrawable(((ImageButton) v).getDrawable());
            m_List.setAdapter(m_LocalAdapter);
            m_ViewAnimator.setInAnimation(NookFileManager.this, R.anim.fromright);
            m_ViewAnimator.showNext();
            m_Local = true;
            m_FileView = true;
            m_DirDetails = false;
            m_Current = f;
        }
    }
    
    class FileSelectListener implements OnClickListener, OnLongClickListener {
        
        public void onClick(View v) {
            clickAction(v);
        }
        
        public boolean onLongClick(View v) {
            m_DirDetails = true;
            return false;
        }
    }
    
    public void onItemClick(AdapterView<?> view, View parent, int position, long id) {
        if (m_Local) {
            switch (position) {
                case CUT:
                    if (m_Current == null) {
                        displayError(R.string.operation_invalid);
                        return;
                    }
                    m_CutOperation.put( m_Current.getAbsolutePath(), true);
                case COPY:
                    if (m_Current == null) {
                        displayError(R.string.operation_invalid);
                        return;
                    }
                    m_CopyFile.add(m_Current);
                    m_PasteButton.setVisibility(View.VISIBLE);
                    clickAction(m_Back);
                    break;
                case DELETE:
                    if (m_Current == null) {
                        Editor e = getPreferences(MODE_PRIVATE).edit();
                        e.putString("IP_ADDRESS" + m_CurrentNode, "");
                        e.commit();
                        loadNodes();
                        clickAction(m_Back);
                        
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.delete);
                        builder.setMessage(R.string.confirm);
                        builder.setNegativeButton(android.R.string.no, null).setCancelable(true);
                        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (!deleteDir(m_Current)) {
                                    displayError(R.string.delete_error);
                                }
                                clickAction(m_Back);
                            }
                        });
                        builder.show();
                    }
                    break;
                case RENAME:
                    if (m_Current == null) {
                        displayError(R.string.operation_invalid);
                        return;
                    }
                    m_Dialog = new Dialog(NookFileManager.this, android.R.style.Theme_Panel);
                    m_Dialog.setContentView(R.layout.folderinput);
                    EditText txt = (EditText) m_Dialog.findViewById(R.id.EditText04);
                    txt.setOnKeyListener(m_TextListener);
                    m_Dialog.setCancelable(true);
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    m_Rename = true;
                    m_Dialog.show();
                    break;
                case SET_AS_TARGET:
                    m_RemoteMenuItems.remove(1);
                    m_CurrentTarget = m_Current;
                    m_RemoteMenuItems.add("Copy to " + m_Current.getName());
                    m_RemoteAdapter =
                        new IconArrayAdapter<CharSequence>(m_List.getContext(), R.layout.listitem,m_RemoteMenuItems, remoteicons);
                    m_RemoteAdapter.setImageField(R.id.ListImageView);
                    m_RemoteAdapter.setTextField(R.id.ListTextView);
                    Toast.makeText(this, R.string.target_set, Toast.LENGTH_SHORT).show();
            }
        } else {
            if (position == 0) {
                // plain copy
                m_RemoteCopy.add(m_CurrentRemote);
                m_PasteButton.setVisibility(View.VISIBLE);
                clickAction(m_Back);
            } else {
                String target;
                if( m_CurrentTarget ==null) {
                    if (m_externalMyDownloads.exists()) {
                        target = m_externalMyDownloads.getAbsolutePath();
                    } else {
                        target = m_MyDownloads.getAbsolutePath();
                        if (!m_MyDownloads.exists()) {
                            m_MyDownloads.mkdir();
                        }
                    }
                } else {
                    target = m_CurrentTarget.getAbsolutePath();
                }
                RemoteCopyTask task = new RemoteCopyTask(target, m_CurrentRemote);
                clickAction(m_Back);
                task.execute();
            }
            
        }
        
    }
    
    private void displayError(final int resid) {
        m_Handler.post(new Runnable() {
            public void run() {
                Toast.makeText(NookFileManager.this, resid, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void displayInfo(final int resid) {
        m_Handler.post(new Runnable() {
            public void run() {
                Toast.makeText(NookFileManager.this, resid, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String element : children) {
                boolean success = deleteDir(new File(dir, element));
                if (!success) { return false; }
            }
        }
        
        // The directory is now empty so delete it
        return dir.delete();
    }
    
    private void copyRemoteDirectory(SmbFile sourceLocation, File targetLocation, RemoteCopyTask task)
        throws IOException {
        
        if (sourceLocation.isDirectory()) {
            sourceLocation = new SmbFile(sourceLocation.getCanonicalPath() + "/");
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            
            String[] children = sourceLocation.list();
            for (String element : children) {
                copyRemoteDirectory(new SmbFile(sourceLocation, element), new File(targetLocation, element), task);
            }
        } else {
            
            SmbFileInputStream in = new SmbFileInputStream(sourceLocation);
            FileOutputStream out = new FileOutputStream(targetLocation);
            
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            float size = sourceLocation.length();
            float current = 0;
            int len;
            int prevProgress = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                current += len;
                int progress = (int) (current / size * 99);
                if (progress > 99) {
                    progress = 99;
                }
                if (prevProgress != progress) {
                    task.updateProgress(progress);
                    prevProgress = progress;
                }
            }
            in.close();
            out.close();
        }
    }
    
    private void copyDirectory(File sourceLocation, File targetLocation, CopyTask task) throws IOException {
        
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            
            String[] children = sourceLocation.list();
            for (String element : children) {
                copyDirectory(new File(sourceLocation, element), new File(targetLocation, element), task);
            }
        } else {
            FileInputStream in = new FileInputStream(sourceLocation);
            FileOutputStream out = new FileOutputStream(targetLocation);
            float size = sourceLocation.length();
            float current = 0;
            int prevProgress = 0;
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                current += len;
                int progress = (int) (current / size * 99);
                if (progress > 99) {
                    progress = 99;
                }
                if (prevProgress != progress) {
                    task.updateProgress(progress);
                    prevProgress = progress;
                }
            }
            in.close();
            out.close();
        }
    }
    
    class RemoteCopyTask extends AsyncTask<Void, Integer, Boolean> {
        File m_Target = null;
        String m_CurrFolder = null;
        SmbFile m_Copy;
        int m_Id;
        
        public RemoteCopyTask(String currFolder, SmbFile copy) {
            m_CurrFolder = currFolder;
            m_Copy = copy;
        }
        
        @Override
        protected void onPreExecute() {
            try {
                m_Target = null;
                if (m_Copy == null) { return; }
                if (m_CurrFolder == null) { return; }
                m_Target = new File(m_CurrFolder + "/" + m_Copy.getName());
                m_Id = m_StatusUpdater.addFile(m_Target.getAbsolutePath());
                if (m_Target.exists()) {
                    displayError(R.string.already_exists);
                    m_Target = null;
                    return;
                }
            } catch (Exception ex) {
                displayError(R.string.copy_failed);
                m_Target = null;
            }
        }
        
        public void updateProgress(Integer... params) {
            super.publishProgress(params);
        }
        
        @Override
        protected void onProgressUpdate(Integer... progress) {
            m_StatusUpdater.updateProgress(m_Id, progress[0]);
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
            ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            ConnectivityManager.WakeLock lock = cmgr.newWakeLock(1, "NookFileManager.remoteCopyTask" + hashCode());
            try {
                if (m_Target == null) { return false; }
                lock.acquire();
                copyRemoteDirectory(m_Copy, m_Target, this);
                try {
                    if (m_CutOperation.get(m_Copy.getCanonicalPath()) != null) {
                        m_Copy.delete();
                        m_CutOperation.remove(m_Copy.getCanonicalPath());
                    }
                } catch (Exception ex) {
                    Log.e(LOGTAG, "Failed to delete file after copy.", ex);
                }
                lock.release();
                return true;
            } catch (Exception ex) {
                Log.e(LOGTAG, "remote copy failed...", ex);
                if (lock.isHeld()) {
                    lock.release();
                }
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                displayInfo(R.string.copy_success);
                m_StatusUpdater.updateProgress(m_Id, 100);
                if (m_CurrentFolder != null && m_CurrentFolder.equals(m_Target.getParent())) {
                    String[] subfolder = {
                        m_CurrentFolder
                    };
                    loadFolders(subfolder, false);
                }
                if (m_CopyFile.size() ==0 && m_RemoteCopy.size() ==0) {
                    m_PasteButton.setVisibility(View.INVISIBLE);
                }
            } else if (m_Target != null) {
                displayError(R.string.copy_failed);
                m_StatusUpdater.updateProgress(m_Id, -1);
            } else {
                m_StatusUpdater.updateProgress(m_Id, -1);
            }
            
        }
    }
    
    class CopyTask extends AsyncTask<Void, Integer, Boolean> {
        File m_Target = null;
        String m_CurrFolder = null;
        File m_Copy = null;
        int m_Id;
        
        public CopyTask(String currFolder, File copy) {
            m_CurrFolder = currFolder;
            m_Copy = copy;
        }
        
        @Override
        protected void onPreExecute() {
            try {
                m_Target = null;
                if (m_Copy == null) { return; }
                if (m_CurrFolder == null) { return; }
                m_Target = new File(m_CurrFolder + "/" + m_Copy.getName());
                m_Id = m_StatusUpdater.addFile(m_Target.getAbsolutePath());
                if (m_Target.exists()) {
                    displayError(R.string.already_exists);
                    m_Target = null;
                    return;
                }
            } catch (Exception ex) {
                displayError(R.string.copy_failed);
                m_Target = null;
            }
        }
        
        public void updateProgress(Integer... params) {
            super.publishProgress(params);
        }
        
        @Override
        protected void onProgressUpdate(Integer... progress) {
            m_StatusUpdater.updateProgress(m_Id, progress[0]);
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
            ConnectivityManager cmgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            ConnectivityManager.WakeLock lock = cmgr.newWakeLock(1, "NookFileManager.copyTask" + hashCode());
            try {
                if (m_Target == null) { return false; }
                lock.acquire();
                copyDirectory(m_Copy, m_Target, this);
                try {
                    if (m_CutOperation.get(m_Copy.getAbsolutePath()) != null) {
                        m_Copy.delete();
                        m_CutOperation.remove(m_Copy.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    Log.e(LOGTAG, "Failed to delete file after copy.", ex);
                }
                lock.release();
                return true;
            } catch (Exception ex) {
                Log.e(LOGTAG, "File copy failed...", ex);
                if (lock.isHeld()) {
                    lock.release();
                }
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                displayInfo(R.string.copy_success);
                m_StatusUpdater.updateProgress(m_Id, 100);
                if (m_CurrentFolder != null && m_CurrentFolder.equals(m_Target.getParent())) {
                    String[] subfolder = {
                        m_CurrentFolder
                    };
                    loadFolders(subfolder, false);
                }
                if (m_CopyFile.size()==0 && m_RemoteCopy.size()==0) {
                    m_PasteButton.setVisibility(View.INVISIBLE);
                }
            } else if (m_Target != null) {
                displayError(R.string.copy_failed);
                m_StatusUpdater.updateProgress(m_Id, -1);
            } else {
                m_StatusUpdater.updateProgress(m_Id, -1);
            }
        }
    }
    
    class RemotePC {
        String ip;
        String user;
        String pass;
        int idx;
    }
}
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
package com.nookdevs.library;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.ConditionVariable;
import android.util.Log;
import android.widget.Toast;

import com.bravo.ecm.service.ScannedFile;
import com.nookdevs.common.nookBaseActivity;

public class BNBooks {
    
    public static final String SYNC_ACTION = "com.bravo.intent.action.SYNC_LOCKER";
    public static final String SYNC_COMPLETE = "com.bravo.intent.action.SYNC_COMPLETE";
    public static final String DOWNLOAD_COMPLETE = "com.bravo.intent.action.DOWNLOAD_COMPLETE";
    public static final String DOWNLOAD_ACTION = "com.bravo.intent.action.DOWNLOAD";
    public static final String DOWNLOAD_PROGRESS = "com.bravo.intent.action.DOWNLOAD_PROGRESS";
    public static final String APP_DB = "/data/data/com.bravo.home/theDB.db";
    public static final String LOCAL_BOOKS_TABLE = "appInfo_table";
  //  public static final String FILES_DB = "/data/data/com.bravo.library/AppInfoDB.db";
    public static final String PRODUCT_TABLE = "bn_client_products";
    public static final String PRODUCT_STATE_TABLE = "bn_client_product_states";
    public static final String LOCAL_PRODUCT_STATE_TABLE = "bn_client_local_product_state";
    public static final String DEVICE_TABLE = "bn_client_device";
    public static final String ARCHIVED = "ARCHIVED";
    public static final String DELETED = "DELETED";
    public static final String SAMPLE = "SAMPLE";
    public static final String LENT = "LENT";
    public static final String BORROWED = "BORROWED";
    public static final String DOWNLOAD = "DOWNLOAD";
    public static final String DOWNLOAD_IN_PROGRESS = "DOWNLOADING";
    public static final String AUTH_URL = "https://cart2.barnesandnoble.com/services/service.asp";
    public static final long TIMEOUT = 300000;
    private List<ScannedFile> m_Books = null;
    private List<ScannedFile> m_ArchivedBooks = null;
    private ScannedFile m_DownloadBook = null;
    private ConditionVariable m_SyncDone = new ConditionVariable();
    private ConditionVariable m_DownloadDone = new ConditionVariable();
    private nookBaseActivity m_Context;
    private String m_DownloadEan;
    private int m_DownloadProgress;
    private SQLiteDatabase m_Db;
 //   private SQLiteDatabase m_FilesDb;
    private HashMap<String, ScannedFile> m_EanMap = new HashMap<String, ScannedFile>();
    private static boolean m_Auth = false;
    private boolean m_Sync = false;
    private Timer m_Timer = new Timer();
    private TimerTask m_TimerTask;
    protected ConnectivityManager cmgr;
    protected ConnectivityManager.WakeLock lock;
    private BroadcastReceiver m_DownloadReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // download complete.
            m_TimerTask.cancel();
            if (lock.isHeld()) {
                lock.release();
            }
            String path = "downloadedFileName";
            if (arg1 == null) {
                path = null;
            } else {
                path = arg1.getStringExtra(path);
            }
            if (path == null) {
                m_DownloadBook.setStatus(DOWNLOAD);
                Toast.makeText(m_Context, R.string.download_failed, Toast.LENGTH_LONG).show();
            } else {
                m_DownloadBook.setPathName(path);
                m_DownloadBook.setStatus(null);
                Toast.makeText(m_Context, R.string.download_complete, Toast.LENGTH_SHORT).show();
            }
            m_Context.unregisterReceiver(m_DownloadReceiver);
            m_Context.unregisterReceiver(m_DownloadProgressReceiver);
            m_DownloadDone.open();
        }
    };
    
    private BroadcastReceiver m_SyncReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // sync complete.
            if (lock.isHeld()) {
                lock.release();
            }
            m_TimerTask.cancel();
            m_Context.unregisterReceiver(m_SyncReceiver);
            loadBooksData();
            m_SyncDone.open();
        }
    };;
    private BroadcastReceiver m_DownloadProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // download progress
            m_DownloadBook.setStatus(DOWNLOAD_IN_PROGRESS + " " + ((int) (arg1.getFloatExtra("percent", 0) * 100))
                / 100.0);
        }
    };
    
    public BNBooks(nookBaseActivity context) {
        cmgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        lock = cmgr.newWakeLock(1, "nookLibrary.BNBooks" + hashCode());
        m_Context = context;
    }
    
    public List<ScannedFile> getBooks() {
        return getBooks(false);
    }
    
    public List<ScannedFile> getBooks(boolean refresh) {
        if (refresh && !m_Context.waitForNetwork(lock)) {
            refresh = false;
        }
        m_Db = SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READONLY);
        //m_FilesDb = SQLiteDatabase.openDatabase(FILES_DB, null, SQLiteDatabase.OPEN_READONLY);
        if (refresh && !m_Auth) {
            if (!authenticate()) {
                refresh = false;
            }
        }
        if (refresh) {
            m_Auth = true;
            m_SyncDone.close();
            m_Sync = true;
            registerSyncReceiver();
            m_TimerTask = new TimerTask() {
                @Override
                public void run() {
                    BNBooks.this.cancel();
                }
            };
            m_Timer.schedule(m_TimerTask, TIMEOUT);
            sync();
            m_SyncDone.block();
        } else {
            loadBooksData();
        }
       // m_FilesDb.close();
        m_Db.close();
        m_Db = null;
        return m_Books;
    }
    
    public ScannedFile getBook(ScannedFile file) {
        if (!m_Context.waitForNetwork(lock)) { return null; }
        if (!m_Auth && !authenticate()) {
            if( lock.isHeld()) lock.release();
            return null;
        }
        m_DownloadEan = file.getEan();
        m_DownloadBook = file;
        m_Db = SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READONLY);
       // m_FilesDb = SQLiteDatabase.openDatabase(FILES_DB, null, SQLiteDatabase.OPEN_READONLY);
        m_Sync = false;
        m_DownloadDone.close();
        registerDownloadReceiver();
        m_TimerTask = new TimerTask() {
            @Override
            public void run() {
                BNBooks.this.cancel();
            }
            
        };
        m_Timer.schedule(m_TimerTask, TIMEOUT);
        download();
        m_DownloadDone.block();
        //m_FilesDb.close();
        m_Db.close();
        return m_DownloadBook;
    }
    
    public void cancel() {
        if (!m_Sync) {
            m_DownloadReceiver.onReceive(null, null);
        } else {
            m_SyncReceiver.onReceive(null, null);
        }
    }
    
    private void registerDownloadReceiver() {
        IntentFilter filter = new IntentFilter(DOWNLOAD_COMPLETE);
        m_Context.registerReceiver(m_DownloadReceiver, filter);
        IntentFilter filter1 = new IntentFilter(DOWNLOAD_PROGRESS);
        m_Context.registerReceiver(m_DownloadProgressReceiver, filter1);
    }
    
    private void sync() {
        Intent intent = new Intent(SYNC_ACTION);
        m_Context.startService(intent);
    }
    
    private void download() {
        Intent intent = new Intent(DOWNLOAD_ACTION);
        intent.putExtra("dstDir", "/system/media/sdcard/my B&N Downloads/");
      //  intent.putExtra("continue", true);
        ArrayList<String> values = new ArrayList<String>(1);
        values.add(m_DownloadEan);
        intent.putStringArrayListExtra("producteanList", values);
        m_Context.startService(intent);
    }
    
    private void registerSyncReceiver() {
        IntentFilter filter = new IntentFilter(SYNC_COMPLETE);
        m_Context.registerReceiver(m_SyncReceiver, filter);
        
    }
    
    private static boolean authenticate() {
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READONLY);
            String user = "";
            String pass = "";
            String devId = "";
            String url = AUTH_URL;
            url += "?sourceID=P001000016&service=1&UIAction=signIn&stage=signIn&";
            String[] columns = {
                "device_id", "user_hash", "password"
            };
            Cursor cursor = db.query(DEVICE_TABLE, columns, null, null, null, null, null);
            cursor.moveToFirst();
            if (cursor.isAfterLast()) { return false; }
            user = cursor.getString(1);
            devId = cursor.getString(0);
            pass = cursor.getString(2);
            cursor.close();
            url += "emailAddress=" + user + "&";
            url += "acctPassword" + pass;
            url += "&devId=" + devId;
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            BufferedInputStream is = new BufferedInputStream(conn.getInputStream(), 100);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) >= 0) {
                buffer[len] = '\0';
                System.out.print(new String(buffer));
            }
            db.close();
            return true;
        } catch (Exception ex) {
            Log.w("Exception during authenticate", ex);
            return false;
        }
    }
    
    public boolean archiveBook(ScannedFile file, boolean val) {
        if (val) {
            m_ArchivedBooks.add(file);
        } else {
            m_ArchivedBooks.remove(file);
        }
        return true;
    }
    public boolean deleteBook(ScannedFile file) {
        return true;
    }
    
    public List<ScannedFile> getArchived() {
        return m_ArchivedBooks;
    }
    
    private void loadBooksData(String ean) {
        try {
            if (ean == null) {
                loadLocalBooks();
            }
            String selection;
            String[] selectionArgs = null;
            if (ean == null) {
                selection = "where ean = product_ean";
            } else {
                selection = "where ean=? and ean = product_ean";
                selectionArgs = new String[1];
                selectionArgs[0] = ean;
            }
            String sql =
                "select type,ean, titles, synopsis, contributors, categories, editorial_reviews,available,publisher,published, thumb_image,"
                    + "rating, locker_status,lending_state,lendee, lender , locker_item_sample , A.created, A.modified from "
                    + PRODUCT_TABLE + " A ," + PRODUCT_STATE_TABLE + " B " + selection;
            Cursor cursor = m_Db.rawQuery(sql, selectionArgs);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                //boolean available = cursor.getInt(7) == 1;
                boolean archived = false;
                String lockerStatus = cursor.getString(12);
                
                String ean1 = cursor.getString(1);
                boolean addToList = false;
                ScannedFile file = m_EanMap.remove(ean1);
                if (ARCHIVED.equals(lockerStatus) || DELETED.equals(lockerStatus)) {
                    if (file != null) {
                        m_Books.remove(file);
                    }
                    if (ARCHIVED.equals(lockerStatus)) {
                        m_ArchivedBooks.add(file);
                        archived = true;
                    } else {
                        cursor.moveToNext();
                        continue;
                    }
                }
                if (file == null) { // not downloaded yet
                    file = new ScannedFile();
                    if (!archived) {
                        addToList = true;
                    }
                }
                String titles = cursor.getString(2);
                JSONArray array = new JSONArray(titles);
                int sz = array.length();
                List<String> title = new ArrayList<String>(sz);
                for (int i = 0; i < sz; i++) {
                    String str = array.optString(i);
                    if (!title.contains(str)) {
                        title.add(str);
                    }
                }
                if (titles.contains("Merriam-Webster's Pocket Dictionary")) {
                    if (!addToList) {
                        m_Books.remove(file);
                    }
                    cursor.moveToNext();
                    continue;
                }
                file.setTitles(title);
                String keywordData = cursor.getString(5);
                String contributorData = cursor.getString(4);
                String desc = cursor.getString(3);
                String reviews = cursor.getString(6);
                if (reviews != null) {
                    array = new JSONArray(reviews);
                    sz = array.length();
                    for (int i = 0; i < sz; i++) {
                        desc += "<br>";
                        desc += array.optString(i);
                    }
                }
                file.setDescription(desc);
                if( contributorData != null) {
                    array = new JSONArray(contributorData);
                    sz = array.length();
                    for (int i = 0; i < sz; i++) {
                        JSONObject object = array.getJSONObject(i);
                        String firstName = object.optString("first_name");
                        String middle = object.optString("middle_name");
                        String lastName = object.optString("last_name");
                        if (middle != null) {
                            firstName += " " + middle;
                        }
                        file.addContributor(firstName, lastName);
                    }
                }
                if( keywordData != null) {
                    array = new JSONArray(keywordData);
                    sz = array.length();
                    for (int i = 0; i < sz; i++) {
                        JSONObject object = array.getJSONObject(i);
                        String keyword = object.optString("name");
                        file.addKeywords(keyword);
                    }
                }
                String type = cursor.getString(0);
                if (type != null) {
                    file.addKeywords(type);
                }
                file.setPublisher(cursor.getString(8));
                Long l = cursor.getLong(9);
                if (l != null) {
                    file.setPublishedDate(new Date(l));
                }
                l = cursor.getLong(16);
                if (l != null) {
                    file.setCreatedDate(new Date(l));
                }
                l = cursor.getLong(17);
                if (l != null) {
                    file.setLastAccessedDate(new Date(l));
                }
                file.updateLastAccessDate();
                String coverURL = cursor.getString(10);
                if( coverURL != null) {
                    JSONObject object = new JSONObject(coverURL);
                    file.setCover(object.optString("url"));
                }
                String lendState = cursor.getString(13);
                file.setLendState(lendState);
                if (archived) {
                    file.setStatus(ARCHIVED);
                } else if (LENT.equals(lendState)) {
                    file.setStatus(LENT);
                    String lendMessage = "On Loan to " + cursor.getString(14);
                    file.setDescription(lendMessage + "<br>" + file.getDescription());
                } else if (BORROWED.equals(lendState)) {
                    String lendMessage = "Borrowed from " + cursor.getString(15);
                    file.setDescription(lendMessage + "<br>" + file.getDescription());
                    file.setStatus(BORROWED);
                } else if (file.getPathName() == null || file.getPathName().equals("")) {
                    file.setStatus(DOWNLOAD);
                }
                int sample = cursor.getInt(16);
                if (sample == 1) {
                    String tmp = file.getStatus();
                    if (tmp == null) {
                        tmp = SAMPLE;
                    } else {
                        tmp = tmp + " " + SAMPLE;
                    }
                    file.setStatus(tmp);
                    file.addKeywords("Sample");
                }
                file.addKeywords("B&N");
                if (addToList) {
                    m_Books.add(file);
                }
                
                cursor.moveToNext();
            }
            cursor.close();
            if (!m_EanMap.isEmpty()) {
                m_Books.removeAll(m_EanMap.values());
            }
        } catch (Exception ex) {
            Log.e("BNBooks", "Exception loading BN books", ex);
        }
    }
    
    private void loadLocalBooks() {
        try {
            m_Books = new ArrayList<ScannedFile>(50);
            m_ArchivedBooks = new ArrayList<ScannedFile>(10);
            String[] columns = {
                "local_product_ean", "downloaded_path"
            };
            String selection;
            String[] selectionArgs = null;
            selection = null;
            Cursor cursor = m_Db.query(LOCAL_PRODUCT_STATE_TABLE, columns, selection, selectionArgs, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ScannedFile file = new ScannedFile(cursor.getString(1), false);
                file.setEan(cursor.getString(0));
                m_Books.add(file);
                m_EanMap.put(file.getEan(), file);
                cursor.moveToNext();
            }
            cursor.close();
        } catch (Exception ex) {
            Log.e("BNBooks", "Exception loading BN books", ex);
        }
    }
    
    private void loadBooksData() {
        loadBooksData(null);
    }
}

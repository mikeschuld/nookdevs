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
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.Bundle;
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
    public static final String STATUS_ACTION = "com.nookdevs.nooksync.SyncService";
    public static final String DOWNLOAD_PROGRESS = "com.bravo.intent.action.DOWNLOAD_PROGRESS";
    public static final String APP_DB = "/data/data/com.bravo.home/theDB.db";
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
    public static final long TIMEOUT = 20000;
    private List<ScannedFile> m_Books = null;
    private List<ScannedFile> m_ArchivedBooks = new ArrayList<ScannedFile>(10);
    private ScannedFile m_DownloadBook = null;
    private ConditionVariable m_SyncDone = new ConditionVariable();
    private ConditionVariable m_DownloadDone = new ConditionVariable();
    private nookBaseActivity m_Context;
    private String m_DownloadEan;
    private int m_DownloadProgress;
    private SQLiteDatabase m_Db;
    // private SQLiteDatabase m_FilesDb;
    private HashMap<String, ScannedFile> m_EanMap = new HashMap<String, ScannedFile>();
    private static boolean m_Auth = false;
    private boolean m_Sync = false;
    private Timer m_Timer = new Timer();
    private TimerTask m_TimerTask;
    protected ConnectivityManager cmgr;
    protected ConnectivityManager.WakeLock lock;
    private static boolean m_DeviceRegistered=false;
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
                try {
                    path = arg1.getStringExtra(path);
                } catch(Throwable ex) {
                    path=null;
                    Log.w("Exception downloading book ", ex);
                }
            }
            if (path == null) {
                String ex = null;
                try {
                    Object data = arg1.getSerializableExtra("exception");
                    if( data instanceof Throwable) {
                        Log.w("Exception downloading book ", (Throwable)data);
                        ex = ((Throwable)data).getMessage().toLowerCase();
                    }
                    else { 
                        Log.w("Exception downloading book ", data.toString());
                        ex = data.toString().toLowerCase();
                    }
                } catch(Throwable exception) {
                    ex=null;
                }
                if( ex != null && ex.contains("already downloaded")) {
                    if( m_DownloadBook.getPathName() != null) {
                        path = m_DownloadBook.getPathName();
                        File f = new File(path);
                        if( f.exists()) {
                            m_DownloadBook.setStatus(null);
                            Toast.makeText(m_Context, R.string.download_complete, Toast.LENGTH_SHORT).show();
                        } else {
                            m_DownloadBook.setStatus(DOWNLOAD);
                            Toast.makeText(m_Context, R.string.download_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    m_DownloadBook.setStatus(DOWNLOAD);
                    Toast.makeText(m_Context, R.string.download_failed, Toast.LENGTH_LONG).show();
                }
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
            m_TimerTask.cancel();
            m_DownloadBook.setStatus(DOWNLOAD_IN_PROGRESS + " " + ((int) (arg1.getFloatExtra("percent", 0) * 100))
                / 100.0);
            m_TimerTask = new TimerTask() {
                @Override
                public void run() {
                    System.out.println("BNCall Timed out...");
                    BNBooks.this.cancel();
                }
                
            };
            m_Timer.schedule(m_TimerTask, TIMEOUT*3);
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
        try {
            if (refresh && !m_Context.waitForNetwork(lock)) {
                refresh = false;
            }
            if( m_Db == null)
                m_Db = SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READONLY);
            // m_FilesDb = SQLiteDatabase.openDatabase(FILES_DB, null,
            // SQLiteDatabase.OPEN_READONLY);
            if ((refresh || !m_DeviceRegistered)&& !m_Auth) {
                if (!authenticate()) {
                    System.out.println("Not authenticated...");
                    refresh = false;
                } 
            }
            if (refresh) {
                m_SyncDone.close();
                m_Sync = true;
                registerSyncReceiver();
                m_TimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("BNCall Timed out...");
                        BNBooks.this.cancel();
                    }
                };
                m_Timer.schedule(m_TimerTask, TIMEOUT);
                sync();
                m_SyncDone.block();
            } else {
                if( m_DeviceRegistered) {
                    loadBooksData();
                } else {
                    System.out.println("Device not registered!!");
                }
            }
            // m_FilesDb.close();
            return m_Books;
        } finally {
            if(lock.isHeld()) 
                lock.release();
        }
    }
    public void clear() {
        if( m_Books != null)
            m_Books.clear();
    }
    public void close() {
        if( m_Db != null) {
            m_Db.close();
            m_Db=null;
        }
    }
    
    public ScannedFile getBook(ScannedFile file) {
        try {
            if (!m_Context.waitForNetwork(lock)) { return null; }
            if (!m_Auth && !authenticate()) {
                if (lock.isHeld()) {
                    lock.release();
                }
                return null;
            }
            if( !m_DeviceRegistered) {
                return null;
            }
            m_DownloadEan = file.getEan();
            m_DownloadBook = file;
            if( m_Db == null)
                m_Db = SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READONLY);
            // m_FilesDb = SQLiteDatabase.openDatabase(FILES_DB, null,
            // SQLiteDatabase.OPEN_READONLY);
            m_Sync = false;
            m_DownloadDone.close();
            registerDownloadReceiver();
            m_TimerTask = new TimerTask() {
                @Override
                public void run() {
                    System.out.println("BNCall Timed out...");
                    BNBooks.this.cancel();
                }
                
            };
            m_Timer.schedule(m_TimerTask, TIMEOUT*3);
            download();
            m_DownloadDone.block();
            // m_FilesDb.close();
            return m_DownloadBook;
        } finally {
            if( lock.isHeld()) {
                lock.release();
            }
        }
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
        // intent.putExtra("continue", true);
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
        SQLiteDatabase db = null;
        try {
            db=SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READONLY);
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
            if (cursor.isAfterLast()) { 
                m_DeviceRegistered=false;
                db.close();
                return false; 
            }
            user = cursor.getString(1);
            devId = cursor.getString(0);
            pass = cursor.getString(2);
            cursor.close();
            if( pass == null || pass.trim().equals("")) {
                m_DeviceRegistered=false;
                db.close();
                return false;
            }
            m_DeviceRegistered=true;
//            url += "emailAddress=" + user + "&";
//            url += "acctPassword" + pass;
//            url += "&devId=" + devId;
//            URL aURL = new URL(url);
//            URLConnection conn = aURL.openConnection();
//            conn.connect();
          //  BufferedInputStream is = new BufferedInputStream(conn.getInputStream(), 100);
          //  byte[] buffer = new byte[1024];
          //  int len;
          //  while ((len = is.read(buffer)) >= 0) {
          //      buffer[len] = '\0';
          //      System.out.print(new String(buffer));
          //  }
            db.close();
            m_Auth=true;
            return true;
        } catch (Exception ex) {
            Log.w("Exception during authenticate", ex);
            if( db != null) {
                try {
                    db.close();
                } catch(Exception ex1) {
                    
                }
            }
            return false;
        }
    }
    private boolean archiveBookInServer(String ean, String path, boolean archive) {
        try {
            Intent intent = new Intent(STATUS_ACTION);
            if( archive) {
                intent.putExtra("status", "ARCHIVED");
            }
            else
                intent.putExtra("status","MAIN");
            intent.putExtra("ean", ean);
            intent.putExtra("path", path);
            ComponentName ret = m_Context.startService(intent);
            if( ret == null) {
                Log.e("BNBooks", "unable to load nookSync");
                return false;
            }
        } catch(Exception ex) {
            Log.e("BNBooks", "archive book:" + ex.getMessage(), ex);
            return false;
        }
        return true;
    }
    public boolean archiveBook(ScannedFile file, boolean val) {
        boolean ret = archiveBookInServer(file.getEan(),file.getPathName(),val);
        if( !ret) return ret;
        if (val) {
            m_ArchivedBooks.add(file);
            file.setStatus(ARCHIVED);
        //    deleteBook( file.getPathName());
        } else {
            m_ArchivedBooks.remove(file);
            file.setStatus(DOWNLOAD);
            file.setPathName(null);
        }
        return true;
    }
    
    private boolean deleteBookInServer(String id) {
        try {
            Intent intent = new Intent(STATUS_ACTION);
            intent.putExtra("status", "DELETED");
            intent.putExtra("ean", id);
            ComponentName ret = m_Context.startService(intent);
            if( ret ==null)
                return false;
        } catch(Exception ex) {
            Log.e("BNBooks", "delete book in server :" + ex.getMessage(), ex);
            return false;
        }
        return true;
    }
    private boolean deleteBook(String path) {
        if( path == null || path.trim().equals(""))
            return true;
        File f = new File(path);
        if( !f.exists()) 
            return true;
        return f.delete();
    }
    public boolean deleteBook(ScannedFile file) {
        boolean ret= deleteBookInServer(file.getEan()) && deleteBook(file.getPathName());
        if( ret) {
            if(m_ArchivedBooks.contains(file))
                m_ArchivedBooks.remove(file);
            file.setStatus("DELETED");
        }
        return ret;
    }
    
    public List<ScannedFile> getArchived() {
        return m_ArchivedBooks;
    }
    public Vector<String> searchDescription( String keyword) {
        Vector<String> str = new Vector<String>();
        try {
            if (m_Db == null) {
                m_Db = SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READONLY);
            }
            String selection;
            String[] selectionArgs = null;
            selection = "where synopsis like '%" + keyword + "%' or editorial_reviews like '%" + keyword +"%'";
            String sql =
                "select ean, synopsis,editorial_reviews from "
                    + PRODUCT_TABLE + " A " + selection;
            Cursor cursor = m_Db.rawQuery(sql, null);
            cursor.moveToFirst();
            while( !cursor.isAfterLast()) {
                str.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
        } catch(Exception ex) {
            Log.e("BNBooks", "Exception searching BN books description " + keyword , ex);
        }
        return str;
    }
    public void getKeywords(String ean, List<String> keywords) {
        String keywordStr = getKeywordsString( ean);
        try {
            if (keywordStr != null) {
                JSONArray array = new JSONArray(keywordStr);
                int sz = array.length();
                for (int i = 0; i < sz; i++) {
                    JSONObject object = array.getJSONObject(i);
                    String keyword = object.optString("name");
                    if( !keywords.contains(keyword))
                        keywords.add(keyword);
                }
            }
        } catch(Exception ex) {
            Log.e("BNBooks", "Exception loading BN books keywords List " + ean, ex);
        }
    }    
    public String getKeywordsString(String ean) {
        String keywordStr=null;
        try {
            if (m_Db == null) {
                m_Db = SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READONLY);
            }
            String selection;
            String[] selectionArgs = null;
            selection = " where ean=?";
            selectionArgs = new String[1];
            selectionArgs[0] = ean;
            String sql =
                "select categories from "+ PRODUCT_TABLE + selection;
            Cursor cursor = m_Db.rawQuery(sql, selectionArgs);
            cursor.moveToFirst();
            if( !cursor.isAfterLast())
                keywordStr = cursor.getString(0);
            cursor.close();
        } catch(Exception ex) {
            Log.e("BNBooks", "Exception loading BN books keywords " + ean, ex);
        }
        return keywordStr;
    }
    public String getDescription( String ean) {
        String desc=null;
        try {
            if (m_Db == null) {
                m_Db = SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READONLY);
            }
            String selection;
            String[] selectionArgs = null;
            selection = "where ean=? and ean = product_ean";
            selectionArgs = new String[1];
            selectionArgs[0] = ean;
            String sql =
                "select synopsis, editorial_reviews,lending_state,lendee, lender from "
                    + PRODUCT_TABLE + " A ," + PRODUCT_STATE_TABLE + " B " + selection;
            Cursor cursor = m_Db.rawQuery(sql, selectionArgs);
            cursor.moveToFirst();
            String lendState = cursor.getString(2);
            String lendMessage=null;
            desc = cursor.getString(0);
            if( LENT.equals(lendState)) {
                lendMessage = "On Loan to " + cursor.getString(3);
                desc =lendMessage + "<br>" + desc;
                
            } else if (BORROWED.equals(lendState)) {
                lendMessage = "Borrowed from " + cursor.getString(4);
                desc =lendMessage + "<br>" + desc;
            }
            String reviews = cursor.getString(1);
            if (reviews != null) {
                JSONArray array = new JSONArray(reviews);
                int sz = array.length();
                for (int i = 0; i < sz; i++) {
                    desc += "<br>";
                    desc += array.optString(i);
                }
            }
            cursor.close();
        } catch(Exception ex) {
            Log.e("BNBooks", "Exception loading BN books description ", ex);
        } 
        return desc;
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
                // boolean available = cursor.getInt(7) == 1;
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
                        if( file == null) {
                            file = new ScannedFile();
                            file.setEan(ean1);
                            addToList=false;
                        } 
                        file.setStatus(ARCHIVED);
                        m_ArchivedBooks.add(file);
                        archived = true;
                    } else {
                        cursor.moveToNext();
                        continue;
                    }
                }
                if (file == null) { // not downloaded yet
                    file = new ScannedFile();
                    file.setEan(ean1);
                    addToList = true;
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
                        if( archived) {
                            m_ArchivedBooks.remove(file);
                        } else
                            m_Books.remove(file);
                    }
                    cursor.moveToNext();
                    continue;
                }
                file.setTitles(title);
             //   String keywordData = cursor.getString(5);
                String contributorData = cursor.getString(4);
               // String desc = cursor.getString(3);
               // String reviews = cursor.getString(6);
               // if (reviews != null) {
               //     array = new JSONArray(reviews);
               //     sz = array.length();
               //     for (int i = 0; i < sz; i++) {
               //         desc += "<br>";
               //         desc += array.optString(i);
               //     }
               // }
               // file.setDescription(desc);
                if (contributorData != null) {
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
//                if (keywordData != null) {
//                    array = new JSONArray(keywordData);
//                    sz = array.length();
//                    for (int i = 0; i < sz; i++) {
//                        JSONObject object = array.getJSONObject(i);
//                        String keyword = object.optString("name");
//                        file.addKeywords(keyword);
//                    }
//                }
                String type = cursor.getString(0);
                if (type != null) {
                    file.addKeywords(type);
                }
                file.setPublisher(cursor.getString(8));
                Long l = cursor.getLong(9);
                if (l != null) {
                    file.setPublishedDate(new Date(l));
                }
                l = cursor.getLong(17);
                if (l != null) {
                    file.setCreatedDate(new Date(l));
                }
                l = cursor.getLong(18);
                if (l != null) {
                    file.setLastAccessedDate(new Date(l));
                }
                file.updateLastAccessDate();
                String coverURL = cursor.getString(10);
                if (coverURL != null) {
                    JSONObject object = new JSONObject(coverURL);
                    file.setCover(object.optString("url"));
                }
                String lendState = cursor.getString(13);
                file.setLendState(lendState);
                if (archived) {
                    file.setStatus(ARCHIVED);
                } else if (LENT.equals(lendState)) {
                    file.setStatus(LENT);
        //            String lendMessage = "On Loan to " + cursor.getString(14);
         //           file.setDescription(lendMessage + "<br>" + file.getDescription());
                } else if (BORROWED.equals(lendState)) {
           //         String lendMessage = "Borrowed from " + cursor.getString(15);
           //         file.setDescription(lendMessage + "<br>" + file.getDescription());
                    file.setStatus(BORROWED);
                } else if (file.getPathName() == null || file.getPathName().equals("")) {
                    file.setStatus(DOWNLOAD);
                } else {
                    File f= new File(file.getPathName());
                    if( !f.exists()) {
                        file.setStatus(DOWNLOAD);
                    }
                }
                int sample = cursor.getInt(16);
                if (sample == 1) {
                    file.setSample(true);
                    file.addKeywords("Sample");
                } else {
                    file.setSample(false);
                }
                file.setLibrary("B&N");
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
    public void updatePageNumbers() {
        try {
            String[] columns = {
                "local_product_ean", "downloaded_path", "current_page", "total_pages"
            };
            String selection;
            String[] selectionArgs = null;
            selection = null;
            Cursor cursor = m_Db.query(LOCAL_PRODUCT_STATE_TABLE, columns, selection, selectionArgs, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ScannedFile file = ScannedFile.getFile(cursor.getString(1));
                if( file == null) {
                    cursor.moveToNext();
                    continue;
                }
                file.setCurrentPage( cursor.getInt(2));
                file.setTotalPages( cursor.getInt(3));
                cursor.moveToNext();
            }
            cursor.close();
        } catch(Exception ex) {
            Log.e("BNBooks", "Exception loading BN books", ex);
        }
    }
    private void loadLocalBooks() {
        try {
            m_Books = new ArrayList<ScannedFile>(50);
            m_ArchivedBooks.clear();
            String[] columns = {
                "local_product_ean", "downloaded_path", "current_page", "total_pages"
            };
            String selection;
            String[] selectionArgs = null;
            selection = null;
            Cursor cursor = m_Db.query(LOCAL_PRODUCT_STATE_TABLE, columns, selection, selectionArgs, null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ScannedFile file = new ScannedFile(cursor.getString(1), false);
                file.setCurrentPage( cursor.getInt(2));
                file.setTotalPages( cursor.getInt(3));
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

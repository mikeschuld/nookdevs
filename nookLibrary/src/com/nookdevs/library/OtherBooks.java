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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.util.Log;

import com.bravo.ecm.service.ScannedFile;
import com.nookdevs.common.nookBaseActivity;

public class OtherBooks extends SQLiteOpenHelper {
    public static final String BOOKS_DB = "myBooks.db";
    public static final String CREATE_BOOKS_TABLE =
        " create table books ( id integer primary key autoincrement, ean text, titles text not null, "
            + "authors text not null, desc text, keywords text, publisher text, cover text, published long, created long, path text not null unique, series text, status text)";
    public static final String CREATE_LOG_TABLE = "create table log ( last_update long )";
    private NookLibrary nookLib;
    public static final int VERSION = 10;
    private List<ScannedFile> m_Files = new ArrayList<ScannedFile>(100);
    private List<ScannedFile> m_ArchivedFiles = new ArrayList<ScannedFile>(100);
    private SQLiteDatabase m_Db = null;
    List<String> m_DeleteBooks = new ArrayList<String>(10);
    List<File> m_UpdatedFiles = new ArrayList<File>(100);
    private MediaScannerNotifier m_ScannerNotifier;
    
    public OtherBooks(NookLibrary context) {
        super(context, BOOKS_DB, null, VERSION);
        nookLib = context;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_BOOKS_TABLE);
            db.execSQL(CREATE_LOG_TABLE);
            db.beginTransaction();
            db.execSQL("insert into log values(0)");
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception ex) {
            db.endTransaction();
            Log.e("OtherBooks", "exception while creating tables", ex);
        }
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE BOOKS ADD COLUMN STATUS TEXT");
    }
    
    private void updateLastScan() {
        try {
            if (m_Db == null) {
                m_Db = getWritableDatabase();
            }
            ContentValues values = new ContentValues();
            values.put("last_update", System.currentTimeMillis());
            m_Db.beginTransaction();
            m_Db.update("LOG", values, null, null);
            m_Db.setTransactionSuccessful();
            m_Db.endTransaction();
            return;
        } catch (Exception ex) {
            Log.e("OtherBooks", "Exception updating last scan time", ex);
        }
    }
    
    private Date getLastScanDate() {
        try {
            if (m_Db == null) {
                m_Db = getWritableDatabase();
            }
            String[] columns = {
                "last_update"
            };
            Cursor cursor = m_Db.query("LOG", columns, null, null, null, null, null);
            Date date;
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                date = new Date(cursor.getLong(0));
            } else {
                date = new Date(0);
            }
            cursor.close();
            return date;
        } catch (Exception ex) {
            return new Date(0);
        }
    }
    private boolean deleteBooks() {
        return deleteBooks(false);
    }
    private boolean deleteBooks(boolean path) {
        try {
            String[] values = m_DeleteBooks.toArray(new String[1]);
            String whereclause = "";
            for( int start=0; start < values.length; start+=200) { 
                for (int i = start; i < values.length && i < start+200; i++) {
                    if (i == 0) {
                        if( path)
                            whereclause += "path=?";
                        else
                            whereclause +="id=?";
                    } else {
                        if(path)
                            whereclause += " or path=?";
                        else
                            whereclause +="or id=?";
                    }
                }
                m_Db.beginTransaction();
                int rows = m_Db.delete("BOOKS", whereclause, values);
                Log.i("OtherBooks", "Rows deleted =" + rows);
                m_Db.setTransactionSuccessful();
                m_Db.endTransaction();
            }
        } catch (Exception ex) {
            Log.e("OtherBooks", "Exception deleting books", ex);
            m_Db.endTransaction();
            return false;
        }
        return true;
    }
    public void addBookToDB(ScannedFile file) {
        try {
            ContentValues values = new ContentValues();
            values.put("ean", file.getEan());
            values.put("series", file.getSeries());
            values.put("publisher", file.getPublisher());
            values.put("path", file.getPathName());
            values.put("cover", file.getCover());
            if (file.getPublishedDate() != null) {
                values.put("published", file.getPublishedDate().getTime());
            }
            if (file.getCreatedDate() != null) {
                values.put("created", file.getCreatedDate().getTime());
            }
            values.put("desc", file.getDescription(true));
            List<String> titles = file.getTitles();
            String title = "";
            for (int i = 0; i < titles.size(); i++) {
                if (i == 0) {
                    title += titles.get(i);
                } else {
                    title += "," + titles.get(i);
                }
            }
            values.put("titles", title);
            values.put("authors", file.getAuthor());
            String keyword = "";
            List<String> keywords = file.getKeywords();
            for (int i = 0; i < keywords.size(); i++) {
                if (i != 0) {
                    keyword += ",";
                }
                keyword += keywords.get(i);
            }
            values.put("keywords", keyword);
            values.put("status", file.getStatus());
            m_Db.beginTransaction();
            m_Db.insert("BOOKS", null, values);
            m_Db.setTransactionSuccessful();
            m_Db.endTransaction();
            file.setDescription(null);
            file.setBookInDB(true);
            file.clearKeywords();
        } catch (Exception ex) {
            Log.e("OtherBooks", "Error adding books to DB");
            m_Db.endTransaction();
        }
    }
    public void updateCover(ScannedFile file) {
        try {
            if (m_Db == null || m_Db.isReadOnly()) {
                if( m_Db != null) m_Db.close();
                m_Db = getWritableDatabase();
            }
            ContentValues values = new ContentValues();
            values.put("cover", file.getCover());
            String whereClause = " path=?";
            String [] whereArgs = new String[1];
            whereArgs[0] = file.getPathName();
            m_Db.beginTransaction();
            m_Db.update("BOOKS", values, whereClause, whereArgs);
            m_Db.setTransactionSuccessful();
        } catch(Exception ex) {
            Log.e("OtherBooks", "Exception updating book cover info", ex);
        } finally {
            if( m_Db != null)
                m_Db.endTransaction();
        }
    }
    public Vector<String> searchDescription( String keyword) {
        Vector<String> str = new Vector<String>();
        try {
            if (m_Db == null) {
                m_Db = getReadableDatabase();
            }
            String selection;
            selection = "where desc like '%" + keyword + "%'";
            String sql =
                "select path from books " + selection;
            Cursor cursor = m_Db.rawQuery(sql, null);
            cursor.moveToFirst();
            while( !cursor.isAfterLast()) {
                str.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
        } catch(Exception ex) {
            Log.e("OtherBooks", "Exception searching Other books description ", ex);
        }
        return str;
    }
    public void getKeywords(String path, List<String> keywords) {
        String keywordStr = getKeywordsString( path);
        try {
            if (keywordStr != null) {
                if (keywordStr != null) {
                    StringTokenizer token = new StringTokenizer(keywordStr, ",");
                    while (token.hasMoreTokens()) {
                        String keyword = token.nextToken();
                        if( !keywords.contains(keyword))
                            keywords.add(keyword);
                    }
                }
            }
        } catch(Exception ex) {
            Log.e("OtherBooks", "Exception loading  books keywords List for " + path, ex);
        }
    }    
    public String getKeywordsString(String path) {
        String keywordStr=null;
        try {
            if (m_Db == null) {
                m_Db = getReadableDatabase();
            }
            String selection;
            String[] selectionArgs = null;
            selection = " where path=?";
            selectionArgs = new String[1];
            selectionArgs[0] = path;
            String sql =
                "select keywords from books "+ selection;
            Cursor cursor = m_Db.rawQuery(sql, selectionArgs);
            cursor.moveToFirst();
            if(!cursor.isAfterLast())
                keywordStr = cursor.getString(0);
            cursor.close();
        } catch(Exception ex) {
            Log.e("OtherBooks", "Exception loading books keywords Str for " + path , ex);
        }
        return keywordStr;
    }

    
    public String getDescription(String path) {
        String desc=null;
        try {
            if (m_Db == null) {
                m_Db = getReadableDatabase();
            }
            String query =
                "select desc from books where path=" + path;
            Cursor cursor = m_Db.rawQuery(query, null);
            cursor.moveToFirst();
            if(!cursor.isAfterLast())
                desc = cursor.getString(0);
            cursor.close();
        } catch(Exception ex) {
            Log.e("FictionwiseBooks", "Exception querying book desc", ex);
        }
        return desc;
    }
    private List<ScannedFile> getBooksFromDB() {
        try {
            if (m_Db == null) {
                m_Db = getWritableDatabase();
            }
            m_DeleteBooks.clear();
            Date lastScan = getLastScanDate();
            String query =
                "select id, ean,titles,authors,desc,keywords,publisher,cover,published,created,path,series,status from books";
            Cursor cursor = m_Db.rawQuery(query, null);
            int size = cursor.getCount();
            cursor.moveToFirst();
            for (int i = 0; i < size; i++) {
                String path = cursor.getString(10);
                File file = new File(path);
                File skip = new File(file.getParent() + "/" + ".skip");
                if (!file.exists() || skip.exists()) {
                    m_DeleteBooks.add(cursor.getString(0));
                    cursor.moveToNext();
                    continue;
                }
                String status=null;
                File f = new File( file.getParent(), "." + file.getName() + ".archive");
                if( f.exists()) 
                    status=BNBooks.ARCHIVED;
                else
                    status = cursor.getString(12);
                Date lastModified = new Date(file.lastModified());
                
                if (lastModified.after(lastScan) && !BNBooks.ARCHIVED.equalsIgnoreCase(status)
                        ) {
                    m_DeleteBooks.add(cursor.getString(0));
                    cursor.moveToNext();
                    continue;
                } else {
                    m_UpdatedFiles.add(file);
                }
                ScannedFile sf = new ScannedFile(path, false);
                if( status != null)
                    sf.setStatus(status);
                sf.setBookInDB(true);
                if (sf.getStatus() != null && sf.getStatus().equalsIgnoreCase(BNBooks.ARCHIVED)) {
                    m_ArchivedFiles.add(sf);
                } else {
                    m_Files.add(sf);
                }
                sf.setEan(cursor.getString(1));
                sf.setPublisher(cursor.getString(6));
                sf.setCover(cursor.getString(7));
                sf.setPublishedDate(new Date(cursor.getLong(8)));
                sf.setCreatedDate(new Date(cursor.getLong(9)));
                sf.setLastAccessedDate(lastModified);
                String title = cursor.getString(2);
                StringTokenizer token = new StringTokenizer(title, ",");
                List<String> titles = new ArrayList<String>(1);
                while (token.hasMoreTokens()) {
                    titles.add(token.nextToken());
                }
                sf.setTitles(titles);
                String author = cursor.getString(3);
                token = new StringTokenizer(author, ",");
                while (token.hasMoreTokens()) {
                    String auth = token.nextToken();
                    sf.addContributor(auth, "");
                }
//                String desc = cursor.getString(4);
//                if (desc != null) {
//                    sf.setDescription(desc);
//                }
//                String keywords = cursor.getString(5);
//                if (keywords != null) {
//                    token = new StringTokenizer(keywords, ",");
//                    while (token.hasMoreTokens()) {
//                        String keyword = token.nextToken();
//                        sf.addKeywords(keyword);
//                    }
//                }
                sf.setSeries(cursor.getString(11));
                cursor.moveToNext();
            }
            cursor.close();
            if (m_DeleteBooks.size() > 0) {
                deleteBooks();
                m_DeleteBooks.clear();
            }
            Log.i("OtherBooks", " adding books from db - count = " + m_Files.size());
            Log.i("OtherBooks", " count of books already updated = " + m_UpdatedFiles.size());
            if( nookLib != null) nookLib.updatePageView(m_Files);
            m_Files.clear();
            updateLastScan();
        } catch (Exception ex) {
            Log.e("OtherBooks", "Exception querying datbase", ex);
        }
        return null;
    }
    
    private boolean updateStatusInDB(ScannedFile file) {
        try {
            if (m_Db == null) {
                m_Db = getWritableDatabase();
            }
            String[] whereArgs = {
                file.getPathName()
            };
            ContentValues values = new ContentValues();
            values.put("status", file.getStatus());
            m_Db.beginTransaction();
            m_Db.update("BOOKS", values, "path = ?", whereArgs);
            m_Db.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e("OtherBooks", "Exception updating datbase", ex);
            return false;
        } finally {
            m_Db.endTransaction();
        }
        return true;
    }
    
    public boolean deleteBook(ScannedFile file) {
        m_DeleteBooks.clear();
        m_DeleteBooks.add(file.getPathName());
        deleteBooks(true);
        m_DeleteBooks.clear();
        if( m_ArchivedFiles.contains(file)) {
            m_ArchivedFiles.remove(file);
        }
        File f = new File(file.getPathName());
        f.delete();
        f = new File( f.getParent(), "." + f.getName() + ".archive");
        if( f.exists()) {
            f.delete();
        }
        if (file.getCover() != null) {
            try {
                f = new File(file.getCover());
                f.delete();
            } catch (Exception ex) {
                return true;
            }
        }
        return true;
    }
    
    public List<ScannedFile> getArchived() {
        return m_ArchivedFiles;
    }
    
    public boolean archiveBook(ScannedFile file, boolean val) {
        if (val) {
            file.setStatus(BNBooks.ARCHIVED);
            m_ArchivedFiles.add(file);
            File f = new File( file.getPathName());
            f = new File( f.getParent(), "." + f.getName() + ".archive");
            try {
                f.createNewFile();
            } catch(Exception ex) {
                Log.e("OtherBooks", ex.getMessage(), ex);
            }
       
        } else {
            file.setStatus(null);
            m_ArchivedFiles.remove(file);
            File f = new File( file.getPathName());
            f = new File( f.getParent(), "." + f.getName() + ".archive");
            if( f.exists()) {
                f.delete();
            }
        }
        return updateStatusInDB(file);
    }
    
    public void getOtherBooks() {
        try {
            m_ArchivedFiles.clear();
            if( nookLib != null) {
                m_ScannerNotifier = new MediaScannerNotifier(nookLib);
                getBooksFromDB();
            }
            File file = new File(nookBaseActivity.SDFOLDER);
            File external = new File(nookBaseActivity.EXTERNAL_SDFOLDER);
            FileFilter filter = new FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        if (f.getName().equals("my B&N downloads")) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                    if (m_UpdatedFiles.contains(f)) { return false; }
                    String extension = f.getName().toLowerCase();
                    if (extension.endsWith("epub") || extension.endsWith("htm") || extension.endsWith("txt")
                        || extension.endsWith("html") || extension.endsWith("pdf") || extension.endsWith("fb2")
                        || extension.endsWith("fb2.zip") || extension.endsWith("cbx") || extension.endsWith("cbr")
                        || extension.endsWith("pdb")) {
                        return true;
                    } else {
                        return false;
                    }
                }
                
            };
            retrieveFiles(file, filter);
            retrieveFiles(external, filter);
            if (m_Files.size() > 0 && nookLib != null) {
                nookLib.updatePageView(m_Files);
                m_Files.clear();
            }
            if( nookLib != null) {
                m_ScannerNotifier.waitForCompletion();
                m_UpdatedFiles.clear();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }
    
    private void retrieveFiles(File base, FileFilter filter) {
        File skipFile = new File(base, ".skip");
        if (skipFile.exists()) { return; }
        
        File[] files = base.listFiles(filter);
        if (files == null) { return; }
        for (File file : files) {
            if (file.isDirectory()) {
                retrieveFiles(file, filter);
            } else {
                if (file.getName().startsWith(".")) {
                    continue;
                }
                String ext = file.getAbsolutePath().toLowerCase();
                ext = ext.substring(ext.lastIndexOf('.') + 1);
                if ("pdb".equals(ext)) {
                    m_ScannerNotifier.addFile(file.getAbsolutePath());
                } else {
                    ScannedFile file1 = new ScannedFile(file.getAbsolutePath());
                    file1.setLastAccessedDate(new Date(file.lastModified()));
                    file1.setBookInDB(false);
                    File f1 = new File( file.getParent(), "." + file.getName()+ ".archive");
                    if( f1.exists()) {
                        file1.setStatus(BNBooks.ARCHIVED);
                        m_ArchivedFiles.add(file1);
                        continue;
                    } else
                        m_Files.add(file1);
                    if (m_Files.size() % 100 == 0 && nookLib != null) {
                        nookLib.updatePageView(m_Files);
                        m_Files.clear();
                    }
                }
            }
        }
        return;
    }
    public void updatePageNumbers(final List<ScannedFile> files) {
        Runnable thrd1 = new Runnable() {
            public void run() {
                m_ScannerNotifier = new MediaScannerNotifier(nookLib, true);
                for(ScannedFile f:files) {
                    if( f.matchSubject("B&N") || ( f.getBookID() ==null && "pdb".equals(f.getType()))) {
                        continue;
                    }
                    m_ScannerNotifier.addFile( f.getPathName());
                }
            }
        };
        ( new Thread(thrd1)).start();
        Runnable thrd = new Runnable() {
            public void run() {
                m_ScannerNotifier.waitForCompletion();
                m_ScannerNotifier.setPagesOnly(false);
                nookLib.refreshPage();
            }
        };
        ( new Thread(thrd)).start();
    }
}
class MediaScannerNotifier implements MediaScannerConnectionClient {
    private MediaScannerConnection mConnection;
    private int mReqCount = 0;
    private ArrayList<String> mWaitList = new ArrayList<String>(10);
    private ArrayList<ScannedFile> mFiles = new ArrayList<ScannedFile>(10);
    private NookLibrary mNookLib;
    private boolean mOnlyPages=false;
    
    public synchronized void addFile(String path) {
        mReqCount++;
        mWaitList.add(path);
    }
    public synchronized void scanFile(String path) {
        if (path == null) { return; }
        if (mConnection.isConnected()) {
            String mime = "ebook/";
            String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
            mime += ext;
            mConnection.scanFile(path, mime);
        } 
    }
    public void setPagesOnly(boolean b) {
        mOnlyPages = b;
        
    }
    public MediaScannerNotifier(NookLibrary context) {
        this(context, false);
    }
    public MediaScannerNotifier(NookLibrary context, boolean onlyPages) {
        mConnection = new MediaScannerConnection(context, this);
        mConnection.connect();
        mNookLib = context;
        mOnlyPages=onlyPages;
    }
    
    public void onMediaScannerConnected() {
        if(mReqCount >0) {
            mReqCount--;
            String s = mWaitList.remove(0);
            scanFile(s);
        }
    }
    public void connect() {
        mConnection.connect();
    }
    
    public void onScanCompleted(String path, Uri arg1) {
        if( arg1 ==null) return;
        String ext = path.toLowerCase();
        if (!mOnlyPages && !ext.endsWith("pdb")) { return; }
        ScannedFile file = null;
        if( mOnlyPages)
            file = ScannedFile.getFile(path);
        else
            file = new ScannedFile(path);
        String[] columns;
        String[] columns2 = { "current_page", "total_pages" };
        String[] columns1 = {
            "title", "authors", "ean", "publisher", "date_published", "current_page", "total_pages"
        };
        if( mOnlyPages) {
            columns = columns2;
        } else {
            columns = columns1;
        }
        Cursor dbCursor = mNookLib.getContentResolver().query(arg1, columns, null, null, null);
        dbCursor.moveToFirst();
        if( mOnlyPages) {
            file.setCurrentPage(dbCursor.getInt(0));
            file.setTotalPages(dbCursor.getInt(1));
            dbCursor.close();
            synchronized (this) {
                if( mReqCount >0) {
                    mReqCount--;
                    String s = mWaitList.remove(0);
                    scanFile(s);
                }
            }
            return;
        }
        file.setTitle(dbCursor.getString(0));
        file.addContributor(dbCursor.getString(1), "");
        file.setEan(dbCursor.getString(2));
        file.setPublisher(dbCursor.getString(3));
        file.setPublishedDate(new Date(dbCursor.getLong(4)));
        file.setCurrentPage(dbCursor.getInt(5));
        file.setTotalPages(dbCursor.getInt(6));
        file.updateLastAccessDate();
        file.setBookInDB(false);
        dbCursor.close();
        mFiles.add(file);
        synchronized (this) {
            if( mReqCount >0)   {
                mReqCount--;
                String s = mWaitList.remove(0);
                scanFile(s);
            }
        }
    }
    
    public void waitForCompletion() {
        while (mReqCount > 0) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
                
            }
        }
        mConnection.disconnect();
        if( !mOnlyPages) {
            mNookLib.updatePageView(mFiles);
            mFiles.clear();
        }
    }
}

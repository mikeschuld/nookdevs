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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import com.bravo.ecm.service.ScannedFile;
import com.nookdevs.common.nookBaseActivity;

public class FictionwiseBooks extends SQLiteOpenHelper {
    
    public static final String AUTH_URL =
        "https://www.fictionwise.com/servlet/mw?action=login&si=0&continue=&mobile=mobile.fictionwise.com";
    public static final String BOOKSHELF_URL = "http://www.fictionwise.com/servlet/mw?a=mv&t=m_bookshelf&si=0";
    public static final String DOWNLOAD_URL = "http://www.fictionwise.com/servlet/mw?action=download";
    public static final String BOOKS_DB = "fictionwise.db";
    public static final String CREATE_BOOKS_TABLE =
        " create table books ( id integer primary key autoincrement, ean text, titles text not null, "
            + "authors text not null, desc text, keywords text, publisher text, cover text, published long, created long, path text, series text, bookid text not null unique, downloadUrl text, status text)";
    public static final String CREATE_USER_TABLE = " create table user ( login text not null, pass text not null)";
    protected NookLibrary nookLib;
    public static final int VERSION = 11;
    protected ConnectivityManager cmgr;
    protected ConnectivityManager.WakeLock lock;
    private SQLiteDatabase m_Db;
    protected static String m_User;
    protected static String m_Pass;
    protected String m_BookShelfHtml;
    protected DefaultHttpClient httpClient;
    protected ArrayList<ScannedFile> m_Files = new ArrayList<ScannedFile>(50);
    protected ArrayList<ScannedFile> m_ArchivedFiles = new ArrayList<ScannedFile>(10);
    protected HashMap<String, ScannedFile> m_BookIdMap = new HashMap<String, ScannedFile>();
    private static String m_BaseDir;
    protected static boolean m_Auth = false;
    static {
        try {
            File file = new File(nookBaseActivity.EXTERNAL_SDFOLDER + "/" + "fictionwise/");
            if (!file.exists()) {
                file = new File(nookBaseActivity.SDFOLDER + "/" + "fictionwise/");
                file.mkdir();
            }
            m_BaseDir = file.getAbsolutePath() + "/";
            file = new File(m_BaseDir + ".skip");
            file.createNewFile();
            
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception in init static block ", ex);
        }
    }
    
    public FictionwiseBooks(NookLibrary context) {
        this(context, BOOKS_DB, VERSION);
    }
    
    public FictionwiseBooks(NookLibrary context, String dbName, int version) {
        super(context, dbName, null, version);
        nookLib = context;
        cmgr = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        lock = cmgr.newWakeLock(1, "nookLibrary.FictionwiseBooks" + hashCode());
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 100);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        httpClient = new DefaultHttpClient(params);
    }
    
    private boolean authenticate() {
        String url = AUTH_URL;
        try {
            nookLib.waitForNetwork(lock);
            SSLSocketFactory factory = SSLSocketFactory.getSocketFactory();
            X509HostnameVerifier orgVerifier = factory.getHostnameVerifier();
            factory.setHostnameVerifier(new AllowAllHostnameVerifier());
            HttpPost request = new HttpPost(url);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("loginid", m_User));
            nvps.add(new BasicNameValuePair("password", m_Pass));
            nvps.add(new BasicNameValuePair("login", "Login"));
            request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            
            HttpResponse response = httpClient.execute(request);
            m_BookShelfHtml = EntityUtils.toString(response.getEntity());
            factory.setHostnameVerifier(orgVerifier);
            m_Auth = true;
            return true;
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception during authentication", ex);
            return false;
        }
    }
    
    private boolean parseBookData(String url, ScannedFile file) {
        try {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            String bookData = EntityUtils.toString(response.getEntity());
            String cover;
            String keyword;
            String desc;
            int idx1 = bookData.indexOf("cover_");
            if (idx1 == -1) { return false; }
            idx1 = bookData.indexOf("src=", idx1);
            if (idx1 == -1) { return false; }
            int idx2 = bookData.indexOf('\"', idx1 + 6);
            if (idx2 == -1) { return false; }
            cover = bookData.substring(idx1 + 5, idx2);
            file.setCover("http://m.fictionwise.com" + cover);
            idx1 = bookData.indexOf("Category", idx2);
            if (idx1 == -1) { return false; }
            idx2 = bookData.indexOf("href", idx1 + 8);
            if (idx2 == -1) { return false; }
            idx1 = bookData.indexOf('>', idx2);
            if (idx1 == -1) { return false; }
            idx2 = bookData.indexOf('<', idx1 + 1);
            if (idx2 == -1) { return false; }
            keyword = bookData.substring(idx1 + 1, idx2);
            file.addKeywords(keyword);
            file.addKeywords(nookLib.getString(R.string.fictionwise));
            idx1 = bookData.indexOf("Add to Cart", idx2);
            if (idx1 == -1) { return false; }
            idx2 = bookData.indexOf("<p>", idx1);
            idx1 = bookData.indexOf("</p>", idx2 + 3);
            desc = bookData.substring(idx2 + 3, idx1);
            file.setDescription(desc);
            return true;
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception during authentication", ex);
            return false;
        }
    }
    
    private boolean parseBookShelf() {
        try {
            m_Files.clear();
            int idx1 = m_BookShelfHtml.indexOf("Displaying 1");
            if (idx1 == -1) { return false; }
            int idx2;
            idx1 = m_BookShelfHtml.indexOf("<table", idx1 + 1);
            idx2 = m_BookShelfHtml.indexOf("</table>", idx1 + 1);
            String data = m_BookShelfHtml.substring(idx1, idx2 + 8);
            while (true) {
                idx1 = data.indexOf("<tr");
                if (idx1 == -1) {
                    break;
                }
                idx2 = data.indexOf("</tr>", idx1 + 3);
                if (idx2 == -1) {
                    break;
                }
                String rowData = data.substring(idx1, idx2 + 5);
                data = data.substring(idx2 + 5);
                idx2 = rowData.indexOf("http://", idx1 + 1);
                if (idx2 == -1) {
                    continue;
                }
                idx1 = rowData.indexOf('>', idx2);
                if (idx1 == -1) {
                    continue;
                }
                String url = rowData.substring(idx2, idx1 - 1);
                System.out.println("URL = " + url);
                
                int idx3 = 0;
                for (int i = 0; i < 4; i++) {
                    idx3 = url.indexOf('/', idx3 + 1);
                }
                int idx4 = url.indexOf('/', idx3 + 1);
                String bookId = url.substring(idx3 + 2, idx4);
                System.out.println("bookId =" + bookId);
                ScannedFile file = m_BookIdMap.get(bookId);
                if (file != null) {
                    continue;
                }
                file = new ScannedFile();
                file.setBookID(bookId);
                boolean ret = parseBookData(url, file);
                if (!ret) {
                    System.out.println("Parse book data failed ...");
                } else {
                    System.out.println("Parse book data successful ...");
                }
                idx2 = rowData.indexOf('<', idx1 + 1);
                if (idx2 == -1) {
                    continue;
                }
                String name = rowData.substring(idx1 + 1, idx2);
                System.out.println("Name =" + name);
                file.setTitle(name);
                idx1 = rowData.indexOf("<br>", idx2 + 1);
                if (idx1 == -1) {
                    continue;
                }
                idx2 = rowData.indexOf("<br>", idx1 + 3);
                if (idx2 == -1) {
                    continue;
                }
                String author = rowData.substring(idx1 + 6, idx2);
                System.out.println("Author =" + author);
                file.addContributor(author, "");
                idx1 = rowData.indexOf("name=formattype", idx2);
                String field = "formattype";
                if (idx1 == -1) {
                    idx1 = rowData.indexOf("name=format");
                    field = "format";
                }
                if (idx1 == -1) {
                    System.out.println("No download data ... for " + name);
                    continue;
                }
                String downloadUrl = DOWNLOAD_URL + "&bookid=" + bookId + "&" + field + "=";
                idx1 = rowData.indexOf("value=", idx1 + 1);
                idx2 = rowData.indexOf('>', idx1 + 6);
                String value = rowData.substring(idx1 + 6, idx2);
                value = value.trim().replace("\"", "");
                // check for multiple options
                if (value.trim().equals("")) {
                    ArrayList<String> values = new ArrayList<String>(10);
                    while (true) {
                        idx1 = rowData.indexOf("value=", idx2);
                        if (idx1 == -1) {
                            break;
                        }
                        idx2 = rowData.indexOf('>', idx1 + 6);
                        if (idx2 == -1) {
                            break;
                        }
                        value = rowData.substring(idx1 + 6, idx2);
                        value = value.trim().replace("\"", "");
                        if (!value.trim().equals("")) {
                            values.add(value);
                        }
                    }
                    if (values.contains("epub")) {
                        downloadUrl += "epub";
                    } else if (values.contains("-er.pdb")) {
                        downloadUrl += "-er.pdb";
                    } else if (values.contains("pdb")) {
                        downloadUrl += "pdb";
                    } else if (values.contains("pdf")) {
                        downloadUrl += "pdf";
                    }
                } else {
                    downloadUrl += value;
                }
                idx1 = rowData.indexOf("hashcode", idx2);
                if (idx1 != -1) {
                    field = "hashcode";
                    downloadUrl += "&hashcode=";
                    idx1 = rowData.indexOf("value=", idx2);
                    if (idx1 == -1) {
                        break;
                    }
                    idx2 = rowData.indexOf('>', idx1 + 6);
                    if (idx2 == -1) {
                        break;
                    }
                    value = rowData.substring(idx1 + 6, idx2);
                    value = value.trim().replace("\"", "");
                    downloadUrl += value;
                }
                System.out.println("Download URL =" + downloadUrl);
                file.setDownloadUrl(downloadUrl);
                file.setStatus(BNBooks.DOWNLOAD);
                file.setCreatedDate(new Date());
                m_Files.add(file);
                file.setBookInDB(false);
                System.out.println("File =" + file);
            }
            return m_Files.size() > 0;
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception while creating parsing bookshelf", ex);
            return false;
        }
    }
    
    public void getBooks(boolean refresh) {
        getBooksfromDB();
        if (refresh) {
            if (authenticate()) {
                if (parseBookShelf()) {
                    nookLib.updatePageView(m_Files);
                }
            }
        }
        if (lock.isHeld()) {
            lock.release();
        }
    }
    
    public void close() {
        try {
            if (m_Db != null) {
                m_Db.close();
                m_Db = null;
            }
        } catch (Exception ex) {
            
        }
    }
    
    public boolean archiveInServer(ScannedFile file) {
        // not supported for fictionwise as of now.
        return true;
    }
    
    public List<ScannedFile> getArchived() {
        return m_ArchivedFiles;
    }
    public boolean deleteBook(ScannedFile file) {
        archiveBook(file, true);
        if( file.getCover() != null) {
            try {
                File f = new File(file.getCover());
                f.delete();
            } catch(Exception ex) {
                return true;
            }
        }
        return true;
    }
    public boolean archiveBook(ScannedFile file, boolean val) {
        if (!archiveInServer(file)) { return false; }
        if (val) {
            file.setStatus(BNBooks.ARCHIVED);
            m_ArchivedFiles.add(file);
            File f = new File(file.getPathName());
            f.delete();
        } else {
            file.setStatus(BNBooks.DOWNLOAD);
            m_ArchivedFiles.remove(file);
        }
        if (m_Db == null) {
            m_Db = getWritableDatabase();
        }
        try {
            m_Db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put("status", file.getStatus());
            values.put("path", "");
            String[] whereArgs = {
                file.getBookID()
            };
            m_Db.update("BOOKS", values, "bookid=?", whereArgs);
            m_Db.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "Exception updating status in database", ex);
            return false;
        } finally {
            m_Db.endTransaction();
        }
        return true;
    }
    
    protected void getBooksfromDB() {
        try {
            m_Files.clear();
            m_ArchivedFiles.clear();
            if (m_Db == null) {
                m_Db = getWritableDatabase();
            }
            String query =
                "select id, ean,titles,authors,desc,keywords,publisher,cover,published,created,path,series, bookid, downloadUrl , status from books";
            Cursor cursor = m_Db.rawQuery(query, null);
            int size = cursor.getCount();
            cursor.moveToFirst();
            for (int i = 0; i < size; i++) {
                String bookId = cursor.getString(12);
                String path = cursor.getString(10);
                ScannedFile sf = new ScannedFile(path, false);
                if (path != null && !path.trim().equals("")) {
                    sf.updateLastAccessDate();
                }
                m_BookIdMap.put(bookId, sf);
                sf.setBookID(bookId);
                sf.setDownloadUrl(cursor.getString(13));
                sf.setStatus(cursor.getString(14));
                sf.setEan(cursor.getString(1));
                sf.setPublisher(cursor.getString(6));
                sf.setCover(cursor.getString(7));
                sf.setPublishedDate(new Date(cursor.getLong(8)));
                sf.setCreatedDate(new Date(cursor.getLong(9)));
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
                String desc = cursor.getString(4);
                if (desc != null) {
                    sf.setDescription(desc);
                }
                String keywords = cursor.getString(5);
                if (keywords != null) {
                    token = new StringTokenizer(keywords, ",");
                    while (token.hasMoreTokens()) {
                        String keyword = token.nextToken();
                        sf.addKeywords(keyword);
                    }
                }
                sf.setSeries(cursor.getString(11));
                sf.setBookInDB(true);
                if (BNBooks.ARCHIVED.equals(sf.getStatus())) {
                    m_ArchivedFiles.add(sf);
                } else {
                    m_Files.add(sf);
                }
                cursor.moveToNext();
            }
            cursor.close();
            nookLib.updatePageView(m_Files);
            m_Files.clear();
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "Exception querying datbase", ex);
        }
        
    }
    
    public void updateBookInDB(ScannedFile file) {
        if (m_Db == null) {
            m_Db = getWritableDatabase();
        }
        try {
            m_Db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put("status", file.getStatus());
            values.put("path", file.getPathName());
            String[] whereArgs = {
                file.getBookID()
            };
            m_Db.update("BOOKS", values, "bookid=?", whereArgs);
            m_Db.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "Exception updating datbase", ex);
        } finally {
            m_Db.endTransaction();
        }
    }
    
    public void addBookToDB(ScannedFile file) {
        if (m_Db == null) {
            m_Db = getWritableDatabase();
        }
        try {
            ContentValues values = new ContentValues();
            values.put("ean", file.getEan());
            values.put("downloadurl", file.getDownloadUrl());
            values.put("bookid", file.getBookID());
            values.put("series", file.getSeries());
            values.put("publisher", file.getPublisher());
            values.put("path", file.getPathName());
            values.put("cover", file.getCover());
            values.put("status", file.getStatus());
            if (file.getPublishedDate() != null) {
                values.put("published", file.getPublishedDate().getTime());
            }
            if (file.getCreatedDate() != null) {
                values.put("created", file.getCreatedDate().getTime());
            } else {
                long time = (new Date()).getTime();
                values.put("created", time);
            }
            values.put("desc", file.getDescription());
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
            for (int i = 0; keywords != null && i < keywords.size(); i++) {
                if (i != 0) {
                    keyword += ",";
                }
                keyword += keywords.get(i);
            }
            values.put("keywords", keyword);
            m_Db.beginTransaction();
            m_Db.insert("BOOKS", null, values);
            m_Db.setTransactionSuccessful();
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception while adding user info", ex);
        } finally {
            m_Db.endTransaction();
        }
    }
    
    public void downloadBook(ScannedFile file) {
        try {
            if (m_Auth) {
                nookLib.waitForNetwork(lock);
            }
            if (!m_Auth && !authenticate()) { throw new Exception("Authentication error"); }
            
            HttpGet request = new HttpGet(file.getDownloadUrl());
            HttpResponse response = httpClient.execute(request);
            InputStream in = response.getEntity().getContent();
            String contentType = response.getEntity().getContentType().getValue();
            String type;
            if (contentType.contains("epub") || contentType.contains("octet-stream")) {
                type = ".epub";
            } else if (contentType.contains("pdb")) {
                type = ".pdb";
            } else if (contentType.contains("pdf")) {
                type = ".pdf";
            } else {
                throw new Exception("Unknown book type");
            }
            String name = m_BaseDir + file.getTitles().get(0) + type;
            BufferedInputStream bis = new BufferedInputStream(in, 8096);
            FileOutputStream fout = new FileOutputStream(new File(name));
            byte[] buffer = new byte[8096];
            int len;
            while ((len = bis.read(buffer)) >= 0) {
                fout.write(buffer, 0, len);
            }
            bis.close();
            fout.close();
            file.setPathName(name);
            file.setStatus(null);
            file.updateLastAccessDate();
            updateBookInDB(file);
            nookLib.getHandler().post(new Runnable() {
                public void run() {
                    Toast.makeText(nookLib, R.string.download_complete, Toast.LENGTH_SHORT).show();
                }
            });
            close();
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception while downloading book", ex);
            nookLib.getHandler().post(new Runnable() {
                public void run() {
                    Toast.makeText(nookLib, R.string.download_failed, Toast.LENGTH_LONG).show();
                }
            });
            file.setStatus(BNBooks.DOWNLOAD);
            close();
        }
        if (lock.isHeld()) {
            lock.release();
        }
    }
    
    public static String getBaseDir() {
        return m_BaseDir;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_BOOKS_TABLE);
            db.execSQL(CREATE_USER_TABLE);
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception while creating ficionwise tables", ex);
        }
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL("ALTER TABLE BOOKS ADD COLUMN STATUS TEXT");
        } catch (Exception ex) {
            onCreate(db);
        }
    }
    
    public void deleteAll() {
        try {
            if (m_Db == null) {
                m_Db = getWritableDatabase();
            }
            m_Db.delete("BOOKS", null, null);
            m_Db.delete("USER", null, null);
            m_User = null;
            m_Pass = null;
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception while deleting ficionwise tables", ex);
        }
    }
    
    public boolean addUser(String user, String pass) {
        if (m_Db == null) {
            m_Db = getWritableDatabase();
        }
        m_User = user;
        m_Pass = pass;
        try {
            m_Db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put("login", user);
            values.put("pass", pass);
            m_Db.insert("USER", null, values);
            m_Db.setTransactionSuccessful();
            return true;
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception while adding user info", ex);
            return false;
        } finally {
            m_Db.endTransaction();
        }
    }
    
    public boolean getUser() {
        if (m_User != null) { return true; }
        if (m_Db == null) {
            m_Db = getWritableDatabase();
        }
        try {
            String[] columns = {
                "login", "pass"
            };
            Cursor c = m_Db.query("USER", columns, null, null, null, null, null);
            if (c.getCount() >= 1) {
                c.moveToFirst();
                m_User = c.getString(0);
                m_Pass = c.getString(1);
                c.close();
                return true;
            } else {
                Log.e("FictionwiseBooks", "No User Info");
                c.close();
                return false;
            }
        } catch (Exception ex) {
            Log.e("FictionwiseBooks", "exception while adding user info", ex);
        }
        return false;
    }
}

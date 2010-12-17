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
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import com.bravo.ecm.service.ScannedFile;
import com.nookdevs.common.nookBaseActivity;

public class Smashwords extends FictionwiseBooks {
    
    public static final String AUTH_URL = "https://www.smashwords.com/library?output=xml";
    public static final String DOWNLOAD_URL = "http://www.fictionwise.com/servlet/mw?action=download";
    public static final String BOOKS_DB = "smashwords.db";
    public static final String CREATE_BOOKS_TABLE =
        " create table books ( id integer primary key autoincrement, ean text, titles text not null, "
            + "authors text not null, desc text, keywords text, publisher text, cover text, published long, created long, path text, series text, bookid text not null unique, downloadUrl text)";
    public static final String CREATE_USER_TABLE = " create table user ( login text not null, pass text not null)";
    protected NookLibrary nookLib;
    public static final int VERSION = 11;
    private static String m_BaseDir;
    
    static {
        try {
            File file = new File(nookBaseActivity.EXTERNAL_SDFOLDER + "/" + "smashwords/");
            if (!file.exists()) {
                file = new File(nookBaseActivity.SDFOLDER + "/" + "smashwords/");
                file.mkdir();
            }
            m_BaseDir = file.getAbsolutePath() + "/";
            file = new File(m_BaseDir + ".skip");
            file.createNewFile();
        } catch (Exception ex) {
            Log.e("Smashwords", "exception in init static block", ex);
        }
    }
    
    public Smashwords(NookLibrary context) {
        super(context, BOOKS_DB, VERSION);
        nookLib = context;
        cmgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        lock = cmgr.newWakeLock(1, "nookLibrary.Smashwords" + hashCode());
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 100);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        httpClient = new DefaultHttpClient(params);
        m_library = nookLib.getString(R.string.smashwords);
    }
    
    public static String getBaseDir() {
        return m_BaseDir;
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
            nvps.add(new BasicNameValuePair("username", m_User));
            nvps.add(new BasicNameValuePair("password", m_Pass));
            request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpClient.execute(request);
            parseBookShelf(response.getEntity().getContent());
            factory.setHostnameVerifier(orgVerifier);
            lock.release();
            return true;
        } catch (Exception ex) {
            Log.e("Smashwords", "exception during authentication", ex);
            return false;
        }
    }
    
    private boolean parseBookShelf(InputStream inp) {
        try {
            XmlPullParserFactory fact = XmlPullParserFactory.newInstance();
            fact.setNamespaceAware(false);
            XmlPullParser parser = fact.newPullParser();
            parser.setInput(inp, null);
            int type;
            boolean valid = false;
            String name = "";
            ScannedFile file = null;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    name = parser.getName();
                    if (name.equals("books")) {
                        String val = parser.getAttributeValue(null, "list");
                        if (val.equals("purchased")) {
                            valid = true;
                        }
                    }
                    if (name.equals("book") && valid) {
                        String id = parser.getAttributeValue(null, "id");
                        if (m_BookIdMap.containsKey(id)) {
                            file = null;
                        } else {
                            file = new ScannedFile();
                            file.setBookID(id);
                        }
                    }
                } else if (valid && type == XmlPullParser.END_TAG) {
                    name = parser.getName();
                    if (name.equals("books")) {
                        valid = false;
                    } else if (name.equals("book")) {
                        if (file != null) {
                            file.setBookInDB(false);
                            file.setStatus(BNBooks.DOWNLOAD);
                            file.setLibrary( m_library);
                            file.setCreatedDate(new Date());
                            m_Files.add(file);
                        }
                    }
                } else if (file != null && valid && type == XmlPullParser.TEXT) {
                    if (name.equals("book") || name.equals("books")) {
                        continue;
                    }
                    String text = parser.getText();
                    if (text.trim().equals("") || text.trim().equals("\n")) {
                        continue;
                    }
                    if (name.equals("dc:creator")) {
                        file.addContributor(text, "");
                    } else if (name.equals("dc:title")) {
                        file.setTitle(text);
                    } else if (name.equals("dc:subject")) {
                        StringTokenizer token = new StringTokenizer(text, ";");
                        while (token.hasMoreTokens()) {
                            file.addKeywords(token.nextToken());
                        }
                    } else if (name.equals("dc:description")) {
                        file.setDescription(text);
                    } else if (name.equals("cover")) {
                        file.setCover(text);
                    } else if (name.equals("epub")) {
                        file.setDownloadUrl(text);
                    } else if (name.equals("pdb") && file.getDownloadUrl() == null) {
                        file.setDownloadUrl(text);
                    } else if (name.equals("pdf") && file.getDownloadUrl() == null) {
                        file.setDownloadUrl(text);
                    }
                }
                
            }
            
            return m_Files.size() > 0;
        } catch (Exception ex) {
            Log.e("Smashwords", "exception while creating parsing bookshelf", ex);
            return false;
        }
    }
    
    @Override
    public void getBooks(boolean refresh) {
        getBooksfromDB();
        if (refresh) {
            if (authenticate()) {
                nookLib.updatePageView(m_Files);
            }
        }
    }
    
    @Override
    public void downloadBook(ScannedFile file) {
        try {
            if (m_User == null) {
                getUser();
            }
            nookLib.waitForNetwork(lock);
            HttpPost request = new HttpPost(file.getDownloadUrl());
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("username", m_User));
            nvps.add(new BasicNameValuePair("password", m_Pass));
            request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpClient.execute(request);
            String type = response.getEntity().getContentType().getValue();
            InputStream in = response.getEntity().getContent();
            if (type.contains("xml")) {
                XmlPullParserFactory fact = XmlPullParserFactory.newInstance();
                fact.setNamespaceAware(false);
                XmlPullParser parser = fact.newPullParser();
                parser.setInput(in, null);
                while ((parser.next()) != XmlPullParser.END_DOCUMENT) {
                    String text = parser.getText();
                    if (text == null) {
                        continue;
                    }
                    text = text.trim();
                    if (text.equals("\n") || text.equals("")) {
                        continue;
                    }
                    if (text.startsWith("http")) {
                        file.setDownloadUrl(text);
                        downloadBook(file);
                        return;
                    }
                }
                // failed.
                throw new Exception("Invalid data returned.");
            } else if (type.contains("html")) { throw new Exception("Invalid data returned."); }
            int idx = file.getDownloadUrl().lastIndexOf('/');
            String name = m_BaseDir + file.getDownloadUrl().substring(idx + 1);
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
            Log.e("Smashwords", "exception while downloading book", ex);
            nookLib.getHandler().post(new Runnable() {
                public void run() {
                    Toast.makeText(nookLib, R.string.download_failed, Toast.LENGTH_LONG).show();
                }
            });
            file.setStatus(BNBooks.DOWNLOAD);
            close();
        } finally {
            if (lock.isHeld()) {
                lock.release();
            }
        }
        return;
    }
    
    @Override
    public boolean addUser(String user, String pass) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] data = pass.getBytes();
            m.update(data, 0, data.length);
            BigInteger i = new BigInteger(1, m.digest());
            String pass1 = String.format("%1$032X", i);
            return super.addUser(user, pass1);
        } catch (Exception ex) {
            Log.e("Smashwords", "exception while adding user info", ex);
            return false;
        }
    }
}

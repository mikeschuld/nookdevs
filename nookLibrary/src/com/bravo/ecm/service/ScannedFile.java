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

// This is my custom implementation of the ScannedFile interface which is
// returned as output from ecmscannerservice.
//
// Interface is from ecmscannerservice package and this has to be part of that
// package due to class casting issues.
//
package com.bravo.ecm.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory.Options;
import android.net.ConnectivityManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.nookdevs.common.nookBaseActivity;
import com.nookdevs.library.BNBooks;
import com.nookdevs.library.EpubMetaReader;
import com.nookdevs.library.FictionwiseBooks;
import com.nookdevs.library.NookLibrary;
import com.nookdevs.library.Smashwords;

public class ScannedFile implements Parcelable, Comparable<ScannedFile>, Serializable {
    private static final long serialVersionUID = -1908968223219359322L;
    private static int m_SortType;
    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_AUTHOR = 1;
    public static final int SORT_BY_AUTHOR_LAST = 2;
    public static final int SORT_BY_LATEST = 3;
    public static List<String> m_StandardKeywords = null;
    private String lendState = "";
    private static ArrayList<String> m_AuthorsList = new ArrayList<String>(100);
    public static final String ReservedChars = "|\\?*<\":>+[]/'#";
    private static HashMap<String,ScannedFile> m_FilesMap = new HashMap<String,ScannedFile>();
    private boolean m_Dummy=false;
    public static List<String> getAuthors() {
        return m_AuthorsList;
    }
    private static NookLibrary m_NookLibrary;

    public static void setContext(NookLibrary ctx) {
        m_NookLibrary = ctx;
    }

    public static synchronized void setSortType(int type) {
        if (type >= 0 && type <= 3) {
            m_SortType = type;
        }
    }
    private static boolean m_SortReversed=false;
    public static void setSortReversed(boolean reversed) {
        m_SortReversed=reversed;
    }
    public static boolean isSortReversed() {
        return m_SortReversed;
    }

    public static void loadStandardKeywords() {
        try {
            m_AuthorsList.clear();
            File f = new File(nookBaseActivity.SDFOLDER + "/" + "mybooks.xml");
            if (!f.exists()) {
                f = new File(nookBaseActivity.EXTERNAL_SDFOLDER + "/" + "mybooks.xml");
            }
            if (f.exists()) {
                m_StandardKeywords = new ArrayList<String>(50);
                FileInputStream inp = new FileInputStream(f);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inp, null);
                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (type == XmlPullParser.TEXT) {
                        String txt = parser.getText();
                        if (txt != null && !txt.trim().equals("")) {
                            m_StandardKeywords.add(parser.getText());
                        }
                    }
                }
                inp.close();
                if (!m_StandardKeywords.contains("epub")) {
                    m_StandardKeywords.add("epub");
                }
                if (!m_StandardKeywords.contains("pdb")) {
                    m_StandardKeywords.add("pdb");
                }
                if (!m_StandardKeywords.contains("pdf")) {
                    m_StandardKeywords.add("pdf");
                }
                if (!m_StandardKeywords.contains("txt")) {
                    m_StandardKeywords.add("txt");
                }
                if (!m_StandardKeywords.contains("htm")) {
                    m_StandardKeywords.add("htm");
                }
            } else {
                m_StandardKeywords = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            m_StandardKeywords = null;
        }
    }

    public static int getSortType() {
        return m_SortType;
    }

    public static final Parcelable.Creator<ScannedFile> CREATOR = new Parcelable.Creator<ScannedFile>() {
        public ScannedFile createFromParcel(Parcel in) {
            ScannedFile file = new ScannedFile();
            file.readFromParcel(in);
            return file;
        }

        public ScannedFile[] newArray(int size) {
            return new ScannedFile[size];
        }
    };

    List<Contributors> contributors = new ArrayList<Contributors>();
    String ean;
    String pathname;
    Date publishedDate;
    String publisher;
    List<String> titles = new ArrayList<String>();
    Date createdDate;
    Date lastAccessedDate;
    private String m_Cover = null;
    private String description=null;
    private List<String> m_Keywords;
    private String type;
    private String library;
    private String m_Series;
    private String m_Status;
    private String m_DownloadUrl;
    private String m_BookId;
    private boolean m_Sample=false;

    public void setLibrary(String str) {
        library = str;
    }

    public void setSample(boolean sample) {
        m_Sample=sample;
    }
    public boolean isSample() {
        return m_Sample;
    }
    public String getBookID() {
        return m_BookId;
    }

    public void setBookID(String id) {
        m_BookId = id;
    }

    public void setDownloadUrl(String url) {
        m_DownloadUrl = url;
    }

    public String getDownloadUrl() {
        return m_DownloadUrl;
    }

    public void setStatus(String status) {
        m_Status = status;
    }

    public String getStatus() {
        if( m_Sample) {
            if( m_Status == null || m_Status.trim().equals(""))
                return BNBooks.SAMPLE;
            else
                return m_Status + " " + BNBooks.SAMPLE;
        }
        return m_Status;
    }

    public void setSeries(String s) {
        m_Series = s;
    }

    public String getSeries() {
        return m_Series;
    }

    public ScannedFile() {
        super();
    }

    public void setPathName(String path) {
        pathname = path;
        if (path != null) {
            int idx = pathname.lastIndexOf('.');
            type = pathname.substring(idx + 1).toLowerCase();
            if (type.equals("html")) {
                type = "htm";
            }
            if( type.equals("zip"))
                type="fb2";
            addKeywords(type);
            m_FilesMap.put( path, this);
        }
    }
    public boolean loadCover(ConnectivityManager.WakeLock lock) {
        try {
            if (getCover() != null) {
                if (getCover().startsWith("http")) {
                    String name;
                    if (m_BookId != null) {
                        if (matchSubject("Fictionwise")) {
                            if (pathname != null && !pathname.trim().equals("")) {
                                name = (new File(pathname)).getName();
                                int idx = name.lastIndexOf('.');
                                if (idx == -1) {
                                    idx = name.length();
                                }
                                name = name.substring(0, idx);

                            } else {
                                name = titles.get(0);
                            }
                            for (int i = 0; i < ReservedChars.length(); i++) {
                                name = name.replace(ReservedChars.charAt(i), '_');
                            }
                            name = FictionwiseBooks.getBaseDir() + name + ".jpg";
                        } else {
                            int idx = m_DownloadUrl.lastIndexOf('/');
                            int idx1 = m_DownloadUrl.lastIndexOf('.');
                            name = m_DownloadUrl.substring(idx + 1, idx1);
                            name = Smashwords.getBaseDir() + name + ".jpg";
                        }
                    } else {
                        if (pathname != null && !pathname.trim().equals("")) {
                            name = (new File(pathname)).getName();
                            int idx = name.lastIndexOf('.');
                            name = name.substring(0, idx);
                        } else {
                            name = titles.get(0);
                        }
                        for (int i = 0; i < ReservedChars.length(); i++) {
                            name = name.replace(ReservedChars.charAt(i), '_');
                        }
                        name = "/system/media/sdcard/my B&N downloads/" + name + ".jpg";
                    }
                    try {
                        if ((new File(name)).exists()) {
                            setCover(name);
                            m_NookLibrary.updateCover(this);
                            return true;
                        }
                        if( lock != null && !lock.isHeld()) {
                            boolean ret =m_NookLibrary.waitForNetwork(lock);
                            if( !ret) return false;
                        }
                        DefaultHttpClient httpClient = new DefaultHttpClient();
                        HttpGet request = new HttpGet(getCover());
                        HttpResponse response = httpClient.execute(request);
                        BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent(), 1024);
                        FileOutputStream fout = new FileOutputStream(new File(name));
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = bis.read(buffer)) >= 0) {
                            fout.write(buffer, 0, len);
                        }
                        bis.close();
                        fout.close();
                        setCover(name);
                        m_NookLibrary.updateCover(this);
                        return true;
                    } catch (Exception ex) {
                        Log.e("loadCover", ex.getMessage(), ex);
                        File tmp = new File(name);
                        tmp.delete();
                        ex.printStackTrace();
                    }
                } else {
                    return true;
                }
            }
            String[] fileTypes = {
                ".jpg", ".jpeg", ".PNG", ".png", ".gif", ".JPG", ".JPEG", ".GIF"
            };
            boolean found = false;
            String cover = "";
            int idx;
            String path1;
            if (pathname != null && !pathname.trim().equals("")) {
                idx = pathname.lastIndexOf('.');
                if (idx == -1) {
                    path1 = pathname;
                } else {
                    path1 = pathname.substring(0, idx);
                }
            } else {
                path1 = "/system/media/sdcard/my B&N downloads/" + titles.get(0);
            }
            int attempt = 1;
            while (!found && attempt < 4) {
                for (String s : fileTypes) {
                    File f = new File(path1 + s);
                    if (f.exists()) {
                        cover = path1 + s;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    setCover(cover);
                    m_NookLibrary.updateCover(this);
                    return found;
                } else if (attempt == 1) {
                    attempt++;
                    path1 = pathname.replace("/sdcard/", "/sdcard/Digital Editions/Thumbnails/");
                } else if (attempt == 2) {
                    path1 = pathname.replace("/sdcard/", "/system/media/sdcard/Digital Editions/Thumbnails/");
                    attempt++;
                } else {
                    attempt++;
                }
            }
            if( "epub".equals(type)) {
                boolean ret= EpubMetaReader.loadCover(this);
                if( ret)  {
                    m_NookLibrary.updateCover(this);
                }
                return ret;
            }
        } catch (Exception ex) {
            Log.e("loadCover", ex.getMessage(), ex);
            return false;
        }
        return false;
    }


    public ScannedFile(String pathName) {
        this(pathName, true);
    }
    public ScannedFile(String pathName, boolean update) {
        this( pathName, update, false);
    }
    public ScannedFile(String pathName, boolean update, boolean dummy) {
        pathname = pathName;
        m_Dummy =dummy;
        if (pathname != null && !pathname.trim().equals("")) {
            int idx = pathname.lastIndexOf('.');
            if( idx == -1)
                type="";
            else
                type = pathname.substring(idx + 1).toLowerCase();
            if (type.equals("html")) {
                type = "htm";
            }
            if( type.equals("zip"))
                type="fb2";
            if( !dummy)
                addKeywords(type);
            if(!dummy) m_FilesMap.put(pathname, this);
        }
    }


    public ScannedFile(android.os.Parcel parcel) {
        readFromParcel(parcel);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel arg0, int arg1) {
        // Not required for the client side.
    }
    private int coverId;
    public void setCoverId(int id) {
        coverId=id;
    }
    public int getCoverId() {
        return coverId;
    }

    public void readFromParcel(Parcel parcel) {
        pathname = parcel.readString();
        ean = parcel.readString();
        parcel.readStringList(titles);
        publisher = parcel.readString();

        try {
            long date = parcel.readLong();
            publishedDate = new Date(date);
        } catch (Exception ex) {
            publishedDate = null;
        }

        int size = parcel.readInt();
        for (int i = 0; i < size; i++) {
            String first = parcel.readString();
            String last = parcel.readString();
            addContributor(first, last);
        }

        try {
            parcel.readLong();
            lastAccessedDate = new Date((new File(pathname)).lastModified());
        } catch (Exception ex) {
            lastAccessedDate = null;
        }
        try {
            long date = parcel.readLong();
            createdDate = new Date(date);
        } catch (Exception ex) {
            createdDate = null;
        }
        int idx = pathname.lastIndexOf('.');
        String ext = pathname.substring(idx + 1).toLowerCase();
        if (ext.equals("html")) {
            ext = "htm";
        }
        if( type.equals("zip"))
            type="fb2";
        type = ext;
        addKeywords(ext);
    }

    public static ScannedFile getFile(String path) {
        if( path == null) return null;
        return m_FilesMap.get(path);
    }

    public void updateLastAccessDate() {
        try {
            if (pathname != null && !pathname.trim().equals("")) {
                lastAccessedDate = new Date((new File(pathname)).lastModified());
            }

        } catch (Exception ex) {

        }

    }

    public String getType() {
        return type;
    }

    public void setLendState(String state) {
        lendState = state;
    }

    public String getLendState() {
        return lendState;
    }

    public int compareTo(ScannedFile file1) {
        int ret=0;
        try {
            switch (m_SortType) {
                case SORT_BY_NAME:
                    ret=getTitle().compareToIgnoreCase(file1.getTitle());
                    break;
                case SORT_BY_AUTHOR:
                    int authsort = getAuthor().compareToIgnoreCase(file1.getAuthor());
                    if (authsort == 0) {
                        ret=getTitle().compareToIgnoreCase(file1.getTitle());
                    } else {
                        ret=authsort;
                    }
                    break;
                case SORT_BY_AUTHOR_LAST:
                    authsort = getAuthorLast().compareToIgnoreCase(file1.getAuthorLast());
                    if (authsort == 0) {
                        authsort = getAuthor().compareToIgnoreCase(file1.getAuthor());
                    }
                    if (authsort == 0) {
                        ret=getTitle().compareToIgnoreCase(file1.getTitle());
                    } else {
                        ret=authsort;
                    }
                    break;
                case SORT_BY_LATEST:
                    if (getLastAccessedDate() == null) {
                        if (file1.getLastAccessedDate() != null) {
                            return -1;
                        } else {
                            return getTitle().compareToIgnoreCase(file1.getTitle());
                        }
                    } else {
                        ret = getLastAccessedDate().compareTo(file1.getLastAccessedDate());
                        if (ret == 0) {
                            ret=getTitle().compareToIgnoreCase(file1.getTitle());
                        } else {
                            ret=-ret;
                        }
                    }
            }
        } catch (Exception ex) {
            ret=getTitle().compareToIgnoreCase(file1.getTitle());
        }
        if( m_SortReversed) ret=-ret;
        return ret;
    }

    public List<Contributors> getContributors() {
        return contributors;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date date) {
        createdDate = date;
        if (lastAccessedDate == null) {
            lastAccessedDate = date;
        }
    }

    public Date getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(Date date) {
        publishedDate = date;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public Date getLastAccessedDate() {
        return lastAccessedDate;
    }

    public String getPathName() {
        return pathname;
    }

    public String getPublisher() {
        return publisher;
    }

    public List<String> getTitles() {
        return titles;
    }

    public void setPublisher(String p) {
        publisher = p;
    }

    public void setLastAccessedDate(Date date) {
        lastAccessedDate = date;
    }

    public void setTitle(String val) {
        if (val == null) { return; }
        if (titles == null) {
            titles = new ArrayList<String>();
        }
        if (val == null || val.trim().equals("")) { return; }
        titles.add(val);
    }

    public void setTitles(List<String> titles) {
        this.titles = titles;
    }

    public String getTitle() {
        if( m_Dummy) return pathname;
        String title = m_Series == null ? "" : m_Series + " ";
        if (titles == null || titles.size() == 0) {
            if( pathname != null) {
                int idx = pathname.lastIndexOf("/");
                title += pathname.substring(idx + 1);
            } else {
                Log.e("ScannedFile", "Book with invalid metadata:" + this);
            }
        } else {
            for (int i = 0; i < titles.size(); i++) {
                String tmp = titles.get(i).trim();
                if (i == 0 || !title.contains(tmp)) {
                    if (i != 0) {
                        tmp = "," + tmp;
                    }
                    title += tmp;
                }
            }
        }
        if (m_Status != null && !BNBooks.ARCHIVED.equals(m_Status)) {
            title += "-" + m_Status;
            if( m_Sample) {
                title += " " + BNBooks.SAMPLE;
            }
        } else if( m_Sample) {
            title += "-" + BNBooks.SAMPLE;
        }
        return title;
    }
    public String getDescription(boolean skipdb) {
        return description;
    }
    public String getDescription() {
        if( description != null)
            return description;
        String desc = m_NookLibrary.readDescription(this);
        return desc;
    }

    public void setDescription(String desc) {
        description = desc;
    }
    public void addKeywords(String keyword) {
        addKeywords(keyword, false);
    }

    public void addKeywords(String keyword, boolean dup) {
        if (keyword == null || keyword.trim().equals("")) { return; }
        keyword = keyword.trim();
        if (m_Keywords == null) {
            m_Keywords = new ArrayList<String>(10);
        }
        if (m_Keywords.contains(keyword)) { return; }
        m_Keywords.add(keyword);
    }
    public void updateKeywords() {
        for(Contributors c:contributors) {
            String tmp = c.toString().trim();
            if (!m_AuthorsList.contains(tmp)) {
                m_AuthorsList.add(tmp);
            }
        }
    }

    public boolean matchSubject(String subject) {
        return ( subject.equals( type) || subject.equals(library));
    }

    public List<String> getKeywords() {
        return m_Keywords;
    }
    public void clearKeywords() {
       m_Keywords.clear();
       m_Keywords =null;
    }

    public String getDetails() {
        String text1 = "<b>" + getTitle() + "</b><br/><br/>";
        StringBuffer text = new StringBuffer();
        if (contributors.size() > 0) {
            text.append("&nbsp;&nbsp;&nbsp;&nbsp;by " + getAuthor() + "<br/>");
        } else {
            text.append("&nbsp;&nbsp;&nbsp;&nbsp; " + getAuthor() + "<br/>");
        }
        text.append("<br/>");
        String tmp = getPublisher();
        if (tmp != null && !tmp.trim().equals("")) {
            text.append("Publisher:");
            text.append(tmp);
            text.append("<br/>");
        }
        List<String> keywords = getKeywords();
        boolean tmpFlag = false;
        if (keywords != null && keywords.size() > 0) {
            text.append("<br/>Keyword(s):");
            for (int i = 0; i < keywords.size(); i++) {
                if (i > 0) {
                    text.append(',');
                }
                text.append(keywords.get(i));
                tmpFlag = true;
            }
        }
        if (tmpFlag) {
            text.append("<br/><br/>");
        }
        tmp = getDescription();
        if (tmp != null && !tmp.trim().equals("")) {
            text.append(tmp);
            text.append("<br/><br/>");
        }
        if (pathname != null && !pathname.equals("")) {
            text.append("<b>File Path:</b>&nbsp;");
            text.append(pathname);
        }
        String details = text.toString();
        return text1 + details;
    }

    public String getAuthorLast() {
        if( m_Dummy) return "";
        if (contributors == null || contributors.size() == 0) {
            return "No Author Info";
        } else {
            String auth = contributors.get(0).lastName;
            if (auth == null || auth.trim().equals("")) { return getAuthor(); }
            return auth.trim();
        }
    }
    public Spanned getAuthor(boolean page) {
        Spanned txt = null;
        if( page && current_page >0 && total_pages >0) {
            txt = Html.fromHtml(getAuthor() + "," + " on " + "<b>page " + current_page + " of " + total_pages + "</b>");
        } else {
            txt = Html.fromHtml(getAuthor());
        }
        return txt;
    }
    public String getAuthor() {
        if( m_Dummy) return "";
        if (contributors == null || contributors.size() == 0) {
            return "No Author Info";
        } else {
            StringBuffer auth = new StringBuffer();
            int count = contributors.size();
            for (int i = 0; i < count; i++) {
                if (i != 0) {
                    auth.append(",");
                }
                auth.append(contributors.get(i).toString());
            }
            return auth.toString().trim();
        }
    }
    public List<String> getContributorsStr() {
        return contributors_str;
    }
    private ArrayList<String>  contributors_str = new ArrayList<String>(2);
    public void addContributor(String first, String last) {
        if ((first == null || (first = first.trim()).equals("")) && (last == null || (last = last.trim()).equals(""))) { return; }
        Contributors c = new Contributors(first, last);
        contributors_str.add( first + " " + last);
        if (!contributors.contains(c)) {
            contributors.add(c);
            String tmp = c.toString().trim();
            if (!BNBooks.ARCHIVED.equals(m_Status) && !m_AuthorsList.contains(tmp)) {
                m_AuthorsList.add(tmp);
            }
        }
    }

    private final class Contributors {
        private String firstName = "", lastName = "";

        public Contributors(String firstName, String lastName) {
            if (firstName != null && (lastName == null || lastName.trim().equals(""))) {
                int idx = firstName.lastIndexOf(' ');
                if (idx == -1) {
                    idx = firstName.lastIndexOf(',');
                }
                if (idx != -1) {
                    this.firstName = firstName.substring(0, idx).trim();
                    this.lastName = firstName.substring(idx + 1).trim();
                } else {
                    this.firstName = firstName.trim();
                    this.lastName = lastName;
                }
            } else {
                this.firstName = firstName;
                this.lastName = lastName;
            }
        }

        @Override
        public String toString() {
            return (firstName + " " + lastName).trim();
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof Contributors) {
                Contributors other = (Contributors) o;
                if (toString().equals(other.toString())) { return true; }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }

    @SuppressWarnings({ "ResultOfMethodCallIgnored" })
    public void setCover(String cover) {
        m_Cover = cover;

        // down-scale the cover if too large in order to avoid out-of-memory issues with the
        // gallery...
        if (m_Cover != null && !m_Cover.startsWith("http://")) {
            try {
                // check file size...
                File f = new File(m_Cover);
                if (f.length() < 100000) {
                    return;  // file size sufficiently small: abort
                }

                // determine scaled-down dimensions...
                Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                FileInputStream fin = new FileInputStream(m_Cover);
                BitmapFactory.decodeStream(fin, null, options);
                fin.close();
                final int MAX_WIDTH = 100;
                final int MAX_HEIGHT = 144;
                if (options.outHeight * options.outWidth < MAX_HEIGHT * MAX_WIDTH) {
                    return;  // dimensions sufficiently small: abort
                }
                boolean scaleByHeight =
                    options.outWidth * 1.0f / options.outHeight < MAX_WIDTH * 1.0f / MAX_HEIGHT;
                double sampleSize =
                    scaleByHeight ? options.outHeight / MAX_HEIGHT
                                  : options.outWidth / MAX_WIDTH;
                options.inSampleSize = (int)
                    Math.pow(2.0, Math.floor(Math.log(sampleSize) / Math.log(2.0)));
                if (options.inSampleSize == 1) {
                    return;  // down-sampling would be moot: abort
                }

                // replace the cover image file with a scaled-down one, retaining the original
                // renamed as "FILE.orig.EXTENSION", for this is required for the "screen saver
                // of covers" feature to make any sense...
                // read/decode the image...
                options.inJustDecodeBounds = false;
                fin = new FileInputStream(m_Cover);
                Bitmap img = BitmapFactory.decodeStream(fin, null, options);
                fin.close();
                // create a backup of the original...
                File orig = new File(originalCover(cover));
                orig.delete();
                f.renameTo(orig);
                // write/encode the scaled-down cover...
                FileOutputStream fout = new FileOutputStream(m_Cover);
                Bitmap.CompressFormat format =
                    m_Cover.matches(".*(?i:\\.jpe?g)$") ? CompressFormat.JPEG : CompressFormat.PNG;
                img.compress(format, 100, fout);
                fout.close();
            } catch(Exception ex1) {
                Log.e("setCover", ex1.getMessage(), ex1);
            }
        }
    }

    public String getCover() {
        return m_Cover;
    }

    protected String originalCover(String cover) {
        return cover.replaceFirst("(\\.[^/]+)?$", ".orig$1");
    }

    public String getOriginalCover() {
        if (m_Cover != null) {
            String orig = originalCover(m_Cover);
            if (new File(orig).isFile()) {
                return orig;
            }
        }
        return m_Cover;
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("File Details Start *********\n");
        str.append("Name = " + pathname + "\n");
        str.append("ean = " + ean + "\n");
        str.append("publisher =" + publisher + "\n");
        str.append("published date =" + publishedDate + "\n");
        str.append("last accessed date=" + lastAccessedDate + "\n");
        str.append("created date=" + createdDate + "\n");
        str.append("titles =" + titles + "\n");
        str.append("contributors  =" + contributors + "\n");
        str.append("File Details End *********\n");
        return str.toString();
    }

    public String getData() {
        StringBuffer str = new StringBuffer();
        str.append(pathname);
        str.append(" ");
        str.append(ean);
        str.append(" ");
        str.append(publisher);
        str.append(" ");
        str.append(titles);
        str.append(" ");
        str.append(contributors);
        str.append(" ");
        str.append(m_Series);
        str.append(" ");
        str.append(m_Keywords);
        str.append(" ");
      //  str.append(getDescription());
        return str.toString().toLowerCase();
    }

    private boolean m_BookInDB = true;

    public void setBookInDB(boolean val) {
        m_BookInDB = val;
    }

    public boolean getBookInDB() {
        return m_BookInDB;
    }

    private int current_page=0;
    private int total_pages=0;
    public int getCurrentPage() {
        return current_page;
    }
    public int getTotalPages() {
        return total_pages;
    }
    public void setCurrentPage(int p) {
        current_page=p;
    }
    public void setTotalPages(int p) {
        total_pages=p;
    }

}

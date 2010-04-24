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
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.os.Parcel;
import android.os.Parcelable;

import com.nookdevs.common.nookBaseActivity;
import com.nookdevs.library.BNBooks;
import com.nookdevs.library.EpubMetaReader;
import com.nookdevs.library.FictionwiseBooks;
import com.nookdevs.library.PdfMetaReader;
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
    // private static String[] m_StdKeywords =
    // { "Fiction", "Nonfiction", "Adventure","Classics", "Crime","Mystery",
    // "Science Fiction", "Romance",
    // "Historical", "Biography", "Politics", "Reference", "Religion",
    // "History","Feed", "News", "Sports", "Science","Travel", "epub", "pdb",
    // "pdf", "htm", "txt"};
    private static ArrayList<String> m_KeyWordsList = new ArrayList<String>(200);
    private static ArrayList<String> m_AuthorsList = new ArrayList<String>(100);
    
    public static List<String> getAuthors() {
        return m_AuthorsList;
    }
    
    public static synchronized void setSortType(int type) {
        if (type >= 0 && type <= 3) {
            m_SortType = type;
        }
    }
    
    public static void loadStandardKeywords() {
        try {
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
    private String m_Data = null;
    private StringBuffer m_DataBuffer = new StringBuffer();
    private String m_Cover = null;
    private String description;
    private List<String> m_Keywords;
    private String type;
    private EpubMetaReader epub;
    private PdfMetaReader pdf;
    private String m_Series;
    private String m_Status;
    private String m_DownloadUrl;
    private String m_BookId;
    
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
        return m_Status;
    }
    
    public void setSeries(String s) {
        m_Series = s;
        m_DataBuffer.append(" ");
        m_DataBuffer.append(s);
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
            System.out.println("setPathName called - path=" + path + " type=" + type);
            addKeywords(type);
            if (m_Details != null) {
                m_Details += "<b>File Path:</b>&nbsp;" + pathname;
            }
        }
    }
    
    public boolean loadCover() {
        try {
            if (getCover() != null) {
                if (getCover().startsWith("http")) {
                    String name;
                    if (m_BookId != null) {
                        if (matchSubject("Fictionwise")) {
                            name = FictionwiseBooks.getBaseDir() + titles.get(0) + ".jpg";
                        } else {
                            int idx = m_DownloadUrl.lastIndexOf('/');
                            int idx1 = m_DownloadUrl.lastIndexOf('.');
                            name = m_DownloadUrl.substring(idx + 1, idx1);
                            name = Smashwords.getBaseDir() + name + ".jpg";
                        }
                    } else {
                        name = "/system/media/sdcard/my B&N downloads/" + titles.get(0) + ".jpg";
                    }
                    try {
                        if ((new File(name)).exists()) {
                            setCover(name);
                            return true;
                        }
                        URL aURL = new URL(getCover());
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
                        return true;
                    } catch (Exception ex) {
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
            if (!"epub".equals(type) || !epub.loadCover()) { return false; }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
    
    public void updateMetaData() {
        updateMetaData(true);
    }
    
    public void updateMetaData(boolean quick) {
        if ("epub".equals(type)) {
            epub = new EpubMetaReader(this);
        } else if ("pdf".equalsIgnoreCase(type)) {
            pdf = new PdfMetaReader(this, quick);
        }
    }
    
    public ScannedFile(String pathName) {
        this(pathName, true);
    }
    
    public ScannedFile(String pathName, boolean update) {
        pathname = pathName;
        if (pathname != null) {
            m_DataBuffer.append(pathname);
            int idx = pathname.lastIndexOf('.');
            type = pathname.substring(idx + 1).toLowerCase();
            if (type.equals("html")) {
                type = "htm";
            }
            addKeywords(type);
            if (update) {
                updateMetaData();
            }
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
    
    public void readFromParcel(Parcel parcel) {
        pathname = parcel.readString();
        ean = parcel.readString();
        m_DataBuffer.append(ean);
        m_DataBuffer.append(" ");
        parcel.readStringList(titles);
        m_DataBuffer.append(titles.toString());
        publisher = parcel.readString();
        m_DataBuffer.append(" ");
        m_DataBuffer.append(publisher);
        
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
            m_DataBuffer.append(first);
            m_DataBuffer.append(" ");
            m_DataBuffer.append(last);
            m_DataBuffer.append(" ");
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
        type = ext;
        addKeywords(ext);
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
        try {
            switch (m_SortType) {
                case SORT_BY_NAME:
                    return getTitle().compareToIgnoreCase(file1.getTitle());
                case SORT_BY_AUTHOR:
                    int authsort = getAuthor().compareToIgnoreCase(file1.getAuthor());
                    if (authsort == 0) {
                        return getTitle().compareToIgnoreCase(file1.getTitle());
                    } else {
                        return authsort;
                    }
                case SORT_BY_AUTHOR_LAST:
                    authsort = getAuthorLast().compareToIgnoreCase(file1.getAuthorLast());
                    if (authsort == 0) {
                        return getTitle().compareToIgnoreCase(file1.getTitle());
                    } else {
                        return authsort;
                    }
                case SORT_BY_LATEST:
                    if (getLastAccessedDate() == null) {
                        if (file1.getLastAccessedDate() != null) {
                            return -1;
                        } else {
                            return getTitle().compareToIgnoreCase(file1.getTitle());
                        }
                    } else {
                        int ret = getLastAccessedDate().compareTo(file1.getLastAccessedDate());
                        if (ret == 0) {
                            return getTitle().compareToIgnoreCase(file1.getTitle());
                        } else {
                            return -ret;
                        }
                    }
            }
        } catch (Exception ex) {
            return getTitle().compareToIgnoreCase(file1.getTitle());
        }
        if (file1.pathname != null) { return file1.pathname.compareTo(pathname); }
        
        return -1;
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
        m_DataBuffer.append(" ");
        m_DataBuffer.append(publisher);
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
        m_DataBuffer.append(" ");
        m_DataBuffer.append(val);
    }
    
    public void setTitles(List<String> titles) {
        this.titles = titles;
    }
    
    public String getTitle() {
        String title = m_Series == null ? "" : m_Series + " ";
        if (titles == null || titles.size() == 0) {
            int idx = pathname.lastIndexOf("/");
            title += pathname.substring(idx + 1);
        } else {
            for (int i = 0; i < titles.size(); i++) {
                String tmp = titles.get(i).trim();
                if (i==0 || !title.contains(tmp)) {
                    if (i != 0) {
                        tmp = "," + tmp;
                    }
                    title += tmp;
                }
            }
        }
        if (m_Status != null && !BNBooks.ARCHIVED.equals(m_Status)) {
            title += "-" + m_Status;
        }
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String desc) {
        description = desc;
        m_DataBuffer.append(" ");
        m_DataBuffer.append(desc);
    }
    
    public void addKeywords(String keyword) {
        if (keyword == null || keyword.trim().equals("")) { return; }
        keyword = keyword.trim();
        if (m_Keywords == null) {
            m_Keywords = new ArrayList<String>(10);
        }
        if (m_Keywords.contains(keyword)) { return; }
        m_Keywords.add(keyword);
        if (!m_KeyWordsList.contains(keyword)) {
            m_KeyWordsList.add(keyword);
        }
        m_DataBuffer.append(" ");
        m_DataBuffer.append(keyword);
    }
    
    public boolean matchSubject(String subject) {
        return m_Keywords != null && m_Keywords.contains(subject);
    }
    
    public static List<String> getAvailableKeywords() {
        return ScannedFile.m_KeyWordsList;
    }
    
    public List<String> getKeywords() {
        return m_Keywords;
    }
    
    private String m_Details = null;
    
    public String getDetails() {
        String text1 = "<b>" + getTitle() + "</b><br/><br/>";
        if (m_Details != null) { return text1 + m_Details; }
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
        m_Details = text.toString();
        return text1 + m_Details;
    }
    
    public String getAuthorLast() {
        if (contributors == null || contributors.size() == 0) {
            return "No Author Info";
        } else {
            StringBuffer auth = new StringBuffer();
            auth.append(contributors.get(0).lastName);
            return auth.toString().trim();
        }
    }
    
    public String getAuthor() {
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
    
    public void addContributor(String first, String last) {
        if ((first == null || (first = first.trim()).equals("")) && (last == null || (last = last.trim()).equals(""))) { return; }
        Contributors c = new Contributors(first, last);
        if (!contributors.contains(c)) {
            contributors.add(c);
            m_DataBuffer.append(" ");
            String tmp = c.toString().trim();
            m_DataBuffer.append(tmp);
            if (!m_AuthorsList.contains(tmp)) {
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
            return firstName + " " + lastName;
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
    
    public void setCover(String cover) {
        m_Cover = cover;
    }
    
    public String getCover() {
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
        if (m_Data == null) {
            m_Data = m_DataBuffer.toString().toLowerCase();
        }
        return m_Data;
    }
    
    private boolean m_BookInDB = true;
    
    public void setBookInDB(boolean val) {
        m_BookInDB = val;
    }
    
    public boolean getBookInDB() {
        return m_BookInDB;
    }
    
}

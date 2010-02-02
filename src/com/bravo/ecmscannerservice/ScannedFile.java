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
package com.bravo.ecmscannerservice;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.nookdevs.library.EpubMetaReader;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class ScannedFile implements Parcelable, Comparable<ScannedFile>, Serializable {
    private static final long serialVersionUID = -1908968223219359322L;
    private static int m_SortType;
    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_AUTHOR = 1;
    public static final int SORT_BY_LATEST = 2;
    private static ArrayList<String> m_KeyWordsList = new ArrayList<String>(200);
    
    public static synchronized void setSortType(int type) {
        if (type >= 0 && type <= 2) {
            m_SortType = type;
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
    private String m_Cover =null;
    private String description;
    private List<String> m_Keywords;
    private String type;
    private EpubMetaReader epub;
    
    public ScannedFile() {
        super();
    }
    public boolean loadCover() {
    	try {
    		String[] fileTypes = {
                    ".jpg", ".png", ".jpeg", ".gif", ".JPG", ".JPEG",".PNG", ".GIF"
                };
            boolean found = false;
            String cover="";
            int idx = pathname.lastIndexOf('.');
            String path1 = pathname.substring(0, idx);
            int attempt=1;
            while( !found && attempt < 4) {
	
	            for (String s : fileTypes) {
	                File f = new File(path1 + s);
	                if (f.exists()) {
	                	cover=path1+s;
	                    found = true;
	                    break;
	                }
	            }
	            if( found) {
	            	setCover(cover);
	            	return found;
	            } else if( attempt ==1) {
	            	attempt++;
	            	path1 = pathname.replace("/sdcard/", "/sdcard/Digital Editions/Thumbnails/"); 
	            } else if( attempt ==2){
	            	path1 = pathname.replace("/sdcard/", "/system/media/sdcard/Digital Editions/Thumbnails/");
	            	attempt++;
	            } else {
	            	attempt ++;
	            }
            }
    		if( !"epub".equals(type) || !epub.loadCover()) {
    			Log.e("nookLibrary", "Load Cover failed for " + pathname);
    			return false;
    		}
    	} catch(Exception ex) {
    		Log.e("nookLibrary", "Load Cover failed for " + pathname);
    		return false;
    	}
    	return true;
    }
    public void updateMetaData() {
    	if( "epub".equals(type)) {
    		epub = new EpubMetaReader(this);
    	}
    }
    public ScannedFile(String pathName) {
        pathname = pathName;
        if( pathname != null) {
	        m_DataBuffer.append(pathname);
	        int idx = pathname.lastIndexOf('.');
	        type = pathname.substring(idx + 1);
	        addKeywords(type);
	        updateMetaData();
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
            long date = parcel.readLong();
            lastAccessedDate = new Date(date);
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
        String ext = pathname.substring(idx + 1);
        addKeywords(ext);
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
    
    public Date getPublishedDate() {
        return publishedDate;
    }
    
    public String getEan() {
        return ean;
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
        titles.add(val);
        m_DataBuffer.append(" ");
        m_DataBuffer.append(val);
    }
    
    public String getTitle() {
        if (titles == null || titles.size() == 0) {
            int idx = pathname.lastIndexOf("/");
            return pathname.substring(idx + 1);
        } else {
            return titles.get(0).trim();
        }
    }
    public String getDescription() {
    	return description;
    }
    public void setDescription(String desc) {
    	description = desc;
    	m_DataBuffer.append(" ");
    	m_DataBuffer.append( desc);
    }
    public void addKeywords(String keyword) {
    	if( keyword == null) return;
    	if( m_Keywords == null) m_Keywords = new ArrayList<String>(10);
    	m_Keywords.add(keyword);
    	if( !m_KeyWordsList.contains(keyword)) {
    		m_KeyWordsList.add(keyword);
    	}
    	m_DataBuffer.append(" ");
    	m_DataBuffer.append(keyword);
    }
    public boolean matchSubject(String subject) {
    	return m_Keywords.contains(subject);
    }
    public static List<String> getAvailableKeywords() {
    	return ScannedFile.m_KeyWordsList;
    }
    public List<String> getKeywords() {
    	return m_Keywords;
    }
    private String m_Details=null;
    public String getDetails() {
    	if( m_Details != null)
    		return m_Details;
        StringBuffer text = new StringBuffer("<b>");
        text.append(getTitle());
        text.append("</b><br/><br/>");
        if( this.contributors.size()>0)
        	text.append("&nbsp;&nbsp;&nbsp;&nbsp;by "+getAuthor()+"<br/>");
        else
        	text.append("&nbsp;&nbsp;&nbsp;&nbsp; "+getAuthor()+"<br/>");
        text.append("<br/>");
        String tmp = getPublisher();
        if( tmp != null && !tmp.trim().equals("")) {
        	text.append("Publisher:");
        	text.append(tmp);
        	text.append("<br/>");
        }
        List<String> keywords = getKeywords();
        if( keywords.size() >1) {
        	text.append("<br/>Keyword(s):");
        }
        boolean tmpFlag=false;
        for(int i=1; i< keywords.size(); i++) {
        	if( i >1) text.append(',');
        	text.append(keywords.get(i));
        	tmpFlag=true;
        }
        if( tmpFlag) text.append("<br/><br/>");
        tmp = getDescription();
        if( tmp != null && !tmp.trim().equals("")) {
        	text.append(tmp);
        	text.append("<br/><br/>");
        }
        text.append("<b>File Path:</b>&nbsp;");
        text.append(pathname);
        m_Details = text.toString();
        return m_Details;
    }
    public String getAuthor() {
        if (contributors == null || contributors.size() == 0) {
            return "No Author Info";
        } else {
        	StringBuffer auth = new StringBuffer();
        	int count = contributors.size();
        	for(int i=0; i<count; i++) {
        		auth.append(contributors.get(i).toString());
        		auth.append(" ");
        	}
            return auth.toString();
        }
    }
    
    public void addContributor(String first, String last) {
        Contributors c = new Contributors(first, last);
        if( !contributors.contains(c)) {
        	contributors.add(c);
        	m_DataBuffer.append( " ");
        	m_DataBuffer.append(c.toString());
        }
    }
    
    private final class Contributors {
        private String firstName="", lastName="";
        
        public Contributors(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
        
        @Override
        public String toString() {
            return firstName + " " + lastName;
        }
        @Override
        public boolean equals(Object o) {
        	if( o != null && o instanceof Contributors) {
        		Contributors other = (Contributors) o;
        		if( toString().equals(other.toString()))
        			return true;
        	}
        	return false;
        }
        @Override
        public int hashCode() {
        	return toString().hashCode();
        }
    }
    public void setCover(String cover) {
    	m_Cover=cover;
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
    	if( m_Data == null) {
    		m_Data = m_DataBuffer.toString().toLowerCase();
    	}
        return m_Data;
    }
}

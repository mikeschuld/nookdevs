//This is my custom implementation of the ScannedFile interface which is
// returned as output from ecmscannerservice. 
//Interface is from ecmscannerservice package and this has to be part of that 
//package due to class casting issues.
//
package com.bravo.ecmscannerservice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class ScannedFile implements Parcelable, Comparable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static int m_SortType;
    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_AUTHOR = 1;
    public static final int SORT_BY_LATEST = 2;
    
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
    
    public ScannedFile() {
        super();
    }
    
    public ScannedFile(String pathName) {
        pathname = pathName;
    }
    
    public ScannedFile(android.os.Parcel parcel) {
        readFromParcel(parcel);
    }
    
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void writeToParcel(Parcel arg0, int arg1) {
        // TODO Auto-generated method stub
        // Not required for the client side.
    }
    
    public void readFromParcel(Parcel parcel) {
        StringBuffer buffer = new StringBuffer();
        pathname = parcel.readString();
        ean = parcel.readString();
        buffer.append(ean);
        buffer.append(" ");
        parcel.readStringList(titles);
        buffer.append(titles.toString());
        publisher = parcel.readString();
        buffer.append(" ");
        buffer.append(publisher);
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
            buffer.append(first);
            buffer.append(" ");
            buffer.append(last);
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
        m_Data = buffer.toString().toLowerCase();
    }
    
    public int compareTo(Object arg0) {
        if (arg0 instanceof ScannedFile) {
            ScannedFile file1 = (ScannedFile) arg0;
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
        }
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
    
    public void setTitle(String val) {
        if (val == null) { return; }
        if (titles == null) {
            titles = new ArrayList<String>();
        }
        titles.add(val);
    }
    
    public String getTitle() {
        if (titles == null || titles.size() == 0) {
            int idx = pathname.lastIndexOf("/");
            return pathname.substring(idx + 1);
        } else {
            return titles.get(0).trim();
        }
    }
    
    public String getAuthor() {
        if (contributors == null || contributors.size() == 0) {
            return "No Author Info";
        } else {
            return contributors.get(0).toString().trim();
        }
    }
    
    public void addContributor(String first, String last) {
        Contributors c = new Contributors(first, last);
        contributors.add(c);
    }
    
    private final class Contributors implements Serializable {
        String firstName, lastName;
        
        public Contributors(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
        
        @Override
        public String toString() {
            return firstName + " " + lastName;
        }
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
        return m_Data;
    }
    
}

package com.nookdevs.library;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.bravo.ecm.dto.BNContent;

public class BooksData implements Parcelable {
    public static final Parcelable.Creator<BooksData> CREATOR = new Parcelable.Creator<BooksData>() {
        public BooksData createFromParcel(Parcel in) {
            BooksData file = new BooksData();
            file.readFromParcel(in);
            return file;
        }
        
        public BooksData[] newArray(int size) {
            return new BooksData[size];
        }
    };
    public void readFromParcel( Parcel in) {
        ean = in.readString();
        publisher = in.readString();
        description = in.readString();
        series = in.readString();
        in.readStringList(titles);
        in.readStringList(contributors);
        in.readStringList(keywords);
    }
    public List<String> titles = new ArrayList<String>(1);
    public List<String> contributors =  new ArrayList<String>(1);
    public List<String> keywords = new ArrayList<String>(10);
    public String publisher;
    public String series;
    public String description;
    public String ean;
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }
    public void writeToParcel(Parcel out, int arg1) {
        out.writeString(ean);
        out.writeString(publisher);
        out.writeString(description);
        out.writeString(series);
        out.writeStringList(titles);
        out.writeStringList(contributors);
        out.writeStringList(keywords);
    }
}

package com.bravo.ecm.dto;

import android.os.Parcel;
import android.os.Parcelable;

import com.bravo.ecm.service.ScannedFile;

public class BNContent extends ScannedFile {
    public static final Parcelable.Creator<BNContent> CREATOR = new Parcelable.Creator<BNContent>() {
        public BNContent createFromParcel(Parcel in) {
            BNContent file = new BNContent();
            file.readFromParcel(in);
            return file;
        }
        
        public BNContent[] newArray(int size) {
            return new BNContent[size];
        }
    };
}

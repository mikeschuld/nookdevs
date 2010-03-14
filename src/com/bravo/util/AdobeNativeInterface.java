package com.bravo.util;

public class AdobeNativeInterface {
    static {
        System.loadLibrary("pdfhost");
    }
    public static native void cancelProcessing();
    public static synchronized native String getMetaData(String param);
    public static synchronized native int openPDF(String name);
    public static synchronized native int closePDF();
}

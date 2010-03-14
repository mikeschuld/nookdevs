package com.bravo.ecmscannerservice;
import com.bravo.ecmscannerservice.ScannedFile;
interface IECMScannerServiceCallback {
	void getList( inout List<ScannedFile> list);
    void getBatchList(inout List<ScannedFile> files);	
	void getFileFound( inout ScannedFile file);
	void setTotalSize(int size);
	void appendFiles(inout List<ScannedFile> files);
}
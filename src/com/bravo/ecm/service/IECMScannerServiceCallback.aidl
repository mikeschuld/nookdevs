package com.bravo.ecm.service;
import com.bravo.ecm.dto.BNContent;
interface IECMScannerServiceCallback {
	void appendFiles(inout List<BNContent> files);
	void getBatchList(inout List<BNContent> files);
	void getList( inout List<BNContent> list);
    void getSearchList(inout List<BNContent> files);
	void setNumOfSearchResult(int size);
	void setTotalSize(int size);
}
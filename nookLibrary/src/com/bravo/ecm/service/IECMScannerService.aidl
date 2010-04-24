package com.bravo.ecm.service;
import com.bravo.ecm.dto.BNContent;
import com.bravo.ecm.service.IECMScannerServiceCallback;
interface IECMScannerService {
	void cancel();
	int createMediaFolders();
	List<BNContent> getNext(int p1, int p2, int p3);
	List<BNContent> getNextSearchBatch(int p1, int p2, int p3);
	List<BNContent> getPrevious(int p1, int p2, int p3);
	List<BNContent> getPreviousSearchBatch(int p1, int p2, int p3);
	void scanDirectories(int p1, boolean p2, int p3, int p4, int p5,IECMScannerServiceCallback callback);
	void search(String key, boolean p2,int p3, int p4, int p5, IECMScannerServiceCallback callback);
	void sort(int p1, boolean p2, int p3, int p4, IECMScannerServiceCallback callback);
	boolean updateAccessDate(in BNContent bn);
}
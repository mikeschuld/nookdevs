package com.bravo.ecmscannerservice;
import com.bravo.ecmscannerservice.ScannedFile;
import com.bravo.ecmscannerservice.IECMScannerServiceCallback;
interface IECMScannerService {
	void registerCallback(in IECMScannerServiceCallback callback);
	void unregisterCallback(in IECMScannerServiceCallback callback);
	void scanDirectories(int type);
	void scanDirectoriesExt(int type, in String[] ext);
	void scanDirectoriesBatch(int type, in String[] folders, in String[] ext, int count, IECMScannerServiceCallback callback);
	String getPathname(String file);
	void findFile( inout ScannedFile file);
	int createMediaFolders();
	List<ScannedFile> getNextBatchOfContent(int start, int end);
	List<ScannedFile> getPreviousBatchOfContent(int start, int end);
}
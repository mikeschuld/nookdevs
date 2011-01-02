package com.nookdevs.library;
import com.nookdevs.library.IBooksServiceCallback;
interface IBooksService {
	void setData(in List<String> p, int start, int end);
	void setCallback(IBooksServiceCallback cb);
	void stopService();
}
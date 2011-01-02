package com.nookdevs.library;
import com.nookdevs.library.BooksData;
interface IBooksServiceCallback {
	void getMetaData(in List<BooksData> d, int start, int end);
}
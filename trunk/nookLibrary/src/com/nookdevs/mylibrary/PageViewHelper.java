/**
 *     This file is part of customlibrary app for nook.

    This is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    AppLauncher is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License.
    If not, see <http://www.gnu.org/licenses/>.

 */
package com.nookdevs.mylibrary;

import java.util.List;

import com.bravo.ecmscannerservice.ScannedFile;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PageViewHelper {
	LinearLayout m_PageViewMain;
	Activity m_Activity;
	int m_NumPages;
	int m_NumItems;
	public static final int ITEMS_PER_PAGE=10;
	int m_CurrentPage;
	int m_CurrentItem;
	TextView[] m_BookNames = new TextView[ITEMS_PER_PAGE];
	TextView[] m_Authors = new TextView[ITEMS_PER_PAGE];
	ImageView[] m_Dividers = new ImageView[ITEMS_PER_PAGE+1];
	ImageView[] m_Pointers = new ImageView[ITEMS_PER_PAGE];
	TextView m_Title ;
	TextView m_Header1;
	TextView m_Header2;
	List<ScannedFile> m_Files;
	public PageViewHelper(Activity activity, LinearLayout mainLayout, List<ScannedFile> files) {
		m_PageViewMain = mainLayout;
		m_Activity = activity;
		m_Files = files;
		createPage();
	}
	public void setFiles(List<ScannedFile> files) {
		m_Files = files;
		if( m_CurrentItem >0) {
			m_Dividers[m_CurrentItem].setVisibility(View.INVISIBLE);
			m_Dividers[m_CurrentItem-1].setVisibility(View.INVISIBLE);
			m_Pointers[m_CurrentItem-1].setVisibility(View.INVISIBLE);
		}
		initPage();
	}
	public List<ScannedFile> getFiles() {
		return m_Files;
	}
	public void gotoPage(int page) {
		if( m_CurrentPage == page) return;
		m_CurrentPage=page;
		int currentOffset = (page-1) * ITEMS_PER_PAGE;
		int i=0;
		for(; i< ITEMS_PER_PAGE && (currentOffset+i)<m_NumItems; i++) {
			ScannedFile file = m_Files.get(currentOffset +i);
			m_BookNames[i].setText( file.getTitle());
			m_Authors[i].setText( file.getAuthor());
		}
		for( ;i< ITEMS_PER_PAGE;i++) {
			m_BookNames[i].setText("");
			m_Authors[i].setText("");
		}
		updateHeader();
	}
	public void gotoItem(int item) {
		if( item <=0 || item > m_NumItems) return;
		int page = (item-1) / ITEMS_PER_PAGE +1;
		int itemidx = (item) % ITEMS_PER_PAGE;
		if( itemidx ==0) itemidx=ITEMS_PER_PAGE;
		gotoPage(page);
		m_Dividers[m_CurrentItem].setVisibility(View.INVISIBLE);
		m_Dividers[m_CurrentItem-1].setVisibility(View.INVISIBLE);
		m_Pointers[m_CurrentItem-1].setVisibility(View.INVISIBLE);
		m_CurrentItem = itemidx;
		m_Pointers[m_CurrentItem-1].setVisibility(View.VISIBLE);
		m_Dividers[m_CurrentItem-1].setVisibility(View.VISIBLE);
		m_Dividers[m_CurrentItem].setVisibility(View.VISIBLE);
	}
	private void createPage() {
		final LayoutInflater inflater = m_Activity.getLayoutInflater();
		m_PageViewMain.removeAllViews();
		m_Title = (TextView) inflater.inflate(R.layout.title, m_PageViewMain, false);
		m_PageViewMain.addView(m_Title);
		LinearLayout header = (LinearLayout) inflater.inflate(R.layout.pageheader, m_PageViewMain, false);
		m_Header1 = (TextView) header.findViewById(R.id.header01);
		m_Header2 = (TextView) header.findViewById(R.id.pageno);
		m_PageViewMain.addView(header);
		for(int i=0; i<ITEMS_PER_PAGE; i++) {
			LinearLayout pageItem = (LinearLayout) inflater.inflate(R.layout.pageitem, m_PageViewMain,false);
			ImageView img = (ImageView) inflater.inflate(R.layout.divider, m_PageViewMain, false);
			ImageView imgp = (ImageView) pageItem.findViewById(R.id.pageitempointer);
			m_BookNames[i] = (TextView) pageItem.findViewById(R.id.booktitle);
			m_Authors[i] = (TextView) pageItem.findViewById(R.id.bookauthor);
			m_Dividers[i] = img;
			m_Pointers[i] = imgp;
			m_PageViewMain.addView(img);
			m_PageViewMain.addView(pageItem);
			img.setVisibility(View.INVISIBLE);
			imgp.setVisibility(View.INVISIBLE);
		}
		ImageView img = (ImageView) inflater.inflate(R.layout.divider, m_PageViewMain, false);
		m_Dividers[ITEMS_PER_PAGE]= img;
		m_PageViewMain.addView(img);
		img.setVisibility(View.INVISIBLE);
		setTitle(R.string.my_documents);
		initPage();
	}
	private void initPage() {
		m_CurrentPage=0;
		m_CurrentItem=1;
		m_NumItems = m_Files.size();
		m_NumPages = (int) Math.ceil(m_NumItems / (float)ITEMS_PER_PAGE);
		loadNextPage();
		if( m_CurrentPage ==1) {
			m_Dividers[0].setVisibility(View.VISIBLE);
			m_Dividers[1].setVisibility(View.VISIBLE);
			m_Pointers[0].setVisibility(View.VISIBLE);
		}
	}
	private  void loadPrevPage() {
		if( m_Files == null || m_Files.size() ==0) {
			return;
		}
		if( m_CurrentPage ==1) {
			return;
		}
		m_CurrentPage--;
		int currentOffset = (m_CurrentPage-1) * ITEMS_PER_PAGE;
		for(int i=0; i< ITEMS_PER_PAGE; i++) {
			ScannedFile file = m_Files.get(currentOffset +i);
			m_BookNames[i].setText( file.getTitle());
			m_Authors[i].setText( file.getAuthor());
		}
		updateHeader();
	}
	private void clearData() {
		for(int i=0; i< ITEMS_PER_PAGE; i++) {
			m_Dividers[i].setVisibility(View.INVISIBLE);
			m_Pointers[i].setVisibility(View.INVISIBLE);
			m_BookNames[i].setText("");
			m_Authors[i].setText("");
		}
		m_Dividers[ITEMS_PER_PAGE].setVisibility(View.INVISIBLE);
		m_Header1.setText(R.string.no_result);
		m_Header2.setText("");
	}
	private void loadNextPage() {
		if( m_Files == null || m_Files.size() ==0) {
			clearData();
			return;
		}
		if( m_CurrentPage >= m_NumPages) {
			return;
		}
		int currentOffset = m_CurrentPage * ITEMS_PER_PAGE;
		m_CurrentPage++;
		int i=0;
		for(;i< ITEMS_PER_PAGE && (currentOffset+i)<m_NumItems ; i++) {
			ScannedFile file = m_Files.get(currentOffset +i);
			m_BookNames[i].setText( file.getTitle());
			m_Authors[i].setText( file.getAuthor());
		}
		if( m_CurrentItem >i) {
			m_Dividers[m_CurrentItem].setVisibility(View.INVISIBLE);
			m_Dividers[m_CurrentItem-1].setVisibility(View.INVISIBLE);
			m_Pointers[m_CurrentItem-1].setVisibility(View.INVISIBLE);
			m_CurrentItem = i;
			m_Dividers[m_CurrentItem].setVisibility(View.VISIBLE);
			m_Dividers[m_CurrentItem-1].setVisibility(View.VISIBLE);
			m_Pointers[m_CurrentItem-1].setVisibility(View.VISIBLE);
			
		}
		for( ;i< ITEMS_PER_PAGE;i++) {
			m_BookNames[i].setText("");
			m_Authors[i].setText("");
		}
		updateHeader();
	}
	public void pageUp() {
		if( m_CurrentPage >1) {
			loadPrevPage();
		}
	}
	public void pageDown() {
		if( m_CurrentPage < m_NumPages) {
			loadNextPage();
		}
	}
	public void selectNext() {
		int prev = m_CurrentItem-1;
		if( m_CurrentPage == m_NumPages) {
			int total = m_NumItems % ITEMS_PER_PAGE;
			if( total ==0) total = ITEMS_PER_PAGE;
			if( m_CurrentItem >=total) {
				return;
			}
		}
		if( m_CurrentItem == ITEMS_PER_PAGE) {
			m_Dividers[ITEMS_PER_PAGE].setVisibility(View.INVISIBLE);
			m_Dividers[ITEMS_PER_PAGE-1].setVisibility(View.INVISIBLE);
			m_Pointers[ITEMS_PER_PAGE-1].setVisibility(View.INVISIBLE);
			
			m_CurrentItem=0;
			loadNextPage();
		} else {
			m_Dividers[prev].setVisibility(View.INVISIBLE);
			m_Pointers[m_CurrentItem-1].setVisibility(View.INVISIBLE);
		}
		m_Dividers[m_CurrentItem].setVisibility(View.VISIBLE);
		m_Dividers[m_CurrentItem+1].setVisibility(View.VISIBLE);
		m_Pointers[m_CurrentItem].setVisibility(View.VISIBLE);
		
		m_CurrentItem++;
		
	}
	public void selectPrev() {
		int prev = m_CurrentItem;
		if( m_CurrentItem == 1) {
			if( m_CurrentPage ==1) return;
			m_Dividers[1].setVisibility(View.INVISIBLE);
			m_Dividers[0].setVisibility(View.INVISIBLE);
			m_Pointers[0].setVisibility(View.INVISIBLE);
	
			m_CurrentItem=ITEMS_PER_PAGE+1;
			loadPrevPage();
		} else {
			m_Dividers[prev].setVisibility(View.INVISIBLE);
			m_Pointers[prev-1].setVisibility(View.INVISIBLE);
		}
		m_CurrentItem--;
		m_Dividers[m_CurrentItem].setVisibility(View.VISIBLE);
		m_Dividers[m_CurrentItem-1].setVisibility(View.VISIBLE);
		m_Pointers[m_CurrentItem-1].setVisibility(View.VISIBLE);
	}
	public void setTitle(String title) {
		m_Title.setText(title);
	}
	public void setTitle(int res) {
		m_Title.setText(res);
	}
	private void updateHeader() {
		if( m_CurrentPage ==0) 
			updateHeader(0);
		else
			updateHeader((m_CurrentPage-1) * ITEMS_PER_PAGE +1 );
	}
	private void updateHeader(int curr) {
		if( curr ==0) {
			m_Header1.setText(R.string.no_result);
			m_Header2.setText("");
			return;
		}
		int end = curr + ITEMS_PER_PAGE-1;
		if( end > m_NumItems) end= m_NumItems;
		m_Header1.setText("Displaying " + curr + " to " + end + " of " + m_NumItems);	
		m_Header2.setText(m_CurrentPage +"|" + m_NumPages);
	}
	public int getCurrentIndex() {
		return (m_CurrentPage-1)*ITEMS_PER_PAGE + m_CurrentItem;
	}
	public ScannedFile getCurrent() {
		if( m_CurrentPage ==0) return null;
		int currentOffset = (m_CurrentPage-1) * ITEMS_PER_PAGE;
		if( m_Files == null || m_Files.size() < (currentOffset+m_CurrentItem-1)) {
			return null;
		}
		return m_Files.get( currentOffset + m_CurrentItem-1);
	}
}

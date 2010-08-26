/*
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nookdevs.fileselector;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
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
    public static final int ITEMS_PER_PAGE = 16;
    int m_CurrentPage;
    int m_CurrentItem;
    TextView[] m_BookNames = new TextView[ITEMS_PER_PAGE];
 //   TextView[] m_Authors = new TextView[ITEMS_PER_PAGE];
    ImageView[] m_Dividers = new ImageView[ITEMS_PER_PAGE + 1];
    ImageView[] m_Pointers = new ImageView[ITEMS_PER_PAGE];
    TextView m_Title;
    TextView m_Header1;
    TextView m_Header2;
    LinearLayout m_Footer;
    File[] m_Files;
    String [] m_Types;
    File m_Root;
    File m_Current;
    FilenameFilter m_FileFilter;
    public enum SortType {
        NAME, TYPE, DATE
    };
    private SortType m_SortType=SortType.NAME;
    private boolean m_SortReverse=false;
    private Comparator<File> m_Comparator = new Comparator<File>() {

        public int compare(File arg0, File arg1) {
            if( m_SortReverse) {
                File t = arg0;
                arg0 = arg1;
                arg1=t;
            }
            if( arg0 == null) 
                return -1;
            if( arg1 == null)
                return 1;
            if( arg0.isDirectory() && !arg1.isDirectory()) {
                return -1;
            }
            if( arg1.isDirectory() && !arg0.isDirectory()) {
                return 1;
            }
            switch (m_SortType) {
                case NAME:
                    return arg0.getName().compareToIgnoreCase(arg1.getName());
                case TYPE:
                    String t1 = arg0.getName();
                    int idx = t1.lastIndexOf('.');
                    t1 = t1.substring(idx+1);
                    String t2 = arg1.getName();
                    idx = t2.lastIndexOf('.');
                    t2 = t2.substring(idx+1);
                    return t1.compareToIgnoreCase(t2);
                case DATE:
                    long d1 = arg0.lastModified();
                    long d2 = arg1.lastModified();
                    if(d1 > d2) return -1;
                    else if( d1 == d2) return 0;
                    else return 1;
            }
                
            return 0;
        }
        
    };
    public PageViewHelper(Activity activity, LinearLayout mainLayout, String[] types, File folder) {
        m_PageViewMain = mainLayout;
        m_Activity = activity;
        m_Types=types;
        m_Root = folder;
        m_Current = m_Root;
        createPage();
    }
    public void setSortType(SortType type) {
        m_SortType = type;
    }
    public void setSortReversed(boolean val) {
        m_SortReverse=val;
    }
    
    public void gotoPage(int page) {
        if (m_CurrentPage == page) { return; }
        m_CurrentPage = page;
        int currentOffset = (page - 1) * ITEMS_PER_PAGE;
        int i = 0;
        for (; i < ITEMS_PER_PAGE && (i+currentOffset) < m_NumItems; i++) {
            File f = m_Files[i+ currentOffset];
            if( f.isDirectory()) {
                m_BookNames[i].setText("[" + f.getName() + "]");
            } else { 
                m_BookNames[i].setText(f.getName());
            }
        }
        updateHeader();
        updateFooter();
    }
    public void followDirectory(File file) {
        m_Current = file;
        clearData();
        initPage();
    }
    
    public void gotoItem(int item) {
        if (item <= 0 || item > m_NumItems) { return; }
        int page = (item - 1) / ITEMS_PER_PAGE + 1;
        int itemidx = (item) % ITEMS_PER_PAGE;
        if (itemidx == 0) {
            itemidx = ITEMS_PER_PAGE;
        }
        gotoPage(page);
        m_Dividers[m_CurrentItem].setVisibility(View.INVISIBLE);
        m_Dividers[m_CurrentItem - 1].setVisibility(View.INVISIBLE);
        m_Pointers[m_CurrentItem - 1].setVisibility(View.INVISIBLE);
        m_CurrentItem = itemidx;
        m_Pointers[m_CurrentItem - 1].setVisibility(View.VISIBLE);
        m_Dividers[m_CurrentItem - 1].setVisibility(View.VISIBLE);
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
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            LinearLayout pageItem = (LinearLayout) inflater.inflate(R.layout.pageitem, m_PageViewMain, false);
            ImageView img = (ImageView) inflater.inflate(R.layout.divider, m_PageViewMain, false);
            ImageView imgp = (ImageView) pageItem.findViewById(R.id.pageitempointer);
            m_BookNames[i] = (TextView) pageItem.findViewById(R.id.booktitle);
          //  m_Authors[i] = (TextView) pageItem.findViewById(R.id.bookauthor);
            m_Dividers[i] = img;
            m_Pointers[i] = imgp;
            m_PageViewMain.addView(img);
            m_PageViewMain.addView(pageItem);
            img.setVisibility(View.INVISIBLE);
            imgp.setVisibility(View.INVISIBLE);
        }
        ImageView img = (ImageView) inflater.inflate(R.layout.divider, m_PageViewMain, false);
        m_Dividers[ITEMS_PER_PAGE] = img;
        m_PageViewMain.addView(img);
        m_Footer = (LinearLayout) inflater.inflate(R.layout.dots, m_PageViewMain, false);
        m_PageViewMain.addView(m_Footer);
        img.setVisibility(View.INVISIBLE);
        setTitle(R.string.my_documents);
        m_FileFilter = new FilenameFilter() {

            public boolean accept(File dir, String filename) {
                if( m_Types == null) return true;
                File f = new File(dir.getAbsolutePath()+"/" + filename);
                if( f.isDirectory()) return true;
                for (String type:m_Types) {
                    if( filename.matches(type)) {
                        return true;
                    }
                }
                return false;
            }
            
        };
        initPage();
    }
    
    private void initPage() {
        m_CurrentPage = 0;
        m_CurrentItem = 1;
        //m_Files = new ArrayList(m_Current.list(m_FileFilter));
        System.out.println("File Folder =" + m_Current);
        m_Files = m_Current.listFiles(m_FileFilter);
        if( m_Files == null) 
            m_Files = new File[0];
        else {
            List<File> list = Arrays.asList(m_Files);
            Collections.sort( list, m_Comparator);
            m_Files = list.toArray(new File[0]);
        }
        m_NumItems  = m_Files.length;
        m_NumPages = (int) Math.ceil(m_NumItems / (float) ITEMS_PER_PAGE);
        loadNextPage();
        if (m_CurrentPage == 1) {
            m_Dividers[0].setVisibility(View.VISIBLE);
            m_Dividers[1].setVisibility(View.VISIBLE);
            m_Pointers[0].setVisibility(View.VISIBLE);
        }
    }
    
    private void loadPrevPage() {
        if (m_Files == null || m_Files.length == 0) { return; }
        if (m_CurrentPage == 1) { return; }
        m_CurrentPage--;
        int currentOffset = (m_CurrentPage - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            File f = m_Files[i+ currentOffset];
            if( f.isDirectory()) {
                m_BookNames[i].setText("[" + f.getName() + "]");
            } else { 
                m_BookNames[i].setText(f.getName());
            }
             //   m_Authors[i].setText(file.getAuthor());
        }
        updateHeader();
        updateFooter();
    }
    
    public void update() {
        if (m_CurrentItem > 0) {
            File f = getCurrent();
            if( f.isDirectory()) {
                m_BookNames[m_CurrentItem-1].setText("[" + f.getName() + "]");
            } else { 
                m_BookNames[m_CurrentItem-1].setText(f.getName());
            }
        }
    }
    
    private void clearData() {
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            m_Dividers[i].setVisibility(View.INVISIBLE);
            m_Pointers[i].setVisibility(View.INVISIBLE);
            m_BookNames[i].setText("");
    //        m_Authors[i].setText("");
        }
        m_Dividers[ITEMS_PER_PAGE].setVisibility(View.INVISIBLE);
        m_Header1.setText(R.string.no_result);
        m_Header2.setText("");
    }
    
    private void loadNextPage() {
        if (m_Files == null || m_Files.length == 0) {
            clearData();
            updateFooter();
            return;
        }
        if (m_CurrentPage >= m_NumPages) { return; }
        int currentOffset = m_CurrentPage * ITEMS_PER_PAGE;
        m_CurrentPage++;
        int i = 0;
        for (; i < ITEMS_PER_PAGE && (currentOffset + i) < m_NumItems; i++) {
                File f = m_Files[currentOffset + i];
                if( f.isDirectory()) 
                    m_BookNames[i].setText("[" + m_Files[currentOffset + i].getName()+"]");
                else
                    m_BookNames[i].setText(m_Files[currentOffset + i].getName());
            //    m_Authors[i].setText(file.getAuthor());
        }
        if (m_CurrentItem > i) {
            m_Dividers[m_CurrentItem].setVisibility(View.INVISIBLE);
            m_Dividers[m_CurrentItem - 1].setVisibility(View.INVISIBLE);
            m_Pointers[m_CurrentItem - 1].setVisibility(View.INVISIBLE);
            m_CurrentItem = i;
            m_Dividers[m_CurrentItem].setVisibility(View.VISIBLE);
            m_Dividers[m_CurrentItem - 1].setVisibility(View.VISIBLE);
            m_Pointers[m_CurrentItem - 1].setVisibility(View.VISIBLE);
            
        }
        for (; i < ITEMS_PER_PAGE; i++) {
            m_BookNames[i].setText("");
     //       m_Authors[i].setText("");
        }
        updateHeader();
        updateFooter();
    }
    
    public void pageUp() {
        if (m_CurrentPage > 1) {
            loadPrevPage();
        }
    }
    
    public void pageDown() {
        if (m_CurrentPage < m_NumPages) {
            loadNextPage();
        }
    }
    
    public void selectNext() {
        if( m_NumItems ==0) return;
        int prev = m_CurrentItem - 1;
        if (m_CurrentPage == m_NumPages) {
            int total = m_NumItems % ITEMS_PER_PAGE;
            if (total == 0) {
                total = ITEMS_PER_PAGE;
            }
            if (m_CurrentItem >= total) { return; }
        }
        if (m_CurrentItem == ITEMS_PER_PAGE) {
            m_Dividers[ITEMS_PER_PAGE].setVisibility(View.INVISIBLE);
            m_Dividers[ITEMS_PER_PAGE - 1].setVisibility(View.INVISIBLE);
            m_Pointers[ITEMS_PER_PAGE - 1].setVisibility(View.INVISIBLE);
            
            m_CurrentItem = 0;
            loadNextPage();
        } else {
            m_Dividers[prev].setVisibility(View.INVISIBLE);
            m_Pointers[m_CurrentItem - 1].setVisibility(View.INVISIBLE);
        }
        m_Dividers[m_CurrentItem].setVisibility(View.VISIBLE);
        m_Dividers[m_CurrentItem + 1].setVisibility(View.VISIBLE);
        m_Pointers[m_CurrentItem].setVisibility(View.VISIBLE);
        
        m_CurrentItem++;
        
    }
    
    public void selectPrev() {
        if( m_NumItems ==0) return;
        int prev = m_CurrentItem;
        if (m_CurrentItem == 1) {
            if (m_CurrentPage == 1) { return; }
            m_Dividers[1].setVisibility(View.INVISIBLE);
            m_Dividers[0].setVisibility(View.INVISIBLE);
            m_Pointers[0].setVisibility(View.INVISIBLE);
            
            m_CurrentItem = ITEMS_PER_PAGE + 1;
            loadPrevPage();
        } else {
            m_Dividers[prev].setVisibility(View.INVISIBLE);
            m_Pointers[prev - 1].setVisibility(View.INVISIBLE);
        }
        m_CurrentItem--;
        m_Dividers[m_CurrentItem].setVisibility(View.VISIBLE);
        m_Dividers[m_CurrentItem - 1].setVisibility(View.VISIBLE);
        m_Pointers[m_CurrentItem - 1].setVisibility(View.VISIBLE);
    }
    
    public void setTitle(String title) {
        m_Title.setText(title);
    }
    
    public void setTitle(int res) {
        m_Title.setText(res);
    }
    
    private void updateHeader() {
        if (m_CurrentPage == 0) {
            updateHeader(0);
        } else {
            updateHeader((m_CurrentPage - 1) * ITEMS_PER_PAGE + 1);
        }
    }
    
    private void updateHeader(int curr) {
        if (curr == 0) {
            m_Header1.setText(R.string.no_result);
            m_Header2.setText("");
            return;
        }
        int end = curr + ITEMS_PER_PAGE - 1;
        if (end > m_NumItems) {
            end = m_NumItems;
        }
        m_Header1.setText("Displaying " + curr + " to " + end + " of " + m_NumItems);
        m_Header2.setText(m_CurrentPage + "|" + m_NumPages);
    }
    
    private void updateFooter() {
        boolean showDots = (m_NumPages > 1 && m_NumPages <= 10);
        for (int i = 0; i < 10; i++) {
            m_Footer.findViewWithTag("dot_filled_" + i).setVisibility(
                showDots && i < m_CurrentPage ? View.VISIBLE : View.GONE);
        }
        for (int i = 0; i < 9; i++) {
            m_Footer.findViewWithTag("dot_empty_" + i).setVisibility(
                showDots && i < m_NumPages - m_CurrentPage ? View.VISIBLE : View.GONE);
        }
    }
    
    public void gotoTop() {
        int item = (m_CurrentPage - 1) * ITEMS_PER_PAGE + 1;
        gotoItem(item);
    }
    
    public void gotoBottom() {
        int item = (m_CurrentPage - 1) * ITEMS_PER_PAGE + ITEMS_PER_PAGE;
        if( m_NumItems < item) {
            item = m_NumItems;
        }
        gotoItem(item);
    }
    
    public int getCurrentIndex() {
        return (m_CurrentPage - 1) * ITEMS_PER_PAGE + m_CurrentItem;
    }
    
    public File getCurrent() {
        if (m_CurrentPage == 0) { return null; }
        int currentOffset = (m_CurrentPage - 1) * ITEMS_PER_PAGE;
        if (m_Files == null || m_Files.length < (currentOffset + m_CurrentItem - 1)) { return null; }
        return  m_Files[currentOffset + m_CurrentItem - 1];
    }
    public File getCurrentFolder() {
        return m_Current;
    }
}

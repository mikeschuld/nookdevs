/* 
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.nookdevs.library;

import java.util.ArrayList;
import java.util.List;

import org.geometerplus.fbreader.library.Author;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Tag;
import org.geometerplus.zlibrary.core.filesystem.ZLArchiveEntryFile;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;

import com.bravo.ecm.service.ScannedFile;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class BooksService extends Service {

    private ScannedFile m_File;
    IBooksServiceCallback m_Callback = null;
    ArrayList<BooksData> data = new ArrayList<BooksData>(10);
    
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final IBooksService.Stub mBinder = new IBooksService.Stub(){

        public void setData(List<String> paths, int start, int end) throws RemoteException {
            for(String path: paths) {
               m_File = new ScannedFile( path,false,false);
               String type = m_File.getType();
               if ("epub".equals(type)) {
                   EpubMetaReader.parse(m_File);
               } else if ("pdf".equalsIgnoreCase(type)) {
                   PdfMetaReader.setContext(getBaseContext());
                   PdfMetaReader.readMetadata(m_File);
               } else if ("fb2".equalsIgnoreCase(type)) { 
                   Book book = tryGetFB2Book(ZLFile.createFileByPath(path)); 
                   if( book != null) {
                       m_File.setTitle(book.getTitle()); 
                       for(Tag s : book.tags()) 
                       { 
                           m_File.addKeywords(s.Name,true); 
                       } 
                       for(Author s : book.authors()) 
                       { 
                           m_File.addContributor(s.DisplayName, ""); 
                       }
                   }
               }
               BooksData bd =new BooksData();
               bd.contributors.addAll(m_File.getContributorsStr());
               bd.titles.addAll(m_File.getTitles());
               bd.keywords.addAll(m_File.getKeywords());
               bd.publisher = m_File.getPublisher();
               bd.series = m_File.getSeries();
               bd.description = m_File.getDescription(true);
               bd.ean = m_File.getEan();
               data.add(bd);
           }
           if( m_Callback != null)
               m_Callback.getMetaData(data, start, end);
           data.clear();
        }

        public void setCallback(IBooksServiceCallback cb) throws RemoteException {
            m_Callback = cb;
            
        }

        public void stopService() throws RemoteException {
            stopSelf();
            
        }
    };
    Book tryGetFB2Book(ZLFile file)
    {
        try {
            if (file.isArchive()) {
                Book book = new Book(ZLArchiveEntryFile.archiveEntries(file).get(0));
                book.readMetaInfo();
                return book;
            }
            else
            {
                Book book = new Book(file);
                book.readMetaInfo();
                return book;
            }
        } catch(Error ex) {
            return null;
        }
        
    }
}

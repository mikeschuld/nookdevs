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

import java.util.StringTokenizer;

import android.util.Log;

import com.bravo.ecmscannerservice.ScannedFile;

import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PInfo;

public class PdfMetaReader {
    ScannedFile m_File;
    
    public PdfMetaReader(ScannedFile file) {
        m_File = file;
        readMetadata();
    }
    private boolean readMetadata() {
        try {
            Document doc = new Document();
            doc.setFile(m_File.getPathName());
            PInfo info = doc.getInfo();
       //     m_File.addContributor(info.getAuthor(), "");
       //     m_File.setTitle( info.getTitle());
            m_File.setDescription(info.getSubject());
            StringTokenizer token = new StringTokenizer(info.getKeywords(),",; ");
            while( token.hasMoreTokens()) {
                m_File.addKeywords(token.nextToken());
            }
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            Log.w("PdfMetaReader", "No Metadata for " + m_File.getPathName());
            return false;
        }
   
        return true;
    }
}


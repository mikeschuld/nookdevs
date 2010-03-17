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

import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PInfo;

import android.util.Log;

import com.bravo.ecmscannerservice.ScannedFile;
import com.bravo.util.AdobeNativeInterface;

public class PdfMetaReader {
    ScannedFile m_File;
    
    public PdfMetaReader(ScannedFile file, boolean quick) {
        m_File = file;
        readMetadata(quick);
    }
    
    private boolean readMetadata(boolean quick) {
        try {
            if (quick) {
                AdobeNativeInterface.openPDF(m_File.getPathName());
                m_File.setTitle(AdobeNativeInterface.getMetaData("DC.title"));
                m_File.addContributor(AdobeNativeInterface.getMetaData("DC.creator"), "");
                AdobeNativeInterface.closePDF();
            } else {
                Document doc = new Document();
                try {
                    doc.setFile(m_File.getPathName());
                } catch (Throwable er) {
                    
                }
                PInfo info = doc.getInfo();
                m_File.setDescription(info.getSubject());
                StringTokenizer token = new StringTokenizer(info.getKeywords(), ",; ");
                while (token.hasMoreTokens()) {
                    m_File.addKeywords(token.nextToken());
                }
            }
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            Log.w("PdfMetaReader", "No Metadata for " + m_File.getPathName());
            return false;
        }
        
        return true;
    }
}

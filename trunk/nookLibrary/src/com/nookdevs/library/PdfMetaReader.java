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

import java.io.File;
import java.io.FileDescriptor;
import java.util.StringTokenizer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bravo.ecm.service.ScannedFile;
import com.bravo.util.AdobeNativeInterface;

public class PdfMetaReader {
    private static Context m_Context;
    public static void setContext(Context context) {
        m_Context = context;
    }
    
    public static boolean readMetadata(ScannedFile m_File) {
        try {
//            if (quick) {
//                int ret = AdobeNativeInterface.openPDF(m_File.getPathName());
//                if (ret != 0) { return false; }
//                m_File.setTitle(AdobeNativeInterface.getMetaData("DC.title"));
//                m_File.addContributor(AdobeNativeInterface.getMetaData("DC.creator"), "");
//                m_File.setPublisher(AdobeNativeInterface.getMetaData("DC.publisher"));
//                m_File.setEan(AdobeNativeInterface.getMetaData("DC.identifier"));
//                AdobeNativeInterface.closePDF();
//            } else {
                FileDescriptor fd = m_Context.getContentResolver().openAssetFileDescriptor(
                                    Uri.fromFile(new File(m_File.getPathName())),"r").getFileDescriptor();
                PDFDocument doc = new PDFDocument(fd,"","");
                if( !doc.isOk()) {
                    return false;
                }
                String xml = doc.getMetadata();
                //we only need desc and keywords. so, no need to do xml parsing here.
                int idx1,idx2;
                idx1 = xml.indexOf("<dc:description>");
                if( idx1 >=0) {
                    idx2 = xml.indexOf("</dc:description>");
                    String tmp = xml.substring(idx1, idx2);
                    StringBuffer desc=new StringBuffer();
                    int len = tmp.length();
                    for(int i=0; i< len; i++) {
                        if( tmp.charAt(i) =='<') {
                            i++;
                            while( i < len && tmp.charAt(i) != '>') {
                                i++;
                            }
                        } else {
                            desc.append(tmp.charAt(i));
                        }
                    }
                    m_File.setDescription( desc.toString());
                } 
                idx1 = xml.indexOf("<pdf:Keywords>");
                idx2 = xml.indexOf("</pdf:Keywords>");
                if( idx1 >=0) {
                    String tmp = xml.substring(idx1+14, idx2);
                    StringTokenizer token = new StringTokenizer(tmp,";,\n");
                    while( token.hasMoreTokens()) {
                        m_File.addKeywords( token.nextToken(),true);
                    }
                } 
      //      }
        } catch (Throwable e) {
            Log.e("PDFMetaReader", e.getMessage(), e);
            return false;
        }
        
        return true;
    }
}

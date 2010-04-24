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
package com.nookdevs.library;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

import com.bravo.ecm.service.ScannedFile;

public class EpubMetaReader {
    public static final String TITLE = "title";
    public static final String CREATOR = "creator";
    public static final String CONTRIBUTOR = "contributor";
    public static final String PUBLISHER = "publisher";
    public static final String DESCRIPTION = "description";
    public static final String SUBJECT = "subject";
    public static final String IDENTIFIER = "identifier";
    public static final String SERIES = "series";
    public static final String SERIES_INDEX = "series_index";
    public static final String DATE = "date";
    public static final String ISBN = "ISBN";
    String[] entries = {
        TITLE, CREATOR, PUBLISHER, DESCRIPTION, SUBJECT, IDENTIFIER, SERIES, SERIES_INDEX
    };
    
    private static List<String> m_ValidEntries;
    ScannedFile m_File;
    
    public EpubMetaReader(ScannedFile file) {
        m_File = file;
        if (m_ValidEntries == null) {
            m_ValidEntries = Arrays.asList(entries);
        }
        parse();
    }
    
    public boolean loadCover() {
        ZipFile zip = null;
        try {
            zip = new ZipFile(m_File.getPathName());
            ZipEntry entry;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    if (name.contains("cover") || name.contains("cvi")) {
                        if (name.endsWith("jpg") || name.endsWith("jpeg") || name.endsWith("png")
                            || name.endsWith("gif") || name.endsWith("JPG") || name.endsWith("PNG")
                            || name.endsWith("JPEG") || name.endsWith("GIF")) {
                            // got the cover
                            int idx = name.lastIndexOf(".");
                            String ext = name.substring(idx);
                            idx = m_File.getPathName().lastIndexOf('.');
                            String imgname = m_File.getPathName().substring(0, idx);
                            imgname += ext;
                            FileOutputStream out = new FileOutputStream(imgname);
                            m_File.setCover(imgname);
                            InputStream inp = zip.getInputStream(entry);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = inp.read(buffer)) >= 0) {
                                out.write(buffer, 0, len);
                            }
                            
                            inp.close();
                            out.close();
                            return true;
                        }
                    }
                    
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }
    
    private boolean parse() {
        String path = m_File.getPathName();
        if (path == null) { return false; }
        File file = new File(path);
        if (!file.exists() || !path.toLowerCase().endsWith(".epub")) { return false; }
        ZipFile zip = null;
        try {
            zip = new ZipFile(file);
            ZipEntry container = zip.getEntry("META-INF/container.xml");
            if (container == null) { return false; }
            InputStream inp = zip.getInputStream(container);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inp, null);
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if ("rootfile".equalsIgnoreCase(name)) {
                        String nameSpace = parser.getAttributeNamespace(0);
                        String value = parser.getAttributeValue(nameSpace, "full-path");
                        if (value != null) {
                            ZipEntry entry = zip.getEntry(value);
                            InputStream inp1 = zip.getInputStream(entry);
                            parser.setInput(inp1, null);
                            break;
                        }
                    }
                }
            }
            // parse opf file here
            boolean valid = false;
            String idx = "";
            boolean seriesTag = false;
            boolean seriesIdx = false;
            String name = "";
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.END_TAG) {
                    name = parser.getName();
                    if ("metadata".equalsIgnoreCase(name)) {
                        break;
                    }
                }
                if (type == XmlPullParser.START_TAG) {
                    name = parser.getName();
                    if (m_ValidEntries.contains(name)) {
                        valid = true;
                    } else {
                        valid = false;
                    }
                    if (name.equals(IDENTIFIER)) {
                        int count = parser.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            String attr = parser.getAttributeName(i);
                            if (attr != null && attr.contains("scheme")) {
                                String val = parser.getAttributeValue(i);
                                if (!val.equals(ISBN)) {
                                    name = "";
                                }
                            }
                        }
                    }
                    if (name.equals("meta") && !(seriesTag && seriesIdx)) {
                        int count = parser.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            String attr = parser.getAttributeName(i);
                            String val = parser.getAttributeValue(i);
                            if (attr.equals("name") && val.equals("calibre:series")) {
                                seriesTag = true;
                                i++;
                                attr = parser.getAttributeName(i);
                                val = parser.getAttributeValue(i);
                                m_File.setSeries(val);
                                m_File.addKeywords(val);
                                break;
                            } else if (attr.equals("name") && val.equals("calibre:series_index")) {
                                i++;
                                attr = parser.getAttributeName(i);
                                idx = parser.getAttributeValue(i);
                                int dot = idx.indexOf('.');
                                if (dot != -1) {
                                    idx = idx.substring(0, dot);
                                }
                                if (idx.length() == 1) {
                                    idx = "0" + idx;
                                }
                                seriesIdx = true;
                                break;
                            }
                        }
                    }
                }
                if (type == XmlPullParser.TEXT && valid) {
                    String text = parser.getText();
                    if (name.equals(TITLE)) {
                        m_File.setTitle(text);
                    } else if (name.equals(CREATOR)) {
                        m_File.addContributor(text, "");
                    } else if (name.equals(CONTRIBUTOR)) {
                        m_File.addContributor(text, "");
                    } else if (name.equals(PUBLISHER)) {
                        m_File.setPublisher(text);
                    } else if (name.equals(DESCRIPTION)) {
                        m_File.setDescription(text);
                    } else if (name.equals(SUBJECT)) {
                        m_File.addKeywords(text);
                    } else if (name.equals(IDENTIFIER)) {
                        m_File.setEan(text);
                    } else if (name.equals(SERIES)) {
                        seriesTag = true;
                        m_File.setSeries(text);
                        m_File.addKeywords(text);
                    } else if (name.equals(SERIES_INDEX)) {
                        idx = text;
                        int dot = idx.indexOf('.');
                        if (dot != -1) {
                            idx = idx.substring(0, dot);
                        }
                        if (idx.length() == 1) {
                            idx = "0" + idx;
                        }
                    }
                    valid = false;
                }
            }
            if (seriesTag) {
                m_File.setSeries(m_File.getSeries() + " " + idx + "-");
            }
        } catch (Exception ex) {
            Log.e("EpuBMetaReader", "Exception parsing metadata", ex);
            return false;
        }
        return true;
        
    }
}

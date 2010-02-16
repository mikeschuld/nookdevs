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
package com.nookdevs.filemanager;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StatusUpdater {
    private Activity m_Activity;
    LinearLayout m_Layout;
    int m_Resource;
    String m_Failed;
    ArrayList<FileStatus> m_Files = new ArrayList<FileStatus>(10);
    
    public StatusUpdater(Activity activity, LinearLayout layout, int resource) {
        m_Activity = activity;
        m_Layout = layout;
        m_Resource = resource;
        m_Failed = m_Activity.getString(R.string.failed);
    }
    
    public int addFile(String file) {
        FileStatus fs = new FileStatus();
        // if( file.length() > 50) {
        // file = file.substring(file.length()-50);
        // }
        fs.m_File = file;
        fs.progress = 0;
        LayoutInflater inflater = m_Activity.getLayoutInflater();
        TextView filedetails = (TextView) inflater.inflate(m_Resource, m_Layout, false);
        filedetails.setText(file + "  " + fs.progress + "% completed.");
        m_Layout.addView(filedetails, 0);
        fs.txt = filedetails;
        m_Files.add(fs);
        return m_Files.size() - 1;
    }
    
    public void removeCompleted() {
        int count = m_Files.size();
        if (count < 10) { return; }
        for (int i = 10; i < count; i++) {
            FileStatus fs = m_Files.get(i);
            if (fs.progress >= 100 || fs.progress == -1) {
                m_Layout.removeView(fs.txt);
                m_Files.remove(fs);
            }
        }
    }
    
    public void updateProgress(int id, int percent) {
        FileStatus fs = m_Files.get(id);
        if (percent == -1) {
            fs.txt.setText(fs.m_File + " " + m_Failed + ".");
        } else {
            fs.progress = percent;
            fs.txt.setText(fs.m_File + "  " + fs.progress + "% completed.");
        }
    }
}

class FileStatus {
    String m_File;
    int progress;
    TextView txt;
}

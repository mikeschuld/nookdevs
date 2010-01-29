/**
 *     This file is part of nookCommon.

    nookCommon is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    nookCommon is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with nookCommon.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.nookdevs.common;

import java.util.Vector;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;

public class ImageAdapter extends BaseAdapter implements SpinnerAdapter {
    
    // just for testing
    Vector<String> m_AppIcons;
    int[] m_Resources;
    Drawable m_CurrentImage;
    Context m_Context;
    int m_GalleryItemBackground;
    int m_Default;
    
    public ImageAdapter(Context context, Vector<String> icons, int[] resources) {
        super();
        m_Context = context;
        if (icons == null) {
            m_AppIcons = new Vector<String>();
        } else {
            m_AppIcons = icons;
        }
        if (resources == null) {
            m_Resources = new int[0];
        } else {
            m_Resources = resources;
        }
        
    }
    
    public void setDefault(int res) {
        m_Default = res;
    }
    
    public void setBackgroundStyle(int id) {
        m_GalleryItemBackground = id;
    }
    
    public void setCurrentImage(Drawable img) {
        m_CurrentImage = img;
    }
    
    public void setImageUrls(Vector<String> images) {
        if (images != null) {
            m_AppIcons = images;
        }
    }
    
    public int getCount() {
        if (m_CurrentImage == null) {
            return m_AppIcons.size() + m_Resources.length;
        } else {
            return m_AppIcons.size() + m_Resources.length + 1;
        }
    }
    
    public Object getItem(int id) {
        return id;
    }
    
    public long getItemId(int id) {
        return id;
    }
    
    public String getImageUri(int pos) {
        if (pos == 0) { return null; }
        if (pos <= m_Resources.length) { return null; }
        return m_AppIcons.get(pos - m_Resources.length - 1);
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView i = null;
        if (convertView == null || !(convertView instanceof ImageView)) {
            i = new ImageView(m_Context);
        } else {
            i = (ImageView) convertView;
        }
        int curr = (m_CurrentImage != null) ? 1 : 0;
        if (position == 0 && m_CurrentImage != null) {
            i.setImageDrawable(m_CurrentImage);
        } else if (position > 0 && position <= m_Resources.length) {
            i.setImageResource(m_Resources[position - 1]);
        } else if (position - m_Resources.length - 1 < m_AppIcons.size()) {
            if (m_AppIcons.get(position - m_Resources.length - curr) == null) {
                i.setImageResource(m_Default);
            } else {
                i.setImageURI(Uri.parse(m_AppIcons.get(position - m_Resources.length - curr)));
            }
        }
        /* Image should be scaled as width/height are set. */
        i.setScaleType(ImageView.ScaleType.FIT_XY);
        /* Set the Width/Height of the ImageView. */
        i.setLayoutParams(new Gallery.LayoutParams(87, 130)); // 0.9 of 96x144.
        i.setDrawingCacheEnabled(true);
        i.setDrawingCacheQuality(1);
        i.setBackgroundResource(m_GalleryItemBackground);
        return i;
    }
    
}

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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class IconArrayAdapter<E> extends ArrayAdapter<E> {
    int[] m_Icons;
    int m_TextFieldId;
    int m_ImageFieldId;
    int m_ListItemId;
    int m_SubTextFieldId = -1;
    private TextView[] m_SubTextFields = null;
    private String[] m_SubTextValues = null;
    
    public IconArrayAdapter(Context context, int textViewResourceId, List<E> objects, int[] icons) {
        super(context, textViewResourceId, objects);
        m_ListItemId = textViewResourceId;
        m_Icons = icons;
    }
    
    public void setIcons(int[] icons) {
        m_Icons = icons;
    }
    
    public void setImageField(int id) {
        m_ImageFieldId = id;
    }
    
    public void setTextField(int id) {
        m_TextFieldId = id;
    }
    
    public void setSubTextField(int id) {
        m_SubTextFieldId = id;
        m_SubTextFields = new TextView[getCount()];
        m_SubTextValues = new String[getCount()];
    }
    
    public void setSubText(int idx, String val) {
        if (m_SubTextFields[idx] != null) {
            m_SubTextFields[idx].setText(val);
        }
        m_SubTextValues[idx] = val;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = null;
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            row = inflater.inflate(m_ListItemId, parent, false);
        } else {
            row = convertView;
        }
        TextView label = (TextView) row.findViewById(m_TextFieldId);
        label.setText(getItem(position).toString());
        ImageView icon = (ImageView) row.findViewById(m_ImageFieldId);
        if (m_Icons[position] != -1) {
            icon.setImageResource(m_Icons[position]);
        } else {
            icon.setImageDrawable(null);
        }
        if (m_SubTextFieldId != -1) {
            TextView sub = (TextView) row.findViewById(m_SubTextFieldId);
            String val = m_SubTextValues[position];
            if (val != null) {
                sub.setText(val);
            } else {
                sub.setText("");
            }
            m_SubTextFields[position] = sub;
        }
        
        return (row);
    }
    
}

/**
 *     This file is part of AppLauncher.

    AppLauncher is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    AppLauncher is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with AppLauncher.  If not, see <http://www.gnu.org/licenses/>.

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
    
    public IconArrayAdapter(Context context, int textViewResourceId, List<E> objects, int[] icons) {
        super(context, textViewResourceId, objects);
        m_ListItemId = textViewResourceId;
        m_Icons = icons;
    }
    
    public void setImageField(int id) {
        m_ImageFieldId = id;
    }
    
    public void setTextField(int id) {
        m_TextFieldId = id;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
        View row = inflater.inflate(m_ListItemId, parent, false);
        TextView label = (TextView) row.findViewById(m_TextFieldId);
        
        label.setText(getItem(position).toString());
        ImageView icon = (ImageView) row.findViewById(m_ImageFieldId);
        if (m_Icons[position] != -1) {
            icon.setImageResource(m_Icons[position]);
        }
        return (row);
    }
    
}

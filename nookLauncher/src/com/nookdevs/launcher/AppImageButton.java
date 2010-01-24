/**
 *     This file is part of nookLauncher.

    nookLauncher is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    nookLauncher is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with nookLauncher.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.nookdevs.launcher;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class AppImageButton extends ImageButton {
    public AppImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public AppImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public AppImageButton(Context context) {
        super(context);
    }
    
    @Override
    public void setImageURI(Uri uri) {
        if (uri == null) {
            this.uri = null;
            return;
        } else {
            this.uri = uri.toString();
        }
        super.setImageURI(uri);
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setPackage(String pkg) {
        this.pkg = pkg;
    }
    
    public String getURI() {
        return uri;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPackage() {
        return pkg;
    }
    
    String uri;
    String name;
    String pkg;
    
}

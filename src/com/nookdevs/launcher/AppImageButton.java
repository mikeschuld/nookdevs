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

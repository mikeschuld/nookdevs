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

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.Gallery;

public class CustomGallery extends Gallery {
    
    float m_Alpha = 1.0f;
    float m_Size = 0.8f;
    
    public CustomGallery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public CustomGallery(Context context) {
        super(context);
    }
    
    public CustomGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void setSize(float size) {
        m_Size = size;
    }
    
    @Override
    public void setUnselectedAlpha(float alpha) {
        m_Alpha = alpha;
        super.setUnselectedAlpha(alpha);
    }
    
    @Override
    public boolean getChildStaticTransformation(View child, Transformation t) {
        View view = getSelectedView();
        if (view != null && view.equals(child)) {
            t.clear();
            t.setAlpha(m_Alpha);
            t.setTransformationType(Transformation.TYPE_BOTH);
            Matrix mat = t.getMatrix();
            mat.postTranslate(39, 0);
        } else {
            t.clear();
            t.setAlpha(m_Alpha);
            t.setTransformationType(Transformation.TYPE_BOTH);
            Matrix mat = t.getMatrix();
            Matrix mat1 = new Matrix();
            int centerx, centery;
            centery = child.getTop() + child.getMeasuredHeight() / 2;
            centerx = child.getLeft() + child.getMeasuredWidth() / 2;
            mat.postTranslate(-centerx, -centery);
            mat1.postScale(m_Size, m_Size);
            mat.postConcat(mat1);
            mat1 = new Matrix();
            mat1.postTranslate(centerx, centery);
            mat.postConcat(mat1);
        }
        return true;
        // return false;
    }
    
}

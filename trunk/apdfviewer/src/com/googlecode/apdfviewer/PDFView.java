/*
 * Copyright (C) 2009 Li Wenhao <liwenhao.g@gmail.com>
 *
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
package com.googlecode.apdfviewer;

import java.io.FileNotFoundException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.ConditionVariable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.googlecode.apdfviewer.PDF.Size;

/**
 * View class for render PDF document.
 * 
 * @author Li Wenhao
 */
public class PDFView extends View {
    final static String TAG = "PDFView";
    
    /**
     * Interface for listener the status of the PDF view.
     */
    public interface StatusListener {
        /**
         * Called when PDFView start to load a document.
         */
        public void onLoadingStart(PDFView v);
        
        /**
         * Called when load is finished.
         */
        public void onLoadingEnd(PDFView v);
        
        /**
         * Called when PDFView start to render a page.
         */
        public void onRenderingStart(PDFView v);
        
        /**
         * Called when rendering is finished.
         */
        public void onRenderingEnd(PDFView v);
        
        /**
         * page changed.
         */
        public void onPageChanged(PDFView v, int page);
        
        /**
         * error.
         */
        public void onError(PDFView v, String msg);
    }
    
    StatusListener m_listener;
    
    /**
     * offset of current slice.
     */
    Point m_offset = new Point();
    
    /**
     * The PDFDocument object
     */
    private PDF m_doc = null;
    
    /**
     * file descriptor of the PDF document.
     */
    private AssetFileDescriptor m_descriptor = null;
    
    /**
     * current page number, default is 1.
     */
    private int m_current_page = 0;
    
    private int m_page_count = 0;
    
    private int m_startX = 0;
    
    private int m_startY = 0;
    
    private int m_maxX = 0;
    
    private int m_maxY = 0;
    
    private Size m_size = new Size();
    
    /**
     * zoom factor.
     */
    private float m_zoom_factor = 1.0F;
    
    private float m_realzoom = 1.0F;
    
    /**
     * rotate degree
     */
    private int m_Rotate = 0;
    
    /**
     * system DPI.
     */
    private PointF m_sys_dpi = new PointF();
    
    /**
     * bitmap as cache.
     */
    private Bitmap m_cache_bitmap = null;
    
    /**
     * bitmap as cache.
     */
    private Bitmap m_cache_bitmap_prev = null;
    
    /**
     * bitmap as cache.
     */
    private Bitmap m_cache_bitmap_next = null;
    /**
     * canvas to draw the cache.
     */
    private Canvas m_cache_canvas = new Canvas();
    
    /**
     * canvas to draw the cache.
     */
    private Canvas m_cache_canvas_next = new Canvas();
    
    /**
     * bitmap configure
     */
    Bitmap.Config m_bitmap_config = Bitmap.Config.ARGB_8888;
    
    ConditionVariable m_updatecache = new ConditionVariable();
    
    public static final int HEIGHT = 745;
    public static final int WIDTH = 595;
    
    /**
     * @see android.view.View#View(android.content.Context)
     */
    public PDFView(Context context) {
        super(context);
        initView();
    }
    
    /**
     * @see android.view.View#View(android.content.Context)
     */
    public PDFView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        initView();
    }
    
    /**
     * @see android.view.View#View(android.content.Context)
     */
    public PDFView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        initView();
    }
    
    /**
     * @return the status listener
     */
    public StatusListener getStatusListener() {
        return m_listener;
    }
    
    /**
     * @param mListener
     *            the m_listener to set
     */
    public void setStatusListener(StatusListener l) {
        m_listener = l;
    }
    
    private void initConfig() {
        // get system DPI.
        WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) { return; }
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        m_sys_dpi.set(metrics.xdpi, metrics.ydpi);
        
        // bitmap configure
        switch (manager.getDefaultDisplay().getPixelFormat()) {
            case PixelFormat.A_8:
                m_bitmap_config = Bitmap.Config.ALPHA_8;
                break;
            case PixelFormat.RGB_565:
                m_bitmap_config = Bitmap.Config.RGB_565;
                break;
            case PixelFormat.RGBA_4444:
                m_bitmap_config = Bitmap.Config.ARGB_4444;
                break;
            case PixelFormat.RGBA_8888:
                m_bitmap_config = Bitmap.Config.ARGB_8888;
                break;
        }
    }
    
    private void initView() {
        setClickable(true);
        // initialize configure
        initConfig();
        // touch scroll handler
        OnTouchListener l = new View.OnTouchListener() {
            private GestureDetector g =
                new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                        
                        if (m_doc == null) { return false; }
                        
                        m_offset.offset((int) distanceX, (int) distanceY);
                        
                        postInvalidate();
                        
                        return true;
                    }
                });
            
            public boolean onTouch(View v, MotionEvent event) {
                // if (g != null) {
                // return g.onTouchEvent(event);
                // }
                
                return true;
            }
        };
        setOnTouchListener(l);
    }
    
    // private void ensureOffset() {
    // float zoom = getRealZoomFactor();
    // int w;
    // int h;
    // w = (int)(m_size.width*m_sys_dpi.x*zoom/72.0F);
    // h = (int)(m_size.height*m_sys_dpi.y*zoom/72.0F);
    // if (m_offset.x > w - getWidth())
    // m_offset.x = w - getWidth();
    //
    // if (m_offset.x < 0)
    // m_offset.x = 0;
    //
    // if (m_offset.y > h - getHeight())
    // m_offset.y = h - getHeight();
    //
    // if (m_offset.y < 0)
    // m_offset.y = 0;
    // }
    
    /**
     * Open PDF contents from the URI.
     * 
     * @param uri
     */
    public void openUri(Uri uri) {
        // reset
        if (m_doc != null) {
            // TODO: clean up?
            m_doc = null;
        }
        m_current_page = 0;
        // open uri
        try {
            m_descriptor = getContext().getContentResolver().openAssetFileDescriptor(uri, "r");
            if (m_descriptor == null) {
                Log.e(TAG, "File desciptor is null.");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Open file failed.");
            return;
        }
        
        // open document
        m_doc = new PDF(m_descriptor.getFileDescriptor());
        if (m_doc == null) {
            // TODO: report error.
            return;
        }
        m_page_count = m_doc.getPageCount();
        pageChanged();
    }
    
    private void pageChanged() {
        pageChanged(0);
    }
    
    private void pageChanged(int status) {
        m_doc.getPageSize(m_current_page, m_size);
        calculateRealZoomFactor();
        m_maxX = (int) (m_realzoom * m_size.width);
        m_maxY = (int) (m_realzoom * m_size.height);
        m_listener.onPageChanged(this, m_current_page + 1);
        if (status >= 2) {
            m_startX = m_Rotate == 0 ? WIDTH : HEIGHT;
            m_startY = m_Rotate == 0 ? HEIGHT : WIDTH;
            int t = m_maxX % m_startX;
            if (t == 0) {
                m_startX = m_maxX - m_startX;
            } else {
                m_startX = m_maxX - t;
            }
            t = m_maxY % m_startY;
            if (t == 0) {
                m_startY = m_maxY - m_startY;
            } else {
                m_startY = m_maxY - t;
            }
            status -= 2;
        } else {
            m_startX = 0;
            m_startY = 0;
        }
        
        if (status != 1) {
            dirty();
        }
    }
    
    /**
     * Show next page if exist.
     */
    public void nextPage() {
        int cached = 0;
        if (m_doc != null) {
            if (m_cache_bitmap_prev != null) {
                m_cache_bitmap_prev.recycle();
            }
            m_cache_bitmap_prev = m_cache_bitmap;
            if (m_cache_bitmap_next != null) {
                m_updatecache.block();
                m_cache_bitmap = m_cache_bitmap_next;
                m_cache_bitmap_next = null;
                cached = 1;
                dirty();
            }
            int orgStartx = m_startX;
            int orgStarty = m_startY;
            m_startX += (m_Rotate == 0) ? WIDTH : HEIGHT;
            if (m_startX >= m_maxX) {
                m_startX = 0;
                m_startY += (m_Rotate == 0) ? HEIGHT : WIDTH;
                if (m_startY >= m_maxY) {
                    if (m_current_page != m_page_count - 1) {
                        m_current_page++;
                        pageChanged(cached);
                    } else {
                        m_startY = orgStartx;
                        m_startX = orgStarty;
                    }
                    return;
                }
            }
            if (cached != 1) {
                if (m_startX != 0) {
                    m_startX -= 10;
                }
                if (m_startY != 0) {
                    m_startY -= 10;
                }
                dirty();
                if (m_startX != 0) {
                    m_startX += 10;
                }
                if (m_startY != 0) {
                    m_startY += 10;
                }
            }
        }
    }
    
    public void nextPageCache() {
        if (m_doc != null) {
            m_updatecache.close();
            m_cache_bitmap_next = null;
            int x = m_startX;
            int y = m_startY;
            int page = m_current_page;
            x += (m_Rotate == 0) ? WIDTH : HEIGHT;
            if (x >= m_maxX) {
                x = 0;
                y += (m_Rotate == 0) ? HEIGHT : WIDTH;
                if (y >= m_maxY) {
                    if (page != m_page_count - 1) {
                        page++;
                    }
                    updateCache(0, 0, page);
                    return;
                }
            }
            x -= 10;
            y -= 10;
            updateCache(x, y, page);
            
        }
    }
    
    /**
     * Show previews page if exist.
     */
    public void prevPage() {
        int cache = 0;
        if (m_doc != null) {
            if (m_cache_bitmap_next != null) {
                m_cache_bitmap_next.recycle();
            }
            m_cache_bitmap_next = m_cache_bitmap;
            if (m_cache_bitmap_prev != null) {
                m_cache_bitmap = m_cache_bitmap_prev;
                m_cache_bitmap_prev = null;
                dirty();
                cache = 1;
            } else {
                m_cache_bitmap = null;
            }
            m_startX -= m_Rotate == 0 ? WIDTH : HEIGHT;
            System.out.println("new StartX = " + m_startX + " maxX =" + m_maxX);
            if (m_startX < 0) {
                m_startX = m_maxX;
                m_startX -= m_Rotate == 0 ? WIDTH : HEIGHT;
                m_startY -= m_Rotate == 0 ? HEIGHT : WIDTH;
                System.out.println("new StartY = " + m_startY + " maxY =" + m_maxY);
                if (m_startY < 0) {
                    if (m_current_page != 0) {
                        m_current_page--;
                        pageChanged(2 + cache);
                    } else {
                        m_startY = 0;
                        m_startX = 0;
                        return;
                    }
                }
            }
            if (cache != 1) {
                if (m_startX != m_maxX) {
                    m_startX += 10;
                }
                if (m_startY != m_maxY) {
                    m_startY += 10;
                }
                dirty();
                if (m_startX != m_maxX) {
                    m_startX -= 10;
                }
                if (m_startY != m_maxY) {
                    m_startY -= 10;
                }
            }
        }
    }
    
    /**
     * Set zoom factor.
     * 
     * @param z
     *            the zoom factor, < 0 means fit width.
     */
    public void setZoomFactor(float z) {
        if (Float.compare(m_zoom_factor, z) != 0) {
            m_zoom_factor = z;
            // reset offset?
            if (m_cache_bitmap_prev != null) {
                m_cache_bitmap_prev.recycle();
            }
            if (m_cache_bitmap_next != null) {
                m_cache_bitmap_next.recycle();
            }
            if (m_cache_bitmap != null) {
                m_cache_bitmap.recycle();
            }
            m_cache_bitmap = null;
            m_cache_bitmap_prev = null;
            m_cache_bitmap_next = null;
            pageChanged();
        }
    }
    
    /**
     * Get real zoom factor.
     */
    private float getRealZoomFactor() {
        return m_realzoom;
    }
    
    private float calculateRealZoomFactor(int page) {
        float zoom;
        if (m_zoom_factor <= +0.0F) {
            if (m_doc == null) { return 1.0F; }
            Size size = new Size();
            m_doc.getPageSize(page, size);
            if (m_Rotate != 0) {
                zoom = (float) ((HEIGHT) * 1.0 / size.width);
            } else {
                zoom = Math.min((float) ((HEIGHT) * 1.0 / size.height), (float) ((WIDTH) * 1.0 / size.width));
            }
        } else {
            zoom = m_zoom_factor;
        }
        return zoom;
    }
    
    private float calculateRealZoomFactor() {
        if (m_zoom_factor <= +0.0F) {
            if (m_doc == null) { return 1.0F; }
            if (m_Rotate != 0) {
                m_realzoom = (float) ((HEIGHT) * 1.0 / m_size.width);
            } else {
                m_realzoom = Math.min((float) ((HEIGHT) * 1.0 / m_size.height), (float) ((WIDTH) * 1.0 / m_size.width));
            }
        } else {
            m_realzoom = m_zoom_factor;
        }
        return m_realzoom;
    }
    
    /**
     * Get current zoom factor.
     */
    public float getZoomFactor() {
        return m_zoom_factor;
    }
    
    /**
     * Goto the given page.
     * 
     * @param page
     *            the page number.
     */
    public void gotoPage(int page) {
        if (page != m_current_page && page > 0 && m_doc != null && page < m_page_count) {
            m_current_page = page;
            if (m_cache_bitmap_prev != null) {
                m_cache_bitmap_prev.recycle();
            }
            if (m_cache_bitmap_next != null) {
                m_cache_bitmap_next.recycle();
            }
            if (m_cache_bitmap != null) {
                m_cache_bitmap.recycle();
            }
            m_cache_bitmap_next = null;
            m_cache_bitmap_prev = null;
            m_cache_bitmap = null;
            pageChanged();
        }
    }
    
    private void dirty() {
        invalidate();
    }
    
    private boolean isCached() {
        return (m_cache_bitmap != null);
    }
    
    private boolean updateCache(int x, int y, int page) {
        float zoom = calculateRealZoomFactor(page);
        int w;
        int h;
        if (m_Rotate == 0) {
            w = WIDTH + 10;
            h = HEIGHT + 10;
        } else {
            h = WIDTH + 10;
            w = HEIGHT + 10;
        }
        if (m_cache_bitmap_next == null || m_cache_bitmap_next.getWidth() != w || m_cache_bitmap_next.getHeight() != h) {
            
            if (m_cache_bitmap_next != null) {
                m_cache_bitmap_next.recycle();
            }
            
            try {
                m_cache_bitmap_next = Bitmap.createBitmap(w, h, m_bitmap_config);
            } catch (Throwable a) {
                m_cache_bitmap_next = null;
                return false;
            }
            m_cache_canvas_next.setBitmap(m_cache_bitmap_next);
        }
        Size size = new Size();
        size.height = h;
        size.width = w;
        m_doc.renderPage(page, (int) (zoom * 1000), x, y, 0, size, m_cache_canvas_next);
        // Paint p = new Paint();
        // ColorFilter filter = new
        // PorterDuffColorFilter(Color.WHITE,PorterDuff.Mode.MULTIPLY);
        // p.setColorFilter(filter);
        // m_cache_canvas_next.drawBitmap(buf, 0, w, 0, 0, w, h, true, p);
        m_updatecache.open();
        return true;
    }
    
    private boolean ensureCache() {
        float zoom = getRealZoomFactor();
        int w;
        int h;
        if (m_Rotate == 0) {
            w = WIDTH + 10;
            h = HEIGHT + 10;
        } else {
            h = WIDTH + 10;
            w = HEIGHT + 10;
        }
        if (m_cache_bitmap == null || m_cache_bitmap.getWidth() != w || m_cache_bitmap.getHeight() != h) {
            
            if (m_cache_bitmap != null) {
                m_cache_bitmap.recycle();
            }
            
            try {
                m_cache_bitmap = Bitmap.createBitmap(w, h, m_bitmap_config);
            } catch (Throwable a) {
                m_cache_bitmap = null;
                return false;
            }
            m_cache_canvas.setBitmap(m_cache_bitmap);
        }
        Size size = new Size();
        size.height = h;
        size.width = w;
        m_doc.renderPage(m_current_page, (int) (zoom * 1000), m_startX, m_startY, 0, size, m_cache_canvas);
        // Paint p = new Paint();
        // ColorFilter filter = new
        // PorterDuffColorFilter(Color.WHITE,PorterDuff.Mode.MULTIPLY);
        // p.setColorFilter(filter);
        // m_cache_canvas.drawBitmap(buf, 0, w, 0, 0, w, h, true, p);
        postInvalidate();
        return true;
    }
    
    /**
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // do nothing if no document loaded.
        if (m_doc == null) { return; }
        // ensureOffset();
        if (m_Rotate > 0) {
            canvas.rotate(m_Rotate, (getHeight()) / 2, getWidth() / 2);
            if (m_Rotate == 270) {
                canvas.translate(-90, -90);
            } else {
                canvas.translate(80, 80);
            }
        }
        // draw
        if (isCached()) {
            Paint p = new Paint();
            ColorFilter filter = new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
            p.setColorFilter(filter);
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(m_cache_bitmap, -m_offset.x, -m_offset.y, p);
        } else {
            if (ensureCache()) {
            } else {
                // use drawPageSlice.
                // canvas.setDrawFilter(new
                // PaintFlagsDrawFilter(0,Paint.FAKE_BOLD_TEXT_FLAG));
                // m_doc.drawPageSlice(canvas, m_current_page,
                // m_offset.x, m_offset.y, getWidth(), getHeight());
            }
        }
        if (m_cache_bitmap_next == null) {
            Runnable run = new Runnable() {
                public void run() {
                    nextPageCache();
                }
            };
            (new Thread(run)).start();
        }
    }
    
    public int getPagesCount() {
        return m_page_count;
    }
    
    public int getCurrentPage() {
        return m_current_page;
    }
    
    public void setRotate(int degree) {
        m_Rotate = degree;
        scrollTo(0, 0);
        m_offset.set(0, 0);
        if (m_cache_bitmap_prev != null) {
            m_cache_bitmap_prev.recycle();
        }
        if (m_cache_bitmap_next != null) {
            m_cache_bitmap_next.recycle();
        }
        if (m_cache_bitmap != null) {
            m_cache_bitmap.recycle();
        }
        m_cache_bitmap = null;
        m_cache_bitmap_prev = null;
        m_cache_bitmap_next = null;
        pageChanged();
    }
    
    public int getMediaHeight() {
        if (m_doc != null) {
            float zoom = getRealZoomFactor();
            int h = (int) (m_size.height * m_sys_dpi.y * zoom / 72.0F);
            return h;
        }
        return 0;
    }
    
    public int getMediaWidth() {
        if (m_doc != null) {
            float zoom = getRealZoomFactor();
            int w = (int) (m_size.width * m_sys_dpi.y * zoom / 72.0F);
            return w;
        }
        return 0;
    }
    
}

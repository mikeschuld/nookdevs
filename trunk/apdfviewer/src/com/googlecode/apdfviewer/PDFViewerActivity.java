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
/**
 * Modified the apdfviewer to customize it for nook. - Hari.
 */
package com.googlecode.apdfviewer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.googlecode.apdfviewer.PDFView.StatusListener;
import com.nookdevs.common.nookBaseActivity;

/**
 * @author Li Wenhao
 */
public class PDFViewerActivity extends nookBaseActivity {
    // private final static String TAG = "PDFViewerActivity";
    
    private static final int ABOUT = 1;
    
    PDFView m_pdf_view;
    ViewAnimator m_animator;
    private SeekBar m_seek_view;
    TextView m_text;
    // TextView m_name;
    boolean m_landscapeleft = false;
    boolean m_landscaperight = false;
    private static final int SCROLL_PX_Y = 850;
    private static final int SCROLL_PX_X = 650;
    Handler m_Handler = new Handler();
    CurrentPageInfo pageInfo;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        // Check the intent for the content to view
        Intent intent = getIntent();
        if (intent.getData() == null) { return; }
        
        Uri uri = intent.getData();
        m_pdf_view = (PDFView) findViewById(R.id.view);
        String file = uri.getPath();
        new MediaScannerNotifier(file);
        initZoomSpinner();
        initButtons();
        initListener();
        m_pdf_view.openUri(uri);
        int idx = file.lastIndexOf('/');
        file = file.substring(idx + 1) + "userData";
        try {
            ObjectInputStream inp = new ObjectInputStream(openFileInput(file));
            pageInfo = (CurrentPageInfo) inp.readObject();
            inp.close();
            m_pdf_view.gotoPage(pageInfo.pageno);
            if (pageInfo.landscapeleft) {
                m_landscapeleft = true;
                m_pdf_view.setRotate(90);
            } else if (pageInfo.landscaperight) {
                m_landscaperight = true;
                m_pdf_view.setRotate(270);
            }
            m_pdf_view.scrollTo(pageInfo.scrollX, pageInfo.scrollY);
        } catch (Exception ex) {
            Log.e(LOGTAG, ex.getMessage(), ex);
            pageInfo = new CurrentPageInfo();
            pageInfo.file = file;
        }
        // m_name = (TextView)findViewById(R.id.name);
        m_animator = (ViewAnimator) findViewById(R.id.viewanim);
        m_seek_view = (SeekBar) findViewById(R.id.page_picker_seeker);
        m_seek_view.setMax(m_pdf_view.getPagesCount());
        m_text = (TextView) findViewById(R.id.page_picker_message);
        m_seek_view.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    progress = 1;
                }
                m_text.setText(progress + "");
            }
            
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        
        ImageButton close = (ImageButton) findViewById(R.id.page_picker_close);
        ImageButton plus = (ImageButton) findViewById(R.id.page_picker_plus);
        plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_seek_view.incrementProgressBy(1);
            }
        });
        ImageButton minus = (ImageButton) findViewById(R.id.page_picker_minus);
        minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (m_seek_view.getProgress() != 0) {
                    m_seek_view.incrementProgressBy(-1);
                }
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (m_seek_view.getProgress() == 0) {
                    m_pdf_view.gotoPage(0);
                } else {
                    m_pdf_view.gotoPage(m_seek_view.getProgress() - 1);
                }
                m_animator.showNext();
            }
        });
    }
    
    private void initListener() {
        m_pdf_view.setStatusListener(new StatusListener() {
            
            public void onError(PDFView v, String msg) {
                // TODO Auto-generated method stub
                
            }
            
            public void onLoadingEnd(PDFView v) {
                // TODO Auto-generated method stub
                
            }
            
            public void onLoadingStart(PDFView v) {
                // TODO Auto-generated method stub
                
            }
            
            public void onPageChanged(PDFView v, int page) {
                // TODO Auto-generated method stub
                ImageButton prev = (ImageButton) findViewById(R.id.prev_page);
                ImageButton next = (ImageButton) findViewById(R.id.next_page);
                prev.setEnabled(true);
                next.setEnabled(true);
                TextView tv = (TextView) findViewById(R.id.page_number_view);
                tv.setText(page + "/" + v.getPagesCount());
                
            }
            
            public void onRenderingEnd(PDFView v) {
                // TODO Auto-generated method stub
                
            }
            
            public void onRenderingStart(PDFView v) {
                // TODO Auto-generated method stub
                
            }
        });
    }
    
    private void pageUp() {
        m_pdf_view.prevPage();
    }
    
    private void pageDown() {
        m_pdf_view.nextPage();
    }
    
    private void initButtons() {
        ImageButton btn = (ImageButton) findViewById(R.id.exit);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_pdf_view.close();
                System.gc();
                finish();
            }
        });
        btn = (ImageButton) findViewById(R.id.prev_page);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (m_landscapeleft) {
                    pageDown();
                } else {
                    pageUp();
                }
                saveData();
            }
        });
        btn.setOnLongClickListener(new View.OnLongClickListener() {
            
            public boolean onLongClick(View v) {
                m_seek_view.setProgress(m_pdf_view.getCurrentPage() + 1);
                m_animator.showNext();
                return true;
            }
        });
        
        btn = (ImageButton) findViewById(R.id.next_page);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (m_landscapeleft) {
                    pageUp();
                } else {
                    pageDown();
                }
                saveData();
            }
        });
        btn.setOnLongClickListener(new View.OnLongClickListener() {
            
            public boolean onLongClick(View v) {
                m_seek_view.setProgress(m_pdf_view.getCurrentPage() + 1);
                m_animator.showNext();
                return true;
            }
        });
        
        btn = (ImageButton) findViewById(R.id.switch_page);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (m_landscaperight || m_landscapeleft) {
                    m_pdf_view.setRotate(0);
                    m_landscaperight = false;
                    m_landscapeleft = false;
                } else {
                    m_pdf_view.setRotate(90);
                    m_landscapeleft = true;
                }
                saveData();
            }
        });
        btn.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (m_landscaperight || m_landscapeleft) {
                    m_pdf_view.setRotate(0);
                    m_landscaperight = false;
                    m_landscapeleft = false;
                } else {
                    m_pdf_view.setRotate(270);
                    m_landscaperight = true;
                }
                saveData();
                return true;
            }
        });
    }
    
    private void initZoomSpinner() {
        Spinner s = (Spinner) findViewById(R.id.zoom);
        
        s.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> view) {
            }
            
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                float factor = 1.0F;
                switch (pos) {
                    case 1:
                        factor = 0.25F;
                        break;
                    case 2:
                        factor = 0.50F;
                        break;
                    case 3:
                        factor = 0.75F;
                        break;
                    case 4:
                        factor = 1.00F;
                        break;
                    case 5:
                        factor = 1.25F;
                        break;
                    case 6:
                        factor = 1.5F;
                        break;
                    case 7:
                        factor = 2.0F;
                        break;
                    case 8:
                        factor = 2.5F;
                        break;
                    case 9:
                        factor = 3.0F;
                        break;
                    default:
                        factor = -1.0F;
                        break;
                }
                m_pdf_view.setZoomFactor(factor);
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    /**
     * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }
    
    private void saveData() {
        Runnable run = new Runnable() {
            public void run() {
                try {
                    pageInfo.pageno = m_pdf_view.getCurrentPage();
                    pageInfo.scrollX = m_pdf_view.getScrollX();
                    pageInfo.scrollY = m_pdf_view.getScrollY();
                    pageInfo.landscapeleft = m_landscapeleft;
                    pageInfo.landscaperight = m_landscaperight;
                    ObjectOutputStream out =
                        new ObjectOutputStream(PDFViewerActivity.this.openFileOutput(pageInfo.file,
                            Context.MODE_PRIVATE));
                    out.writeObject(pageInfo);
                    out.close();
                } catch (Exception ex) {
                    Log.e(LOGTAG, ex.getMessage(), ex);
                }
            }
        };
        (new Thread(run)).start();
        
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case NOOK_PAGE_UP_KEY_LEFT:
            case NOOK_PAGE_UP_KEY_RIGHT:
                pageUp();
                handled = true;
                break;
            
            case NOOK_PAGE_DOWN_KEY_LEFT:
            case NOOK_PAGE_DOWN_KEY_RIGHT:
                pageDown();
                handled = true;
                break;
            case NOOK_PAGE_UP_SWIPE:
                if (m_landscapeleft) {
                    pageDown();
                } else {
                    pageUp();
                }
                handled = true;
                break;
            case NOOK_PAGE_DOWN_SWIPE:
                if (m_landscapeleft) {
                    pageUp();
                } else {
                    pageDown();
                }
                handled = true;
                break;
            default:
                break;
        }
        return handled;
    }
    
    class MediaScannerNotifier implements MediaScannerConnectionClient {
        private MediaScannerConnection mConnection;
        private String mPath;
        
        public synchronized void scanFile(String path) {
            if (path == null) { return; }
            String mime = "ebook/";
            String ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
            mime += ext;
            mConnection.scanFile(path, mime);
        }
        
        public MediaScannerNotifier(String path) {
            mConnection = new MediaScannerConnection(PDFViewerActivity.this, this);
            mConnection.connect();
            mPath = path;
        }
        
        public void onMediaScannerConnected() {
            scanFile(mPath);
        }
        
        public void onScanCompleted(String path, Uri arg1) {
            System.out.println("On Scan completed" + path + "  " + arg1);
            String[] columns = {
                "title", "authors"
            };
            final Cursor dbCursor = getContentResolver().query(arg1, columns, null, null, null);
            dbCursor.moveToFirst();
            m_Handler.post(new Runnable() {
                public void run() {
                    // m_name.setText(dbCursor.getString(0));
                    updateTitle(dbCursor.getString(0));
                    dbCursor.close();
                }
            });
            mConnection.disconnect();
        }
    }
}

class CurrentPageInfo implements Serializable {
    int pageno = 1;
    boolean landscaperight = false;
    boolean landscapeleft = false;
    int scrollX = 0;
    int scrollY = 0;
    String file;
    
}

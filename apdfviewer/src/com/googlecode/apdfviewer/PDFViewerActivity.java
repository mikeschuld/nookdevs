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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import com.googlecode.apdfviewer.PDFView.StatusListener;
import com.googlecode.apdfviewer.R;
import com.nookdevs.common.nookBaseActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;


/**
 * @author Li Wenhao
 */
public class PDFViewerActivity extends nookBaseActivity implements OnKeyListener {
	//private final static String TAG = "PDFViewerActivity";
	
  
    private static final int ABOUT = 1;
    
	PDFView m_pdf_view;
	ViewAnimator m_animator;
	private SeekBar m_seek_view;
	TextView m_text ;
	TextView m_name;
	boolean m_landscape=false;
	private static final int SCROLL_PX_Y = 850;
	private static final int SCROLL_PX_X = 650;
	Handler m_Handler = new Handler();
	CurrentPageInfo pageInfo;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
		
		// Check the intent for the content to view
		Intent intent = getIntent();
		if (intent.getData() == null)
			return;

		Uri uri = intent.getData();		
		m_pdf_view = (PDFView)findViewById(R.id.view);
		String file=uri.toString().substring(7);
		MediaScannerNotifier notifier = new MediaScannerNotifier(file);
		initZoomSpinner();
		initButtons();
		initListener();
		m_pdf_view.openUri(uri);
		m_pdf_view.setOnKeyListener(this);
		int idx = file.lastIndexOf('/');
		file = file.substring(idx+1) + "userData";
		try {
		    ObjectInputStream inp = new ObjectInputStream(openFileInput(file));
		    pageInfo = (CurrentPageInfo) inp.readObject();
		    inp.close();
		    m_pdf_view.gotoPage(pageInfo.pageno);
		    if( pageInfo.landscape) { 
		        m_landscape=true;
		        m_pdf_view.setRotate(90);
		    }
		    m_pdf_view.scrollTo(pageInfo.scrollX, pageInfo.scrollY);
		} catch(Exception ex) {
		    Log.e(LOGTAG, ex.getMessage(),ex);
		    pageInfo = new CurrentPageInfo();
		    pageInfo.file = file;
		}
		m_name = (TextView)findViewById(R.id.name);
		m_animator = (ViewAnimator)findViewById(R.id.viewanim);
		m_seek_view = (SeekBar)findViewById(R.id.page_picker_seeker);
		m_seek_view.setMax(m_pdf_view.getPagesCount());
		m_text = (TextView) findViewById(R.id.page_picker_message);
		m_seek_view.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                m_text.setText(progress+"");
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
		
		ImageButton close = (ImageButton)findViewById(R.id.page_picker_close);
		ImageButton plus = (ImageButton)findViewById(R.id.page_picker_plus);
		plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_seek_view.incrementProgressBy(1);
            }
		});
		ImageButton minus = (ImageButton)findViewById(R.id.page_picker_minus);
		minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_seek_view.incrementProgressBy(-1);
            }
        });
		close.setOnClickListener( new View.OnClickListener() {
		    public void onClick(View v) {
		        m_pdf_view.gotoPage(m_seek_view.getProgress());
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
				ImageButton prev = (ImageButton)findViewById(R.id.prev_page);
				ImageButton next = (ImageButton)findViewById(R.id.next_page);
				prev.setEnabled(true);
				next.setEnabled(true);
				TextView tv = (TextView)findViewById(R.id.page_number_view);
				tv.setText(page+ "/" + v.getPagesCount());
				
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
        int cury = m_pdf_view.getScrollY();
        int curx = m_pdf_view.getScrollX();
        if (cury == 0) { 
            m_pdf_view.prevPage();
            m_pdf_view.scrollTo(curx, m_pdf_view.getMediaHeight() - SCROLL_PX_Y);
            return;
        }
        int newy = cury - SCROLL_PX_Y;
        if (newy < 0)
            newy=0;
        m_pdf_view.scrollTo(0, newy);
   }
    private void pageDown() {
        int cury = m_pdf_view.getScrollY();
        int curx = m_pdf_view.getScrollX();
        int hmax = (int)m_pdf_view.getMediaHeight() - SCROLL_PX_Y;
        if( cury >= hmax) { 
            m_pdf_view.nextPage();
            m_pdf_view.scrollTo(curx,0);
            return;
        }
        int newy = cury + SCROLL_PX_Y;
        if (newy > hmax) {
            newy = hmax;
        }
        if (cury != newy) {
            m_pdf_view.scrollTo(0, newy);
        } else {
            m_pdf_view.nextPage();
            m_pdf_view.scrollTo(curx, 0);
        }

    }
	private void pageUpL() {
	    int curx = m_pdf_view.getScrollX();
	    int cury = m_pdf_view.getScrollY();
	    if (curx == 0) { 
           m_pdf_view.prevPage();
           m_pdf_view.scrollTo(-(m_pdf_view.getMediaHeight()-SCROLL_PX_X), cury);
           return;
        }
        int newx = curx + SCROLL_PX_X;
        if( newx >0) newx=0;
        m_pdf_view.scrollTo(newx,0);
   }
	private void pageDownL() {
        int curx = m_pdf_view.getScrollX();
        int cury = m_pdf_view.getScrollY();
        int hmax = (int)m_pdf_view.getMediaHeight() - SCROLL_PX_X;
        
        if( curx <= -hmax) { 
            m_pdf_view.nextPage();
            m_pdf_view.scrollTo(0,cury);
            return;
        }
        int newx = curx - SCROLL_PX_X;
        if (newx < -hmax) {
            newx = -hmax;
        }
        if (curx != newx) {
            m_pdf_view.scrollTo(newx,0);
        } else {
            m_pdf_view.nextPage();
            m_pdf_view.scrollTo(0,cury);
        }
    }
    
	private void initButtons() {
	    ImageButton btn = (ImageButton)findViewById(R.id.exit);
	    btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                finish();
            }
        });
        btn = (ImageButton)findViewById(R.id.prev_page);
		btn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
			    if( m_landscape) pageDownL();
			    else 
			        pageUp();
			    saveData();
			}
		});
		btn.setOnLongClickListener( new View.OnLongClickListener() {
            
            public boolean onLongClick(View v) {
                m_seek_view.setProgress(m_pdf_view.getCurrentPage());
                m_animator.showNext();
                return true;
            }
        });

		btn = (ImageButton)findViewById(R.id.next_page);
		btn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
			    if( m_landscape) 
			        pageUpL();
			    else
			        pageDown();
	            saveData();
			}
		});
		btn.setOnLongClickListener( new View.OnLongClickListener() {
            
            public boolean onLongClick(View v) {
                m_seek_view.setProgress(m_pdf_view.getCurrentPage());
                m_animator.showNext();
                return true;
            }
        });
		
		btn = (ImageButton)findViewById(R.id.switch_page);
		btn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
			    if( m_landscape) {
			        m_pdf_view.setRotate(0);
			    } else {
			        m_pdf_view.setRotate(90);
			    }
			    m_landscape=!m_landscape;
			    saveData();
			}
		});
	}
	
	private void initZoomSpinner() {
		Spinner s = (Spinner)findViewById(R.id.zoom);
		
		s.setOnItemSelectedListener(new OnItemSelectedListener(){
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
					factor = 0.60F;
					break;
				case 4:
					factor = 0.75F;
					break;
				case 5:
					factor = 0.90F;
					break;
				case 6:
				    factor = 1.00F;
				    break;
				case 7:
				    factor = 1.15F;
				case 8:
				    factor = 1.25F;
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

	/**
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = menu.add(0, 0, 0, "About");
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				showDialog(ABOUT);
				return true;
			}	
	    });
	    return true;
	}
	private void saveData() {
	    Runnable run = new Runnable() {
	        public void run() {
	        try {
	            pageInfo.pageno = m_pdf_view.getCurrentPage();
                pageInfo.scrollX = m_pdf_view.getScrollX();
                pageInfo.scrollY = m_pdf_view.getScrollY();
                pageInfo.landscape = m_landscape;
                ObjectOutputStream out = new ObjectOutputStream(
                PDFViewerActivity.this.openFileOutput(pageInfo.file, PDFViewerActivity.MODE_PRIVATE));
                out.writeObject(pageInfo);
                out.close();
            } catch(Exception ex) {
                Log.e(LOGTAG, ex.getMessage(), ex);
            }
	        }
	    };
	    (new Thread(run)).start();
	        
    }
	public boolean onKey(View view, int keyCode, KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case NOOK_PAGE_UP_KEY_LEFT:
                case NOOK_PAGE_UP_KEY_RIGHT:
                    if( m_landscape) 
                        pageUpL();
                    else 
                        pageUp();
                    handled = true;
                    break;
                
                case NOOK_PAGE_DOWN_KEY_LEFT:
                case NOOK_PAGE_DOWN_KEY_RIGHT:
                    if( m_landscape)
                        pageDownL();
                    else
                        pageDown();
                    handled = true;
                    break;
                case NOOK_PAGE_UP_SWIPE:
                     if( m_landscape)
                         pageDownL();
                     else
                         pageUp();
                     handled = true;
                     break;
                case NOOK_PAGE_DOWN_SWIPE:
                    if( m_landscape)
                        pageUpL();
                    else
                        pageDown();
                    handled = true;
                    break;
                default:
                    break;
            }
        }
        return handled;
    }
    
	class MediaScannerNotifier implements
	MediaScannerConnectionClient {
	    private MediaScannerConnection mConnection;
	    private String mPath;
	    public synchronized void scanFile( String path) {
	        if( path == null) return;
	        String mime = "ebook/";
	        String ext = path.substring(path.lastIndexOf(".")+1).toLowerCase();
	        mime += ext;
	        mConnection.scanFile(path, mime);
	    }
	    public MediaScannerNotifier(String path) {
	        mConnection = new MediaScannerConnection(PDFViewerActivity.this, this);
	        mConnection.connect();
	        mPath=path;
	    }

	    public void onMediaScannerConnected() {
	        scanFile(mPath);
	    }

	    public void onScanCompleted(String path, Uri arg1) {
	        System.out.println("On Scan completed" + path + "  " + arg1);
	        String [] columns = {"title","authors"};
	        final Cursor dbCursor = PDFViewerActivity.this.getContentResolver().query(arg1, columns, null,null, null);
	        dbCursor.moveToFirst();
	        PDFViewerActivity.this.m_Handler.post ( new Runnable() {
	            public void run() {
	                m_name.setText(dbCursor.getString(0));
	                dbCursor.close();
	            }
	        });
	        mConnection.disconnect();
	    }
	}
}
class CurrentPageInfo implements Serializable {
    int pageno=1;
    boolean landscape=false;
    int scrollX=0;
    int scrollY=0;
    String file;
    
}

package com.nookdevs.fileselector;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.nookdevs.common.IconArrayAdapter;
import com.nookdevs.common.ImageAdapter;
import com.nookdevs.common.nookBaseActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ViewAnimator;
import android.widget.AdapterView.OnItemClickListener;

public class FileSelector extends nookBaseActivity {
    private ListView lview;
    private ListView submenu;
    private ViewAnimator animator;
    private int m_SubMenuType = -1;
    private PageViewHelper pageViewHelper;
    private Handler m_Handler = new Handler();
    private ImageAdapter m_IconAdapter = null;
    public static final String [] ALL_FILES={ ".*" };
    public static final String ROOT="ROOT";
    public static final String FILE="FILE";
    public static final String FILTER="FILTER";
    public static final String TITLE="TITLE";
    public static final int SORT_BY_NAME=0;
    public static final int SORT_BY_TYPE=1;
    public static final int SORT_BY_DATE=2;
    public static final int SORT_REVERSE_ORDER=3;
    private boolean m_Reversed=false;
    
    int m_level=0;
    private int[] subicons =
    {
        -1, R.drawable.submenu_image, R.drawable.search_image, R.drawable.covers_image, R.drawable.submenu_image,
        R.drawable.submenu_image, R.drawable.submenu_image, -1, -1, -1, -1, -1, -1, -1, -1
    };

    private int[] icons = {
        R.drawable.check, -1, -1, -1
    };
    private Button backButton, upButton, downButton;
    private ImageButton goButton;
    private IconArrayAdapter m_ListAdapter;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "FileSelector";
        NAME = getString(R.string.caption);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        goButton = (ImageButton) findViewById(R.id.go);
        backButton = (Button) findViewById(R.id.back);
        upButton = (Button) findViewById(R.id.up);
        downButton = (Button) findViewById(R.id.down);
        goButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                File file = pageViewHelper.getCurrent();
                if( file == null) return;
                if( file.isDirectory()) {
                    pageViewHelper.followDirectory(file);
                    m_level++;
                } else {
                    //set return data
                    Intent result = new Intent();
                    result.putExtra(FILE, file.getAbsolutePath());
                    setResult(RESULT_OK, result);
                    finish();
                }
            }
        });
        upButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                pageViewHelper.gotoTop();
                return true;
            }
        });
        upButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                pageViewHelper.selectPrev();
            }
        });
        downButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                pageViewHelper.selectNext();
            }
        });
        downButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                pageViewHelper.gotoBottom();
                return true;
            }
        });

        backButton.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if( m_level ==0) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                else {
                    m_level--;
                    File file = pageViewHelper.getCurrentFolder();
                    file = file.getParentFile();
                    pageViewHelper.followDirectory(file);
                }
                    
            }
        });
        LinearLayout pageview = (LinearLayout) findViewById(R.id.pageview);
        Bundle b = getIntent().getExtras();
        File folder;
        String s = b == null?null:b.getString(ROOT);
        if( s == null) {
            s="/system/media/sdcard/";
        }
        String[] types = b==null?null:b.getStringArray(FILTER);
        if( types == null) {
            types= ALL_FILES;
        }
        String title = b.getString(TITLE);
            //read type from activity data
        //String[] types = {"puz", "xpf", "xml"};
        folder =new File(s);
        pageViewHelper = new PageViewHelper(this, pageview, types, folder);
        if( title != null) {
            pageViewHelper.setTitle(title);
        }
        lview = (ListView) findViewById(R.id.list);
        lview.setOnItemClickListener( new OnItemClickListener() {
            public void onItemClick(AdapterView<?> v, View parent, int position, long id) {
                if( position <=2) {
                    icons[0] = -1;
                    icons[1] = -1;
                    icons[2] = -1;
                    icons[position] = R.drawable.check;
                }
                switch (position) {
                    case SORT_BY_NAME:
                        pageViewHelper.setSortType(PageViewHelper.SortType.NAME);
                        break;
                    case SORT_BY_TYPE:
                        pageViewHelper.setSortType(PageViewHelper.SortType.TYPE);
                        break;
                    case SORT_BY_DATE:
                        pageViewHelper.setSortType(PageViewHelper.SortType.DATE);
                        break;
                    case SORT_REVERSE_ORDER:
                        m_Reversed=!m_Reversed;
                        if( m_Reversed) icons[position] = R.drawable.check;
                        else
                            icons[position] = -1;
                        pageViewHelper.setSortReversed(m_Reversed);
                        break;
                }
                m_ListAdapter.setIcons(icons);
                lview.setAdapter(m_ListAdapter);
                pageViewHelper.followDirectory(pageViewHelper.getCurrentFolder());
            }
        });
        CharSequence[] menuitems = getResources().getTextArray(R.array.mainmenu);
        List<CharSequence> menuitemsList = Arrays.asList(menuitems);
        m_ListAdapter = new IconArrayAdapter<CharSequence>(lview.getContext(), R.layout.listitem, menuitemsList, icons);
        m_ListAdapter.setImageField(R.id.ListImageView);
        m_ListAdapter.setTextField(R.id.ListTextView);
        m_ListAdapter.setSubTextField(R.id.ListSubTextView);
        lview.setAdapter(m_ListAdapter);
      
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case NOOK_PAGE_UP_KEY_LEFT:
            case NOOK_PAGE_UP_KEY_RIGHT:
            case NOOK_PAGE_UP_SWIPE:
                pageViewHelper.pageUp();
                handled = true;
                break;
            
            case NOOK_PAGE_DOWN_KEY_LEFT:
            case NOOK_PAGE_DOWN_KEY_RIGHT:
            case NOOK_PAGE_DOWN_SWIPE:
                pageViewHelper.pageDown();
                handled = true;
                break;
            default:
                break;
        }
        return handled;
    }

}
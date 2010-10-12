package com.nookdevs.crossword;


import android.os.Bundle;

import android.content.SharedPreferences.Editor;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ImageView;
//import android.util.Log;

public class SettingsActivity extends BaseActivity {
	LayoutInflater inflater ;
	ListView submenu ;
	
	int[] settingLabels = {
			R.string.hints_settings_mark_wrong_answers_button_label,
			R.string.hints_settings_freeze_right_answers_button_label,
			R.string.nav_settings_cursor_next_clue_label,
			R.string.nav_settings_cursor_wraps_label,
	} ;	
	String[] settingNames = {
			CROSSWORD_PREFERENCES_MARK_WRONG_ANSWERS,
			CROSSWORD_PREFERENCES_FREEZE_RIGHT_ANSWERS,
			CROSSWORD_PREFERENCES_CURSOR_NEXT_CLUE,
			CROSSWORD_PREFERENCES_CURSOR_WRAPS,
	} ;
	boolean[] settingDefaults = {
			CROSSWORD_PREFERENCES_MARK_WRONG_ANSWERS_DEFAULT,
			CROSSWORD_PREFERENCES_FREEZE_RIGHT_ANSWERS_DEFAULT,
			CROSSWORD_PREFERENCES_CURSOR_NEXT_CLUE_DEFAULT,
			CROSSWORD_PREFERENCES_CURSOR_WRAPS_DEFAULT,
	} ;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
		
        //  Touchscreen submenu list adapter:
		submenu = (ListView) findViewById(R.id.settings_submenu);
		submenu.setAdapter(new SettingsMenuAdapter(this));		
		submenu.setChoiceMode( ListView.CHOICE_MODE_SINGLE );
		submenu.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
            	toggleSetting( settingNames[pos] );
            	//  Special case: these two options are kinduv/sortuv mutually exclusive,
            	//  in that nextclue basically overrides cursorwrap, so if they select one,
            	//  unselect the other.  Both can be unset, though.
            	if ( settingNames[pos].equals( CROSSWORD_PREFERENCES_CURSOR_NEXT_CLUE ) ) {
                	if ( mSettings.getBoolean(CROSSWORD_PREFERENCES_CURSOR_NEXT_CLUE, CROSSWORD_PREFERENCES_CURSOR_NEXT_CLUE_DEFAULT) == true ) {
                		setSetting( CROSSWORD_PREFERENCES_CURSOR_WRAPS, false );
                	}

            	} else if ( settingNames[pos].equals( CROSSWORD_PREFERENCES_CURSOR_WRAPS ) ) {
                	if ( mSettings.getBoolean(CROSSWORD_PREFERENCES_CURSOR_WRAPS, CROSSWORD_PREFERENCES_CURSOR_WRAPS_DEFAULT) == true ) {
                		setSetting( CROSSWORD_PREFERENCES_CURSOR_NEXT_CLUE, false );
                	}
            	}
            	//  Re-draw as necessary:
        		((BaseAdapter) submenu.getAdapter()).notifyDataSetChanged();
            }
		});

        //  our adapter will make use of this:
        inflater = getLayoutInflater();
        
        // Touchscreen back button:
        Button back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	finish();
                }
        });

    }
    
    private void toggleSetting( String sName ) {
    	if ( mSettings.getBoolean(sName, false) == true ) {
    		setSetting(sName, false);
    	} else {
    		setSetting(sName, true);
    	}
    } // toggleSetting
    
    private void setSetting( String sName, boolean setting ) {
		Editor editor = mSettings.edit();
		editor.putBoolean(sName, setting);
    	editor.commit();
    } // setSetting

    
	private class SettingsMenuAdapter extends BaseAdapter {
		// Context mContext ;

		public SettingsMenuAdapter(Context c) {
			// mContext = c ;
		}

		public int getCount() {
			if ( settingLabels == null ) return 0;
			return settingLabels.length ;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout l;
			if ( convertView == null ) {
				l = (LinearLayout) inflater.inflate( R.layout.listitem, null );
			} else {
				l = (LinearLayout)convertView ;
			}
			TextView tv = (TextView) l.findViewById(R.id.ListTextView);
			tv.setText( settingLabels[position] );
			ImageView icon = (ImageView) l.findViewById(R.id.ListImageView);
			if ( mSettings.getBoolean(settingNames[position], settingDefaults[position]) == true ) {
				icon.setImageResource( R.drawable.check_mark_pressable );
			} else {
				icon.setImageDrawable(null);
			}
			return l;
		} // getView
		
	} // SettingsMenuAdapter
	
        
} // SettingsActivity

package com.nookdevs.mines;

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


public class SettingsActivity extends MinesActivity {
	public ListView sizes_menu ;
	LayoutInflater inflater ;
    int cur_rows = 0 ;
    int cur_cols = 0 ;
    int cur_num_mines = 0 ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        
        if ( mSettings.contains(MINES_PREFERENCES_ROWS) &&
        		mSettings.contains(MINES_PREFERENCES_COLS) &&
        		mSettings.contains(MINES_PREFERENCES_NUM_MINES) ) {
        	cur_rows = mSettings.getInt(MINES_PREFERENCES_ROWS, DEFAULT_ROWS);
        	cur_cols = mSettings.getInt(MINES_PREFERENCES_COLS, DEFAULT_COLS);
        	cur_num_mines = mSettings.getInt(MINES_PREFERENCES_NUM_MINES, DEFAULT_NUM_MINES);
        }

        //  our adapter will make use of this:
        inflater = getLayoutInflater();
        
        // Touchscreen back button:
        Button back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	finish();
                }
        });
        
        //  Touchscreen menu list adapter:
		sizes_menu = (ListView) findViewById(R.id.sizes_list);
		sizes_menu.setAdapter(new SizesListAdapter(this));		
		sizes_menu.setChoiceMode( ListView.CHOICE_MODE_SINGLE );
		sizes_menu.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
    			int rows = mineFieldSizes[pos][0];
    			int cols = mineFieldSizes[pos][1];
    			int num_mines = mineFieldSizes[pos][2];
                setMineFieldSize(rows,cols,num_mines);
                cur_rows = rows ; cur_cols = cols ; cur_num_mines = num_mines ;
        		((BaseAdapter) sizes_menu.getAdapter()).notifyDataSetChanged();
            }
		});

    } // onCreate
    
    
    public void setMineFieldSize(int rows, int cols, int num_mines) {
		Editor editor = mSettings.edit();
		editor.putInt( MinesActivity.MINES_PREFERENCES_ROWS, rows );
		editor.putInt( MinesActivity.MINES_PREFERENCES_COLS, cols );
		editor.putInt( MinesActivity.MINES_PREFERENCES_NUM_MINES, num_mines );
		editor.commit();
    } // setMineFieldSize
    
	
	private class SizesListAdapter extends BaseAdapter {
		// Context mContext ;

		public SizesListAdapter(Context c) {
			// mContext = c ;
		}

		public int getCount() {
			return mineFieldSizes.length ;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			int rows = mineFieldSizes[position][0];
			int cols = mineFieldSizes[position][1];
			int num_mines = mineFieldSizes[position][2];
			boolean this_option_is_selected = false ;
			if ( (cur_rows == rows) && (cur_cols == cols) && (cur_num_mines == num_mines) ) {
				this_option_is_selected = true ;
			}			
			
			LinearLayout l;
			if ( convertView == null ) {
				l = (LinearLayout) inflater.inflate( R.layout.listitem, null );
			} else {
				l = (LinearLayout)convertView ;
			}
			TextView tv = (TextView) l.findViewById(R.id.ListTextView);
			String label = String.format( getString(R.string.size_button_label_fmt), cols, rows, num_mines) ;
			tv.setText(label);
			
			ImageView icon = (ImageView) l.findViewById(R.id.ListImageView);

			if ( this_option_is_selected ) {
				 icon.setImageResource( R.drawable.check_mark_pressable );
			} else {
				icon.setImageDrawable(null);
			}
						
			return l;
		} // getView
		
	} // SizesListAdapter

}

/**
 *     This file is part of nookBrowser.

    nookBrowser is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    nookBrowser is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with nookBrowser.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.nookdevs.browser;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

class FavsDB extends SQLiteOpenHelper {
    private SQLiteDatabase m_ReadDb;
    private SQLiteDatabase m_WriteDb;
    private static final String DATABASE_NAME="BROWSER";
    private static final String TABLE_NAME="FAVS";
    private static final String DROP_TABLE = " DROP TABLE IF EXISTS " + TABLE_NAME;
    private static final String CREATE_TABLE = " CREATE TABLE " + TABLE_NAME
        + " ( id integer primary key autoincrement, name text not null,"
        + " value text not null)";
    private static String TAG="FavsDB";
    private boolean m_DataLoaded=false;
    private ArrayList<CharSequence> m_Names= new ArrayList<CharSequence>();
    private ArrayList<CharSequence> m_Values= new ArrayList<CharSequence>();
    private ArrayList<Integer> m_Ids = new ArrayList<Integer>();
    private Context m_Context=null;
    
    public FavsDB(Context context, CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, version);
        m_Context =context;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading db from " + oldVersion + " to " + newVersion + ". All data will be lost");
        db.execSQL(DROP_TABLE);
        onCreate(db);
    }
    
    private void loadData() {
        try {
            if (m_ReadDb == null) {
                m_ReadDb = getReadableDatabase();
            }
            String [] columns = {"name", "value", "id"};
            Cursor cursor = m_ReadDb.query(TABLE_NAME, columns, null,null,null,null,"id");
            cursor.moveToFirst();
            while ( !cursor.isAfterLast()) {
            	m_Names.add(cursor.getString(0));
            	m_Values.add(cursor.getString(1));
            	m_Ids.add(cursor.getInt(2));
            	cursor.moveToNext();
            }
            cursor.close();
            m_Names.add(0, m_Context.getString(R.string.add_fav));
            m_DataLoaded=true;
        } catch (Exception ex) {
        	Log.e(TAG, "Exception reading favs ... ", ex);
        }
    }
    public List<CharSequence> getNames() {
    	if( !m_DataLoaded) {
    		loadData();
    	}
    	return m_Names;
    }
    public List<CharSequence> getValues() {
    	if( !m_DataLoaded) {
    		loadData();
    	}
    	return m_Values;
    }
    public void deleteFav(int position) {
    	try {
    	   String idStr = String.valueOf(m_Ids.get(position-1));
           if (m_WriteDb == null) {
	            m_WriteDb = getWritableDatabase();
	        }
	        String [] values = {idStr};
	        m_WriteDb.beginTransaction();
	        m_WriteDb.delete(TABLE_NAME, "id=?", values);
	        m_WriteDb.setTransactionSuccessful();
	        m_Names.remove(position);
	        m_Values.remove(position-1);
	        m_Ids.remove(position-1);
	        m_WriteDb.endTransaction();
	    } catch(Exception ex) {
	    	Log.e(TAG, "Exception deleting fav ... ", ex);
	        m_WriteDb.endTransaction();
	    }
    }
    public void addFav(String name, String val) {
    	try {
    		m_Names.add(name);
    		m_Values.add(val);
    		if (m_WriteDb == null) {
	            m_WriteDb = getWritableDatabase();
	        }
	        m_WriteDb.beginTransaction();
	        ContentValues values = new ContentValues();
	        values.put("name",name);
	        values.put("value", val);
	        m_WriteDb.insert(TABLE_NAME, null, values);
	        m_WriteDb.setTransactionSuccessful();
	        m_WriteDb.endTransaction();
    	} catch(Exception ex) {
    		Log.e(TAG, "Exception adding favs ... ", ex);
    		m_WriteDb.endTransaction();
    	}
    	
    }
}
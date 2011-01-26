/* 
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *              http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

/*
 * This class handles the database where we save our positions in
 * the files we've read
 */


package com.nookdevs.mtextview;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import java.util.ArrayList;

public class DatabaseHelper {
	private static final String DATABASE_NAME = "textviewerDB" ;
	private static final int DATABASE_VERSION = 1 ;
	private static final long MAX_RECORDS = 100 ;
	private SQLiteDatabase db;

	public DatabaseHelper(Context context) {
		db = (new OpenHelper(context)).getWritableDatabase();
	}

	private static class OpenHelper extends SQLiteOpenHelper {
		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE textfiles (" +
						"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
						"path TEXT, " +
						"filesize BIGINT, " +
						"reading_offset BIGINT, " +
						"reading_offset_timestamp DATE )"
					);
		} // onCreate
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS textfiles"); onCreate(db);
		} // onUpgrade
	} // OpenHelper


	public long getSavedOffset(String fileName) {
		if (fileName == null || fileName.equals("")) return(0l);
		Long id = getTextFileRecordID(fileName);
		if (id == null) return(0l);
		String sql = "SELECT reading_offset FROM textfiles WHERE _id=?" ;
		String[] args = new String[1];  args[0] = id.toString();
		try {
			Cursor c = db.rawQuery(sql, args);
			if( ! c.moveToFirst() ) {
				return(0l);
			}
			return( c.getLong( c.getColumnIndexOrThrow("reading_offset") ) );
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception trying to read from database: " + ex);
			return(0l);
		}
	} // getSavedOffset

	public void saveOffset(String fileName, long fileLength, long offset) {
		if (fileName == null || fileName.equals("")) return;
		ContentValues record = new ContentValues();
		record.put("path", fileName);
		record.put("filesize", fileLength );
		record.put("reading_offset", offset );
		record.put("reading_offset_timestamp", System.currentTimeMillis() );
		Long id = getTextFileRecordID(fileName);
		try {
			if ( id == null ) {
				db.insert("textfiles", null, record);
				enforceDBSizeLimit();
			} else {
				db.update("textfiles", record, "_id=?", new String[]{id.toString()} );
			}
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception trying to save file position: " + ex);
		}
	} // saveOffset

	public void close() {
		try {
			db.close();
		} catch (Exception ex) {
			Log.d(this.toString(), "Error: db.close(): " + ex);
		}
		db = null;
	} // close

	private Long getTextFileRecordID(String fileName) {
		if (fileName == null || fileName.equals("")) return(null);
		String sql = "SELECT _id FROM textfiles WHERE path=?" ;
		String[] args = new String[1];  args[0] = fileName ;
		try {
			Cursor c = db.rawQuery(sql, args);
			if( ! c.moveToFirst() ) {
				return(null);
			}
			return( c.getLong( c.getColumnIndexOrThrow("_id") ) );
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception trying to read from database: " + ex);
			return(null);
		}
	} // getTextFileRecordID

	//  We don't want the database to grow without bounds, so if it exceeds
	//  a maximum size, we make room by deleting old records
	private void enforceDBSizeLimit() {
		//  Determine how many records the DB contains:
		long numRecords ;
		try {
			String sql = "SELECT COUNT(_id) from textfiles" ;
			Cursor c = db.rawQuery(sql, null);
			c.moveToFirst();
			numRecords = c.getLong(0);
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception reading database size: " + ex);
			return;
		}
		//  Are there too many?:
		if ( numRecords <= MAX_RECORDS ) return ;
		long overflow = numRecords - MAX_RECORDS ;  // normally 1
		//  Get the IDs of the textfiles we haven't opened for the longest time:
		ArrayList<Long> idsToRemove = new ArrayList<Long>() ;
		try {
			String sql = "SELECT _id FROM textfiles ORDER BY reading_offset_timestamp LIMIT ?" ;
			String[] args = new String[1];  args[0] = Long.toString(overflow) ;
			Cursor c = db.rawQuery(sql, args);
			if( ! c.moveToFirst() ) {
				Log.e(this.toString(), "Unexpected empty result from: " + sql + ", " + overflow);
				return;
			}
			idsToRemove.add( c.getLong( c.getColumnIndex("_id") )  );
			while( c.moveToNext() ) {
				idsToRemove.add( c.getLong( c.getColumnIndex("_id") )  );
			}
		} catch (Exception ex) {
			Log.e(this.toString(), "Exception querying database: " + ex);
			return;
		}
		//  Delete those old records from the database:
		for( Long id : idsToRemove ) {
			String[] args = new String[1];  args[0] = id.toString();
			try {
				db.delete("textfiles", "_id=?", args );
			} catch (Exception ex) {
				Log.e(this.toString(), "Exception deleting old record: " + ex);
				return;
			}
		}
	} // enforceDBSizeLimit

}

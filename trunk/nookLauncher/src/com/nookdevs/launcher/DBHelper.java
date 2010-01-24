/**
 *     This file is part of nookLauncher.

    nookLauncher is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    nookLauncher is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with nookLauncher.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.nookdevs.launcher;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    private SQLiteDatabase m_ReadDb;
    private SQLiteDatabase m_WriteDb;
    
    public DBHelper(Context context, CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, version);
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
    
    public Cursor getApps() {
        Cursor cursor = null;
        try {
            if (m_ReadDb == null) {
                m_ReadDb = getReadableDatabase();
            }
            cursor = m_ReadDb.rawQuery("SELECT NAME, resources, iconpath, keyboard FROM " + TABLE_NAME
                + " ORDER BY ORDERNUM", null);
            
            if (cursor != null) {
                cursor.moveToFirst();
            }
        } catch (Exception ex) {
            Log.e(TAG, "error selecting from db - ", ex);
        }
        return cursor;
    }
    
    public void removeData(String name) {
        try {
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            String[] values = {
                name
            };
            m_WriteDb.delete(TABLE_NAME, " name =?", values);
            m_WriteDb.setTransactionSuccessful();
            m_WriteDb.endTransaction();
        } catch (Exception ex) {
            Log.e(TAG, "error deleting " + name + " from db.");
            m_WriteDb.endTransaction();
        }
    }
    
    public void addInitData(String[] apps, int[] resources) {
        try {
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            for (int i = 0; i < apps.length; i++) {
                String[] values = {
                    apps[i], String.valueOf(i + 1), String.valueOf(resources[i]), "0"
                };
                m_WriteDb.execSQL(
                    "insert into " + TABLE_NAME + "(name, ordernum, resources, keyboard) values(?,?,?,?)", values);
            }
            m_WriteDb.setTransactionSuccessful();
            m_WriteDb.endTransaction();
        } catch (Exception ex) {
            Log.e(TAG, "error inserting into db -", ex);
            m_WriteDb.endTransaction();
        }
        return;
    }
    
    public void addData(String app, int id, String uri, String keyboard) {
        try {
            int order = getMaxOrder() + 1;
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            String[] values = {
                app, String.valueOf(order), String.valueOf(id), uri, keyboard
            };
            m_WriteDb.execSQL("insert into " + TABLE_NAME
                + "(name, ordernum, resources, iconpath, keyboard ) values(?,?,?,?,?)", values);
            m_WriteDb.setTransactionSuccessful();
            m_WriteDb.endTransaction();
        } catch (Exception ex) {
            Log.e(TAG, "error inserting into db -", ex);
            m_WriteDb.endTransaction();
        }
    }
    
    public int getMaxOrder() {
        try {
            if (m_ReadDb == null) {
                m_ReadDb = getReadableDatabase();
            }
            Cursor cursor = m_ReadDb.rawQuery("SELECT max(ordernum) FROM " + TABLE_NAME, null);
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            return count;
        } catch (Exception ex) {
            return 0;
        }
    }
    
    public int getAppCount() {
        try {
            if (m_ReadDb == null) {
                m_ReadDb = getReadableDatabase();
            }
            Cursor cursor = m_ReadDb.rawQuery("SELECT count(*) FROM " + TABLE_NAME, null);
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            return count;
        } catch (Exception ex) {
            return 0;
        }
    }
    
    public int getOrderNumber(String app) {
        int order = 0;
        try {
            if (m_ReadDb == null) {
                m_ReadDb = getReadableDatabase();
            }
            String[] values = {
                app
            };
            Cursor cursor = m_ReadDb.rawQuery("SELECT ordernum FROM " + TABLE_NAME + " WHERE NAME=?", values);
            cursor.moveToFirst();
            order = cursor.getInt(0);
            cursor.close();
        } catch (Exception ex) {
            
        }
        return order;
    }
    
    public void updateData(String app, String appPrev) {
        try {
            int order = getOrderNumber(appPrev);
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            String[] values = {
                String.valueOf(order)
            };
            m_WriteDb.execSQL("update " + TABLE_NAME + " set ordernum=ordernum+1 where ordernum >=?", values);
            String[] values2 = {
                String.valueOf(order), app
            };
            m_WriteDb.execSQL("update " + TABLE_NAME + " set ordernum=? where name=?", values2);
            m_WriteDb.setTransactionSuccessful();
            m_WriteDb.endTransaction();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
    private static final String TAG = "DBHelper";
    private static String DATABASE_NAME = "LAUNCH_SETTINGS";
    private String TABLE_NAME = "APPS";
    private String DROP_TABLE = " DROP TABLE IF EXISTS " + TABLE_NAME;
    private String CREATE_TABLE = " CREATE TABLE " + TABLE_NAME
        + " ( _id integer primary key autoincrement, name text not null,"
        + "ordernum integer not null, resources int , iconpath string, keyboard integer not null)";
    
}

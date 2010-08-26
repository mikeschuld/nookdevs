/*
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        try {
            int i = 0;
            db.beginTransaction();
            for (String app : LauncherSettings.apps) {
                String [] values = { String.valueOf(i++), app}; 
                db.execSQL("update " + TABLE_NAME + " set resources=? WHERE name=?", values);
            }
            db.execSQL("update " + TABLE_NAME + " set name=\'com.bravo.app.settings.wifi.WifiActivity\' where name=\'com.bravo.app.settings.WifiActivity\'");
            db.setTransactionSuccessful();
            db.endTransaction();
            db.beginTransaction();
            if( oldVersion <= 12) {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN LEVEL INT DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN FOLDER INT DEFAULT 0");
                db.setTransactionSuccessful();
                db.endTransaction();
            }
        } catch (Throwable ex) {
            Log.w(TAG, "Upgrading db from " + oldVersion + " to " + newVersion + ". All data will be lost", ex);
            db.endTransaction();
            db.execSQL(DROP_TABLE);
            onCreate(db);
        }

    }
    public Cursor getApps(int level) {
        Cursor cursor = null;
        try {
            if (m_ReadDb == null) {
                m_ReadDb = getReadableDatabase();
            }
            String[] args = { String.valueOf(level) };
            cursor =
                m_ReadDb.rawQuery("SELECT NAME, resources, iconpath, level, folder FROM " + TABLE_NAME
                    + " WHERE LEVEL=? ORDER BY ORDERNUM ", args);

            if (cursor != null) {
                cursor.moveToFirst();
            }
        } catch (Exception ex) {
            Log.e(TAG, "error selecting from db - ", ex);
        }
        return cursor;
    }

    public void removeData(String name, int folder) {
        try {
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            String[] values = {
                name, String.valueOf(folder)
            };
            m_WriteDb.delete(TABLE_NAME, " name =? and folder=?", values);
            String[] values2 = {
                String.valueOf(folder)
            };
            if( folder >0)
                m_WriteDb.execSQL("update " + TABLE_NAME + " set level=0 where level=?", values2);
            m_WriteDb.setTransactionSuccessful();
            m_WriteDb.endTransaction();
        } catch (Exception ex) {
            Log.e(TAG, "error deleting " + name + " from db.");
            m_WriteDb.endTransaction();
        }
    }
    public void updateIcon(String appName,int folder,String imageUri, int resid) {
        try {
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            String[] values = {
                    imageUri, appName, String.valueOf(folder)
            };
            m_WriteDb.execSQL(
                    "UPDATE " + TABLE_NAME + " SET iconpath=? where NAME=? and FOLDER=?", values);
            values[0] = String.valueOf(resid);
            m_WriteDb.execSQL(
                "UPDATE " + TABLE_NAME + " SET resources=? where NAME=? and FOLDER=?", values);
            m_WriteDb.setTransactionSuccessful();
            m_WriteDb.endTransaction();
        } catch (Exception ex) {
            Log.e(TAG, "error updating icon detail in db -", ex);
            m_WriteDb.endTransaction();
        }
        return;
        
    }

    public void addInitData(String[] apps, int[] resources) {
        try {
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            for (int i = 0; i < apps.length; i++) {
                String[] values = {
                    apps[i], String.valueOf(i + 1), String.valueOf(i), "0"
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

    public int getNextLevel() {
        try {
            if (m_ReadDb == null) {
                m_ReadDb = getReadableDatabase();
            }
            Cursor cursor = m_ReadDb.rawQuery("SELECT max(level) FROM " + TABLE_NAME, null);
            cursor.moveToFirst();
            int l = cursor.getInt(0);
            cursor.close();
            return l+1;
        } catch (Exception ex) {
            return 1;
        }
    }
    public void addData(String app, int id, String uri, int level, int folder) {
        try {
            int order = getMaxOrder() + 1;
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            String[] values = {
                app, String.valueOf(order), String.valueOf(id), uri, String.valueOf(level), String.valueOf(folder), "0"
            };
            m_WriteDb.execSQL("insert into " + TABLE_NAME
                + "(name, ordernum, resources, iconpath, level, folder, keyboard ) values(?,?,?,?,?,?,?)", values);
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

    public int getOrderNumber(String app , int folder ) {
        int order = 0;
        try {
            if (m_ReadDb == null) {
                m_ReadDb = getReadableDatabase();
            }
            String[] values = {
                app, String.valueOf(folder)
            };
            Cursor cursor = m_ReadDb.rawQuery("SELECT ordernum FROM " + TABLE_NAME + " WHERE NAME=? and FOLDER=?", values);
            cursor.moveToFirst();
            order = cursor.getInt(0);
            cursor.close();
        } catch (Exception ex) {

        }
        return order;
    }

    public void updateData(String app, String appPrev, int folder, int prevFolder) {
        try {
            int order = getOrderNumber(appPrev, prevFolder);
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            String[] values = {
                String.valueOf(order)
            };
            m_WriteDb.execSQL("update " + TABLE_NAME + " set ordernum=ordernum+1 where ordernum >=?", values);
            String[] values2 = {
                String.valueOf(order), app, String.valueOf(folder)
            };
            m_WriteDb.execSQL("update " + TABLE_NAME + " set ordernum=? where name=? and folder=?", values2);
            m_WriteDb.setTransactionSuccessful();
            m_WriteDb.endTransaction();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
    public void updateLevel(String app, int level, int folder) {
        try {
            System.out.println("Updating level for " + app + " to " + level);
            if (m_WriteDb == null) {
                m_WriteDb = getWritableDatabase();
            }
            m_WriteDb.beginTransaction();
            String[] values2 = {
                String.valueOf(level), app, String.valueOf(folder)
            };
            m_WriteDb.execSQL("update " + TABLE_NAME + " set level=? where name=? and folder=?", values2);
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
    private String CREATE_TABLE =
        " CREATE TABLE " + TABLE_NAME + " ( _id integer primary key autoincrement, name text not null,"
            + "ordernum integer not null, resources int , iconpath string, keyboard integer not null, level integer default 0, folder integer default 0)";

}

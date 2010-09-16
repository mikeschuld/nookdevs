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
package com.nookdevs.nooksync;

import java.io.File;
import java.util.Date;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class SyncService extends Service {
    private SQLiteDatabase m_Db;
    public static final String APP_DB = "/data/data/com.bravo.home/theDB.db";
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Bundle b = intent.getExtras();
        if( b != null) {
            String ean = b.getString("ean");
            String status = b.getString("status");
            String path = b.getString("path");
            m_Db = SQLiteDatabase.openDatabase(APP_DB, null, SQLiteDatabase.OPEN_READWRITE);
            try {
                Log.i("SyncService"," ean =" + ean);
                Log.i("SyncService"," status =" + status);
                String [] whereArgs = { ean};
                String whereClause = "product_ean=?";
                ContentValues values = new ContentValues();
                values.put("locker_status", status);
                m_Db.beginTransaction();
                m_Db.update("bn_client_product_states", values, whereClause, whereArgs);
                String id="";
                String data_id="";
                values.clear();
                String[] columns = { "id"};
                Cursor c = m_Db.query("bn_client_product_states", columns, whereClause, whereArgs,null, null,null);
                c.moveToFirst();
                if( !c.isAfterLast()) {
                    data_id = c.getString(0);
                }
                c.close();
                c = m_Db.rawQuery("select max(id) +1 from bn_client_sync_items",null);
                c.moveToFirst();
                if( !c.isAfterLast()) {
                    id = c.getString(0);
                }
                c.close();
                String dateval = String.valueOf(System.currentTimeMillis()
                    );
                values.clear();
                values.put("id", id);
                values.put("data_id", data_id);
                values.put("sync_operation_source","CLIENT");
                values.put("sync_operation","REPLACE");
                values.put("data_type", "PRODUCT_STATE");
                values.put("data_type", "PRODUCT_STATE");
                values.put("acked",0);
                values.put("mapped",0);
                values.put("data", ean);
                values.put("created", dateval);
                values.put("modified", dateval);
                m_Db.insertOrThrow("bn_client_sync_items", null, values);
                boolean del=false;
                if( path != null && !path.trim().equals("")) {
                    File f = new File(path);
                    if( f .exists()) {
                        del=f.delete();
                    }
                } else {
                    del=true;
                }
                if( del) {
                    String[] vals = { ean};
                    values.clear();
                    values.put("downloaded_path","");
                    m_Db.update("bn_client_local_product_state", values, "local_product_ean=?", vals);
                }
                m_Db.setTransactionSuccessful();
                m_Db.endTransaction();
                m_Db.close();
                stopSelf();
            } catch(Exception ex) {
                Log.e("SyncService:", ex.getMessage(), ex);
                m_Db.endTransaction();
                m_Db.close();
            }
        }
    }
}

/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.craftycoder.music;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.lang.Integer;

import com.craftycoder.music.R;
import com.nookdevs.common.nookListActivity;

public class VideoBrowserActivity extends nookListActivity implements MusicUtils.Defs
{
    public VideoBrowserActivity()
    {
    	NAME = "nookMedia: Video Browser";
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent evnt) {
        boolean handled = false;
    	int pos = this.mCursor.getPosition();
    	if(pos == this.mCursor.getCount()) {
    		pos = -1;
    	}
    	KeyEvent event;
    	Log.d(LOGTAG,"Current Position " + pos);
        switch (keyCode) {
            case NOOK_PAGE_UP_KEY_LEFT:
            	event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);
            	getListView().onKeyDown(KeyEvent.KEYCODE_DPAD_UP, event);
                event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP);
                getListView().onKeyUp(KeyEvent.KEYCODE_DPAD_UP, event);
                getListView().dispatchKeyEvent(event);            	
            	break;
            case NOOK_PAGE_UP_KEY_RIGHT:
            	Log.d(LOGTAG,"BACK");
            	keyCode = KeyEvent.KEYCODE_BACK;
            	super.onKeyDown(keyCode,new KeyEvent(evnt));
            	break;
            case NOOK_PAGE_UP_SWIPE:
            case NOOK_PAGE_DOWN_KEY_LEFT:
                event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
                getListView().onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, event);
                event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN);
                getListView().onKeyUp(KeyEvent.KEYCODE_DPAD_DOWN, event);
                getListView().dispatchKeyEvent(event);               	
            	break;            	
            case NOOK_PAGE_DOWN_KEY_RIGHT:
                event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
                getListView().onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, event);
                event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER);
                getListView().onKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, event); 
                getListView().dispatchKeyEvent(event);
            	Log.d(LOGTAG,"FORWARD");
            	break;            	
            case NOOK_PAGE_DOWN_SWIPE:
            default:
                break;
        }
        if (handled) {

        } 
        return handled;
    }     
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        init();
    }

    public void init() {

        // Set the layout for this activity.  You can find it
        // in assets/res/any/layout/media_picker_activity.xml
        setContentView(R.layout.media_picker_activity);

        MakeCursor();

        if (mCursor == null) {
            MusicUtils.displayDatabaseError(this);
            return;
        }

        if (mCursor.getCount() > 0) {
            setTitle(R.string.videos_title);
        } else {
            setTitle(R.string.no_videos_title);
        }

        // Map Cursor columns to views defined in media_list_item.xml
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_1,
                mCursor,
                new String[] { MediaStore.Video.Media.TITLE},
                new int[] { android.R.id.text1 });

        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        mCursor.moveToPosition(position);
        String type = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
        intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), type);
        
        startActivity(intent);
    }

    private void MakeCursor() {
        String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.ARTIST
        };
        ContentResolver resolver = getContentResolver();
        if (resolver == null) {
            System.out.println("resolver = null");
        } else {
            mSortOrder = MediaStore.Video.Media.TITLE + " COLLATE UNICODE";
            mWhereClause = MediaStore.Video.Media.TITLE + " != ''";
            mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                cols, mWhereClause , null, mSortOrder);
        }
    }

    private Cursor mCursor;
    private String mWhereClause;
    private String mSortOrder;
}


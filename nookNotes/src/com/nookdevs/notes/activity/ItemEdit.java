/*
 * nookNotes, copyright (C) 2010 nookdevs
 *
 * Written by Marco Goetze, <gomar@gmx.net>.
 *
 * A notes-taking application for the Barnes & Noble nook ebook reader.
 *
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

package com.nookdevs.notes.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.nookdevs.notes.R;
import com.nookdevs.notes.provider.Item;
import com.nookdevs.notes.provider.Notes;
import com.nookdevs.notes.provider.NotesUtils;
import com.nookdevs.notes.util.NookSpecifics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.NotesUris.*;


/**
 * Activity for editing an item of a note.  Deals with URIs of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_ITEM}.    Yields a URI of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_NOTE} for convenience.
 *
 * @author Marco Goetze
 */
public class ItemEdit extends BaseActivity implements View.OnKeyListener
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    /** The item's URI, validated to indeed be a single item's URI. */
    @NotNull protected Uri mUri;

    /** The text input field. */
    @NotNull protected EditText vText;

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @NotNull @Override
    protected String getActivityTitle() {
        return getString(R.string.activity_item_edit);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // sanity checks...
        final Uri uri = getIntent().getData();
        if (!isSingleItemUri(uri)) {
            throw new IllegalArgumentException("Unsupported URI: <" + uri + ">!");
        }
        Integer noteId = noteIdOfUri(uri);
        assert noteId != null;
        final int itemIndex = itemIndexOfUri(uri);
        Item item = NotesUtils.getItem(getContentResolver(), noteId, itemIndex);
        if (item == null) {
            throw new IllegalArgumentException("Item does not exist for URI <" + uri + ">!");
        }
        mUri = uri;

        // overall initialization...
        setContentView(R.layout.dialog_item_text);
        showSoftInput(R.layout.dialog_item_text);

        // set up views...
        vText = (EditText) findViewById(R.id.item_text_input);
        vText.setText(item.getText());
        vText.requestFocus();
        vText.setOnKeyListener(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.item_text_input).requestFocus();
    }

    //..................................................................... interface OnKeyListener

    /** {@inheritDoc} */
    @Override
    public boolean onKey(@NotNull View view,
                         int keyCode,
                         @NotNull KeyEvent ev)
    {
        if (ev.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case NookSpecifics.KEY_SOFT_CLEAR:
                    vText.setText("");
                    return true;
                case NookSpecifics.KEY_SOFT_CANCEL:
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                case NookSpecifics.KEY_SOFT_SUBMIT: {
                    boolean ok = updateItem(vText.getText().toString());
                    if (ok) {
                        setResult(RESULT_OK, new Intent(Intent.ACTION_VIEW, mUri));
                    } else {
                        setResult(RESULT_ERROR);
                    }
                    finish();
                    return true;
                }

                default:
                    // fall through
            }
        }
        return false;
    }

    // own methods...

    /**
     * Updates the item being edited with a given text.
     *
     * @param text the item's text (may be <code>null</code>)
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    protected boolean updateItem(@Nullable String text) {
        ContentValues values = new ContentValues();
        values.put(Notes.KEY_ITEM_TEXT, text);
        try {
            return (getContentResolver().update(mUri, values, null, null) > 0);
        } catch (Exception e) {
            Log.e(mLogTag, "Error updating item <" + mUri + ">!", e);
        }
        return false;
    }
}

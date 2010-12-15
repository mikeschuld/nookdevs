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

import java.util.Map;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.nookdevs.notes.R;
import com.nookdevs.notes.gui.EditingUtils;
import com.nookdevs.notes.provider.Note;
import com.nookdevs.notes.provider.Notes;
import com.nookdevs.notes.util.NookSpecifics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.NotesUris.*;
import static com.nookdevs.notes.provider.NotesUtils.checkedCounts;
import static com.nookdevs.notes.provider.NotesUtils.getNote;


/**
 * Activity for adding an item to a note.  Deals with URIs of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_ITEMS} or
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_ITEM}, adding the item add the end
 * in the former or at a particular index in the latter case.  Yields a URI of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_ITEM}.
 *
 * @author Marco Goetze
 */
public class ItemAdd extends BaseActivity implements View.OnKeyListener
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    /** The ID of the note to which to add an item. */
    protected int mNoteId;
    /** The index at which to insert the item, or <code>null</code> if not specified. */
    @Nullable protected Integer mItemIndex;

    /** The text input field. */
    @NotNull protected EditText vText;

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @NotNull @Override
    protected String getActivityTitle() {
        return getString(R.string.activity_item_add);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // sanity checks...
        final Uri uri = getIntent().getData();
        if (!isItemsUri(uri)) {
            throw new IllegalArgumentException("Unsupported URI: <" + uri + ">!");
        }
        mNoteId = noteIdOfUri(uri);
        mItemIndex = itemIndexOfUri(uri);

        // overall initialization...
        setContentView(R.layout.dialog_item_text);
        showSoftInput(R.layout.dialog_item_text);

        // set up views...
        vText = (EditText) findViewById(R.id.item_text_input);
        vText.requestFocus();
        createAndRegisterInputStringReplacer(vText);
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
        if (EditingUtils.handleDefaultKeys(vText, ev)) return true;
        if (ev.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case NookSpecifics.KEY_SOFT_CANCEL:
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                case NookSpecifics.KEY_SOFT_SUBMIT:
                    Uri uri = addItem(vText.getText().toString());
                    if (uri != null) {
                        setResult(RESULT_OK, new Intent(Intent.ACTION_VIEW, uri));
                    } else {
                        setResult(RESULT_ERROR);
                    }
                    finish();
                    return true;

                default:
                    // fall through
            }
        }
        return false;
    }

    // own methods...

    /**
     * Adds an item with the given text (at the pre-defined index, if any, otherwise at the end of
     * the note's list of items).
     *
     * @param text the item's text (may be <code>null</code>)
     * @return the item's index URI, if successfully added, <code>null</code> otherwise
     */
    @Nullable
    protected Uri addItem(@Nullable String text) {
        // gather values...
        ContentValues values = new ContentValues();
        values.put(Notes.KEY_ITEM_TEXT, text);
        Note note = getNote(getContentResolver(), mNoteId);
        if (note == null) return null;
        Map<Integer,Integer> checkedCounts = checkedCounts(note);
        // make checkable but unchecked if the note contains only checkable items...
        if (!checkedCounts.isEmpty() && !checkedCounts.containsKey(Notes.ITEM_CHECKED_NONE)) {
            values.put(Notes.KEY_ITEM_CHECKED, Notes.ITEM_CHECKED_UNCHECKED);
        }
        if (mItemIndex != null) values.put(Notes.KEY_ITEM_INDEX, mItemIndex);

        // do add the item...
        try {
            return getContentResolver().insert(itemsUri(mNoteId), values);
        } catch (Exception e) {
            Log.e(mLogTag, "Error adding item to note #" + mNoteId + "!", e);
        }
        return null;
    }
}

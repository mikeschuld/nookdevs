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
import com.nookdevs.notes.gui.EditingUtils;
import com.nookdevs.notes.provider.Notes;
import com.nookdevs.notes.util.NookSpecifics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.NotesUris.notesUri;


/**
 * Activity for adding a note.  Yields a URI of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_NOTE}.
 *
 * @author Marco Goetze
 */
public class NoteAdd extends BaseActivity implements View.OnKeyListener
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    /** The title input field. */
    @NotNull protected EditText vTitle;

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @Override
    protected String getActivityTitle() {
        return getString(R.string.activity_note_add);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // overall initialization...
        setContentView(R.layout.dialog_note_title);
        showSoftInput(R.layout.dialog_note_title);

        // set up views...
        vTitle = (EditText) findViewById(R.id.note_title_input);
        vTitle.requestFocus();
        createAndRegisterInputStringReplacer(vTitle);
        vTitle.setOnKeyListener(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.note_title_input).requestFocus();
    }

    //..................................................................... interface OnKeyListener

    /** {@inheritDoc} */
    @Override
    public boolean onKey(@NotNull View view,
                         int keyCode,
                         @NotNull KeyEvent ev)
    {
        if (EditingUtils.handleDefaultKeys(vTitle, ev)) return true;
        if (ev.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case NookSpecifics.KEY_SOFT_CANCEL:
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                case NookSpecifics.KEY_SOFT_SUBMIT: {
                    Uri uri = addNote(vTitle.getText().toString());
                    if (uri != null) {
                        setResult(RESULT_OK, new Intent(Intent.ACTION_VIEW, uri));
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
     * Adds a note with a given title.
     *
     * @param title the note's title (may be <code>null</code>)
     * @return the new note's URI on success, <code>null</code> otherwise
     */
    @Nullable
    protected Uri addNote(@Nullable String title) {
        ContentValues values = new ContentValues();
        values.put(Notes.KEY_NOTE_TITLE, title);
        try {
            return getContentResolver().insert(notesUri(), values);
        } catch (Exception e) {
            Log.e(mLogTag, "Error adding note!", e);
        }
        return null;
    }
}

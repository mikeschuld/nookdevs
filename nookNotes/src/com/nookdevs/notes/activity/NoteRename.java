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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.nookdevs.notes.R;
import com.nookdevs.notes.gui.EditingUtils;
import com.nookdevs.notes.provider.Note;
import com.nookdevs.notes.provider.Notes;
import com.nookdevs.notes.provider.NotesUris;
import com.nookdevs.notes.provider.NotesUtils;
import com.nookdevs.notes.util.NookSpecifics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.NotesUris.isSingleNoteUri;
import static com.nookdevs.notes.provider.NotesUris.noteIdOfUri;


/**
 * Activity for editing a note's title.  Deals with URIs of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_NOTE}.
 *
 * @author Marco Goetze
 */
public class NoteRename extends BaseActivity implements View.OnKeyListener
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //....................................................................................... views

    /** The title input field. */
    @NotNull protected EditText vTitle;

    //............................................................................. other internals

    /** The ID of the note being edited. */
    protected int mNoteId;

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @NotNull @Override
    protected String getActivityTitle() {
        return getString(R.string.activity_note_rename);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // sanity checks...
        Uri uri = getIntent().getData();
        if (!isSingleNoteUri(uri)) {
            throw new IllegalArgumentException("Unsupported URI: <" + uri + ">!");
        }
        mNoteId = noteIdOfUri(uri);
        Note note = NotesUtils.getNote(getContentResolver(), mNoteId);
        if (note == null) {
            throw new IllegalArgumentException("Note #" + mNoteId + " does not exist!");
        }

        super.onCreate(savedInstanceState);

        // overall initialization...
        setContentView(R.layout.dialog_note_title);
        showSoftInput(R.layout.dialog_note_title);

        // set up views...
        vTitle = (EditText) findViewById(R.id.note_title_input);
        String title = note.getTitle();
        vTitle.setText(TextUtils.isEmpty(title) ? "" : title);
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
                    boolean ok = updateNote(vTitle.getText().toString());
                    setResult(ok ? RESULT_OK : RESULT_ERROR);
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
     * Updates the note.
     *
     * @param title the note's new title (may be <code>null</code>)
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    protected boolean updateNote(@Nullable String title) {
        ContentValues values = new ContentValues();
        values.put(Notes.KEY_NOTE_TITLE, title);
        ContentResolver cr = getContentResolver();
        try {
            return (cr.update(NotesUris.singleNoteUri(mNoteId), values, null, null) > 0);
        } catch (Exception e) {
            Log.e(mLogTag, "Error update note #" + mNoteId + "!", e);
        }
        return false;
    }
}

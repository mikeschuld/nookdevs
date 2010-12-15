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

package com.nookdevs.notes.provider;

import android.app.Activity;
import android.database.Cursor;

import com.nookdevs.notes.data.CursorBasedListItemsProvider;
import org.jetbrains.annotations.NotNull;

import static com.nookdevs.notes.provider.NotesUris.notesUri;
import static com.nookdevs.notes.provider.NotesUtils.getNote;


/**
 * Class adapting a {@link com.nookdevs.notes.provider.Notes}-type content provider to the
 * {@link com.nookdevs.notes.data.ListItemsProvider} interface for {@link Note}s.
 *
 * @author Marco Goetze
 */
public class NoteListItemsProvider extends CursorBasedListItemsProvider<Note>
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //............................................................................ public constants

    /** Constant indicating to sort notes alphabetically by title. */
    public static final int SORT_BY_LAST_VIEWED = 1;
    /** Constant indicating to sort notes by when they were last viewed. */
    public static final int SORT_BY_LAST_EDITED = 2;
    /** Constant indicating to sort notes by when they were last edited. */
    public static final int SORT_BY_TITLE = 3;

    //................................................................................... internals

    /** Specifies what notes are sorted by (one of the <code>SORT_BY_*</code> constants). */
    protected int mSortBy;

    ////////////////////////////////////////// METHODS ////////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates a {@link NoteListItemsProvider}.  If <code>sortBy</code> is invalid, notes will be
     * sorted by when they were last viewed.
     *
     * @param activity the activity using the provider
     * @param sortBy   specifies what to sort the notes by (one of the <code>SORT_BY_*</code>
     *                 constants)
     */
    public NoteListItemsProvider(@NotNull Activity activity,
                                 int sortBy)
    {
        super(activity, notesUri());
        mSortBy = sortBy;

        requery();
    }

    // inherited methods...

    /** {@inheritDoc} */
    @Override
    protected Cursor query() {
        // build ORDER BY expression...
        String orderBy;
        switch (mSortBy) {
            case SORT_BY_LAST_EDITED:
                orderBy = Notes.KEY_NOTE_ORDER_EDITED + " DESC, " +
                          Notes.KEY_NOTE_ORDER_VIEWED + " DESC";
                break;
            case SORT_BY_TITLE:
                orderBy = Notes.KEY_NOTE_TITLE + " COLLATE NOCASE";
                break;
            case SORT_BY_LAST_VIEWED:
            default:
                orderBy = Notes.KEY_NOTE_ORDER_VIEWED + " DESC, " +
                          Notes.KEY_NOTE_ORDER_EDITED + " DESC";
        }

        // fetch note IDs...
        return mActivity.getContentResolver().query(notesUri(), null, null, null, orderBy);
    }

    /** {@inheritDoc} */
    @NotNull @Override
    protected Note getItem(@NotNull Cursor cursor) {
        return getNote(mActivity.getContentResolver(), cursor);
    }

    // own methods...

    /**
     * Changes what notes are sorted by.  If the given value is invalid, notes will be sorted by
     * when they were last viewed.
     *
     * @param sortBy  specifies what to sort the notes by (one of the <code>SORT_BY_*</code>
     *                constants)
     */
    public void sortBy(int sortBy) {
        mSortBy = sortBy;
        requery(true);
    }
}

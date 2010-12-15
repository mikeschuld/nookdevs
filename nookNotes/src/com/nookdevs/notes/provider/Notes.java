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

import android.content.ContentProvider;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * <p>Base class for {@link android.content.ContentProvider}s for notes.</p>
 *
 * <p>The default order for notes a provided by a {@link com.nookdevs.notes.provider.Notes}
 * provider is by date, descendingly.</p>
 *
 * @author Marco Goetze
 */
public abstract class Notes extends ContentProvider
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //............................................................................ public constants

    /** The content type for multiple notes. */
    @NotNull public static final String CONTENT_TYPE_NOTES =
        "vnd.android.cursor.dir/vnd.nookdevs.notes.note";
    /** The content type for a single note. */
    @NotNull public static final String CONTENT_TYPE_SINGLE_NOTE =
        "vnd.android.cursor.item/vnd.nookdevs.notes.note";
    /** The content type for multiple items of a note. */
    @NotNull public static final String CONTENT_TYPE_ITEMS =
        "vnd.android.cursor.dir/vnd.nookdevs.notes.item";
    /** The content type for a single item of a note. */
    @NotNull public static final String CONTENT_TYPE_SINGLE_ITEM =
        "vnd.android.cursor.item/vnd.nookdevs.notes.item";

    /** Column key for a note's ID. */
    @NotNull public static final String KEY_NOTE_ID = "_id";
    /** Column key for a note's title. */
    @NotNull public static final String KEY_NOTE_TITLE = "title";
    /**
     * Column key for a column for ordering notes by when they were last edited.  Higher values
     * mean later-created/-modified.
     */
    @NotNull public static final String KEY_NOTE_ORDER_EDITED = "edited";
    /**
     * Column key for a column for ordering notes by when they were last viewed.  Higher values
     * mean later-viewed.
     */
    @NotNull public static final String KEY_NOTE_ORDER_VIEWED = "viewed";

    /** Column key for a note's item's note ID. */
    @NotNull public static final String KEY_ITEM_NOTE_ID = "note";
    /** Column key for a note's item's text. */
    @NotNull public static final String KEY_ITEM_TEXT = "text";
    /** Column key for a note's item's index, serving as an ordering attribute. */
    @NotNull public static final String KEY_ITEM_INDEX = "seq";
    /**
     * Column key for an item's "checked" state.  The column's values have to be among the
     * <code>ITEM_CHECKED_*</code> constants.
     *
     * @see #ITEM_CHECKED_NONE
     * @see #ITEM_CHECKED_UNCHECKED
     * @see #ITEM_CHECKED_CHECKED
     */
    @NotNull public static final String KEY_ITEM_CHECKED = "checked";
    /**
     * Column key for a note's item's cached display height.  The attribute is reset to
     * <code>NULL</code> whenever the item is touched.
     */
    @NotNull public static final String KEY_ITEM_HEIGHT = "height";

    /**
     * Constant for {@link #KEY_ITEM_CHECKED} noting that the item is neither checked nor
     * unchecked, i.e., no check box is to be displayed.
     */
    public static final int ITEM_CHECKED_NONE = 0;
    /** Constant for {@link #KEY_ITEM_CHECKED} noting that the item is checkable but unchecked. */
    public static final int ITEM_CHECKED_UNCHECKED = 1;
    /** Constant for {@link #KEY_ITEM_CHECKED} noting that the item is checked. */
    public static final int ITEM_CHECKED_CHECKED = 2;

    //................................................................................... internals

    /** The provider's log tag. */
    @NotNull protected final String mLogTag = "Notes.provider." + getClass().getSimpleName();

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @Nullable @Override
    public String getType(@NotNull Uri uri) {
        switch (NotesUris.uriMatcher.match(uri)) {
            case NotesUris.URI_NOTES_ALL:
                return CONTENT_TYPE_NOTES;
            case NotesUris.URI_NOTES_SINGLE:
            case NotesUris.URI_NOTES_SINGLE_VIEW:
                return CONTENT_TYPE_SINGLE_NOTE;
            case NotesUris.URI_ITEMS_ALL:
            case NotesUris.URI_ITEMS_ALL_SORT_ALPHA:
            case NotesUris.URI_ITEMS_ALL_SORT_CHECKED:
            case NotesUris.URI_ITEMS_ALL_REVERSE:
            case NotesUris.URI_ITEMS_ALL_CLEAR:
            case NotesUris.URI_ITEMS_ALL_DELETE_CHECKED:
                return CONTENT_TYPE_ITEMS;
            case NotesUris.URI_ITEMS_SINGLE:
            case NotesUris.URI_ITEMS_SINGLE_MOVE:
            case NotesUris.URI_ITEMS_SINGLE_HEIGHT:
                return CONTENT_TYPE_SINGLE_ITEM;

            default:
                return null;
        }
    }
}

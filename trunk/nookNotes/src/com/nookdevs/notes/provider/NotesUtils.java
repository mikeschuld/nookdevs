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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.Notes.*;
import static com.nookdevs.notes.provider.NotesUris.*;


/**
 * Utility class for dealing with notes and items retrieved via a content provider.
 *
 * @author Marco Goetze
 */
@SuppressWarnings({ "UnusedDeclaration" })
public abstract class NotesUtils
{
    //////////////////////////////////////////// METHODS ////////////////////////////////////////////

    // own methods...

    /**
     * Returns the number of notes stored.
     *
     * @param contentResolver the content resolver
     * @return the number of notes
     */
    public static int notesCount(@NotNull ContentResolver contentResolver) {
        Cursor cursor = contentResolver.query(notesUri(), null, null, null, null);
        int n = cursor.getCount();
        cursor.close();
        return n;
    }

    /**
     * Returns the number of items of a note.
     *
     * @param contentResolver the content resolver
     * @param noteId          the note's ID
     * @return the note's number of items
     */
    public static int itemsCount(@NotNull ContentResolver contentResolver,
                                 int noteId)
    {
        Cursor cursor = contentResolver.query(itemsUri(noteId), null, null, null, null);
        int n = cursor.getCount();
        cursor.close();
        return n;
    }

    /**
     * Retrieves a note given its ID.
     *
     * @param contentResolver the content resolver
     * @param noteId          the note's ID
     * @return the corresponding note, or <code>null</code> if the retrieval failed
     */
    @Nullable
    public static Note getNote(@NotNull ContentResolver contentResolver,
                               int noteId)
    {
        Note note = null;
        Cursor cursor = contentResolver.query(singleNoteUri(noteId), null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                note = getNote(contentResolver, cursor);
            }
            cursor.close();
        }
        return note;
    }

    /**
     * Creates a {@link com.nookdevs.notes.provider.Note} given a notes cursor.  The cursor has
     * to provide all note fields.
     *
     * @param contentResolver the content resolve
     * @param cursor          the cursor
     * @return a note instance
     * @throws IllegalArgumentException if the cursor lacks required columns
     */
    @NotNull
    public static Note getNote(@NotNull ContentResolver contentResolver,
                               @NotNull Cursor cursor)
        throws IllegalArgumentException
    {
        int noteId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTE_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTE_TITLE));
        List<Item> items = getItems(contentResolver, noteId);
        return new Note(noteId, title, items);
    }

    /**
     * Retrieves a note's items.
     *
     * @param contentResolver the content resolver
     * @param noteId          the note's ID
     * @return the note's items
     */
    @NotNull
    public static List<Item> getItems(@NotNull ContentResolver contentResolver,
                                      int noteId)
    {
        List<Item> items = new LinkedList<Item>();
        Cursor cursor = contentResolver.query(itemsUri(noteId), null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) items.add(getItem(cursor));
            cursor.close();
        }
        return items;
    }

    /**
     * Creates a {@link com.nookdevs.notes.provider.Item} given an items cursor.  The cursor has
     * to provide all item fields.
     *
     * @param cursor the cursor
     * @return an item instance
     * @throws IllegalArgumentException if the cursor lacks required columns
     */
    @NotNull
    public static Item getItem(@NotNull Cursor cursor) {
        Integer height = null;
        int heightIdx = cursor.getColumnIndexOrThrow(KEY_ITEM_HEIGHT);
        if (!cursor.isNull(heightIdx)) height = cursor.getInt(heightIdx);
        return new Item(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ITEM_INDEX)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ITEM_TEXT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ITEM_CHECKED)),
                        height);
    }

    /**
     * Retrieves an item of a note given the note's ID and the item's index.
     *
     * @param contentResolver the content resolver
     * @param noteId          the note's ID
     * @param itemIndex       the item's index
     * @return the corresponding item, or <code>null</code> if the retrieval failed
     */
    @Nullable
    public static Item getItem(@NotNull ContentResolver contentResolver,
                               int noteId,
                               int itemIndex)
    {
        Cursor cursor =
            contentResolver.query(singleItemUri(noteId, itemIndex), null, null, null, null);
        if (cursor == null) return null;
        if (!cursor.moveToNext()) {
            cursor.close();
            return null;
        }
        Integer height = null;
        int heightIdx = cursor.getColumnIndexOrThrow(KEY_ITEM_HEIGHT);
        if (!cursor.isNull(heightIdx)) height = cursor.getInt(heightIdx);
        Item item = new Item(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ITEM_INDEX)),
                             cursor.getString(cursor.getColumnIndexOrThrow(KEY_ITEM_TEXT)),
                             cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ITEM_CHECKED)),
                             height);
        cursor.close();
        return item;
    }

    /**
     * Stores a (new) note including its items, if any.  If the note has an ID, an existing note
     * with that ID will be updated.
     *
     * @param contentResolver the content resolver
     * @param note            the note to store
     * @return the new note's URI, or <code>null</code> if there was an error
     */
    @Nullable
    public static Uri storeNote(@NotNull ContentResolver contentResolver,
                                @NotNull Note note) {
        // update the note's basic data if it exists and delete its items, create a new one
        // otherwise...
        ContentValues values = new ContentValues();
        values.put(Notes.KEY_NOTE_TITLE, note.getTitle());
        Integer noteId = note.getId();
        if (noteId != null && getNote(contentResolver, noteId) != null) {  // exists?
            contentResolver.update(singleNoteUri(noteId), values, null, null);
            contentResolver.update(clearItemsUri(noteId), null, null, null);
        } else {  // does not exist: create
            Uri uri = contentResolver.insert(notesUri(), values);
            if (uri == null) return null;
            noteId = noteIdOfUri(uri);
            assert noteId != null;
        }

        // add items to the note...
        Uri uri = singleNoteUri(noteId);
        for (Item item : note.getItems()) {
            values = new ContentValues();
            values.put(Notes.KEY_ITEM_TEXT, item.getText());
            values.put(Notes.KEY_ITEM_CHECKED, item.getChecked());
            contentResolver.insert(uri, values);
        }

        return uri;
    }

    /**
     * Updates an existing item of a note.  <code>item</code>'s index attribute, if any, will be
     * ignored.
     *
     * @param contentResolver the content resolver to use
     * @param noteId          the note's ID
     * @param itemIndex       the item's index
     * @param item            the item
     * @return <code>true</code> if successful, <code>false</code> if the update failed or the
     *         update did not incur any actual changes
     */
    public static boolean updateItem(@NotNull ContentResolver contentResolver,
                                     int noteId,
                                     int itemIndex,
                                     @NotNull Item item)
    {
        ContentValues values = new ContentValues();
        String text = item.getText();
        if (text != null) values.put(Notes.KEY_ITEM_TEXT, text);
        values.put(Notes.KEY_ITEM_CHECKED, item.getChecked());
        return (contentResolver.update(singleItemUri(noteId, itemIndex), values, null, null) > 0);
    }

    /**
     * Returns the distribution of "checked" attributes of a note's items.
     *
     * @param note the note
     * @return mapping of "checked" values (<code>Notes.ITEM_CHECKED_*</code> constants) to their
     *         counts
     */
    @NotNull
    public static Map<Integer, Integer> checkedCounts(@NotNull Note note) {
        Map<Integer, Integer> distribution = new HashMap<Integer, Integer>();
        for (Item item : note.getItems()) {
            int checked = item.getChecked();
            Integer count = distribution.get(checked);
            distribution.put(checked, (count != null ? count + 1 : 1));
        }
        return distribution;
    }
}

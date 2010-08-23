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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.NotesUris.*;


/**
 * <em>SQLite</em>-based implementation of a {@link com.nookdevs.notes.provider.Notes} provider.
 *
 * @author Marco Goetze
 */
public class NotesSQLite extends Notes
{
    /////////////////////////////////////// NESTED CLASSES ////////////////////////////////////////

    /**
     * Helper class for opening and creating the underlying database and managing migrations.
     *
     * @author Marco Goetze
     */
    protected class DatabaseHelper extends SQLiteOpenHelper
    {
        // methods...

        // constructors/destructors...

        /**
         * Creates a {@link com.nookdevs.notes.provider.NotesSQLite.DatabaseHelper}.
         *
         * @param context the application's context
         */
        public DatabaseHelper(@NotNull Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /** {@inheritDoc} */
        @Override
        public void onCreate(@NotNull SQLiteDatabase db) {
            /*
             * NOTE: SQLite doesn't support foreign keys, and thus also no "ON DELETE CASCADE".
             *       While the latter can be emulated using (several) triggers, we chose to rather
             *       handle things ourselves.
             */
            db.execSQL("CREATE TABLE " + TABLE_NOTES + " " +
                       "(" + KEY_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                             KEY_NOTE_TITLE + " TEXT, " +
                             KEY_NOTE_ORDER_EDITED + " INTEGER NOT NULL DEFAULT -1, " +
                             KEY_NOTE_ORDER_VIEWED + " INTEGER NOT NULL DEFAULT -1);");
            db.execSQL("CREATE TABLE " + TABLE_ITEMS + " " +
                       "(" + KEY_ITEM_NOTE_ID + " INTEGER NOT NULL, " +
                             KEY_ITEM_INDEX + " INTEGER NOT NULL DEFAULT -1, " +
                             KEY_ITEM_TEXT + " TEXT, " +
                             KEY_ITEM_CHECKED + " INTEGER NOT NULL DEFAULT 0, " +
                             KEY_ITEM_HEIGHT + " INTEGER);");
            db.setVersion(DATABASE_VERSION);
        }

        /** {@inheritDoc} */
        @Override
        public void onUpgrade(@NotNull SQLiteDatabase db,
                              int oldVersion,
                              int newVersion)
        {
            Log.i(mLogTag, "Migrating database from v" + oldVersion + " to v" + newVersion + "...");
            for (int version = oldVersion; version < newVersion; ) {
                try {
                    db.beginTransaction();

                    switch (version) {
                        case 1:
                        case 2:
                            // versions 1 through 2 were pre-release, won't be encountered any more
                            break;
                        case 3:  // TABLE_ITEMS += KEY_ITEM_HEIGHT
                            db.execSQL("ALTER TABLE " + TABLE_ITEMS + " " +
                                       "ADD COLUMN " + KEY_ITEM_HEIGHT + " INTEGER;");
                            break;

                        default:
                            throw new SQLException("Missing migration step from v" + version +
                                                   " to v" + (version + 1) + "!");
                    }

                    db.setTransactionSuccessful();
                    db.setVersion(++version);
                } finally {
                    db.endTransaction();
                }
            }
            Log.i(mLogTag, "Migration successfully completed.");
        }
    }

    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //......................................................................... protected constants

    /** The name of the database. */
    @NotNull protected static final String DATABASE_NAME = "notes.db";
    /** The current database version. */
    protected static final int DATABASE_VERSION = 4;

    /** The name of the notes table. */
    @NotNull protected static final String TABLE_NOTES = "notes";
    /** The name of the items table. */
    @NotNull protected static final String TABLE_ITEMS = "items";

    /** Constant indicating to {@link #sortItems(int, int)} to sort alphabetically. */
    protected static int SORT_CHECKED = 1;
    /** Constant indicating to {@link #sortItems(int, int)} to sort by "checked" attribute. */
    protected static int SORT_ALPHA = 2;

    //................................................................................... internals

    /** The database used. */
    @NotNull protected SQLiteDatabase mDatabase;
    /** The database helper used. */
    @NotNull protected DatabaseHelper mDatabaseHelper;

    ////////////////////////////////////////// METHODS ////////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @Override
    public synchronized boolean onCreate() {
        // open/create the database...
        mDatabaseHelper = new DatabaseHelper(getContext());
        mDatabase = mDatabaseHelper.getWritableDatabase();
        //noinspection ConstantConditions
        if (mDatabase == null) return false;
        Log.d(mLogTag, getClass().getSimpleName() + " provider created.");
        return true;
    }

    /** {@inheritDoc} */
    @NotNull @Override
    public synchronized Cursor query(@NotNull Uri uri,
                                     @Nullable String[] projection,
                                     @Nullable String selection,
                                     @Nullable String[] selectionArgs,
                                     @Nullable String sortOrder)
    {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (uriMatcher.match(uri)) {
            case URI_NOTES_ALL:
                qb.setTables(TABLE_NOTES);
                if (TextUtils.isEmpty(sortOrder)) sortOrder = KEY_NOTE_ID;
                break;
            case URI_NOTES_SINGLE_VIEW:
                // update "viewed" order (but don't notify about this)...
                touchNoteAndNotify(noteIdOfUri(uri), KEY_NOTE_ORDER_VIEWED);
                // fall through
            case URI_NOTES_SINGLE: {
                int noteId = noteIdOfUri(uri);
                qb.setTables(TABLE_NOTES);
                qb.appendWhere(KEY_NOTE_ID + "=" + noteId);
                break;
            }
            case URI_ITEMS_ALL: {
                int noteId = noteIdOfUri(uri);
                qb.setTables(TABLE_ITEMS);
                qb.appendWhere(KEY_ITEM_NOTE_ID + "=" + noteId);
                if (TextUtils.isEmpty(sortOrder)) sortOrder = KEY_ITEM_INDEX;
                break;
            }
            case URI_ITEMS_SINGLE: {
                int noteId = noteIdOfUri(uri);
                int index = itemIndexOfUri(uri);
                qb.setTables(TABLE_ITEMS);
                qb.appendWhere(KEY_ITEM_NOTE_ID + "=" + noteId + " AND " +
                               KEY_ITEM_INDEX + "=" + index);
                break;
            }

            default:
                throw new IllegalArgumentException("Unsupported URI in query(): <" + uri + ">!");
        }
        Cursor c = qb.query(mDatabase, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /** {@inheritDoc} */
    @NotNull @Override
    public synchronized Uri insert(@NotNull Uri uri,
                                   @Nullable ContentValues values)
    {
        if (values == null) values = new ContentValues();
        switch (uriMatcher.match(uri)) {
            case URI_NOTES_ALL: {
                values.remove(KEY_NOTE_ID);
                values.remove(KEY_NOTE_ORDER_EDITED);
                values.remove(KEY_NOTE_ORDER_VIEWED);
                String title = values.getAsString(KEY_NOTE_TITLE);
                if (TextUtils.isEmpty(title) || title.matches("\\s+")) {
                    values.remove(KEY_NOTE_TITLE);
                }

                int noteId;
                try {
                    mDatabase.beginTransaction();

                    noteId = (int) mDatabase.insert(TABLE_NOTES, KEY_NOTE_TITLE, values);
                    if (noteId < 0) {
                        throw new SQLException("Failed to insert() row for <" + uri + ">!");
                    }

                    mDatabase.setTransactionSuccessful();
                } finally {
                    mDatabase.endTransaction();
                }

                touchNoteAndNotify(noteId, KEY_NOTE_ORDER_EDITED);
                return singleNoteUri(noteId);
            }
            case URI_NOTES_SINGLE:
            case URI_ITEMS_ALL:
            case URI_ITEMS_SINGLE: {
                // determine insertion index...
                int noteId = noteIdOfUri(uri);
                if (!existsNote(noteId))
                    throw new IllegalArgumentException("Invalid note ID in URI <" + uri + ">!");
                Integer itemIndex = itemIndexOfUri(uri);
                int itemCount = itemCount(noteId);
                if (itemIndex == null) itemIndex = values.getAsInteger(KEY_ITEM_INDEX);
                if (itemIndex == null) itemIndex = itemCount;
                itemIndex = Math.min(Math.max(itemIndex, 0), itemCount);

                try {
                    mDatabase.beginTransaction();

                    // update existing items "order" attributes if inserting at a position other
                    // than the end of the note's list of items...
                    if (itemIndex < itemCount) {
                        mDatabase.execSQL("UPDATE " + TABLE_ITEMS + " " +
                                          "SET " + KEY_ITEM_INDEX + "=" + KEY_ITEM_INDEX + "+1 " +
                                          "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + " AND " +
                                              KEY_ITEM_INDEX + ">=" + itemIndex + ";");
                    }

                    // insert new item with "order" 0...
                    values.put(KEY_ITEM_NOTE_ID, noteId);
                    values.put(KEY_ITEM_INDEX, itemIndex);
                    String text = values.getAsString(KEY_ITEM_TEXT);
                    if (TextUtils.isEmpty(text) || text.matches("\\s+")) {
                        values.remove(KEY_ITEM_TEXT);
                    }
                    long rowId = mDatabase.insert(TABLE_ITEMS, KEY_ITEM_TEXT, values);
                    if (rowId < 0) {
                        throw new SQLException("Failed to insert() row for <" + uri + ">!");
                    }

                    mDatabase.setTransactionSuccessful();
                } finally {
                    mDatabase.endTransaction();
                }
                touchNoteAndNotify(noteId, KEY_NOTE_ORDER_EDITED);
                return singleItemUri(noteId, itemIndex);
            }

            default:
                throw new IllegalArgumentException("Unsupported URI in insert(): <" + uri + ">!");
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized int delete(@NotNull Uri uri,
                                   @Nullable String selection,
                                   @Nullable String[] selectionArgs)
    {
        int count;
        switch (uriMatcher.match(uri)) {
            case URI_NOTES_SINGLE: {
                try {
                    mDatabase.beginTransaction();

                    // delete note's items...
                    int noteId = noteIdOfUri(uri);
                    mDatabase.delete(TABLE_ITEMS, KEY_ITEM_NOTE_ID + "=" + noteId, null);

                    // delete note...
                    count = mDatabase.delete(TABLE_NOTES, KEY_NOTE_ID + "=" + noteId, null);
                    if (count == 0) {
                        throw new SQLException("Element does not exist for URI <" + uri + ">!");
                    }

                    mDatabase.setTransactionSuccessful();
                } finally {
                    mDatabase.endTransaction();
                }

                getContext().getContentResolver().notifyChange(notesUri(), null);
                break;
            }
            case URI_ITEMS_SINGLE: {
                int noteId = noteIdOfUri(uri);
                int index = itemIndexOfUri(uri);

                try {
                    mDatabase.beginTransaction();

                    // delete item...
                    count =
                        mDatabase.delete(
                            TABLE_ITEMS,
                            KEY_ITEM_NOTE_ID + "=" + noteId + " AND " + KEY_ITEM_INDEX + "=" +
                                index,
                            null);
                    if (count == 0) {
                        throw new SQLException("Element does not exist for URI <" + uri + ">!");
                    }

                    // update indices of the remaining items...
                    mDatabase.execSQL("UPDATE " + TABLE_ITEMS + " " +
                                      "SET " + KEY_ITEM_INDEX + "=" + KEY_ITEM_INDEX + "-1 " +
                                      "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + " AND " +
                                          KEY_ITEM_INDEX + ">" + index + ";");

                    mDatabase.setTransactionSuccessful();
                } finally {
                    mDatabase.endTransaction();
                }

                touchNoteAndNotify(noteId, KEY_NOTE_ORDER_EDITED);
                break;
            }

            default:
                throw new IllegalArgumentException("Unsupported URI in delete(): <" + uri + ">!");
        }
        return count;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized int update(@NotNull Uri uri,
                                   @Nullable ContentValues values,
                                   @Nullable String selection,
                                   @Nullable String[] selectionArgs)
    {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case URI_NOTES_SINGLE: {
                int noteId = noteIdOfUri(uri);
                if (values == null) break;
                values.put(KEY_NOTE_ID, noteId);
                values.remove(KEY_NOTE_ORDER_EDITED);
                values.remove(KEY_NOTE_ORDER_VIEWED);
                String title = values.getAsString(KEY_NOTE_TITLE);
                if (TextUtils.isEmpty(title) || title.matches("\\s+")) {
                    values.remove(KEY_NOTE_TITLE);
                }

                try {
                    mDatabase.beginTransaction();

                    count = mDatabase.update(TABLE_NOTES, values, KEY_NOTE_ID + "=" + noteId, null);

                    mDatabase.setTransactionSuccessful();
                } finally {
                    mDatabase.endTransaction();
                }

                if (count > 0) touchNoteAndNotify(noteId, KEY_NOTE_ORDER_EDITED);
                break;
            }
            case URI_ITEMS_ALL_SORT_ALPHA:
            case URI_ITEMS_ALL_SORT_CHECKED:
            case URI_ITEMS_ALL_REVERSE:
            case URI_ITEMS_ALL_CLEAR: {
                int noteId = noteIdOfUri(uri);

                try {
                    mDatabase.beginTransaction();

                    // apply transformation...
                    if (isSortItemsAlphabeticallyUri(uri)) {
                        sortItems(noteId, SORT_ALPHA);
                    } else if (isSortItemsByCheckedUri(uri)) {
                        sortItems(noteId, SORT_CHECKED);
                    } else if (isReverseItemsUri(uri)) {
                        int itemCount = itemCount(noteId);
                        mDatabase.execSQL("UPDATE " + TABLE_ITEMS + " " +
                                          "SET " + KEY_ITEM_INDEX + "=" + (itemCount - 1) + "-" +
                                              KEY_ITEM_INDEX + " " +
                                          "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + ";");
                    } else if (isClearItemsUri(uri)) {
                        mDatabase.execSQL("DELETE FROM " + TABLE_ITEMS + " " +
                                          "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + ";");
                    }

                    mDatabase.setTransactionSuccessful();
                } finally {
                    mDatabase.endTransaction();
                }

                count = 1;
                touchNoteAndNotify(noteId, KEY_NOTE_ORDER_EDITED);
                break;
            }
            case URI_ITEMS_SINGLE:
            case URI_ITEMS_SINGLE_MOVE: {
                int noteId = noteIdOfUri(uri);
                int index = itemIndexOfUri(uri);

                try {
                    mDatabase.beginTransaction();

                    // update the item's data...
                    if (values != null) {
                        values.put(KEY_ITEM_NOTE_ID, noteId);
                        values.put(KEY_ITEM_INDEX, index);
                        values.put(KEY_ITEM_HEIGHT, (Integer) null);
                        String text = values.getAsString(KEY_ITEM_TEXT);
                        if (TextUtils.isEmpty(text) || text.matches("\\s+")) {
                            values.remove(KEY_ITEM_TEXT);
                        }
                        count =
                            mDatabase.update(
                                TABLE_ITEMS,
                                values,
                                KEY_ITEM_NOTE_ID + "=" + noteId + " AND " + KEY_ITEM_INDEX + "=" +
                                    index,
                                null);
                    }

                    // move the item, if requested by the URI...
                    if (isMoveItemUri(uri)) {
                        Integer newIndex = itemNewIndexOfUri(uri);
                        if (index != newIndex && changeItemIndex(noteId, index, newIndex) != null) {
                            count = 1;
                        }
                    }

                    mDatabase.setTransactionSuccessful();
                } finally {
                    mDatabase.endTransaction();
                }

                touchNoteAndNotify(noteId, KEY_NOTE_ORDER_EDITED);
                break;
            }
            case URI_ITEMS_SINGLE_HEIGHT: {
                int noteId = noteIdOfUri(uri);
                int index = itemIndexOfUri(uri);
                Integer height = (values != null ? values.getAsInteger(KEY_ITEM_HEIGHT) : null);

                try {
                    mDatabase.beginTransaction();

                    // update height field without notifying...
                    mDatabase.execSQL("UPDATE " + TABLE_ITEMS + " " +
                                      "SET " + KEY_ITEM_HEIGHT + "=" +
                                          (height != null ? height : "NULL") + " " +
                                      "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + " AND " +
                                          KEY_ITEM_INDEX + "=" + index + ";");
                    count = 1;

                    mDatabase.setTransactionSuccessful();
                } finally {
                    mDatabase.endTransaction();
                }
                break;
            }

            default:
                throw new IllegalArgumentException("Unsupported URI in update(): <" + uri + ">!");
        }
        return count;
    }

    // own methods...

    /**
     * Returns whether a note with a given ID exists.
     *
     * @param noteId the note's ID
     * @return <code>true</code> if the note exists, <code>false</code> otherwise
     *
     * @throws SQLException on errors accessing the database
     */
    protected boolean existsNote(int noteId) throws SQLException {
        Cursor cursor = mDatabase.query(TABLE_NOTES,
                                        new String[] { KEY_NOTE_ID },
                                        KEY_NOTE_ID + "=" + noteId, null,
                                        null, null, null);
        boolean exists = cursor.moveToNext();
        cursor.close();
        return exists;
    }

    /**
     * Returns the number of icons of a given note.
     *
     * @param noteId the note's ID
     * @return the note's number of items
     *
     * @throws SQLException on errors accessing the database
     */
    protected int itemCount(int noteId) throws SQLException {
        Cursor cursor = mDatabase.query(TABLE_ITEMS,
                                        new String[] { KEY_ITEM_INDEX },
                                        KEY_ITEM_NOTE_ID + "=" + noteId, null,
                                        null, null, null);
        int n = cursor.getCount();
        cursor.close();
        return n;
    }

    /**
     * "Touches" a note, making it "first" for a given order field (when sorting in descending
     * order) and notifies about a single-note modification via the content resolver.  (The
     * notification is independent of whether or not the order field was updated.)
     *
     * @param noteId     the ID of the note to touch
     * @param orderField the order field in question (one of the <code>KEY_NOTE_ORDER_*</code>
     *                   constants)
     *
     * @throws SQLException on errors accessing the database
     */
    protected void touchNoteAndNotify(int noteId,
                                      @NotNull String orderField)
        throws SQLException
    {
        updateOrderIfNecessary(noteId, orderField);
        getContext().getContentResolver().notifyChange(singleNoteUri(noteId), null);
    }

    /**
     * Updates one of the order columns in table {@link #TABLE_NOTES} for a given note only if
     * necessary, i.e., the note doesn't already have the highest value.  Performs its database
     * operations transacted.
     *
     * @param noteId     the note's ID
     * @param orderField the order field in question (one of the <code>KEY_NOTE_ORDER_*</code>
     *                   constants)
     * @return <code>true</code> if an update was necessary and thus performed, <code>false</code>
     *         otherwise
     *
     * @throws SQLException on errors accessing the database
     */
    protected boolean updateOrderIfNecessary(int noteId,
                                             @NotNull String orderField)
        throws SQLException
    {
        try {
            mDatabase.beginTransaction();

            // check whether the note is already the last-edited one...
            boolean doUpdate = false;
            Cursor cursor = mDatabase.query(TABLE_NOTES,
                                            new String[]{ KEY_NOTE_ID },
                                            null, null, null, null,
                                            orderField + " DESC",
                                            "1");
            if (cursor != null) {
                doUpdate = !(cursor.moveToNext() &&
                             noteId == cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTE_ID)));
                cursor.close();
            }

            // touch the note if not the last-edited one...
            if (doUpdate) {
                mDatabase.execSQL(
                    "UPDATE " + TABLE_NOTES + " " +
                    "SET " + orderField + "=1+" +
                        "(SELECT MAX(" + orderField + ") FROM " + TABLE_NOTES + ") " +
                    "WHERE " + KEY_NOTE_ID + "=" + noteId + ";");
            }

            mDatabase.setTransactionSuccessful();
            return doUpdate;
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * Changes an item's index.  Bounds <code>newIndex</code> to within the valid range.  Performs
     * its database operations transacted.
     *
     * @param noteId   the item's note's ID
     * @param index    the item's index
     * @param newIndex the item's new index
     * @return the item's new URI
     *
     * @throws SQLException on errors accessing the database
     */
    @NotNull
    protected Uri changeItemIndex(int noteId,
                                  int index,
                                  int newIndex)
        throws SQLException
    {
        // gather data, validate...
        newIndex = Math.max(Math.min(newIndex, itemCount(noteId) - 1), 0);
        if (index == newIndex) return singleItemUri(noteId, index);
        try {
            mDatabase.beginTransaction();

            // change the item's index to a temporary value...
            mDatabase.execSQL("UPDATE " + TABLE_ITEMS + " " +
                              "SET " + KEY_ITEM_INDEX + "=-1 " +
                              "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + " AND " +
                                  KEY_ITEM_INDEX + "=" + index + ";");

            // shift items in between the two indices...
            String cond;
            int shift;
            if (newIndex > index) {
                cond = KEY_ITEM_INDEX + ">" + index + " AND " + KEY_ITEM_INDEX + "<=" + newIndex;
                shift = -1;
            } else {
                cond = KEY_ITEM_INDEX + ">=" + newIndex + " AND " + KEY_ITEM_INDEX + "<" + index;
                shift = 1;
            }
            mDatabase.execSQL("UPDATE " + TABLE_ITEMS + " " +
                              "SET " + KEY_ITEM_INDEX + "=" + KEY_ITEM_INDEX + "+(" + shift + ") " +
                              "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + " AND " + cond + ";");

            // update the item's index...
            mDatabase.execSQL("UPDATE " + TABLE_ITEMS + " " +
                              "SET " + KEY_ITEM_INDEX + "=" + newIndex + " " +
                              "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + " AND " +
                                  KEY_ITEM_INDEX + "=-1;");

            mDatabase.setTransactionSuccessful();
            return singleItemUri(noteId, newIndex);
        } catch (Throwable t) {
            Log.d(mLogTag,
                  "Exception changing index of item #" + index + " of note #" + noteId + " to " +
                      newIndex + "!",
                  t);
        } finally {
            mDatabase.endTransaction();
        }

        return singleItemUri(noteId, index);
    }

    /**
     * Sorts the items of a note.  Performs its database operations transacted.
     *
     * @param noteId the ID of the note whose items to sort
     * @param type   the sort criterion, one of the <code>SORT_*</code> constants
     *
     * @throws IllegalArgumentException if <code>type</code> is not a valid type constant
     * @throws SQLException             on errors accessing the database
     */
    protected void sortItems(int noteId,
                             int type)
        throws IllegalArgumentException, SQLException
    {
        try {
            mDatabase.beginTransaction();

            // fetch items...
            List<Item> items = new ArrayList<Item>();
            Cursor cursor =
                mDatabase.query(TABLE_ITEMS,
                                null,
                                KEY_ITEM_NOTE_ID + "=" + noteId, null,
                                null, null, KEY_ITEM_INDEX);
            if (cursor != null) {
                while (cursor.moveToNext()) items.add(NotesUtils.getItem(cursor));
                cursor.close();
            }

            // sort items...
            if (type == SORT_ALPHA) {
                Collections.sort(items, new Comparator<Item>() {
                    @Override
                    public int compare(@NotNull Item i1,
                                       @NotNull Item i2)
                    {
                        String t1 = i1.getText();
                        String t2 = i2.getText();
                        String s1 = TextUtils.isEmpty(t1) ? "" : t1;
                        String s2 = TextUtils.isEmpty(t2) ? "" : t2;
                        return s1.compareToIgnoreCase(s2);
                    }
                });
            } else if (type == SORT_CHECKED) {
                Collections.sort(items, new Comparator<Item>() {
                    @Override
                    public int compare(@NotNull Item i1,
                                       @NotNull Item i2)
                    {
                        int[] checked = { i1.getChecked(), i2.getChecked() };
                        for (int i = 0; i < 2; i++) {
                            checked[i] = checked[i] == ITEM_CHECKED_UNCHECKED ? 0 :
                                         checked[i] == ITEM_CHECKED_CHECKED   ? 1 : 2;
                        }
                        int res = Integer.valueOf(checked[0]).compareTo(checked[1]);
                        if (res == 0) {
                            return Integer.valueOf(i1.getIndex()).compareTo(i2.getIndex());
                        }
                        return res;
                    }
                });
            } else {
                throw new IllegalArgumentException("Illegal sort type: " + type + "!");
            }

            // change indices into temporary negative ones...
            mDatabase.execSQL("UPDATE " + TABLE_ITEMS + " " +
                              "SET " + KEY_ITEM_INDEX + "=-1-" + KEY_ITEM_INDEX + " " +
                              "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + ";");

            // update indices...
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                mDatabase.execSQL("UPDATE " + TABLE_ITEMS + " " +
                                  "SET " + KEY_ITEM_INDEX + "=" + i + " " +
                                  "WHERE " + KEY_ITEM_NOTE_ID + "=" + noteId + " AND " +
                                      KEY_ITEM_INDEX + "=" + (-1 - item.getIndex()) + ";");
            }

            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }
}

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

package com.nookdevs.notes.data;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * <p>A {@link ListItemsProvider} retrieving data via a {@link android.database.Cursor}.</p>
 *
 * <p>Sub-classes need to start data acquisition by calling {@link #requery()} from their
 * constructor.</p>
 *
 * @author Marco Goetze
 */
public abstract class CursorBasedListItemsProvider<T extends ListItem>
    extends AbstractListItemsProvider<T>
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    /** The activity using the provider. */
    @NotNull protected final Activity mActivity;
    /** The URI of the data provided by the list items provider. */
    @NotNull protected final Uri mUri;

    /** The cursor via which the data is accessed (may be <code>null</code>). */
    // NOTE: Intentionally not @NotNull/@Nullable-annotated
    private Cursor mCursor;
    /** Content observer use with the cursor. */
    // NOTE: Intentionally not @NotNull/@Nullable-annotated
    private ContentObserver mContentObserver;

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates a {@link CursorBasedListItemsProvider}.
     *
     * @param activity the activity using the provider
     * @param uri      the URI of the data provided by the list items provider
     */
    protected CursorBasedListItemsProvider(@NotNull Activity activity,
                                           @NotNull Uri uri)
    {
        mActivity = activity;
        mUri = uri;

        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                requery();
            }
        };
    }

    // inherited methods...

    /** {@inheritDoc} */
    @Override
    public int getItemCount() {
        return (mCursor != null ? mCursor.getCount() : 0);
    }

    /** {@inheritDoc} */
    @NotNull @Override
    public T getItem(int idx) throws IndexOutOfBoundsException {
        if (mCursor == null || idx < 0 || idx >= getItemCount()) {
            throw new IndexOutOfBoundsException("Invalid item index: " + idx + "!");
        }
        mCursor.moveToPosition(idx);
        return getItem(mCursor);
    }

    // own methods...

    /**
     * Causes the provider's data to be (re)queried.
     */
    public void requery() {
        requery(false);
    }

    //................................................................................... internals

    /**
     * Causes the provider's data to be (re)queried, optional creating a new cursor via
     * {@link #query()}.  The latter makes sense if parameters for the cursor's creation have
     * changed in a subclass since the cursor was originally retrieved.
     *
     * @param newCursor <code>true</code> to create a new cursor, <code>false</code> otherwise
     */
    protected void requery(boolean newCursor) {
        if (mCursor != null && !newCursor) {
            // simply requery...
            mCursor.requery();
        } else {
            // close current cursor, if any...
            if (mCursor != null) {
                mCursor.unregisterContentObserver(mContentObserver);
                mActivity.stopManagingCursor(mCursor);
            }

            // create new cursor...
            mCursor = query();

            // set up observation...
            if (mCursor != null) {
                mActivity.startManagingCursor(mCursor);
                mCursor.registerContentObserver(mContentObserver);
            }
        }

        // notify listeners...
        fireListChanged();

    }

    /**
     * Performs a query for the data provided, yielding a cursor. Extension point.
     *
     * @return a cursor (may be <code>null</code> if there was an error)
     */
    @Nullable
    protected Cursor query() {
        return mActivity.getContentResolver().query(mUri, null, null, null, null);
    }

    /**
     * Returns an item given a cursor, i.e., turns the data at its current position into an
     * instance of the class's generic type.
     *
     * @param cursor the cursor
     * @return an item corresponding to the cursor's current position
     */
    @NotNull
    protected abstract T getItem(@NotNull Cursor cursor);
}

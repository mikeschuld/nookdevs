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
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.nookdevs.notes.R;
import com.nookdevs.notes.data.ListItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.Notes.ITEM_CHECKED_CHECKED;
import static com.nookdevs.notes.provider.Notes.ITEM_CHECKED_UNCHECKED;


/**
 * Immutable class representing a note as managed by the application.
 *
 * @author Marco Goetze
 */
public class Note implements ListItem
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    /**
     * The note's numerical ID (<code>null</code> if not stored, non-<code>null</code> otherwise).
     */
    @Nullable private final Integer mId;
    /** The note's title (may be <code>null</code>). */
    @Nullable
    private final String mTitle;

    /** List of items representing the note's contents (may be <code>null</code>). */
    @NotNull private final List<Item> mItems = new ArrayList<Item>();

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates a {@link Note} with an ID and given contents.
     *
     * @param id    the note's ID (may be <code>null</code>)
     * @param title the note's title (may be <code>null</code>)
     * @param items list of items representing the note's contents (may be <code>null</code>)
     */
    public Note(@Nullable Integer id,
                @Nullable String title,
                @Nullable List<Item> items)
    {
        mId = id;
        mTitle = title;
        if (items != null) mItems.addAll(items);
    }

    /**
     * Creates a ID-less {@link Note} with given contents.
     *
     * @param title the note's title (may be <code>null</code>)
     * @param items list of items representing the note's contents (may be <code>null</code>)
     */
    public Note(@Nullable String title,
                @Nullable List<Item> items)
    {
        this(null, title, items);
    }

    // inherited methods...

    /** {@inheritDoc} */
    @NotNull @Override
    public String toString() {
        return (mTitle != null ? mTitle : super.toString());
    }

    //.......................................................................... interface ListItem

    /** {@inheritDoc} */
    @Nullable @Override
    public Integer getId() {
        return mId;
    }

    /** {@inheritDoc} */
    @NotNull @Override
    public String getListTitle(@NotNull Context context) {
        return (mTitle != null ? mTitle : context.getString(R.string.note_title_untitled));
    }

    /** {@inheritDoc} */
    @Nullable @Override
    public String getListSubTitle(@NotNull Context context) {
        // regular sub-title...
        int itemsCount = getItemCount();
        String s = String.valueOf(itemsCount);

        // render as "m+n items" if there are both checkable and uncheckable items...
        Map<Integer,Integer> counts = NotesUtils.checkedCounts(this);
        int checked =
            counts.containsKey(ITEM_CHECKED_CHECKED) ? counts.get(ITEM_CHECKED_CHECKED) : 0;
        int unchecked =
            counts.containsKey(ITEM_CHECKED_UNCHECKED) ? counts.get(ITEM_CHECKED_UNCHECKED) : 0;
        int checkable = unchecked + checked;
        if (checkable > 0 && checkable < itemsCount) s = checkable + "+" + (itemsCount - checkable);

        return context.getResources().getQuantityString(R.plurals.note_subtitle, itemsCount, s);
    }

    // own method...

    /**
     * Returns the note's title.
     *
     * @return the note's title (may be <code>null</code>)
     */
    @Nullable
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the note's items.
     *
     * @return the note's items (an immutable list optimized for array-based access)
     */
    @NotNull
    public List<Item> getItems() {
        return Collections.unmodifiableList(mItems);
    }

    /**
     * Returns the number of items.
     *
     * @return the number of items
     */
    public int getItemCount() {
        return mItems.size();
    }
}

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

import android.content.Context;

import com.nookdevs.notes.data.ListItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Immutable class representing an item of a <code>Note</code>.
 *
 * @author Marco Goetze
 */
public class Item implements ListItem
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    /** The item's index in its note's list of items (may be <code>null</code>). */
    @Nullable private final Integer mIndex;
    /** The item's text (may be <code>null</code>). */
    @Nullable private final String mText;
    /** The item's "checked" attribute. */
    private final int mChecked;
    /** The item's cached display height (may be <code>null</code>). */
    @Nullable private final Integer mHeight;

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates an {@link Item} with a given index and text.
     *
     * @param index   the item's index in its note's list of items (may be <code>null</code>)
     * @param text    the item's text (may be <code>null</code>)
     * @param checked the item's "checked" attribute, one of the <code>Notes.ITEM_CHECKED_*</code>
     *                constants
     * @param height  the item's cached display height
     */
    public Item(@Nullable Integer index,
                @Nullable String text,
                int checked,
                @Nullable Integer height)
    {
        mIndex = index;
        mText = text;
        mChecked = Math.min(Math.max(checked, 0), 2);
        mHeight = height;
    }

    /**
     * Creates an index-less {@link Item} with a given text and checked state.
     *
     * @param text    the item's text (may be <code>null</code>)
     * @param checked the item's "checked" attribute, one of the <code>Notes.ITEM_CHECKED_*</code>
     *                constants
     */
    public Item(@Nullable String text,
                int checked)
    {
        this(null, text, checked, null);
    }

    // inherited methods...

    /** {@inheritDoc} */
    @NotNull @Override
    public String toString() {
        return (mText != null ? mText : super.toString());
    }

    //.......................................................................... interface ListItem

    /**
     * {@inheritDoc}
     *
     * <p>Class {@link com.nookdevs.notes.provider.Item}'s implementation returns the item's index,
     * which may be <code>null</code>.</p>
     */
    @Nullable @Override
    public Integer getId() {
        return mIndex;
    }

    /** {@inheritDoc} */
    @NotNull @Override
    public String getListTitle(@NotNull Context context) {
        return toString();
    }

    /** {@inheritDoc} */
    @Nullable @Override
    public String getListSubTitle(@NotNull Context context) {
        return null;
    }

    // own method...

    /**
     * Return the item's index in its note's list of items.
     *
     * @return the item's index in its note's list of items (may be <code>null</code>)
     */
    @Nullable
    public Integer getIndex() {
        return mIndex;
    }

    /**
     * Returns the item's text.
     *
     * @return the item's text (may be <code>null</code>)
     */
    @Nullable
    public String getText() {
        return mText;
    }

    /**
     * Returns the item's "checked" attribute, one of the <code>Notes.ITEM_CHECKED_*</code>
     * constants.
     *
     * @return the item's "checked" attribute
     */
    public int getChecked() {
        return mChecked;
    }

    /**
     * Returns the item's cached display height.
     *
     * @return the item's cached display height
     */
    @Nullable
    public Integer getHeight() {
        return mHeight;
    }
}

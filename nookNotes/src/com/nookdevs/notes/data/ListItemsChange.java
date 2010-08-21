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

import java.util.EventObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Event object for changes to list of {@link com.nookdevs.notes.data.ListItem}s.
 *
 * @param <T> the type of list items
 *
 * @author Marco Goetze
 */
@SuppressWarnings({ "UnusedDeclaration" })
public class ListItemsChange<T extends ListItem> extends EventObject
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //............................................................................ public constants

    /** Type constant indicating that a single item has changed. */
    public static final int TYPE_ITEM_CHANGED = 0;
    /** Type constant indicating that the entire list has changed in some way. */
    public static final int TYPE_LIST_CHANGED = 1;

    //................................................................................... internals

    /** Version UID for serialization purposes. */
    private static final long serialVersionUID = 3648966355820056347L;

    /** The type of event (one of the <code>TYPE_*</code> constants). */
    protected final int mType;

    /**
     * The changed index (<code>null</code> if {@link #mType} <code>==</code>
     *  {@link #TYPE_LIST_CHANGED}).
     */
    @Nullable
    protected final Integer mChangedIndex;
    /**
     * The changed item (<code>null</code> if {@link #mType} <code>==</code>
     * {@link #TYPE_LIST_CHANGED}).
     */
    @Nullable
    protected final T mChangedItem;

    ////////////////////////////////////////// METHODS ////////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates a {@link ListItemsChange} for a change of a single item.
     *
     * @param source       the source (the items provider)
     * @param changedIndex the changed index
     * @param changedItem  the changed item
     *
     * @throws NullPointerException if <code>source</code> or <code>changedItem</code> is
     *                              <code>null</code>
     */
    public ListItemsChange(@NotNull Object source,
                           int changedIndex,
                           @NotNull T changedItem)
        throws NullPointerException
    {
        super(source);
        //noinspection ConstantConditions
        if (source == null || changedItem == null) throw new NullPointerException();
        mType = TYPE_ITEM_CHANGED;
        mChangedIndex = changedIndex;
        mChangedItem = changedItem;
    }

    /**
     * Creates a {@link ListItemsChange} for a change of the entire list.
     *
     * @param source the source (the items provider)
     *
     * @throws NullPointerException if <code>source</code> is <code>null</code>
     */
    public ListItemsChange(@NotNull Object source) throws NullPointerException {
        super(source);
        //noinspection ConstantConditions
        if (source == null) throw new NullPointerException();
        mType = TYPE_LIST_CHANGED;
        mChangedIndex = null;
        mChangedItem = null;
    }

    // own methods...

    /**
     * Returns the event's type (a <code>TYPE_*</code> constant).
     *
     * @return the event's type
     */
    public int getType() {
        return mType;
    }

    /**
     * Returns the changed item's index, if the event's type is {@link #TYPE_ITEM_CHANGED}.
     *
     * @return the changed item's index, or <code>null</code>
     *
     * @see #getType()
     */
    @Nullable
    public Integer getChangedIndex() {
        return mChangedIndex;
    }

    /**
     * Returns the changed item, if the event's type is {@link #TYPE_ITEM_CHANGED}.
     *
     * @return the changed item, or <code>null</code>
     *
     * @see #getType()
     */
    @Nullable
    public T getChangedItem() {
        return mChangedItem;
    }
}

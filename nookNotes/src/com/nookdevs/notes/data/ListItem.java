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

import android.content.Context;

import org.jetbrains.annotations.Nullable;


/**
 * Interface for list items provided by <code>ListItemsProvider</code>s and displayed by
 * <code>ListViewHelper</code>s.
 *
 * @author Marco Goetze
 */
public interface ListItem
{
    //////////////////////////////////////////// METHODS ////////////////////////////////////////////

    /**
     * Returns the item's numerical ID, if any.
     *
     * @return the item's numerical ID (may be <code>null</code>)
     */
    @Nullable
    Integer getId();

    /**
     * Returns a title to be used for displaying the item in a graphical list.
     *
     * @param context the Android context (may be required for resource look-up)
     * @return a title to be used for displaying the item in a graphical list (may be
     *         <code>null</code>)
     */
    @Nullable
    String getListTitle(Context context);

    /**
     * Returns a sub-title to be used for displaying the item in a graphical list.
     *
     * @param context the Android context (may be required for resource look-up)
     * @return a sub-title to be used for displaying the item in a graphical list (may be
     *         <code>null</code>)
     */
    @Nullable
    String getListSubTitle(Context context);
}

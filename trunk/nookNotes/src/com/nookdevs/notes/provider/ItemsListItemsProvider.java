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

import static com.nookdevs.notes.provider.NotesUris.itemsUri;


/**
 * Class adapting a {@link Notes}-type content provider to the
 * {@link com.nookdevs.notes.data.ListItemsProvider} interface for
 * {@link Item}s.
 *
 * @author Marco Goetze
 */
public class ItemsListItemsProvider extends CursorBasedListItemsProvider<Item>
{
    ////////////////////////////////////////// METHODS ////////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates a {@link ItemsListItemsProvider}.
     *
     * @param activity the activity using the provider
     * @param noteId   the ID of the note whose items to provide
     */
    public ItemsListItemsProvider(@NotNull Activity activity,
                                  int noteId)
    {
        super(activity, itemsUri(noteId));

        requery();
    }

    // inherited methods...

    /** {@inheritDoc} */
    @NotNull @Override
    protected Item getItem(@NotNull Cursor cursor) {
        return NotesUtils.getItem(cursor);
    }
}

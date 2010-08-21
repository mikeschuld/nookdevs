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

import org.jetbrains.annotations.NotNull;


/**
 * Interface for entities dealing with {@link com.nookdevs.notes.data.ListItem}s as provided by
 * a {@link ListItemsProvider}.  Defines methods for change notifications.
 *
 * @param <T> the type of list item
 *
 * @author Marco Goetze
 */
public interface ListItemsClient<T extends ListItem>
{
    ////////////////////////////////////////// METHODS ////////////////////////////////////////////

    // own methods...

    /**
     * Notifies that a list item at a given index has changed.  Implementors should internally
     * replace the item previously associated with the index with the one given, which should in
     * turn be the same as the provider would yield for the index.
     *
     * @param ev the event describing the change
     */
    void itemChanged(@NotNull ListItemsChange<T> ev);

    /**
     * Notifies that the list has changed in ways mor complex than just concerning a single item.
     * Implementors should re-retrieve their list items data.
     *
     * @param ev the event describing the change
     */
    void listChanged(@NotNull ListItemsChange<T> ev);
}

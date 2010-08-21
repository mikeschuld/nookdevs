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
 * Interface for providers of {@link com.nookdevs.notes.data.ListItem}s managed in lists,
 * accessible by index.
 *
 * @param <T> the type of items provided
 *
 * @author Marco Goetze
 */
@SuppressWarnings({ "UnusedDeclaration" })
public interface ListItemsProvider<T extends ListItem>
{
    ////////////////////////////////////////// METHODS ////////////////////////////////////////////

    //............................................................................. access to items

    /**
     * Returns the number of items.
     *
     * @return the number of items
     */
    int getItemCount();

    /**
     * Returns a particular item by index.
     *
     * @param idx the index
     * @return the corresponding item
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    @NotNull
    T getItem(int idx) throws IndexOutOfBoundsException;

    //................................................................................... listeners

    /**
     * Registers a {@link ListItemsClient}.
     *
     * @param client the client to register
     */
    void addListItemsClient(@NotNull ListItemsClient<T> client);

    /**
     * Unregisters a {@link ListItemsClient}.
     *
     * @param client the client to unregister
     */
    void removeListItemsClient(@NotNull ListItemsClient<T> client);
}

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

import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;


/**
 * Abstract implementation of the {@link ListItemsProvider} interface, providing implementations of
 * the listener registration methods.
 *
 * @author Marco Goetze
 */
@SuppressWarnings({ "UnusedDeclaration" })
public abstract class AbstractListItemsProvider<T extends ListItem> implements ListItemsProvider<T>
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    /** List of registered {@link ListItemsClient}s. */
    @NotNull protected final List<ListItemsClient<T>> mClients = new LinkedList<ListItemsClient<T>>();

    ////////////////////////////////////////// METHODS ////////////////////////////////////////////

    // inherited methods...

    //................................................................. interface ListItemsProvider

    /** {@inheritDoc} */
    @Override
    public void addListItemsClient(@NotNull ListItemsClient<T> client) {
        synchronized (mClients) {
            mClients.add(client);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeListItemsClient(@NotNull ListItemsClient<T> client) {
        synchronized (mClients) {
            mClients.remove(client);
        }
    }

    // own methods...

    /**
     * Notifies registered {@link ListItemsClient}s of a change to a single item.
     *
     * @param idx  the item's index
     * @param item the (new) item
     */
    protected void fireItemChanged(int idx,
                                   @NotNull T item)
    {
        synchronized (mClients) {
            ListItemsChange<T> ev = null;  // lazily-initialized
            for (ListItemsClient<T> client : mClients) {
                if (ev == null) ev = new ListItemsChange<T>(this, idx, item);
                client.itemChanged(ev);
            }
        }
    }

    /**
     * Notifies registered {@link ListItemsClient}s of overall changes to the list.
     */
    protected void fireListChanged() {
        synchronized (mClients) {
            ListItemsChange<T> ev = null;  // lazily-initialized
            for (ListItemsClient<T> client : mClients) {
                if (ev == null) ev = new ListItemsChange<T>(this);
                client.listChanged(ev);
            }
        }
    }
}

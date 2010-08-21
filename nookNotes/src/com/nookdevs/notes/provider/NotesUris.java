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

import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Utility class for dealing with URIs of notes and notes' items.
 *
 * @author Marco Goetze
 */
@SuppressWarnings({ "UnusedDeclaration" })
public abstract class NotesUris
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //................................................................ access-restricted attributes

    /**
     * The content URI of the application's content and thus
     * {@link com.nookdevs.notes.provider.Notes} implementations.
     */
    @NotNull private static final Uri CONTENT_URI =
        Uri.parse("content://com.nookdevs.provider.notes/notes");

    /** URI matcher constant indicating that a URI refers to all notes. */
    static final int URI_NOTES_ALL = 1;
    /** URI matcher constant indicating that a URI refers to a single note. */
    static final int URI_NOTES_SINGLE = 2;
    /**
     * URI matcher constant indicating that a URI refers to a single note, and explicitly for
     * viewing purposes.
     */
    static final int URI_NOTES_SINGLE_VIEW = 3;
    /** URI matcher constant indicating that a URI refers to all of a note's items. */
    static final int URI_ITEMS_ALL = 4;
    /** URI matcher constant for URIs for sorting the items of a note alphabetically. */
    static final int URI_ITEMS_ALL_SORT_ALPHA = 5;
    /**
     * URI matcher constant for URIs for sorting the items of a note by their "checked" attribute.
     */
    static final int URI_ITEMS_ALL_SORT_CHECKED = 6;
    /** URI matcher constant for URIs for reversing the items of a note. */
    static final int URI_ITEMS_ALL_REVERSE = 7;
    /** URI matcher constant for URIs for clearing the items of a note. */
    static final int URI_ITEMS_ALL_CLEAR = 8;
    /** URI matcher constant indicating that a URI refers to a single of a note's items. */
    static final int URI_ITEMS_SINGLE = 9;
    /** URI matcher constant for URIs for moving an item of a note, i.e., changing its index. */
    static final int URI_ITEMS_SINGLE_MOVE = 10;

    /** A URI matcher matching URIs of the applications. */
    @NotNull static final UriMatcher uriMatcher;

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // constructors/destructors...

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes", URI_NOTES_ALL);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes/#", URI_NOTES_SINGLE);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes/#/view", URI_NOTES_SINGLE_VIEW);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes/#/items", URI_ITEMS_ALL);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes/#/items/sort_alpha",
                          URI_ITEMS_ALL_SORT_ALPHA);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes/#/items/sort_checked",
                          URI_ITEMS_ALL_SORT_CHECKED);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes/#/items/reverse", URI_ITEMS_ALL_REVERSE);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes/#/items/clear", URI_ITEMS_ALL_CLEAR);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes/#/items/#", URI_ITEMS_SINGLE);
        uriMatcher.addURI(CONTENT_URI.getHost(), "notes/#/items/#/move/#", URI_ITEMS_SINGLE_MOVE);
    }

    // own methods...

    /**
     * Returns whether a given URI is a notes URI.  Returns true also for more specific URIs, such
     * as those of single notes or items &mdash; checks using this set of methods should thus be
     * performed in most-specific to least-specific order.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is a notes URI, <code>false</code> otherwise
     */
    public static boolean isNotesUri(@Nullable Uri uri) {
        if (uri == null) return false;
        int res = uriMatcher.match(uri);
        return (res >= 1 && res <= 10);
    }

    /**
     * Returns whether a given URI is a URI for a single note.  Returns true also for more
     * specific URIs, such as those of items &mdash; checks using this set of methods should thus
     * be performed in most-specific to least-specific order.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is a single note's URI, <code>false</code> otherwise
     */
    public static boolean isSingleNoteUri(@Nullable Uri uri) {
        if (uri == null) return false;
        int res = uriMatcher.match(uri);
        return (res >= 2 && res <= 10);
    }

    /**
     * Returns whether a given URI is a URI for explicitly viewing a single note.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is a URI for viewing a single note, <code>false</code>
     *         otherwise
     */
    public static boolean isSingleNoteViewUri(@Nullable Uri uri) {
        return (uri != null && uriMatcher.match(uri) == URI_NOTES_SINGLE_VIEW);
    }

    /**
     * Returns whether a given URI is an items URI.  Returns true also for more specific URIs, such
     * as those of single items &mdash; checks using this set of methods should thus be performed
     * in most-specific to least-specific order.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is an items URI, <code>false</code> otherwise
     */
    public static boolean isItemsUri(@Nullable Uri uri) {
        if (uri == null) return false;
        int res = uriMatcher.match(uri);
        return (res >= 4 && res <= 10);
    }

    /**
     * Returns whether a given URI is an URI for sorting the items of a note alphabetically.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is for sorting items, <code>false</code> otherwise
     */
    public static boolean isSortItemsAlphabeticallyUri(@Nullable Uri uri) {
        return (uri != null && uriMatcher.match(uri) == URI_ITEMS_ALL_SORT_ALPHA);
    }

    /**
     * Returns whether a given URI is an URI for sorting the items of a note by their "checked"
     * attribute.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is for sorting items, <code>false</code> otherwise
     */
    public static boolean isSortItemsByCheckedUri(@Nullable Uri uri) {
        return (uri != null && uriMatcher.match(uri) == URI_ITEMS_ALL_SORT_CHECKED);
    }

    /**
     * Returns whether a given URI is an URI for reversing the items of a note.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is for reversing items, <code>false</code> otherwise
     */
    public static boolean isReverseItemsUri(@Nullable Uri uri) {
        return (uri != null && uriMatcher.match(uri) == URI_ITEMS_ALL_REVERSE);
    }

    /**
     * Returns whether a given URI is an URI for clearing the items of a note.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is for clearing items, <code>false</code> otherwise
     */
    public static boolean isClearItemsUri(@Nullable Uri uri) {
        return (uri != null && uriMatcher.match(uri) == URI_ITEMS_ALL_CLEAR);
    }

    /**
     * Returns whether a given URI is a URI of a single item &mdash; checks using this set of
     * methods should thus be performed in most-specific to least-specific order.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is a single item's URI, <code>false</code> otherwise
     */
    public static boolean isSingleItemUri(@Nullable Uri uri) {
        if (uri == null) return false;
        int res = uriMatcher.match(uri);
        return (res >= 9 && res <= 10);
    }

    /**
     * Returns whether a given URI is a URI for moving an item, i.e., changing its index.
     *
     * @param uri the URI in question (may be <code>null</code>)
     * @return <code>true</code> if the URI is for moving a note item, <code>false</code> otherwise
     */
    public static boolean isMoveItemUri(@Nullable Uri uri) {
        return (uri != null && uriMatcher.match(uri) == URI_ITEMS_SINGLE_MOVE);
    }

    /**
     * Returns a URI for all notes.
     *
     * @return a URI for all notes
     */
    @NotNull
    public static Uri notesUri() {
        return CONTENT_URI;
    }

    /**
     * Returns a URI for a single note.  Falls back to a URI for all notes if <code>noteId</code>
     * is lower than 0.
     *
     * @param noteId the ID of the note for which to return a URI
     * @return a URI for the note
     */
    @NotNull
    public static Uri singleNoteUri(int noteId) {
        if (noteId >= 0) return ContentUris.withAppendedId(notesUri(), noteId);
        return notesUri();
    }

    /**
     * Returns a URI for explicitly viewing a single note.  Falls back to a URI for all notes if
     * <code>noteId</code> is lower than 0.
     *
     * @param noteId the ID of the note for which to return a URI
     * @return a URI for the note
     */
    @NotNull
    public static Uri singleNoteViewUri(int noteId) {
        if (noteId >= 0) return Uri.withAppendedPath(singleNoteUri(noteId), "view");
        return notesUri();
    }

    /**
     * Returns a URI for a single note's items.  Falls back to a URI for all notes if
     * <code>noteId</code> is lower than 0.
     *
     * @param noteId the ID of the note for which to return an items URI
     * @return an items URI for the note
     */
    @NotNull
    public static Uri itemsUri(int noteId) {
        if (noteId >= 0) return Uri.withAppendedPath(singleNoteUri(noteId), "items");
        return notesUri();
    }

    /**
     * Returns a URI for sorting a note's items alphabetically.  Falls back to a URI for all notes
     * if <code>noteId</code> is lower than 0.
     *
     * @param noteId the ID of the note for which to return an items URI
     * @return an items URI for the note
     */
    @NotNull
    public static Uri sortItemsAlphabeticallyUri(int noteId) {
        if (noteId >= 0) return Uri.withAppendedPath(itemsUri(noteId), "sort_alpha");
        return notesUri();
    }

    /**
     * Returns a URI for sorting a note's items by their "checked" attribute.  Falls back to a URI
     * for all notes if <code>noteId</code> is lower than 0.
     *
     * @param noteId the ID of the note for which to return an items URI
     * @return an items URI for the note
     */
    @NotNull
    public static Uri sortItemsByCheckedUri(int noteId) {
        if (noteId >= 0) return Uri.withAppendedPath(itemsUri(noteId), "sort_checked");
        return notesUri();
    }

    /**
     * Returns a URI for reversing a note's items.  Falls back to a URI for all notes if
     * <code>noteId</code> is lower than 0.
     *
     * @param noteId the ID of the note for which to return an items URI
     * @return an items URI for the note
     */
    @NotNull
    public static Uri reverseItemsUri(int noteId) {
        if (noteId >= 0) return Uri.withAppendedPath(itemsUri(noteId), "reverse");
        return notesUri();
    }

    /**
     * Returns a URI for clearing a note's items.  Falls back to a URI for all notes if
     * <code>noteId</code> is lower than 0.
     *
     * @param noteId the ID of the note for which to return an items URI
     * @return an items URI for the note
     */
    @NotNull
    public static Uri clearItemsUri(int noteId) {
        if (noteId >= 0) return Uri.withAppendedPath(itemsUri(noteId), "clear");
        return notesUri();
    }

    /**
     * Returns a URI for a single item of some note.  Falls back to a URI for all of the note's
     * items if <code>index</code> is negative, or to a URI for all notes if <code>noteId</code> is
     * lower than 0.
     *
     * @param noteId    the ID of the item's note
     * @param itemIndex the index of the item for which to return a URI
     * @return a URI for the item
     */
    @NotNull
    public static Uri singleItemUri(int noteId, int itemIndex) {
        if (noteId >= 0) {
            if (itemIndex >= 0) return ContentUris.withAppendedId(itemsUri(noteId), itemIndex);
            return itemsUri(noteId);
        }
        return notesUri();
    }

    /**
     * Returns a URI for moving an item of some note, i.e., change its index.  Falls back to a URI
     * for all of the note's items if <code>index</code> or <code>newIndex</code> are negative, or
     * to a URI for all notes if <code>noteId</code> is lower than 0.
     *
     * @param noteId    the ID of the item's note
     * @param itemIndex the index of the item for which to return a URI
     * @param newIndex  the item's new index
     * @return a URI for the item
     */
    @NotNull
    public static Uri moveItemUri(int noteId,
                                  int itemIndex,
                                  int newIndex)
    {
        if (noteId >= 0) {
            if (itemIndex >= 0 && newIndex >= 0) {
                return Uri.withAppendedPath(singleItemUri(noteId, itemIndex), "move/" + newIndex);
            }
            return itemsUri(noteId);
        }
        return notesUri();
    }

    /**
     * Returns the node ID of a compatible URI.
     *
     * @param uri the URI in question
     * @return the URI's note ID, or <code>null</code> if the URI is not a single node's URI
     */
    @Nullable
    public static Integer noteIdOfUri(@Nullable Uri uri) {
        if (isSingleNoteUri(uri)) {
            assert uri != null;
            return Integer.valueOf(uri.getPathSegments().get(1));
        }
        return null;
    }

    /**
     * Returns the item index of a compatible URI.
     *
     * @param uri the URI in question
     * @return the URI's item index, or <code>null</code> if the URI is not a single item's URI
     */
    @Nullable
    public static Integer itemIndexOfUri(@Nullable Uri uri) {
        if (isSingleItemUri(uri)) {
            assert uri != null;
            return Integer.valueOf(uri.getPathSegments().get(3));
        }
        return null;
    }

    /**
     * Returns the new item index of a {@link NotesUris#URI_ITEMS_SINGLE_MOVE}-type URI.
     *
     * @param uri the URI in question
     * @return the URI's item's new index, or <code>null</code> if the URI is a wrongly-typed URI
     */
    @Nullable
    public static Integer itemNewIndexOfUri(@Nullable Uri uri) {
        if (isMoveItemUri(uri)) {
            assert uri != null;
            return Integer.valueOf(uri.getPathSegments().get(5));
        }
        return null;
    }
}

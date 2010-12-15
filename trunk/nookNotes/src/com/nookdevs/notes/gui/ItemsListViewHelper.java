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

package com.nookdevs.notes.gui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nookdevs.notes.R;
import com.nookdevs.notes.provider.Item;
import com.nookdevs.notes.provider.Note;
import com.nookdevs.notes.provider.Notes;
import com.nookdevs.notes.provider.NotesUris;
import com.nookdevs.notes.util.NookSpecifics;
import org.jetbrains.annotations.NotNull;

import static com.nookdevs.notes.provider.NotesUris.*;
import static com.nookdevs.notes.provider.NotesUtils.getNote;
import static com.nookdevs.notes.provider.NotesUtils.notesCount;


/**
 * Helper class handling the display of a list of items of a note on the eInk display.  Displays
 * only titles (the items' text), no sub-titles, but uses variable-height item views.
 *
 * @author Marco Goetze
 */
public class ItemsListViewHelper extends ListViewHelper<Item>
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    /** The ID of the note whose items we display. */
    protected int mNoteId;

    /** List of (absolute, 0-based) item indices for each page. */
    @NotNull protected static final List<Integer> mFirstItemsInPages = new ArrayList<Integer>();

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates a {@link com.nookdevs.notes.gui.ItemsListViewHelper}.  The list view will have no
     * data until a provider has been set via
     * {@link #setProvider(com.nookdevs.notes.data.ListItemsProvider)}.
     *
     * @param activity the activity on behalf of which this instance is created
     * @param view     the view component into which to render
     * @param noteUri  the note's URI
     */
    public ItemsListViewHelper(@NotNull Activity activity,
                               @NotNull LinearLayout view,
                               @NotNull Uri noteUri)
    {
        super(activity, view, getNoteTitle(activity, noteUri));
        assert isSingleNoteUri(noteUri);
        mNoteId = noteIdOfUri(noteUri);
    }

    // inherited methods...

    /** {@inheritDoc} */
    @Override
    protected void initView(int page) {
        // determine the number of pages and distribution of items across them...
        mFirstItemsInPages.clear();
        int itemCount = mItems.getItemCount();
        int firstPageHeight = 0;
        ContentResolver cr = mActivity.getContentResolver();
        for (int i = 0, pageHeight = 0; i < itemCount; i++) {  // for each item...
            if (pageHeight == 0) mFirstItemsInPages.add(i);

            // if the item's height is cached, use it; otherwise measure it and then store it in
            // the database, caching it...
            Item item = mItems.getItem(i);
            Integer height = item.getHeight();
            if (height == null) {  // not cached?
                // prepare item for display, measure its height...
                View vItem = createItemView(i, item);
                vItem.measure(NookSpecifics.EINK_WIDTH, MAX_PAGE_HEIGHT - 2);
                height = vItem.getMeasuredHeight();

                // cache the height...
                ContentValues values = new ContentValues();
                values.put(Notes.KEY_ITEM_HEIGHT, height);
                cr.update(NotesUris.itemHeightUri(mNoteId, i), values, null, null);
            }
            height += 1;  // for one divider

            // update data structure...
            if (pageHeight > 0 && pageHeight + height >= MAX_PAGE_HEIGHT - 1) {
                // NOTE: "- 1" considers the bottom-most "divider".
                if (!mFirstItemsInPages.get(mFirstItemsInPages.size() - 1).equals(i)) {
                    mFirstItemsInPages.add(i);
                }
                pageHeight = height;
            } else {
                pageHeight += height;  // 1 for the divider
            }
            if (mFirstItemsInPages.size() == 1) firstPageHeight += height;
        }

        super.initView(page);

        // add a usage hint if there are only few notes and items (indicating that the user has not
        // yet extensively used the application) and there's room for it...
        if (mNoteId >= 0 &&
            notesCount(cr) <= ITEMS_PER_PAGE / 2 &&
            itemCount <= ITEMS_PER_PAGE / 2 &&
            pageCount() < 2)
        {
            View vUsageHint =
                mActivity.getLayoutInflater().inflate(R.layout.usage_hint, mvMain, false);
            TextView vText = (TextView) vUsageHint.findViewById(R.id.usage_hint);
            vText.setText(R.string.items_usage_hint);
            vUsageHint.measure(NookSpecifics.EINK_WIDTH, MAX_PAGE_HEIGHT);
            if (firstPageHeight + vUsageHint.getMeasuredHeight() <= MAX_PAGE_HEIGHT) {
                mvMain.addView(vUsageHint);
            }
        }
    }

    /** {@inheritDoc} */
    @NotNull @Override
    protected View createItemView(int index,
                                  @NotNull Item item)
    {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View vItem = inflater.inflate(R.layout.items_list_item_note_items, mvMain, false);
        TextView vTitle = (TextView) vItem.findViewById(R.id.items_list_title);
        String text = item.getText();
        if (TextUtils.isEmpty(text)) {
            vTitle.setText(
                styleItemText("~" + mActivity.getString(R.string.item_list_no_text) + "~"));
        } else {
            assert text != null;
            vTitle.setText(isMarkupEnabled() ? styleItemText(text) : text);
        }
        int checked = item.getChecked();
        if (checked == Notes.ITEM_CHECKED_CHECKED) {
            vItem.findViewById(R.id.items_list_checked).setVisibility(View.VISIBLE);
        } else if (checked == Notes.ITEM_CHECKED_UNCHECKED) {
            vItem.findViewById(R.id.items_list_unchecked).setVisibility(View.VISIBLE);
        }
        return vItem;
    }

    /** {@inheritDoc} */
    @Override
    public int pageCount() {
        return mFirstItemsInPages.size();
    }

    /** {@inheritDoc} */
    @Override
    public int lastInPageIndex(int page) {
        return
            page + 1 < mFirstItemsInPages.size()
                ? mFirstItemsInPages.get(page + 1) - mFirstItemsInPages.get(page) - 1
                : mItems.getItemCount() - mFirstItemsInPages.get(mFirstItemsInPages.size() - 1) - 1;
    }

    /** {@inheritDoc} */
    @Override
    public int firstItemInPage(int page) {
        return mFirstItemsInPages.get(page);
    }

    /** {@inheritDoc} */
    @Override
    public int pageOfItem(int index) {
        for (int i = 1; i < mFirstItemsInPages.size(); i++) {
            if (mFirstItemsInPages.get(i) > index) return i - 1;
        }
        return mFirstItemsInPages.size() - 1;
    }

    // own methods...

    /**
     * Updates the note's title as displayed above the list in case it has changed.
     */
    public void refreshTitle() {
        mvTitle.setText(mTitle = getNoteTitle(mActivity, singleNoteUri(mNoteId)));
    }

    //................................................................................... internals

    /**
     * Returns the title of a note given its URI, or a default string if its title is
     * <code>null</code> or empty.
     *
     * @param activity the activity on behalf of which this instance is created
     * @param noteUri  the note's URI
     * @return a title
     */
    @NotNull
    protected static String getNoteTitle(@NotNull Activity activity,
                                         @NotNull Uri noteUri)
    {
        Integer noteId = noteIdOfUri(noteUri);
        if (noteId != null) {
            Note note = getNote(activity.getContentResolver(), noteId);
            if (note != null) {
                String title = note.getTitle();
                if (!TextUtils.isEmpty(title)) {
                    assert title != null;
                    title = title.replaceAll("^\\s+|\\s+$", "").replaceAll("\\s+", " ");
                    return title;
                }
            }
        }
        return activity.getString(R.string.note_title_untitled);
    }
}

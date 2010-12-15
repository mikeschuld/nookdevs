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

import java.util.Map;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nookdevs.notes.R;
import com.nookdevs.notes.provider.Note;
import com.nookdevs.notes.provider.NotesUtils;
import com.nookdevs.notes.util.NookSpecifics;
import org.jetbrains.annotations.NotNull;

import static com.nookdevs.notes.provider.Notes.ITEM_CHECKED_CHECKED;
import static com.nookdevs.notes.provider.Notes.ITEM_CHECKED_UNCHECKED;


/**
 * Helper class handling the display of a list of notes on the eInk display.
 *
 * @author Marco Goetze
 */
public class NotesListViewHelper extends ListViewHelper<Note>
{
    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates a {@link NotesListViewHelper}.  The list view will have no data
     * until a provider has been set via
     * {@link #setProvider(com.nookdevs.notes.data.ListItemsProvider)}.
     *
     * @param activity the activity on behalf of which this instance is created
     * @param view     the view component into which to render
     */
    public NotesListViewHelper(@NotNull Activity activity,
                               @NotNull LinearLayout view)
    {
        super(activity, view, activity.getString(R.string.notes_list_title));
    }

    // inherited methods...

    /** {@inheritDoc} */
    @Override
    protected void initView(int page) {
        super.initView(page);

        // add a usage hint if there are only few notes (indicating that the user has not yet
        // extensively used the application)...
        if (mItems.getItemCount() <= ITEMS_PER_PAGE / 2) {
            View vUsageHint =
                mActivity.getLayoutInflater().inflate(R.layout.usage_hint, mvMain, false);
            TextView vText = (TextView) vUsageHint.findViewById(R.id.usage_hint);
            vText.setText(R.string.notes_usage_hint);
            mvMain.addView(vUsageHint);
        }
    }

    /** {@inheritDoc} */
    @NotNull @Override
    protected View createItemView(int index,
                                  @NotNull Note note) {
        // gather data...
        Map<Integer,Integer> counts = NotesUtils.checkedCounts(note);
        int checked =
            counts.containsKey(ITEM_CHECKED_CHECKED) ? counts.get(ITEM_CHECKED_CHECKED) : 0;
        int unchecked =
            counts.containsKey(ITEM_CHECKED_UNCHECKED) ? counts.get(ITEM_CHECKED_UNCHECKED) : 0;

        // create view...
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View vItem = inflater.inflate(R.layout.items_list_item_notes, mvMain, false);
        TextView vTitle = (TextView) vItem.findViewById(R.id.items_list_title);
        if (TextUtils.isEmpty(note.getTitle())) {
            vTitle.setText(
                styleItemText("~" + mActivity.getString(R.string.note_list_no_title) + "~"));
        } else {
            vTitle.setText(note.getListTitle(mActivity));
        }
        TextView vSubTitle = (TextView) vItem.findViewById(R.id.items_list_subtitle);
        View vCompletionBar = vItem.findViewById(R.id.completion_bar);
        if (checked + unchecked > 0) {  // show completion bar if there are checkable items
            View vCompletionBarInner = vCompletionBar.findViewById(R.id.completion_bar_inner);
            View vComplete = vCompletionBarInner.findViewById(R.id.completion_bar_fill);
            double ratio = (checked * 1.0) / (checked + unchecked);
            vCompletionBar.setVisibility(View.VISIBLE);
            vCompletionBarInner.measure(NookSpecifics.EINK_WIDTH, NookSpecifics.EINK_HEIGHT);
            int width = (int) Math.ceil(vCompletionBarInner.getLayoutParams().width * ratio);
            vComplete.setMinimumWidth(width);
            vSubTitle.setText(Math.min((int) Math.ceil(ratio * 100), 100) + "%, " +
                              note.getListSubTitle(mActivity));
        } else {
            vSubTitle.setText(note.getListSubTitle(mActivity));
        }
        return vItem;
    }
}

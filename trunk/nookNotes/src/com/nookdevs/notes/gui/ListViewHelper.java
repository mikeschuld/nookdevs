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

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nookdevs.notes.R;
import com.nookdevs.notes.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Helper class handling the display of a list of items on the eInk display.
 *
 * @param <T> the type of items in the list
 *
 * @author Marco Goetze
 */
public abstract class ListViewHelper<T extends ListItem> implements ListItemsClient<T>
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //............................................................................ public constants

    /** Selection constant indicating that the very first item in the list is to be selected. */
    public static final int SELECT_FIRST = 1;
    /** Selection constant indicating that the first item on the current page is to be selected. */
    public static final int SELECT_FIRST_IN_PAGE = 2;
    /** Selection constant indicating that the first item on the previous page is to be selected. */
    public static final int SELECT_PREV_PAGE = 3;
    /** Selection constant indicating that the previous list item is to be selected. */
    public static final int SELECT_PREV = 4;
    /** Selection constant indicating that the next list item is to be selected. */
    public static final int SELECT_NEXT = 5;
    /** Selection constant indicating that the first item on the next page is to be selected. */
    public static final int SELECT_NEXT_PAGE = 6;
    /** Selection constant indicating that the last item on the current page is to be selected. */
    public static final int SELECT_LAST_IN_PAGE = 7;
    /** Selection constant indicating that the very last item in the list is to be selected. */
    public static final int SELECT_LAST = 8;

    //......................................................................... protected constants

    /** The minimum height of a list item in pixels. */
    protected static final int MIN_ITEM_HEIGHT = 36;
    /**
     * The constant height of an item in <em>this</em> implementation; need not hold for
     * sub-classes.
     */
    protected static final int ITEM_HEIGHT = 60;
    /** The maximum height of the items area of a page in pixels. */
    protected static final int MAX_PAGE_HEIGHT = 635;
    /** The number of items shown per page. */
    protected static final int ITEMS_PER_PAGE =
        (MAX_PAGE_HEIGHT - 1) / (ITEM_HEIGHT + 1);  // + divider
    /** The maximum number of "dots" to use to indicate the current page and total page count. */
    public static final int MAX_DOTS = 10;

    /**
     * Dummy list items provider used while no actual provider is set.  Facilitates internal
     * handling of this case.
     */
    @NotNull protected final ListItemsProvider<T> NULL_PROVIDER =
        new AbstractListItemsProvider<T>() {
            @Override
            public int getItemCount() {
                return 0;
            }

            @NotNull @Override
            public T getItem(int idx) throws IndexOutOfBoundsException {
                throw new IndexOutOfBoundsException();
            }
        };

    //....................................................................................... views

    /** The view into which we render. */
    @NotNull protected LinearLayout mvMain;
    /** View displaying a title above the list of items. */
    @NotNull protected TextView mvTitle;
    /** View displaying the text portion of the list header. */
    @NotNull protected TextView mvListHeaderText;
    /** View displaying the page number portion of the list header. */
    @NotNull protected TextView mvListHeaderPage;

    /**
     * Array of {@link android.widget.ImageView}s representing dividers in the display of the
     * current list page used to frame the selected item.  Initialized by {@link #initView(int)}.
     * The dividers are viewed as belonging to the element with the respective index; the element
     * at {@link #mLastItem} <code>+ 1</code> is an additional divider below the last element.
     */
    @NotNull protected ImageView[] mvDividers =
        new ImageView[(MAX_PAGE_HEIGHT / MIN_ITEM_HEIGHT) + 1];
    /**
     * Array of {@link android.widget.ImageView}s of arrows used to indicate the selected item.
     * Initialized by {@link #initView(int)}.
     */
    @NotNull protected ImageView[] mvArrows = new ImageView[MAX_PAGE_HEIGHT / MIN_ITEM_HEIGHT];

    //........................................................................... further internals

    /** The activity on behalf of which the helper operates. */
    @NotNull protected final Activity mActivity;
    /** The items storage. */
    @NotNull protected ListItemsProvider<T> mItems = NULL_PROVIDER;

    /** The list title to display. */
    @NotNull protected String mTitle;

    /** The number of pages in the list. */
    protected int mPageCount;
    /** The index of the list page being displayed (0-based). */
    protected int mCurrentPage;
    /** The index (in the page) of the last item in the list (may be <code>-1</code>). */
    protected int mLastItem;

    /** The index (in {@link #mItems}) of the list item being displayed (0-based). */
    protected int mSelectedItem;

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates a {@link com.nookdevs.notes.gui.ListViewHelper}.  The list view will have no data
     * until a provider has been set via
     * {@link #setProvider(com.nookdevs.notes.data.ListItemsProvider)}.
     *
     * @param activity the activity on behalf of which this instance is created
     * @param view     the view component into which to render
     * @param title    the list's title
     */
    public ListViewHelper(@NotNull Activity activity,
                          @NotNull LinearLayout view,
                          @NotNull String title)
    {
        mActivity = activity;
        mvMain = view;
        mTitle = title;

        initView(0);
        if (mItems.getItemCount() > 0) setSelectedIndex(0);
        mItems.addListItemsClient(this);
    }

    // inherited methods...

    //................................................................... interface ListItemsClient

    /** {@inheritDoc} */
    @Override
    public void itemChanged(@NotNull ListItemsChange<T> ev) {
        refresh();
    }

    /** {@inheritDoc} */
    @Override
    public void listChanged(@NotNull ListItemsChange<T> ev) {
        refresh();
    }

    // own methods...

    //.................................................................................... provider

    /**
     * Sets the list view's items provider.
     *
     * @param provider the provider (may be <code>null</code>, which will clear the list)
     */
    public void setProvider(@Nullable ListItemsProvider<T> provider) {
        // unregister as client of the old provider...
        mItems.removeListItemsClient(this);

        // set the new provider...
        mItems = (provider != null ? provider : NULL_PROVIDER);

        // re-initialize...
        initView(0);
        if (mItems.getItemCount() > 0) setSelectedIndex(0);
        mItems.addListItemsClient(this);
    }

    //............................................................................. list navigation

    /**
     * Changes the item selection.
     *
     * @param selectionType the item to be selected (one of the <code>SELECT_*</code> constants)
     */
    public synchronized void changeSelection(int selectionType) {
        int itemCount = mItems.getItemCount();
        if (itemCount >= 0) {
            int page = pageOfItem(mSelectedItem);
            switch (selectionType) {
                case SELECT_FIRST:
                    setSelectedIndex(0);
                    break;
                case SELECT_FIRST_IN_PAGE:
                    setSelectedIndex(firstItemInPage(page));
                    break;
                case SELECT_PREV_PAGE:
                    setSelectedIndex(firstItemInPage(Math.max(page - 1, 0)));
                    break;
                case SELECT_PREV:
                    if (mSelectedItem > 0) setSelectedIndex(mSelectedItem - 1);
                    break;
                case SELECT_NEXT:
                    if (mSelectedItem + 1 < itemCount) setSelectedIndex(mSelectedItem + 1);
                    break;
                case SELECT_NEXT_PAGE:
                    setSelectedIndex(firstItemInPage(Math.min(page + 1, mPageCount - 1)));
                    break;
                case SELECT_LAST_IN_PAGE:
                    setSelectedIndex(firstItemInPage(page) + lastInPageIndex(page));
                    break;
                case SELECT_LAST:
                    setSelectedIndex(itemCount - 1);
                    break;

                default:
                    // ignore
            }
        }
    }

    /**
     * Selects an item in the list corresponding to a given (absolute, not in-page) index.  Bounds
     * the given index to the valid range and does nothing if the list is empty.
     *
     * @param index the item's index
     */
    public synchronized void setSelectedIndex(int index) {
        // sanity checks...
        int itemCount = mItems.getItemCount();
        if (itemCount == 0) return;
        index = Math.max(Math.min(index, itemCount - 1), 0);

        // switch page, if necessary...
        int page = pageOfItem(index);
        if (page != mCurrentPage) initView(page);
        assert page == mCurrentPage;

        // change selection...
        mSelectedItem = index;
        int indexInPage = mSelectedItem - firstItemInPage(pageOfItem(mSelectedItem));
        for (int i = 0; i <= mLastItem; i++) {
            mvDividers[i].setVisibility(
                i == indexInPage || i == indexInPage + 1 ? View.VISIBLE : View.INVISIBLE);
            mvArrows[i].setVisibility(i == indexInPage ? View.VISIBLE : View.INVISIBLE);
        }
        mvDividers[mLastItem + 1].setVisibility(
            indexInPage == mLastItem ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Returns the (absolute, not in-page) index of the currently-selected item.
     *
     * @return the index of the currently-selected item, or -1 if the list is empty
     */
    public synchronized int getSelectedIndex() {
        return mSelectedItem;
    }

    /**
     * Selects a particular item given its ID, if it is in the list.
     *
     * @param itemId the ID of the item to select
     * @return <code>true</code> if the icon was successfully selected, <code>false</code> otherwise
     */
    public synchronized boolean setSelectedItem(int itemId) {
        for (int i = 0; i < mItems.getItemCount(); i++) {
            if (Integer.valueOf(itemId).equals(mItems.getItem(i).getId())) {
                setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the currently-selected item, if any.
     *
     * @return the currently-selected item (will be <code>null</code> if the list is empty,
     *         non-<code>null</code> otherwise)
     */
    @Nullable
    public synchronized T getSelectedItem() {
        if (mSelectedItem >= 0 && mSelectedItem < mItems.getItemCount()) {
            return mItems.getItem(mSelectedItem);
        }
        return null;
    }

    /**
     * Refreshes the list display and optionally changes the selection.  If
     * <code>selectionIndex</code> is lower than zero, the current selection will be maintained
     * or adjusted to a changed list size, if necessary.
     *
     * @param selectionIndex the (absolute, 0-based) index of the item which to select (-1 to
     *                       retain the selection)
     */
    public synchronized void refresh(int selectionIndex) {
        int selection = (selectionIndex >= 0 ? selectionIndex : mSelectedItem);
        int count = mItems.getItemCount();
        initView(Math.max(Math.min(pageOfItem(selection), count - 1), 0));
        if (count > 0) setSelectedIndex(Math.min(selection, count - 1));
    }

    /**
     * Shorthand for calling {@link #refresh(int)} with a negative selection index.
     */
    public synchronized void refresh() {
        refresh(-1);
    }

    /**
     * Returns the index of the page currently being shown.
     *
     * @return the index of the page currently being shown (0-based)
     */
    public int getPage() {
        return mCurrentPage;
    }

    //............................................................................ extension points

    /**
     * Returns the number of pages.  Extension point.
     *
     * @return the number of pages
     */
    public int pageCount() {
        int itemsCount = mItems.getItemCount();
        if (itemsCount != 0) return ((itemsCount - 1) / ITEMS_PER_PAGE) + 1;
        return 0;
    }

    /**
     * Returns the last in-page index of a given page.  Extension point.
     *
     * @param page the page's index (0-based)
     * @return the last in-page index (0-based)
     */
    public int lastInPageIndex(int page) {
        int i = page * ITEMS_PER_PAGE;
        return Math.min(i + ITEMS_PER_PAGE, mItems.getItemCount()) - 1 - i;
    }

    /**
     * Returns the (absolute, 0-based) index of the first item displayed on a given page.
     * Extension point.
     *
     * @param page the page's index (0-based)
     * @return the absolute index of the first item displayed on a given page
     */
    public int firstItemInPage(int page) {
        return page * ITEMS_PER_PAGE;
    }

    /**
     * Returns the page on which a given item is displayed.  Extension point.
     *
     * @param idx the (absolute, 0-based) index of the item in question
     * @return the index of the page displaying the item (0-based)
     */
    public int pageOfItem(int idx) {
        return (idx / ITEMS_PER_PAGE);
    }

    //................................................................................... internals

    /**
     * Initializes the view for displaying a given page.  If <code>pageIndex</code> is invalid,
     * the most sensible page will be displayed instead.  {@link #mSelectedItem} will be set to
     * the first item in the page, but the item will not be visibly selected &mdash; this should
     * be done explicitly by the caller after the method has returned.
     *
     * @param page the page's index (0-based)
     */
    protected synchronized void initView(int page) {
        // prepare...
        mvMain.removeAllViews();
        LayoutInflater inflater = mActivity.getLayoutInflater();

        // add elements...
        // title...
        mvTitle = (TextView) inflater.inflate(R.layout.items_title, mvMain, false);
        mvTitle.setText(mTitle);
        mvMain.addView(mvTitle);
        // list header...
        LinearLayout vHeader = (LinearLayout)
            inflater.inflate(R.layout.items_list_header, mvMain, false);
        mvListHeaderText = (TextView) vHeader.findViewById(R.id.items_list_header_text);
        mvListHeaderPage = (TextView) vHeader.findViewById(R.id.items_list_header_page);
        mvMain.addView(vHeader);
        int itemsCount = mItems.getItemCount();
        mPageCount = pageCount();
        mCurrentPage = Math.max(Math.min(Math.max(page, 0), mPageCount - 1), 0);
        if (itemsCount != 0) {
            mSelectedItem = firstItemInPage(page);
            mLastItem = lastInPageIndex(page);
            int itemsOnPage =
                (mCurrentPage + 1 < mPageCount ? firstItemInPage(mCurrentPage + 1) : itemsCount) -
                firstItemInPage(mCurrentPage);
            mvListHeaderText.setText(
                mActivity.getResources().getQuantityString(
                    R.plurals.items_list_header_text, itemsOnPage,
                    mSelectedItem + 1, mSelectedItem + mLastItem + 1, itemsCount));
            mvListHeaderPage.setText(
                String.format(mActivity.getString(R.string.items_list_header_page),
                              mCurrentPage + 1, mPageCount));
        } else {
            mSelectedItem = mLastItem = -1;
            mvListHeaderText.setText(mActivity.getString(R.string.items_list_header_none));
            mvListHeaderPage.setText("");
        }
        // list data...
        if (mPageCount > 0) {
            for (int i = 0; i <= mLastItem; i++) {
                mvDividers[i] = (ImageView)
                    inflater.inflate(R.layout.items_list_divider, mvMain, false);
                mvMain.addView(mvDividers[i]);
                int idx = firstItemInPage(page) + i;
                View vItem = createItemView(idx, mItems.getItem(idx));
                mvArrows[i] = (ImageView) vItem.findViewById(R.id.items_list_arrow);
                mvMain.addView(vItem);
            }
            mvDividers[mLastItem + 1] = (ImageView)
                inflater.inflate(R.layout.items_list_divider, mvMain, false);
            mvMain.addView(mvDividers[mLastItem + 1]);
        }
        // list footer...
        if (mPageCount > 1 && mPageCount <= MAX_DOTS) {
            ViewGroup vFooter = (ViewGroup)
                inflater.inflate(R.layout.pagination_dots, mvMain, false);
            ViewGroup vDots = (ViewGroup) vFooter.findViewById(R.id.pagination_dots);
            for (int i = 0; i < mPageCount; i++) {
                vDots.addView(
                    inflater.inflate(
                        i <= mCurrentPage ? R.layout.pagination_dot_filled
                                          : R.layout.pagination_dot_empty,
                        vDots,
                        false));
            }
            mvMain.addView(vFooter);
        }
    }

    /**
     * Creates and returns a view for an individual item.  The view has to contain a
     * selection-indicating arrow with ID <code>items_list_arrow</code>.
     *
     * @param index the item's (absolute, 0-based) index
     * @param item  the item
     * @return a view for the item
     */
    @NotNull
    protected View createItemView(int index,
                                  @NotNull T item)
    {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View vItem = inflater.inflate(R.layout.items_list_item, mvMain, false);
        TextView vTitle = (TextView) vItem.findViewById(R.id.items_list_title);
        TextView vSubTitle = (TextView) vItem.findViewById(R.id.items_list_subtitle);
        String title = item.getListTitle(mActivity);
        vTitle.setText(title != null ? title : item.toString());
        vSubTitle.setText(item.getListSubTitle(mActivity));
        return vItem;
    }
}

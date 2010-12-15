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

package com.nookdevs.notes.activity;

import java.util.Arrays;
import java.util.List;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;

import com.nookdevs.common.IconArrayAdapter;
import com.nookdevs.notes.R;
import com.nookdevs.notes.gui.ItemsListViewHelper;
import com.nookdevs.notes.gui.ListViewHelper;
import com.nookdevs.notes.provider.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.NotesUris.*;
import static com.nookdevs.notes.util.NookSpecifics.*;


/**
 * Activity viewing a note (which also allows for it to be edited).  Deals with URIs of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_NOTE},
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_ITEMS}, and
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_ITEM}.  Yields a URI of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_NOTE} for convenience.
 *
 * @author Marco Goetze
 */
public class NoteView
    extends BaseActivity
    implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //................................................................................... constants

    /** Activity request code for adding an item. */
    protected static final int REQUEST_ADD_ITEM = 1;

    /** Saved instance state key for the selected list index. */
    @NotNull protected static final String STATE_SELECTED_INDEX = "selectedIndex";

    //................................................................................... resources

    /** Array of icon IDs corresponding to the main menu's items. */
    @NotNull protected static int[] MAIN_MENU_ICONS = new int[] {
        R.drawable.submenu_pressable,
        R.drawable.menu_icon_toggle_checked_pressable,
        -1,
        -1,
        R.drawable.submenu_pressable,
        -1
    };
    /**
     * Array of icon IDs corresponding to the items of the sub-menu for choosing where to add an
     * item.
     */
    @NotNull protected static int[] SUB_MENU_ADD_ICONS = new int[] {
        R.drawable.menu_icon_add_at_top_pressable,
        R.drawable.menu_icon_add_above_selection_pressable,
        R.drawable.menu_icon_add_below_selection_pressable,
        R.drawable.menu_icon_add_at_bottom_pressable,
    };
    /**
     * Array of icon IDs corresponding to the items of the sub-menu for choosing among list
     * transformations.
     */
    @NotNull protected static int[] SUB_MENU_TRANSFORM_ICONS = new int[] {
        R.drawable.menu_icon_transform_sort_alpha_pressable,
        R.drawable.menu_icon_transform_sort_checked_pressable,
        R.drawable.menu_icon_transform_reverse_pressable,
        R.drawable.menu_icon_transform_clear_pressable,
        R.drawable.menu_icon_transform_delete_checked
    };

    //....................................................................................... views

    /** {@link android.webkit.WebView} displayed on the eInk screen. */
    @NotNull protected LinearLayout mvEInkScreen;

    /** The "back" button on the touch-screen. */
    @NotNull protected Button mvButtonBack;

    /** Animator for the menus. */
    @NotNull protected ViewAnimator mvMenuAnimator;
    /** The list view used as the application's main menu. */
    @NotNull protected ListView mvMenuMain;
    /** The list view used as a second-level menu. */
    @NotNull protected ListView mvMenuSub;

    /** The "up" button. */
    @NotNull protected Button mvButtonUp;
    /** The "down" button. */
    @NotNull protected Button mvButtonDown;
    /** The selection toggle-button. */
    @NotNull protected ToggleButton mvButtonSelection;

    //............................................................................. other internals

    /** The ID of the note being displayed/edited. */
    protected int mNoteId;

    /** Adapter for the sub-menu for adding an item. */
    @NotNull protected IconArrayAdapter<CharSequence> mSubMenuAddAdapter;
    /** Adapter for the sub-menu for transforming the entire list. */
    @NotNull protected IconArrayAdapter<CharSequence> mSubMenuTransformAdapter;

    /** Helper class instance handling the notes list view. */
    @NotNull protected ItemsListViewHelper mListViewHelper;
    /** The items provider used for the notes list view. */
    @NotNull protected ItemsListItemsProvider mItemsProvider;

    /**
     * Flag set to <code>false</code> by {@link #onResume()}, indicating on subsequent runs that
     * this an actual resume, allowing for optimizations in the first pass.
     */
    private boolean mFirstRun = true;

    ////////////////////////////////////////// METHODS ////////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // sanity checks...
        Uri uri = getIntent().getData();
        if (!isSingleNoteUri(uri)) {
            throw new IllegalArgumentException("Unsupported URI: <" + uri + ">!");
        }
        mNoteId = noteIdOfUri(uri);
        Integer itemIndex = itemIndexOfUri(uri);

        super.onCreate(savedInstanceState);

        // overall initialization...
        setContentView(R.layout.note_view);

        // fetch views...
        mvEInkScreen = (LinearLayout) findViewById(R.id.einkscreen);
        mvButtonBack = (Button) findViewById(R.id.button_back);
        mvMenuAnimator = (ViewAnimator) findViewById(R.id.menu_animator);
        mvMenuMain = (ListView) findViewById(R.id.main_menu);
        mvMenuSub = (ListView) findViewById(R.id.sub_menu);
        mvButtonUp = (Button) findViewById(R.id.button_up);
        mvButtonDown = (Button) findViewById(R.id.button_down);
        mvButtonSelection = (ToggleButton) findViewById(R.id.button_selection);

        // initialize the eInk views...
        mListViewHelper =
            new ItemsListViewHelper(this, mvEInkScreen, singleNoteUri(mNoteId));
        mItemsProvider = new ItemsListItemsProvider(this, mNoteId);
        mListViewHelper.setProvider(mItemsProvider);
        if (NotesUris.isSingleItemUri(uri)) {
            List<String> path = uri.getPathSegments();
            mListViewHelper.setSelectedItem(Integer.parseInt(path.get(path.size() - 1)));
        }
        if (itemIndex != null) {
            mListViewHelper.setSelectedIndex(itemIndex);
        } else if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        // build main menu...
        List<CharSequence> menuItems =
            Arrays.asList(getResources().getTextArray(R.array.note_view_menu));
        IconArrayAdapter<CharSequence> menuItemAdapter =
            new IconArrayAdapter<CharSequence>(this,
                                               R.layout.menu_item_complex,
                                               menuItems, MAIN_MENU_ICONS);
        menuItemAdapter.setTextField(R.id.menu_item_text);
        menuItemAdapter.setSubTextField(R.id.menu_item_subtext);
        menuItemAdapter.setImageField(R.id.menu_item_image);
        mvMenuMain.setAdapter(menuItemAdapter);
        mvMenuMain.setOnItemClickListener(this);
        mvMenuMain.setOnItemLongClickListener(this);

        // build sub-menus...
        mvMenuSub.setOnItemClickListener(this);
        // adapter for a sub-menu for choosing where to add an item...
        List<CharSequence> subMenuItems =
            Arrays.asList(getResources().getTextArray(R.array.note_view_add_item_submenu));
        mSubMenuAddAdapter =
            new IconArrayAdapter<CharSequence>(this,
                                               R.layout.menu_item_simple,
                                               subMenuItems, SUB_MENU_ADD_ICONS);
        mSubMenuAddAdapter.setTextField(R.id.menu_item_text);
        mSubMenuAddAdapter.setImageField(R.id.menu_item_image);
        // adapter for a sub-menu for choosing among several list transformations...
        subMenuItems =
            Arrays.asList(getResources().getTextArray(R.array.note_view_transform_submenu));
        mSubMenuTransformAdapter =
            new IconArrayAdapter<CharSequence>(this,
                                               R.layout.menu_item_simple,
                                               subMenuItems, SUB_MENU_TRANSFORM_ICONS);
        mSubMenuTransformAdapter.setTextField(R.id.menu_item_text);
        mSubMenuTransformAdapter.setImageField(R.id.menu_item_image);

        // create listeners...
        mvButtonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                if (mvMenuAnimator.getDisplayedChild() > 1) {
                    mvMenuAnimator.setInAnimation(NoteView.this, R.anim.from_left);
                    mvMenuAnimator.showPrevious();
                } else {
                    setResult(RESULT_OK, new Intent(Intent.ACTION_VIEW, singleNoteUri(mNoteId)));
                    finish();
                }
            }
        });
        mvButtonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                if (mvButtonSelection.isChecked()) {  // move item up by 1
                    int idx = mListViewHelper.getSelectedIndex();
                    if (idx > 0) changeSelectedItemIndex(idx - 1);
                } else {  // select previous item
                    mListViewHelper.changeSelection(ListViewHelper.SELECT_PREV);
                }
            }
        });
        mvButtonUp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(@NotNull View view) {
                // sanity checks...
                int idx = mListViewHelper.getSelectedIndex();
                if (idx < 0) return false;

                // determine context...
                int page = mListViewHelper.getPage();
                int firstInPage = mListViewHelper.firstItemInPage(page);

                if (mvButtonSelection.isChecked()) {
                    // move item to the start of the page, or the start of the previous page if
                    // already there...
                    if (firstInPage != idx) {
                        changeSelectedItemIndex(firstInPage);
                    } else if (page > 0) {
                        changeSelectedItemIndex(mListViewHelper.firstItemInPage(page - 1));
                    }
                } else {
                    // select the first item on the current page, or the first item on the previous
                    // page if already there...
                    if (firstInPage == idx) {
                        mListViewHelper.changeSelection(ListViewHelper.SELECT_PREV_PAGE);
                    } else {
                        mListViewHelper.changeSelection(ListViewHelper.SELECT_FIRST_IN_PAGE);
                    }
                }
                return true;
            }
        });
        mvButtonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                if (mvButtonSelection.isChecked()) {  // move item up by 1
                    int idx = mListViewHelper.getSelectedIndex();
                    if (idx >= 0) changeSelectedItemIndex(idx + 1);
                } else {  // select previous item
                    mListViewHelper.changeSelection(ListViewHelper.SELECT_NEXT);
                }
            }
        });
        mvButtonDown.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(@NotNull View view) {
                // sanity checks...
                int idx = mListViewHelper.getSelectedIndex();
                if (idx < 0) return false;

                // determine context...
                int page = mListViewHelper.getPage();
                int lastInPage;
                boolean onLastPage = (page + 1 < mListViewHelper.pageCount());
                if (onLastPage) {
                    lastInPage = mListViewHelper.firstItemInPage(page + 1) - 1;
                } else {
                    lastInPage = mItemsProvider.getItemCount() - 1;
                }

                if (mvButtonSelection.isChecked()) {
                    // move item to the end of the page, or the end of the next page if already
                    // there...
                    if (lastInPage != idx) {
                        changeSelectedItemIndex(lastInPage);
                    } else if (!onLastPage) {
                        changeSelectedItemIndex(mListViewHelper.lastInPageIndex(page + 1));
                    }
                } else {
                    // select the last item on the current page, or the last item on the next page
                    // if already there...
                    if (lastInPage == idx) {
                        mListViewHelper.changeSelection(ListViewHelper.SELECT_NEXT_PAGE);
                    }
                    mListViewHelper.changeSelection(ListViewHelper.SELECT_LAST_IN_PAGE);
                }
                return true;
            }
        });
        mvButtonSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                if (mItemsProvider.getItemCount() == 0) mvButtonSelection.setChecked(false);
            }
        });

        // finish up...
        mvButtonSelection.setChecked(false);
        mvMenuAnimator.showNext();  // animate menu
    }

    /** {@inheritDoc} */
    @Override
    protected void onResume() {
        super.onResume();

        // requery data...
        if (mFirstRun) {
            mFirstRun = false;
        } else {
            mItemsProvider.requery();  // TODO: Why is this needed despite observers?
            mListViewHelper.refreshTitle();  // may have changed
        }

        // check whether the note is the one last viewed, then make it that...
        boolean lastViewed = isLastViewed();
        Cursor cursor =
            getContentResolver().query(singleNoteViewUri(mNoteId), null, null, null, null);
        if (cursor != null) cursor.close();

        // go "back" to the main menu level -- happens when returning from a sub-activity called
        // from a sub-menu...
        if (mvMenuAnimator.getDisplayedChild() > 1) mvButtonBack.performClick();

        // automatically prompt for an item if this note has no items and is not the one viewed
        // last...
        if (mItemsProvider.getItemCount() == 0 && !lastViewed) {
            startActivity(new Intent(Intent.ACTION_INSERT, itemsUri(mNoteId)));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // restore list selection...
        int idx = savedInstanceState.getInt(STATE_SELECTED_INDEX, -1);
        if (idx >= 0) mListViewHelper.setSelectedIndex(idx);
    }

    /** {@inheritDoc} */
    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        // save list selection...
        if (mItemsProvider.getItemCount() > 0) {
            outState.putInt(STATE_SELECTED_INDEX, mListViewHelper.getSelectedIndex());
        }

        super.onSaveInstanceState(outState);
    }

    /** {@inheritDoc} */
    @Override
    public boolean onKeyDown(int keyCode,
                             @NotNull KeyEvent ev)
    {
        if (ev.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KEY_PAGE_UP_LEFT:
                case KEY_PAGE_UP_RIGHT:
                case GESTURE_PAGE_UP:
                    mvButtonSelection.setChecked(false);
                    mListViewHelper.changeSelection(ListViewHelper.SELECT_PREV_PAGE);
                    return true;

                case KEY_PAGE_DOWN_LEFT:
                case KEY_PAGE_DOWN_RIGHT:
                case GESTURE_PAGE_DOWN:
                    mvButtonSelection.setChecked(false);
                    mListViewHelper.changeSelection(ListViewHelper.SELECT_NEXT_PAGE);
                    return true;

                default:
                    // fall through
            }
        }
        return super.onKeyDown(keyCode, ev);
    }

    //............................................................... interface OnItemClickListener

    /** {@inheritDoc} */
    @Override
    public void onItemClick(@NotNull AdapterView<?> adapterView,
                            @NotNull View view,
                            int position,
                            long id)
    {
        int idx = mListViewHelper.getSelectedIndex();
        if (mvMenuMain.equals(adapterView)) {  // main menu?
            switch (position) {
                case 0:  // "Add"
                    if (mItemsProvider.getItemCount() > 0) {  // sub-menu
                        mvMenuSub.setAdapter(mSubMenuAddAdapter);
                        mvMenuAnimator.setInAnimation(NoteView.this, R.anim.from_right);
                        mvMenuAnimator.showNext();
                    } else {  // add very first item directly
                        startActivity(new Intent(Intent.ACTION_INSERT, itemsUri(mNoteId)));
                    }
                    break;
                case 1: {  // "Toggle"
                    if (idx < 0) break;
                    ContentResolver cr = getContentResolver();
                    Item item = NotesUtils.getItem(cr, mNoteId, idx);
                    if (item != null) {
                        item = new Item(item.getText(), (item.getChecked() + 1) % 3);
                        NotesUtils.updateItem(cr, mNoteId, idx, item);
                    }
                    break;
                }
                case 2:  // "Edit"
                    if (idx < 0) break;
                    startActivity(new Intent(Intent.ACTION_EDIT, singleItemUri(mNoteId, idx)));
                    break;
                case 3:  // "Delete"
                    if (idx < 0) break;
                    startActivity(new Intent(Intent.ACTION_DELETE, singleItemUri(mNoteId, idx)));
                    break;
                case 4:  // "Transform list"
                    mvMenuSub.setAdapter(mSubMenuTransformAdapter);
                    mvMenuAnimator.setInAnimation(NoteView.this, R.anim.from_right);
                    mvMenuAnimator.showNext();
                    break;
                case 5:  // "Rename note"
                    startActivity(new Intent(Intent.ACTION_EDIT, singleNoteUri(mNoteId)));
                    break;

                default:
                    // fall through
            }
        } else if (mSubMenuAddAdapter.equals(adapterView.getAdapter())) {  // "Add item" sub-menu
            switch (position) {
                case 0:  // "Add at top"
                    startActivityForResult(
                        new Intent(Intent.ACTION_INSERT, singleItemUri(mNoteId, 0)),
                        REQUEST_ADD_ITEM);
                    break;
                case 1:  // "Add above selection"
                    startActivityForResult(
                        new Intent(Intent.ACTION_INSERT, singleItemUri(mNoteId, Math.max(idx, 0))),
                        REQUEST_ADD_ITEM);
                    break;
                case 2:  // "Add below selection"
                    startActivityForResult(
                        new Intent(Intent.ACTION_INSERT, singleItemUri(mNoteId, idx + 1)),
                        REQUEST_ADD_ITEM);
                    break;
                case 3:  // "Add at bottom"
                    startActivityForResult(new Intent(Intent.ACTION_INSERT, itemsUri(mNoteId)),
                                           REQUEST_ADD_ITEM);
                    break;

                default:
                    // fall through
            }
        } else if (mSubMenuTransformAdapter.equals(adapterView.getAdapter())) {
            // "Transform list" sub-menu
            if (idx < 0) {  // list already empty: simply return to the main menu
                mvButtonBack.performClick();
            }
            switch (position) {
                case 0:  // "Sort alphabetically"
                    startActivity(new Intent(Intent.ACTION_EDIT,
                                             sortItemsAlphabeticallyUri(mNoteId)));
                    break;
                case 1:  // "Sort by check marks"
                    startActivity(new Intent(Intent.ACTION_EDIT, sortItemsByCheckedUri(mNoteId)));
                    break;
                case 2:  // "Reverse"
                    startActivity(new Intent(Intent.ACTION_EDIT, reverseItemsUri(mNoteId)));
                    break;
                case 3:  // "Clear"
                    startActivity(new Intent(Intent.ACTION_EDIT, clearItemsUri(mNoteId)));
                    break;
                case 4:  // "Delete checked items"
                    startActivity(new Intent(Intent.ACTION_EDIT, deleteCheckedItemsUri(mNoteId)));
                    break;

                default:
                    // fall through
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean onItemLongClick(@NotNull AdapterView<?> adapterView,
                                   @NotNull View view,
                                   int position,
                                   long id)
    {
        int idx = mListViewHelper.getSelectedIndex();
        if (mvMenuMain.equals(adapterView)) {  // main menu?
            switch (position) {
                case 0:  // "Add"
                    // add below selection...
                    startActivityForResult(
                        new Intent(Intent.ACTION_INSERT, singleItemUri(mNoteId, idx + 1)),
                        REQUEST_ADD_ITEM);
                    return true;
                case 3:  // "Delete"
                    if (idx < 0) break;
                    // delete without confirmation...
                    getContentResolver().delete(singleItemUri(mNoteId, idx), null, null);
                    return true;

                default:
                    // fall through
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    @Nullable Intent data)
    {
        switch (requestCode) {
            case REQUEST_ADD_ITEM:   // an item has successfully been added: select
                if (resultCode == RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    if (isSingleItemUri(uri)) {
                        mListViewHelper.setSelectedIndex(itemIndexOfUri(uri));
                    } else {
                        Log.e(mLogTag,
                              "Expected result of adding an item to be a single item's URI -- " +
                              "got <" + uri + ">!");
                    }
                }
                break;

            default:
                Log.w(mLogTag,
                      "Received activity result for unknown request code " + requestCode + "!");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // own methods

    /**
     * Changes the index of the selected item, moving it to another position in the sequence of
     * items.
     *
     * @param newIndex the item's new index
     */
    protected void changeSelectedItemIndex(int newIndex) {
        // sanity checks...
        int index = mListViewHelper.getSelectedIndex();
        ContentResolver cr = getContentResolver();
        if (index < 0 || index >= mItemsProvider.getItemCount()) return;

        // change item's index...
        Uri uri = singleItemUri(mNoteId, index);
        if (cr.update(NotesUris.moveItemUri(mNoteId, index, newIndex), null, null, null) > 0) {
            uri = singleItemUri(mNoteId, newIndex);
        }

        // update list display...
        mListViewHelper.refresh(itemIndexOfUri(uri));
    }

    /**
     * Returns whether the note being viewed is the one last viewed.
     *
     * @return <code>true</code> if the note is the last-viewed one, <code>false</code> otherwise
     */
    protected boolean isLastViewed() {
        boolean b = false;
        Cursor cursor = getContentResolver().query(notesUri(),
                                                   new String[]{ Notes.KEY_NOTE_ID },
                                                   null, null,
                                                   Notes.KEY_NOTE_ORDER_VIEWED + " DESC");
        if (cursor != null) {
            if (cursor.moveToNext()) {
                b = (mNoteId == cursor.getInt(cursor.getColumnIndexOrThrow(Notes.KEY_NOTE_ID)));
            }
            cursor.close();
        }
        return b;
    }
}

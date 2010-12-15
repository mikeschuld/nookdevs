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

package com.nookdevs.notes;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.*;

import com.nookdevs.common.IconArrayAdapter;
import com.nookdevs.notes.activity.BaseActivity;
import com.nookdevs.notes.gui.ListViewHelper;
import com.nookdevs.notes.gui.NotesListViewHelper;
import com.nookdevs.notes.provider.*;
import com.nookdevs.notes.util.Testing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.NoteListItemsProvider.*;
import static com.nookdevs.notes.provider.NotesUris.*;
import static com.nookdevs.notes.provider.NotesUtils.storeNote;
import static com.nookdevs.notes.util.NookSpecifics.*;


/**
 * <p>The main activity provided by <em>nookNotes</em>.  Displays a list of notes and provides
 * browsing and manipulation options.  Optionally accepts both notes and single-note URIs, the
 * latter of which will cause it to preselect the particular note in the list.</p>
 *
 * <p>Not in sub-package <code>activity</code> as the <em>nookLauncher</em> appears to be unable
 * to deal with the additional package level.</p>
 *
 * @author Marco Goetze
 */
public class NookNotes extends BaseActivity implements AdapterView.OnItemClickListener
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //................................................................................... constants

    /** Activity request code for adding a note. */
    protected static final int REQUEST_ADD_NOTE = 1;
    /** Activity request code for viewing an note. */
    protected static final int REQUEST_VIEW_NOTE = 2;

    /** Saved instance state key for the selected list index. */
    @NotNull protected static final String STATE_SELECTED_INDEX = "selectedIndex";

    //................................................................................... resources

    /** Array of icon IDs corresponding to the main menu's items. */
    @NotNull protected static int[] MAIN_MENU_ICONS = new int[] {
        R.drawable.submenu_pressable,
        R.drawable.submenu_pressable,
        -1,
        R.drawable.submenu_pressable,
        R.drawable.submenu_pressable,
        R.drawable.menu_icon_help_pressable
    };

    //....................................................................................... views

    /** {@link WebView} displayed on the eInk screen. */
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
    @NotNull protected Button mvButtonView;

    //............................................................................. other internals

    /** Helper class instance handling the notes list view. */
    @NotNull protected NotesListViewHelper mListViewHelper;
    /** The items provider used for the notes list view. */
    @NotNull protected NoteListItemsProvider mNotesProvider;

    /** Adapter for the main menu. */
    @NotNull protected IconArrayAdapter<CharSequence> mMainMenuAdapter;
    /** Adapter for the sub-menu for changing the sort order. */
    @NotNull protected IconArrayAdapter<CharSequence> mSubMenuSortByAdapter;
    /** Adapter for the settings sub-menu. */
    @NotNull protected IconArrayAdapter<CharSequence> mSubMenuSettingsAdapter;

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
        if (uri != null && !isNotesUri(uri))
            throw new IllegalArgumentException("Unsupported URI: <" + uri + ">!");

        super.onCreate(savedInstanceState);

        // add some helpful notes if there are no notes...
        boolean wasEmpty = (NotesUtils.notesCount(getContentResolver()) == 0);
        if (wasEmpty) generateHelpfulNotes();

        // initialize data store with random data, given the corresponding setting (meant for
        // testing in the emulator)...
        //getPreferences(MODE_PRIVATE).edit().putBoolean(PKEY_EMULATOR, true).commit();
        if (wasEmpty && isEmulator()) {
            String[] lorem = TextUtils.split(Testing.LOREM_IPSUM, "\\s+");
            int n = 5 + (int) (Math.random() * 25);
            for (int i = 0; i < n; i++) {
                List<Item> items = new LinkedList<Item>();
                for (int j = 0; j < (int) (Math.random() * 20); j++) {
                    String[] text = new String[1 + (int) (Math.random() * lorem.length)];
                    System.arraycopy(lorem, 0, text, 0, text.length);
                    items.add(new Item(TextUtils.join(" ", text), Notes.ITEM_CHECKED_NONE));
                }
                storeNote(getContentResolver(), new Note("Test note #" + i, items));
            }
        }

        // overall initialization...
        setContentView(R.layout.main);

        // fetch views...
        mvEInkScreen = (LinearLayout) findViewById(R.id.einkscreen);
        mvButtonBack = (Button) findViewById(R.id.button_back);
        mvMenuAnimator = (ViewAnimator) findViewById(R.id.menu_animator);
        mvMenuMain = (ListView) findViewById(R.id.main_menu);
        mvMenuSub = (ListView) findViewById(R.id.sub_menu);
        mvButtonUp = (Button) findViewById(R.id.button_up);
        mvButtonDown = (Button) findViewById(R.id.button_down);
        mvButtonView = (Button) findViewById(R.id.button_view);

        // initialize the eInk views...
        mListViewHelper = new NotesListViewHelper(this, mvEInkScreen);
        mNotesProvider = new NoteListItemsProvider(this, fetchSortBySetting());
        mListViewHelper.setProvider(mNotesProvider);
        if (isSingleNoteUri(uri)) {
            mListViewHelper.setSelectedItem(noteIdOfUri(uri));
        } else if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        // select an item according to the given URI, if any...
        Integer noteId = noteIdOfUri(uri);
        if (noteId != null) mListViewHelper.setSelectedItem(noteId);

        // build main menu...
        List<CharSequence> menuItems =
            Arrays.asList(getResources().getTextArray(R.array.main_menu));
        mMainMenuAdapter =
            new IconArrayAdapter<CharSequence>(this,
                                               R.layout.menu_item_complex,
                                               menuItems, MAIN_MENU_ICONS);
        mMainMenuAdapter.setImageField(R.id.menu_item_image);
        mMainMenuAdapter.setTextField(R.id.menu_item_text);
        mMainMenuAdapter.setSubTextField(R.id.menu_item_subtext);
        mvMenuMain.setAdapter(mMainMenuAdapter);
        mvMenuMain.setOnItemClickListener(this);
        mMainMenuAdapter.setSubText(
            3,
            getResources().getStringArray(R.array.main_menu_sort_by)[
                sortByMenuIndex(fetchSortBySetting())]);

        // build sub-menus...
        mvMenuSub.setOnItemClickListener(this);
        // adapter for the sub-menu for choosing the sort order...
        List<CharSequence> subMenuItems =
            Arrays.asList(getResources().getTextArray(R.array.main_sort_by_submenu));
        mSubMenuSortByAdapter =
            new IconArrayAdapter<CharSequence>(
                mvMenuSub.getContext(),
                R.layout.menu_item_simple, subMenuItems, subMenuSortByIcons());
        mSubMenuSortByAdapter.setTextField(R.id.menu_item_text);
        mSubMenuSortByAdapter.setImageField(R.id.menu_item_image);
        // adapter for the settings sub-menu...
        subMenuItems =
            Arrays.asList(getResources().getTextArray(R.array.main_settings_submenu));
        mSubMenuSettingsAdapter =
            new IconArrayAdapter<CharSequence>(
                mvMenuSub.getContext(),
                R.layout.menu_item_simple, subMenuItems, subMenuSettingsIcons());
        mSubMenuSettingsAdapter.setTextField(R.id.menu_item_text);
        mSubMenuSettingsAdapter.setImageField(R.id.menu_item_image);

        // create listeners...
        mvButtonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                if (mvMenuAnimator.getDisplayedChild() > 0) {
                    mvMenuAnimator.setInAnimation(NookNotes.this, R.anim.from_left);
                    mvMenuAnimator.showPrevious();
                } else {
                    goHome();
                }
            }
        });
        mvButtonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                mListViewHelper.changeSelection(ListViewHelper.SELECT_PREV);
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

                if (firstInPage == idx) {
                    mListViewHelper.changeSelection(ListViewHelper.SELECT_PREV_PAGE);
                } else {
                    mListViewHelper.changeSelection(ListViewHelper.SELECT_FIRST_IN_PAGE);
                }
                return true;
            }
        });
        mvButtonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                mListViewHelper.changeSelection(ListViewHelper.SELECT_NEXT);
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
                    lastInPage = mNotesProvider.getItemCount() - 1;
                }

                // select the last item on the current page, or the last item on the next page
                // if already there...
                if (lastInPage == idx) {
                    mListViewHelper.changeSelection(ListViewHelper.SELECT_NEXT_PAGE);
                }
                mListViewHelper.changeSelection(ListViewHelper.SELECT_LAST_IN_PAGE);
                return true;
            }
        });
        mvButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NotNull View view) {
                Note note = mListViewHelper.getSelectedItem();
                Integer noteId = (note != null ? note.getId() : null);
                if (noteId != null) {
                    startActivityForResult(new Intent(Intent.ACTION_VIEW, itemsUri(noteId)),
                                           REQUEST_VIEW_NOTE);
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    protected void onResume() {
        super.onResume();

        // if the main menu is not displaying, display it (this may be the case if the user
        // viewed a note while a sub-menu was active)...
        while (mvMenuAnimator.getDisplayedChild() > 0) {
            mvMenuAnimator.setInAnimation(NookNotes.this, R.anim.none);
            mvMenuAnimator.showPrevious();
        }

        // requery data...
        if (mFirstRun) {
            mFirstRun = false;
        } else {
            mNotesProvider.requery();  // TODO: Why is this needed despite observers?
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
        if (mNotesProvider.getItemCount() > 1) {
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
                    mListViewHelper.changeSelection(ListViewHelper.SELECT_PREV_PAGE);
                    return true;

                case KEY_PAGE_DOWN_LEFT:
                case KEY_PAGE_DOWN_RIGHT:
                case GESTURE_PAGE_DOWN:
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
        if (mvMenuMain.equals(adapterView)) {  // main menu?
            Note note = mListViewHelper.getSelectedItem();
            Integer noteId = (note != null ? note.getId() : null);
            switch (position) {
                case 0:  // "Add"
                    startActivityForResult(
                        new Intent(Intent.ACTION_INSERT, notesUri()), REQUEST_ADD_NOTE);
                    break;
                case 1:  // "View"
                    if (noteId == null) break;
                    startActivityForResult(new Intent(Intent.ACTION_VIEW, itemsUri(noteId)),
                                           REQUEST_VIEW_NOTE);
                    break;
                case 2: {  // "Delete"
                    if (noteId == null) break;
                    int idx = mListViewHelper.getSelectedIndex();
                    assert idx >= 0;
                    int nextIdx =
                        idx + 1 < mNotesProvider.getItemCount() ? idx + 1 :
                        idx > 0                                 ? idx - 1 : -1;
                    if (nextIdx >= 0) {
                        startActivity(new Intent(Intent.ACTION_DELETE, singleNoteUri(noteId)));
                    } else {
                        startActivity(new Intent(Intent.ACTION_DELETE, singleNoteUri(noteId)));
                    }
                    break;
            }
                case 3:  // "Sort by"
                    mSubMenuSortByAdapter.setIcons(subMenuSortByIcons());
                    mvMenuSub.setAdapter(mSubMenuSortByAdapter);
                    mvMenuAnimator.setInAnimation(this, R.anim.from_right);
                    mvMenuAnimator.showNext();
                    break;
                case 4:  // "Settings"
                    mSubMenuSortByAdapter.setIcons(subMenuSettingsIcons());
                    mvMenuSub.setAdapter(mSubMenuSettingsAdapter);
                    mvMenuAnimator.setInAnimation(this, R.anim.from_right);
                    mvMenuAnimator.showNext();
                    break;
                case 5: {  // "Help"
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.please_wait_while_generating_title);
                    builder.setMessage(R.string.please_wait_while_generating_message);
                    builder.setCancelable(false);
                    final AlertDialog dlg = builder.show();
                    new Thread() {
                        @Override
                        public void run() {
                            final int welcomeNoteId = generateHelpfulNotes();
                            if (welcomeNoteId >= 0) {
                                startActivityForResult(
                                    new Intent(Intent.ACTION_VIEW, itemsUri(welcomeNoteId)),
                                               REQUEST_VIEW_NOTE);
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dlg.dismiss();
                                }
                            });
                        }
                    }.start();
                    break;
                }

                default:
                    // fall through
            }
        } else if (mSubMenuSortByAdapter.equals(adapterView.getAdapter())) {
            int sortBy = fetchSortBySetting();
            switch (position) {
                case 0:  // "Sort by last viewed"
                    sortBy = SORT_BY_LAST_VIEWED;
                    break;
                case 1:  // "Sort by last edited"
                    sortBy = SORT_BY_LAST_EDITED;
                    break;
                case 2:  // "Sort by title"
                    sortBy = SORT_BY_TITLE;
                    break;

                default:
                    assert false : "Unexpected sort order value: " + sortBy + "!";
            }
            storeSortBySetting(sortBy);
            mSubMenuSortByAdapter.setIcons(subMenuSortByIcons());
            mvMenuSub.setAdapter(mSubMenuSortByAdapter);  // actually update menu display
            mMainMenuAdapter.setSubText(
                3,
                getResources().getStringArray(R.array.main_menu_sort_by)[
                    sortByMenuIndex(fetchSortBySetting())]);
            mvButtonBack.performClick();
            mNotesProvider.sortBy(sortBy);
            mListViewHelper.setSelectedIndex(0);
        } else if (mSubMenuSettingsAdapter.equals(adapterView.getAdapter())) {
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            switch (position) {
                case 0: {  // "Enable markup in items"
                    boolean b = prefs.getBoolean(PKEY_REPLACE_MARKUP, false);
                    prefs.edit().putBoolean(PKEY_REPLACE_MARKUP, !b).commit();
                    break;
                }
                case 1: {  // "Replace umlauts on input"
                    boolean b = prefs.getBoolean(PKEY_REPLACE_UMLAUTS, false);
                    prefs.edit().putBoolean(PKEY_REPLACE_UMLAUTS, !b).commit();
                    break;
                }
                case 2: {  // "Replace symbols on input"
                    boolean b = prefs.getBoolean(PKEY_REPLACE_SYMBOLS, false);
                    prefs.edit().putBoolean(PKEY_REPLACE_SYMBOLS, !b).commit();
                    break;
                }

                default:
                    assert false : "Unexpected settings index: " + position + "!";
            }

            // update icons according to the changed setting...
            int[] icons = subMenuSettingsIcons();
            mSubMenuSettingsAdapter.setIcons(icons);
            mvMenuSub.setAdapter(mSubMenuSettingsAdapter);  // actually update menu display
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    @Nullable Intent data)
    {
        switch (requestCode) {
            case REQUEST_ADD_NOTE:   // a note has successfully been added: select
            case REQUEST_VIEW_NOTE:  // a note has been viewed: reselect, index may have changed
                if (resultCode == RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    if (isSingleNoteUri(uri)) {
                        int noteId = noteIdOfUri(uri);
                        if (requestCode == REQUEST_ADD_NOTE) {
                            startActivityForResult(new Intent(Intent.ACTION_VIEW, itemsUri(noteId)),
                                                   REQUEST_VIEW_NOTE);
                        } else {
                            mListViewHelper.setSelectedItem(noteId);

                            // simulate return from a sub-menu after returning from sub-activity...
                            mvMenuSub.setAdapter(null);
                            mvMenuAnimator.setInAnimation(this, R.anim.none);
                            mvMenuAnimator.showNext();
                            mvMenuAnimator.setInAnimation(this, R.anim.from_left);
                            mvMenuAnimator.showPrevious();
                        }
                    } else {
                        Log.e(mLogTag,
                              "Expected result of adding/viewing a note to be a single note's " +
                                  "URI--got <" + uri + ">!");
                    }
                }
                break;

            default:
                Log.w(mLogTag,
                      "Received activity result for unknown request code " + requestCode + "!");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // own methods...

    /**
     * <p>Generates and stores a note given resources for both title and items.  The given title is
     * prefixed by the application name, delimited by a colon.  If a note of the resulting name
     * already exists, it will be replaced.</p>
     *
     * <p>Replaces the following placeholders in text items:</p>
     *
     * <ul>
     *     <li><code>__APPNAME__</code> is replaced by the application name</li>
     *     <li><code>__CHECKED__</code> is removed and sets the item's "checked" attribute to
     *         {@link com.nookdevs.notes.provider.Notes#ITEM_CHECKED_CHECKED}</li>
     *     <li><code>__UNCHECKED__</code> is removed and sets the item's "checked" attribute to
     *         {@link com.nookdevs.notes.provider.Notes#ITEM_CHECKED_UNCHECKED}</li>
     * </ul>
     *
     * <p>If both <code>__CHECKED__</code> and <code>__UNCHECKED__</code> occur, the latter
     * takes precedence.</p>
     *
     * @param titleId the title's string resource ID
     * @param itemsId the items string-array resource ID
     * @return the note's ID (-1 if there was an error)
     */
    protected int generateNote(int titleId, int itemsId) {
        // check whether to replace a note of the same name...
        String title = getString(R.string.app_name) + ": " + getString(titleId);
        Integer noteId = null;
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(notesUri(),
                                 new String[] { Notes.KEY_NOTE_ID },
                                 Notes.KEY_NOTE_TITLE + "=?", new String[] { title },
                                 null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                noteId = cursor.getInt(cursor.getColumnIndexOrThrow(Notes.KEY_NOTE_ID));
            }
            cursor.close();
        }

        // create/update the note...
        List<Item> items = new LinkedList<Item>();
        for (String text : getResources().getStringArray(itemsId)) {
            int checked = Notes.ITEM_CHECKED_NONE;
            String s = text.replaceAll("__CHECKED__", "");
            if (!s.equals(text)) {
                checked = Notes.ITEM_CHECKED_CHECKED;
                text = s;
            }
            s = text.replaceAll("__UNCHECKED__", "");
            if (!s.equals(text)) {
                checked = Notes.ITEM_CHECKED_UNCHECKED;
                text = s;
            }
            text = text.replaceAll("__APPNAME__", getString(R.string.app_name))
                       .replaceAll("^\\s+|\\s+$", "")
                       .replaceAll("\\s+", " ");
            items.add(new Item(text, checked));
        }
        Uri uri = storeNote(cr, new Note(noteId, title, items));
        if (uri == null) {
            Log.e(mLogTag, "Failed to create helpful note \"" + title + "\"!");
            return -1;
        }
        return noteIdOfUri(uri);
    }

    /**
     * Generates some helpful notes serving as a welcome and in-program help.
     *
     * @return the most generic note's ID (-1 if there was an error)
     */
    protected int generateHelpfulNotes() {
        generateNote(R.string.tips_note_title, R.array.tips_note_items);
        return generateNote(R.string.welcome_note_title, R.array.welcome_note_items);
    }

    /**
     * Returns the sort order setting for the list of notes as stored in the shared preferences.
     *
     * @return the sort order setting for the list of notes (one of the
     *         <code>NoteListItemsProvider.SORT_BY_*</code> constants)
     */
    protected int fetchSortBySetting() {
        int sortBy = getPreferences(MODE_PRIVATE).getInt(PKEY_NOTES_SORT_BY, SORT_BY_LAST_VIEWED);
        if (sortBy != SORT_BY_LAST_VIEWED &&
            sortBy != SORT_BY_LAST_EDITED &&
            sortBy != SORT_BY_TITLE)
        {
            sortBy = SORT_BY_LAST_VIEWED;
        }
        return sortBy;
    }

    /**
     * Stores the sort order setting for the list of notes in the shared preferences.
     *
     * @param sortBy the sort order setting (one of the <code>NoteListItemsProvider.SORT_BY_*</code>
     *               constants)
     */
    protected void storeSortBySetting(int sortBy) {
        getPreferences(MODE_PRIVATE).edit().putInt(PKEY_NOTES_SORT_BY, sortBy).commit();
    }

    protected int sortByMenuIndex(int sortBy) {
        switch (sortBy) {
            case SORT_BY_LAST_EDITED:
                return 1;
            case SORT_BY_TITLE:
                return 2;
            case SORT_BY_LAST_VIEWED:
            default:
                return 0;
        }
    }

    /**
     * Returns the icons for the sub-menu for changing the sort order.  The array of icons will
     * include a check mark at the position of the current setting.
     *
     * @return an array of item IDs
     */
    @NotNull
    protected int[] subMenuSortByIcons() {
        int idx = sortByMenuIndex(fetchSortBySetting());
        int[] icons = new int[3];
        for (int i = 0; i < icons.length; i++) {
            icons[i] = (i == idx ? R.drawable.check_mark_pressable : -1);
        }
        return icons;
    }

    /**
     * Returns the icons for the settings sub-menu.
     *
     * @return an array of item IDs
     */
    @NotNull
    protected int[] subMenuSettingsIcons() {
        int[] icons = new int[3];
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        icons[0] =
            (prefs.getBoolean(PKEY_REPLACE_MARKUP, false) ? R.drawable.check_mark_pressable : -1);
        icons[1] =
            (prefs.getBoolean(PKEY_REPLACE_UMLAUTS, false) ? R.drawable.check_mark_pressable : -1);
        icons[2] =
            (prefs.getBoolean(PKEY_REPLACE_SYMBOLS, false) ? R.drawable.check_mark_pressable : -1);
        return icons;
    }
}

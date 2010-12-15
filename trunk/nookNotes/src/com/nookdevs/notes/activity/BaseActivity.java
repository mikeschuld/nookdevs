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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.nookdevs.notes.NookNotes;
import com.nookdevs.notes.R;
import com.nookdevs.notes.gui.InputStringReplacer;
import com.nookdevs.notes.util.NookSpecifics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.util.NookSpecifics.SYSTEM_SETTINGS_SCREENSAVER_DELAY;


/**
 * <p>Application-specific abstract base class for activities.</p>
 *
 * <p>{@link BaseActivity} maintains and passes on context information using intent extras &mdash;
 * see the <code>INTENT_EXTRA_*</code> constants.</p>
 *
 * @author Marco Goetze
 */
public abstract class BaseActivity extends Activity
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //............................................................................ public constants

    /**
     * Shared preferences key for a <code>boolean</code>-type value specifying whether to enable
     * markup in note items.
     */
    @NotNull public static String PKEY_REPLACE_MARKUP = "replaceMarkup";
    /**
     * Shared preferences key for a <code>boolean</code>-type value specifying whether to replace
     * umlauts in text input.
     */
    @NotNull public static String PKEY_REPLACE_UMLAUTS = "replaceUmlauts";
    /**
     * Shared preferences key for a <code>boolean</code>-type value specifying whether to replace
     * symbols in text input.
     */
    @NotNull public static String PKEY_REPLACE_SYMBOLS = "replaceSymbols";

    //......................................................................... protected constants

    /** Result code indicating an error performing the activity. */
    protected static final int RESULT_ERROR = RESULT_FIRST_USER;

    /**
     * Shared preferences key for a <code>boolean</code>-type value specifying whether the
     * application is running in the emulator.  Has to be set manually as the emulator cannot be
     * detected reliably without ugly kludges.  Setting the option will, i.a., result in random
     * notes being generated when the notes database is empty.
     */
    @NotNull protected static final String PKEY_EMULATOR = "emulator";
    /**
     * Shared preferences key for an <code>int</code>-type value specifying the sort order for the
     * list of notes ((one of the <code>NoteListItemsProvider.SORT_BY_*</code> constants).
     */
    @NotNull protected static final String PKEY_NOTES_SORT_BY = "notesSortBy";

    /**
     * The default screensaver delay/screen lock period in milliseconds.  Used if fetching the
     * corresponding system setting fails.
     */
    private static final long DEFAULT_SCREENSAVER_DELAY = 600000l;

    //................................................................................... internals

    /** The title as displayed in the status bar. */
    @NotNull private String mTitle;

    /** The screensaver delay/screen lock period (in milliseconds). */
    private long mScreenSaverDelay = DEFAULT_SCREENSAVER_DELAY;  // ms
    /**
     * Wake lock preventing the screensaver from kicking in too soon.  May be <code>null</code> if
     * acquisition failed.
     */
    @Nullable private PowerManager.WakeLock mScreenLock;

    /** The activity's log tag. */
    @NotNull protected final String mLogTag = "Notes.activity." + getClass().getSimpleName();

    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize screen lock...
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mScreenLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, mLogTag);
        if (mScreenLock != null) mScreenLock.setReferenceCounted(false);

        // determine application version...
        PackageManager manager = getPackageManager();
        String version = null;
        try {
            version = manager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(mLogTag, "Failed to determine application version!");
        }

        // set title...
        String activityTitle = getActivityTitle();
        mTitle = getString(R.string.app_title) + (version != null ? " " + version : "") +
                 (activityTitle != null ? " \u2014 " + activityTitle : "");
        updateTitle(mTitle);
    }

    /** {@inheritDoc} */
    @Override
    protected void onResume() {
        super.onResume();

        // fetch relevant system settings (we're doing this here in case they have changed
        // intermittently)...
        try {
            mScreenSaverDelay =
                Settings.System.getLong(getContentResolver(), SYSTEM_SETTINGS_SCREENSAVER_DELAY);
            if (mScreenSaverDelay <= 0) mScreenSaverDelay = DEFAULT_SCREENSAVER_DELAY;
        } catch (Settings.SettingNotFoundException e) {
            Log.v(mLogTag, "Failed to fetch screen saver delay system setting!");
        }

        // lock screen for a while...
        if (mScreenLock != null) mScreenLock.acquire(mScreenSaverDelay);

        // make sure the activity's title is displayed...
        updateTitle(mTitle);
    }

    /** {@inheritDoc} */
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        // lock screen for a while...
        if (mScreenLock != null) mScreenLock.acquire(mScreenSaverDelay);
    }

    /** {@inheritDoc} */
    @Override
    protected void onPause() {
        super.onPause();

        // release screen lock...
        if (mScreenLock != null) mScreenLock.release();
    }

    /** {@inheritDoc} */
    @Override
    public void finish() {
        // kludge: // prevent ghosting after closing transparent activities (our "dialogs")
        if (findViewById(R.id.dialog) != null) setContentView(R.layout.blank_eink);

        super.finish();
    }

    // own methods...

    //......................................................... abstract methods & extension points

    /**
     * Returns the activity's title, if any.  Has to be overridden as class {@link BaseActivity}'s
     * implementation always returns <code>null</code>.
     *
     * @return the activity's title (may be <code>null</code>)
     */
    @Nullable
    protected String getActivityTitle() {
        return null;
    }

    //................................................................. protected auxiliary methods

    /**
     * Goes "home", i.e., activates the launcher but does <em>note</em> terminate the current
     * activity.
     */
    protected void goHome() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        startActivity(intent);
    }

    /**
     * Forces the soft keyboard to be displayed for a given view.  Shorthand for the respective
     * {@link android.view.inputmethod.InputMethodManager} method.
     *
     * @param viewId the ID of the view for which to show the keyboard
     */
    protected void showSoftInput(int viewId) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(findViewById(viewId), InputMethodManager.SHOW_FORCED);
    }

    /**
     * Shows a yes/no confirmation dialog and performs actions in either case.  Note that the
     * provided listeners should take care of closing the dialog via
     * {@link android.content.DialogInterface#dismiss()} unless that is done via the returned
     * dialog instance.
     *
     * @param title       the title
     * @param message     the message
     * @param yesListener listener called when the user pressed "yes"
     * @param noListener  listener called when the user pressed "no" (may be <code>null</code>)
     * @return the dialog created
     */
    protected AlertDialog confirm(@NotNull String title,
                                  @NotNull String message,
                                  DialogInterface.OnClickListener yesListener,
                                  DialogInterface.OnClickListener noListener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.yes, yesListener);
        builder.setNegativeButton(android.R.string.no, noListener);
        return builder.show();
    }

    /**
     * Shows a yes/no confirmation dialog and performs actions in either case.  Note that the
     * provided listeners should take care of closing the dialog via
     * {@link android.content.DialogInterface#dismiss()} unless that is done via the returned
     * dialog instance.
     *
     * @param titleId     the title's string resource ID
     * @param messageId   the message's string resource ID
     * @param yesListener listener called when the user pressed "yes"
     * @param noListener  listener called when the user pressed "no" (may be <code>null</code>)
     * @return the dialog created
     */
    protected AlertDialog confirm(int titleId,
                                  int messageId,
                                  DialogInterface.OnClickListener yesListener,
                                  DialogInterface.OnClickListener noListener)
    {
        return confirm(getString(titleId), getString(messageId), yesListener, noListener);
    }

    /**
     * Returns whether the activity is runing in the emulator according to the corresponding
     * shared preference.
     *
     * @return <code>true</code> if the activity is running in the emulator, <code>false</code>
     *         otherwise
     *
     * @see #PKEY_EMULATOR
     */
    protected boolean isEmulator() {
        return getPreferences(MODE_PRIVATE).getBoolean(PKEY_EMULATOR, false);
    }

    //................................................................... private auxiliary methods

    /**
     * Updates the application's title on the status bar.
     *
     * @param title the new title
     */
    private void updateTitle(@NotNull String title) {
        try {
            Intent intent = new Intent(NookSpecifics.ACTION_UPDATE_TITLE);
            String key = "apptitle";
            intent.putExtra(key, title);
            sendBroadcast(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates an {@link com.nookdevs.notes.gui.InputStringReplacer} for a given editor, configured
     * to perform replacements as per the corresponding settings, and registers it with the editor.
     *
     * @param editor the editor component for which to create the replacer
     */
    protected void createAndRegisterInputStringReplacer(@NotNull EditText editor) {
        // configure replacements as per the settings...
        int replacements = 0;
        SharedPreferences prefs =
            getSharedPreferences(NookNotes.class.getSimpleName(), MODE_PRIVATE);
        if (prefs.getBoolean(PKEY_REPLACE_UMLAUTS, false)) {
            replacements |= InputStringReplacer.REPLACE_UMLAUTS;
        }
        if (prefs.getBoolean(PKEY_REPLACE_SYMBOLS, false)) {
            replacements |= InputStringReplacer.REPLACE_SYMBOLS;
        }

        // create and register the replacer...
        editor.addTextChangedListener(new InputStringReplacer(this, editor, replacements));
    }
}

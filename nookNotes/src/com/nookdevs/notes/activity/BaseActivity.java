package com.nookdevs.notes.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.nookdevs.notes.R;
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

    //................................................................................... constants

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
     * Shared preferences key for a <code>int</code>-type value specifying the sort order for the
     * list of notes ((one of the <code>NoteListItemsProvider.SORT_BY_*</code> constants).
     */
    @NotNull protected static final String PKEY_NOTES_SORT_BY = "notesSortBy";

    //................................................................................... internals

    /** The title as displayed in the status bar. */
    @NotNull private String mTitle;

    /** The screensaver delay (in milliseconds). */
    private long mScreenSaverDelay = 600000l;  // ms
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

        // fetch relevant system settings...
        try {
            mScreenSaverDelay =
                Settings.System.getLong(getContentResolver(), SYSTEM_SETTINGS_SCREENSAVER_DELAY);
        } catch (Settings.SettingNotFoundException e) {
            Log.v(mLogTag, "Failed to fetch screensaver delay system setting!");
        }

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
        if (mScreenLock != null) mScreenLock.acquire(mScreenSaverDelay);
        updateTitle(mTitle);
    }

    /** {@inheritDoc} */
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (mScreenLock != null) mScreenLock.acquire(mScreenSaverDelay);
    }

    /** {@inheritDoc} */
    @Override
    protected void onPause() {
        super.onPause();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleId);
        builder.setMessage(messageId);
        builder.setPositiveButton(android.R.string.yes, yesListener);
        builder.setNegativeButton(android.R.string.no, noListener);
        return builder.show();
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
}

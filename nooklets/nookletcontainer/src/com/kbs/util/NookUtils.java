package com.kbs.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.provider.Settings;
import android.database.Cursor;

// A collection of random, nook-specific
// stuff

public class NookUtils
{
    public final static String UPDATE_TITLE =
        "com.bravo.intent.UPDATE_TITLE";
    public final static String UPDATE_STATUSBAR =
        "com.bravo.intent.UPDATE_STATUSBAR";

    public final static String STATUSBAR_ICON =
        "Statusbar.icon";
    public final static String STATUSBAR_ACTION =
        "Statusbar.action";

    public final static long DEFAULT_SCREENSAVER_DELAY = 300*1000L;

    private final static String TAG = "nook-utils";

    public final static void setAppTitle(Context ctx, String title)
    {
        Intent msg = new Intent(UPDATE_TITLE);

        msg.putExtra("apptitle", title);
        ctx.sendBroadcast(msg);
    }

    public final static long getScreenSaverDelay(Context ctx)
    {
        String[] values = {
            "value"
        };

        String[] fname = {
            "bnScreensaverDelay"
        };

        long ret = DEFAULT_SCREENSAVER_DELAY;

        Cursor c = null;

        try {

            c = ctx.getContentResolver().query
                (Settings.System.CONTENT_URI, values, "name=?", fname, null);
            if (c != null) {
                c.moveToFirst();
                long l = c.getLong(0);
                if (l > 0) {
                    ret = l;
                    Log.d(TAG, "Found screen-saver delay as "+ret);
                }
            }
            else {
                Log.d(TAG, "No screen-saver-delay");
            }
        }
        catch (Throwable th) {
            Log.d(TAG, "Ignoring error when reading system settings", th);
        }
        finally {
            if (c != null) {
                try { c.close(); } catch (Throwable ign) {}
                try {c.deactivate(); } catch (Throwable ign) {}
            }
        }
        return ret;
    }

    public final static void showPageNumber
        (Context ctx, int curpage, int maxpage)
    {
        Intent msg = new Intent(UPDATE_STATUSBAR);
        msg.putExtra(STATUSBAR_ICON, 7);
        msg.putExtra(STATUSBAR_ACTION, 1);
        msg.putExtra("current", curpage);
        msg.putExtra("max", maxpage);
        ctx.sendBroadcast(msg);
    }

}

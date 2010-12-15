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

package com.nookdevs.notes.util;

import org.jetbrains.annotations.NotNull;


/**
 * Utility class providing <em>nook</em>-specific constants and utility methods.
 *
 * @author Marco Goetze
 */
@SuppressWarnings({ "UnusedDeclaration" })
public abstract class NookSpecifics
{
    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //..................................................................................... intents

    /** Intent action notifying for broadcasting application title changes */
    @NotNull public static final String ACTION_UPDATE_TITLE = "com.bravo.intent.UPDATE_TITLE";

    //.................................................................................. dimensions

    /** The eInk screen's width in pixels. */
    public static final int EINK_WIDTH = 600;
    /** The eInk screen's height in pixels. */
    public static final int EINK_HEIGHT = 800;
    /** The usable height of the eInk screen in pixels. */
    public static final int EINK_USABLE_HEIGHT = 760;

    /** The LCD touch screen's width in pixels. */
    public static final int LCD_WIDTH = 480;
    /** The LCD touch screen's height in pixels. */
    public static final int LCD_HEIGHT = 144;

    /** The total screen width in pixels. */
    public static final int TOTAL_SCREEN_WIDTH = Math.max(EINK_WIDTH, LCD_WIDTH);
    /** The total screen height in pixels. */
    public static final int TOTAL_SCREEN_HEIGHT = EINK_HEIGHT + LCD_HEIGHT;

    //................................................................................... key codes

    /** Key code of the right page-up key. */
    public static final int KEY_PAGE_UP_RIGHT = 98;
    /** Key code of the right page-down key. */
    public static final int KEY_PAGE_DOWN_RIGHT = 97;
    /** Key code of the left page-up key. */
    public static final int KEY_PAGE_UP_LEFT = 96;
    /** Key code of the left page-down key. */
    public static final int KEY_PAGE_DOWN_LEFT = 95;

    /** Key code of the page-up swipe gesture. */
    public static final int GESTURE_PAGE_UP = 101;
    /** Key code of the page-down swipe gesture. */
    public static final int GESTURE_PAGE_DOWN = 100;

    /** "Clear" button on the soft keyboard. */
    public static final int KEY_SOFT_CLEAR = -13;
    /** "Submit" button on the soft keyboard. */
    public static final int KEY_SOFT_SUBMIT = -8;
    /** "Cancel" button on the soft keyboard. */
    public static final int KEY_SOFT_CANCEL = -3;
    /** "Up" button on the soft keyboard. */
    public static final int KEY_SOFT_UP = 19;
    /** "Down" button on the soft keyboard. */
    public static final int KEY_SOFT_DOWN = 20;

    //............................................................................. system settings

    /**
     * The {@link android.provider.Settings.System} key for the B&amp;N-specific screensaver delay.
     */
    @NotNull public static final String SYSTEM_SETTINGS_SCREENSAVER_DELAY = "bnScreensaverDelay";

    //............................................................................... miscellaneous

    /** The (minimum) duration of a long click (in milliseconds). */
    public static final int LONG_CLICK_DURATION = 750;  // ms
}

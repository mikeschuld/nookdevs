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

import android.view.KeyEvent;
import android.widget.EditText;

import com.nookdevs.notes.util.NookSpecifics;
import org.jetbrains.annotations.NotNull;


/**
 * Utility class for text editors.
 *
 * @author Marco Goetze
 */
public class EditingUtils
{
    /////////////////////////////////////////// METHODS /////////////////////////////////////////////

    // own methods...

    /**
     * <p>Performs default handling of various non-character keys on the soft keyboard.</p>
     *
     * <p>Specifically, the following keys are handled:</p>
     *
     * <ul>
     *     <li>The "Clear" key, clearing the view's text.</li>
     * </ul>
     *
     * @param view the view being edited
     * @param ev   the key event
     * @return <code>true</code> if the key event was handled, false otherwise
     */
/*
     *     <li>Long-presses of the D-pad's left/right keys, skipping by words rather than
     *         single characters.</li>
     *     <li>Long-presses of the D-pad's up/down keys, jumping to the start/end of the text.</li>
 */
    public static boolean handleDefaultKeys(@NotNull EditText view,
                                            @NotNull KeyEvent ev)
    {
        int keyCode = ev.getKeyCode();
        if (ev.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
/*
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    // move to the start of the previous word on long left-presses...
                    int cursor = view.getSelectionStart();
                    if (isLongPress(ev)) {
                        Editable text = view.getText();
                        while (cursor > 0 && Character.isWhitespace(text.charAt(cursor - 1))) {
                            view.setSelection(--cursor);
                        }
                        while (cursor > 0 && !Character.isWhitespace(text.charAt(cursor - 1))) {
                            view.setSelection(--cursor);
                        }
                        return true;
                    } else if (cursor > 0) {
                        view.setSelection(--cursor);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    // move to the start of the previous word on long left-presses...
                    if (isLongPress(ev)) {
                        int cursor = view.getSelectionStart();
                        Editable text = view.getText();
                        while (cursor < text.length() &&
                               !Character.isWhitespace(text.charAt(cursor)))
                        {
                            view.setSelection(++cursor);
                        }
                        while (cursor < text.length() &&
                               Character.isWhitespace(text.charAt(cursor)))
                        {
                            view.setSelection(++cursor);
                        }
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (isLongPress(ev)) {
                        view.setSelection(0);
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (isLongPress(ev)) {
                        view.setSelection(view.getText().length());
                        return true;
                    }
                    break;
*/
                case NookSpecifics.KEY_SOFT_CLEAR:
                    view.setText("");
                    return true;

                default:
                    // fall through
            }
        }
        return false;
    }

    /**
     * Returns whether the key press causing a {@link android.view.KeyEvent#ACTION_UP} event was
     * a long press.
     *
     * @param ev the key event in question
     * @return <code>true</code> on a long press, <code>false</code> otherwise
     */
    public static boolean isLongPress(@NotNull KeyEvent ev) {
        return ev.getAction() == KeyEvent.ACTION_UP &&
               ev.getEventTime() - ev.getDownTime() >= NookSpecifics.LONG_CLICK_DURATION;
    }
}

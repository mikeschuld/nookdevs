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

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link android.text.TextWatcher} replacing predefined strings to the left of the cursor in an
 * input field upon character-yielding key presses, and reverting the replacement when the
 * backspace (del) key is pressed after such a replacement.  To this end, an instance of the class
 * needs to be registered with an {@link android.widget.EditText} via
 * {@link android.widget.EditText#addTextChangedListener(android.text.TextWatcher)}.</p>
 *
 * <p>It also applies some additional replacement logic in any case, namely, the removal of
 * whitespace at the start of the text field and the suppression of subsequent spaces, the latter
 * primarily to work around an issue with the soft keyboard where imprecise tapping can lead to
 * entire sequences of spaces being inserted unintentionally.</p>
 *
 * @author Marco Goetze
 */
public class InputStringReplacer implements TextWatcher
{
    /////////////////////////////////////// NESTED CLASSES ////////////////////////////////////////

    /**
     * Structure-like class encapsulating the information of a previous replacement.
     *
     * @author Marco Goetze
     */
    protected class PreviousReplacement
    {
        // attributes...

        /** The offset of the replacement within the text. */
        public final int offset;
        /** The sub-string replaced. */
        @NotNull public final String replaced;
        /** The replacement string. */
        @NotNull public final String replacement;

        // methods...

        // constructurs/destructors...

        /**
         * Creates a {@link com.nookdevs.notes.gui.InputStringReplacer.PreviousReplacement}.
         *
         * @param offset      the offset of the replacement within the text
         * @param replaced    the sub-string replaced
         * @param replacement the replacement string
         */
        public PreviousReplacement(int offset,
                                   @NotNull String replaced,
                                   @NotNull String replacement)
        {
            this.offset = offset;
            this.replaced = replaced;
            this.replacement = replacement;
        }
    }

    ///////////////////////////////////////// ATTRIBUTES //////////////////////////////////////////

    //........................................................................ replacement mappings

    /** Constant specifying that umlauts are to be replaced. */
    @SuppressWarnings({ "PointlessBitwiseExpression" })
    public static final int REPLACE_UMLAUTS = 1 << 0;
    /** Constant specifying that symbols are to be replaced. */
    public static final int REPLACE_SYMBOLS = 1 << 1;

    /**
     * Mapping of string replacement for umlauts (strings at even indices map to the subsequent
     * strings at odd indices).
     */
    @NotNull protected static final String[] MAPPING_UMLAUTS = new String[] {
        "Ae", "\u00c4",
        "Oe", "\u00d6",
        "Ue", "\u00dc",
        "ae", "\u00e4",
        "oe", "\u00f6",
        "ue", "\u00fc",
        "sz", "\u00df"
    };
    /**
     * Mapping of string replacement for symbols (strings at even indices map to the subsequent
     * strings at odd indices).
     */
    @NotNull protected static final String[] MAPPING_SYMBOLS = new String[] {
        "->",  "\u2192",
        "--",  "\u2015",
    };

    //................................................................................... internals

    /** The activity by which the instance has been created. */
    @NotNull protected final Activity mActivity;
    /** The editor for which the instance has been created. */
    @NotNull protected final EditText mvEditor;

    /** Mapping of string replacements. */
    @NotNull protected final Map<String, String> mReplacements = new HashMap<String, String>();

    /**
     * Information about the last-performed replacement (may be <code>null</code>).  Reset to
     * <code>null</code> upon the next text change.
     */
    // NOTE: Intentionally not @Nullable/@NotNull-annotated.
    protected PreviousReplacement mLastReplacement;
    /**
     * Remembers the previous text length.
     */
    protected int mPreviousTextLength;

    ////////////////////////////////////////// METHODS ////////////////////////////////////////////

    // constructors/destructors...

    /**
     * Creates an {@link InputStringReplacer}.
     *
     * @param activity     the activity by which the instance is being created
     * @param editor       the text editor for which the instance is being created
     * @param replacements disjunctive combination (OR) of <code>REPLACE_*</code> constants
     *                     specifying which replacements to perform
     */
    public InputStringReplacer(@NotNull Activity activity,
                               @NotNull EditText editor,
                               int replacements)
    {
        mActivity = activity;
        mvEditor = editor;
        mPreviousTextLength = editor.getText().length();

        // define replacement mappings...
        if ((replacements & REPLACE_UMLAUTS) != 0) addMapping(MAPPING_UMLAUTS);
        if ((replacements & REPLACE_SYMBOLS) != 0) addMapping(MAPPING_SYMBOLS);
    }

    // inherited methods...

    //....................................................................... interface TextWatcher

    /** {@inheritDoc} */
    @Override
    public void afterTextChanged(@NotNull Editable editable) {
        int cursorPos = mvEditor.getSelectionEnd();
        if (mLastReplacement != null) {  // replaced a sub-string previously?
            // check whether to revert a replacement...
            if (cursorPos == mLastReplacement.offset + mLastReplacement.replacement.length() - 1 &&
                mvEditor.getSelectionStart() == cursorPos)
            {
                String text = editable.toString();
                if (text.length() == mPreviousTextLength - 1) {
                    // given the previously-checked conditions, the user must have erase the
                    // replacement's last character...
                    editable.replace(
                        mLastReplacement.offset,
                        mLastReplacement.offset + mLastReplacement.replacement.length() - 1,
                        mLastReplacement.replaced);
                }
            }
            mLastReplacement = null;
        } else if (editable.length() == mPreviousTextLength + 1) {  // character entered?
            // check whether to apply any replacement, and do so...
            if (cursorPos >= 0 &&
                mvEditor.getSelectionStart() == cursorPos)
            {  // no actual selection?
                String text = editable.toString();
                String beforeCursor = text.substring(0, cursorPos);
                for (Map.Entry<String, String> entry : mReplacements.entrySet()) {
                    String replaced = entry.getKey();
                    String replacement = entry.getValue();
                    if (beforeCursor.endsWith(replaced)) {
                        editable.replace(cursorPos - replaced.length(), cursorPos, replacement);
                        mLastReplacement = new PreviousReplacement(cursorPos - replaced.length(),
                                                                   replaced,
                                                                   replacement);
                        break;
                    }
                }

                // remove leading space, multiple spaces (the latter to work around an issue with
                // the soft keyboard where sloppy tapping may lead to a sequence of spaces being
                // inserted all at once...
                if (cursorPos > 0) {
                    char prev = beforeCursor.charAt(beforeCursor.length() - 1);
                    if (prev == ' ' &&
                        (cursorPos == 1 || beforeCursor.charAt(beforeCursor.length() - 2) == ' '))
                    {
                        editable.replace(Math.max(cursorPos - 2, 0), cursorPos,
                                         cursorPos == 1 ? "" : " ");
                        mLastReplacement = null;
                    }
                }
            }
        }
        mPreviousTextLength = editable.length();
    }

    /** {@inheritDoc} */
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // nothing to be done
    }

    /** {@inheritDoc} */
    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // nothing to be done
    }

    // own methods...

    /**
     * Adds a given mapping to {@link #mReplacements}.
     *
     * @param mapping the mapping (one of the <code>MAPPING_*</code> constants)
     */
    protected void addMapping(@NotNull String[] mapping) {
        for (int i = 0; i + 1 < mapping.length; i += 2) {
            mReplacements.put(mapping[i], mapping[i + 1]);
        }
    }
}

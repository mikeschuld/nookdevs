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

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import com.nookdevs.notes.R;
import com.nookdevs.notes.provider.Notes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Activity for deleting an item of a note.  Deals with URIs of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_SINGLE_ITEM}.
 *
 * @author Marco Goetze
 */
public class ItemDelete extends BaseActivity
{
    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @NotNull @Override
    protected String getActivityTitle() {
        return getString(R.string.activity_item_delete);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // sanity checks...
        final Uri uri = getIntent().getData();
        ContentResolver cr = getContentResolver();
        if (!Notes.CONTENT_TYPE_SINGLE_ITEM.equals(cr.getType(uri)))
            throw new IllegalArgumentException("Unsupported URI: <" + uri + ">!");

        super.onCreate(savedInstanceState);

        // delete after confirmation...
        DialogInterface.OnClickListener yes = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
                if (getContentResolver().delete(uri, null, null) > 0) {
                    setResult(RESULT_OK);
                } else {
                    setResult(RESULT_ERROR);
                }
                finish();
            }
        };
        DialogInterface.OnClickListener no = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
                setResult(RESULT_CANCELED);
                finish();
            }
        };
        confirm(R.string.activity_item_delete, R.string.delete_item_message, yes, no);
    }
}

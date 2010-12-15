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

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import com.nookdevs.notes.R;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.nookdevs.notes.provider.NotesUris.*;


/**
 * Activity for transforming the list of items of a node.  Deals with URIs of type
 * {@link com.nookdevs.notes.provider.Notes#CONTENT_TYPE_ITEMS}.
 *
 * @author Marco Goetze
 */
public class ItemsTransform extends BaseActivity
{
    /////////////////////////////////////////// METHODS ///////////////////////////////////////////

    // inherited methods...

    /** {@inheritDoc} */
    @NotNull @Override
    protected String getActivityTitle() {
        return getString(R.string.activity_items_transform);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // sanity checks...
        final Uri uri = getIntent().getData();
        if (!isItemsUri(uri)) throw new IllegalArgumentException("Unsupported URI: <" + uri + ">!");

        super.onCreate(savedInstanceState);

        // do transform (with confirmation)...
        int resIdTitle = -1, resIdMessage = -1;
        if (isSortItemsAlphabeticallyUri(uri) ||
            isSortItemsByCheckedUri(uri) ||
            isReverseItemsUri(uri))
        {
            resIdTitle = R.string.sort_items_title;
            resIdMessage = R.string.sort_items_message;
        } else if (isClearItemsUri(uri)) {
            resIdTitle = R.string.clear_items_title;
            resIdMessage = R.string.clear_items_message;
        }
        DialogInterface.OnClickListener yes = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
                boolean ok = getContentResolver().update(uri, null, null, null) > 0;
                setResult(ok ? RESULT_OK : RESULT_ERROR);
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
        confirm(resIdTitle, resIdMessage, yes, no);
    }
}

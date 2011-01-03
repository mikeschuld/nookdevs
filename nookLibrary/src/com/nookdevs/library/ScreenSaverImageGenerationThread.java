/*
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nookdevs.library;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.util.Log;

/**
 * <p>Thread fpr generating a screen saver image displaying the covers of recently-accessed books.
 * To this end, its constructor takes an array of cover file paths, in increasing order of access
 * times (i.e., the most-recently-accessed book's cover first).  The up to five first covers will
 * be considered.  If there is an error processing any of the files, the task will abort without
 * generating a screen saver file.</p>
 *
 * <p>The generated image will sport the most-recently-accessed book's cover in the center,
 * scaled down to at most three quarters of the screen's size and randomly rotated by a slight
 * amount.  Any further covers will be arranged behind it, again rotated slightly, and made
 * slightly lighter, in the following order: top-left, bottom-right, top-right, bottom-left.</p>
 *
 * <p>Does not derive from <code>AsyncTask</code> as there are issues with executing multiple
 * instances of the same class in Android 1.5.</p>
 *
 * @author Marco Götze
 */
public class ScreenSaverImageGenerationThread extends Thread
{
    /** The task's context. */
    protected final Context m_context;
    /** The (JPEG) out-file to generate. */
    protected final File m_outFile;
    /**
     * Array of cover files from which to generate the screen saver image (should be in
     * increasing order of access time).
     */
    protected final File[] m_inFiles;

    /** The width of the image to generate. */
    private static final int WIDTH  = 600;
    /** The height of the image to generate. */
    private static final int HEIGHT = 800;
    /**
     * The maximum width of a cover on the generated image.  Covers will be down-scaled to this
     * size, if necessary.
     */
    private static final int MAX_WIDTH  = WIDTH  * 3 / 4;
    /**
     * The maximum height of a cover on the generated image.  Covers will be down-scaled to this
     * size, if necessary.
     */
    private static final int MAX_HEIGHT = HEIGHT * 3 / 4;
    /**
     * The minimum width of a cover on the generated image.  Covers will be up-scaled to this size,
     * if necessary.
     */
    private static final int MIN_WIDTH = MAX_WIDTH / 3;
    /**
     * The minimum height of a cover on the generated image.  Covers will be up-scaled to this
     * size, if necessary.
     */
    private static final int MIN_HEIGHT = MAX_HEIGHT / 3;
    /**
     * The maximum absolute value of rotation of the most-recently-accessed cover.  The actual
     * rotation will be determined randomly in the range (-<code>MAX_DEGREES</code>,
     * <code>MAX_DEGREES</code>).  Less-recently-accessed covers may be rotated by an amount that
     * is greater than this by a constant factor.
     */
    private static final int MAX_DEGREES = 3;

    /** The log tag to use. */
    private static final String LOGTAG = "nookLibrary";

    /**
     * Creates a {@link ScreenSaverImageGenerationThread}.
     *
     * @param context the task's context
     * @param outFile the (JPEG) out-file to generate
     * @param inFiles the cover files from which to generate the screen saver image (should be in
     *                increasing order of access time)
     */
    public ScreenSaverImageGenerationThread(Context context, File outFile, File[] inFiles) {
        m_context = context;
        m_outFile = outFile;
        m_inFiles = inFiles;
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        if (generateScreenSaverImage()) {
            // notify the screen saver application of the change...
            m_context.sendBroadcast(
                new Intent("com.bravo.intent.action.SCREENSAVER_FOLDER_CHANGED"));
        }
    }

    /**
     * Generates the screen saver image.
     *
     * @return <code>true</code> on success, <code>false</code> otherwise
     */
    protected boolean generateScreenSaverImage() {
        if (m_inFiles.length < 1) {
            Log.e(LOGTAG, "Expecting at least one argument!");
            return false;
        }

        // initialize the bitmap to be created...
        Bitmap screenSaverImage = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenSaverImage);
        Paint paint = new Paint(0);
        // NOTE: For the background color, we generally prefer white, but if the image is expected
        //       to be covered more or less completely, fill in the gaps using gray.
        paint.setColor(m_inFiles.length > 4 ? Color.GRAY : Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint);

        // draw covers on the bitmap...
        int firstIdx = Math.min(m_inFiles.length - 1, 4);
        for (int i = firstIdx; i >= 0; i--) {
            // determine location and rotation...
            int x, y;
            float r;
            switch (i) {
                case 0:  // current cover: slightly rotated in center
                    x = WIDTH / 2;
                    y = HEIGHT / 2;
                    r = -MAX_DEGREES + (float) (Math.random() * 2 * MAX_DEGREES);
                    break;
                case 1:  // previous cover: slightly rotated counter-clockwise, towards upper left
                    x = (m_inFiles.length > 3 ? WIDTH / 4 : WIDTH * 3 / 10);
                    y = (m_inFiles.length > 3 ? HEIGHT / 4 : HEIGHT * 3 / 10);
                    r = -MAX_DEGREES - (float) (Math.random() * MAX_DEGREES);
                    break;
                case 2:  // two books back: slightly rotated clockwise, towards lower right
                    x = (m_inFiles.length > 3 ? WIDTH * 3 / 4 : WIDTH * 7 / 10);
                    y = (m_inFiles.length > 3 ? HEIGHT * 3 / 4 : HEIGHT * 7 / 10);
                    r = MAX_DEGREES + (float) (Math.random() * MAX_DEGREES);
                    break;
                case 3:  // three books back: slightly rotated counter-clockwise, towards lower left
                    x = WIDTH / 4;
                    y = HEIGHT * 3 / 4;
                    r = -MAX_DEGREES - (float) (Math.random() * MAX_DEGREES);
                    break;
                case 4:  // four books back: slightly rotated clockwise, towards upper right
                    x = WIDTH * 3 / 4;
                    y = HEIGHT / 4;
                    r = MAX_DEGREES + (float) (Math.random() * MAX_DEGREES);
                    break;
                default:  // any other cover: random location and rotation
                    x = (int) (Math.random() * WIDTH);
                    y = (int) (Math.random() * HEIGHT);
                    r = -(3 * MAX_DEGREES) + (float) (Math.random() * 6 * MAX_DEGREES);
                    break;
            }

            // load the cover...
            Bitmap cover = BitmapFactory.decodeFile(m_inFiles[i].getPath());
            if (cover == null) {
                Log.e(LOGTAG,
                      "Failed to decode cover \"" + m_inFiles[i] + "\" for screen saver image " +
                          "generation!");
                return false;
            }
            int w = cover.getWidth();
            int h = cover.getHeight();
            if (w == 0 || h == 0) {
                Log.e(LOGTAG, "Cover \"" + m_inFiles[i] + "\" has invalid dimensions!");
                return false;
            }

            // determine the cover's display size on the screen saver image...
            float scale = 1.0f;
            if (w > MAX_WIDTH || h > MAX_HEIGHT) {  // downscale if too large
                if (w * 1.0f / h > MAX_WIDTH * 1.0f / MAX_HEIGHT) {
                    scale = MAX_WIDTH * 1.0f / w;
                } else {
                    scale = MAX_HEIGHT * 1.0f / h;
                }
            } else if (w < MIN_WIDTH && h < MIN_HEIGHT) {  // upscale somewhat if too small
                if (w * 1.0f / h > MIN_WIDTH * 1.0f / MIN_HEIGHT) {
                    scale = MIN_WIDTH * 1.0f / w;
                } else {
                    scale = MIN_HEIGHT * 1.0f / h;
                }
            }

            // draw the cover...
            Matrix matrix = new Matrix();
            matrix.postTranslate(-(w / 2), -(h / 2));
            matrix.postScale(scale, scale);
            if (Math.abs(r) > 0.1) {  // only perform the rotation if non-negligible
                matrix.postRotate(r);
            }
            matrix.postTranslate(x, y);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(cover, matrix, paint);
            cover.recycle();

            // draw a border around the cover...
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            canvas.setMatrix(matrix);
            canvas.drawRect(0, 0, w, h, paint);

            // make the older covers slightly lighter...
            if (i > 0) {
                paint.setColor(Color.WHITE);
                paint.setAlpha((int) (Math.min(0.1f * i, 0.3f) * 255));
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawRect(0, 0, w, h, paint);
            }

            canvas.setMatrix(null);
        }

        // save the screen saver image...
        try {
            screenSaverImage.compress(Bitmap.CompressFormat.JPEG, 97,
                                      new FileOutputStream(m_outFile));
            screenSaverImage.recycle();
        } catch (FileNotFoundException e) {
            Log.e(LOGTAG, "Error saving screen saver image \"" + m_outFile + "\"!", e);
        }

        return true;
    }
}

/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEpdf 3.0 open source software code, released
 * May 1st, 2009. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2009 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"
 * License), in which case the provisions of the LGPL License are
 * applicable instead of those above. If you wish to allow use of your
 * version of this file only under the terms of the LGPL License and not to
 * allow others to use your version of this file under the MPL, indicate
 * your decision by deleting the provisions above and replace them with
 * the notice and other provisions required by the LGPL License. If you do
 * not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the LGPL License."
 *
 */
package org.icepdf.core.pobjects;

import org.icepdf.core.events.PaintPageEvent;
import org.icepdf.core.events.PaintPageListener;
import org.icepdf.core.io.SequenceInputStream;
import org.icepdf.core.pobjects.annotations.Annotation;
import org.icepdf.core.pobjects.graphics.Shapes;
import org.icepdf.core.pobjects.graphics.text.PageText;
import org.icepdf.core.util.ContentParser;
import org.icepdf.core.util.Library;
import org.icepdf.core.util.MemoryManageable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>This class represents the leaves of a <code>PageTree</code> object known
 * as <code>Page</code> class. The page dictionary specifies attributes
 * of the single page of the document.  Many of the page's attributes are
 * inherited from the page tree dictionary if not specified in the page
 * dictionary.</p>
 * <p/>
 * <p>The page object also provides a method which will extract a page's content,
 * such as text and images.  The <code>paint</code> method is the core of
 * the ICEpdf renderer, allowing page content to be painted to any Java graphics
 * context. </p>
 * <p/>
 * <p>Page objects in a PDF document have different boundaries defined which
 * govern various aspects of the pre-press process, such as cropping, bleed,
 * and trimming. Facilities for including printer's marks, such a registration
 * targets, gray ramps color bars, and cut marks which assist in the production
 * process.  When getting a page's size, the default boundary used is the crop
 * box which is what most viewer applications should use.  However, if your application
 * requires the use of different page boundaries, they can be specified when
 * using the getSize or paint methods.  If in doubt, always use the crop box
 * constant.</p>
 *
 * @see org.icepdf.core.pobjects.PageTree
 * @since 1.0
 */
public class Page extends Dictionary implements MemoryManageable {

    private static final Logger logger =
            Logger.getLogger(Page.class.toString());

    public static final Name ANNOTS_KEY = new Name("Annots");

    /**
     * Defines the boundaries of the physical medium on which the page is
     * intended to be displayed or printed.
     */
    public static final int BOUNDARY_MEDIABOX = 1;

    /**
     * Defines the visible region of the default user space. When the page
     * is displayed or printed, its contents are to be clipped to this
     * rectangle and then imposed on the output medium in some implementation
     * defined manner.
     */
    public static final int BOUNDARY_CROPBOX = 2;

    /**
     * Defines the region to which the contents of the page should be clipped
     * when output in a production environment (Mainly commercial printing).
     */
    public static final int BOUNDARY_BLEEDBOX = 3;

    /**
     * Defines the intended dimensions of the finished page after trimming.
     */
    public static final int BOUNDARY_TRIMBOX = 4;

    /**
     * Defines the extent of the page's meaningful content as intended by the
     * page's creator.
     */
    public static final int BOUNDARY_ARTBOX = 5;

    // Flag for call to init method, very simple cache
    private boolean isInited = false;

    // resources for page's parent pages, default fonts, etc.
    private Resources resources;

    // Vector of annotations
    private ArrayList<Annotation> annotations;

    // Contents
    private Vector<Stream> contents;
    // Container for all shapes stored on page
    private Shapes shapes = null;

    // the collection of objects listening for page paint events
    private Vector<PaintPageListener> paintPageListeners = new Vector<PaintPageListener>(8);

    // Defines the boundaries of the physical medium on which the page is
    // intended to be displayed on.
    private PRectangle1 mediaBox;
    // Defining the visible region of default user space.
    private PRectangle1 cropBox;
    // Defines the region to which the contents of the page should be clipped
    // when output in a production environment.
    private PRectangle1 bleedBox;
    // Defines the intended dimension of the finished page after trimming.
    private PRectangle1 trimBox;
    // Defines the extent of the pages meaningful content as intended by the
    // pages creator.
    private PRectangle1 artBox;

    // page has default rotation value
    private float pageRotation = 0;

    /**
     * Create a new Page object.  A page object represents a PDF object that
     * has the name page associated with it.  It also conceptually represents
     * a page entity and all of it child elements that are associated with it.
     *
     * @param l pointer to default library containing all document objects
     * @param h hashtable containing all of the dictionary entries
     */
    public Page(Library l, Hashtable h) {
        super(l, h);
    }

    /**
     * Dispose the Page.
     *
     * @param cache if true, cached files are removed; otherwise, objects are freed
     *              but object caches are left intact.
     */
    protected synchronized void dispose(boolean cache) {
        // Do not null out Library library reference here, without taking
        //   into account that MemoryManager.releaseAllByLibrary(Library)
        //   requires Page to still have Library library in getLibrary()
        // dispose only if the pages has been initiated
        if (isInited) {
            // un-init a page to free up memory
            isInited = false;
            // null data collections for page content
            if (annotations != null) {
                annotations.clear();
                annotations.trimToSize();
            }
            // work through contents and null any stream that have images in them
            if (contents != null) {
                //System.out.println("   Content size " + contents.size());
                for (Stream stream : contents) {
                    stream.dispose(cache);
                }
                contents.clear();
                contents.trimToSize();
            }

            // work through contents and null any stream that have images in them
            if (shapes != null) {
                shapes.dispose();
                shapes = null;
            }

            // work through resources and null any images in the image hash
            if (resources != null) {
                resources.dispose(cache, this);
                resources = null;
            }
        }
        // clear vector of listeners
        if (paintPageListeners != null) {
            paintPageListeners.clear();
            paintPageListeners.trimToSize();
        }
    }

    public boolean isInitiated() {
        return isInited;
    }

    private void initPageContents() throws InterruptedException {
        Object pageContent = library.getObject(entries, "Contents");

        // if a stream process it as needed
        if (pageContent instanceof Stream) {
            contents = new Vector<Stream>(1);
            Stream tmpStream = (Stream) pageContent;
            tmpStream.setPObjectReference(
                    library.getObjectReference(entries, "Contents"));
            contents.addElement(tmpStream);
        }
        // if a vector, process it as needed
        else if (pageContent instanceof Vector) {
            Vector conts = (Vector) pageContent;
            int sz = conts.size();
            contents = new Vector<Stream>(Math.max(sz, 1));
            // pull all of the page content references from the library
            for (int i = 0; i < sz; i++) {
                if (Thread.interrupted()) {
                    throw new InterruptedException("Page Content initialization thread interrupted");
                }
                Stream tmpStream = (Stream) library.getObject((Reference) conts.elementAt(i));
                tmpStream.setPObjectReference((Reference) conts.elementAt(i));
                contents.addElement(tmpStream);
            }
        }
    }

    private void initPageResources() throws InterruptedException {
        Resources res = library.getResources(entries, "Resources");
        if (res == null) {
            PageTree pt = getParent();
            while (pt != null) {
                if (Thread.interrupted()) {
                    throw new InterruptedException("Page Resource initialization thread interrupted");
                }
                Resources parentResources = pt.getResources();
                if (parentResources != null) {
                    res = parentResources;
                    break;
                }
                pt = pt.getParent();
            }
        }
        resources = res;
        if (resources != null) {
            resources.addReference(this);
        }
    }

    private void initPageAnnotations() throws InterruptedException {
        // find annotations in main library for our pages dictionary
        Object annots = library.getObject(entries, ANNOTS_KEY.getName());
        if (annots != null && annots instanceof Vector) {
            Vector v = (Vector) annots;
            annotations = new ArrayList<Annotation>(v.size() + 1);
            // add annotations
            Object annotObj;
            org.icepdf.core.pobjects.annotations.Annotation a = null;
            for (int i = 0; i < v.size(); i++) {

                if (Thread.interrupted()) {
                    throw new InterruptedException(
                            "Page Annotation initialization thread interrupted");
                }

                annotObj = v.elementAt(i);
                Reference ref = null;
                // we might have a reference
                if (annotObj instanceof Reference) {
                    ref = (Reference) v.elementAt(i);
                    annotObj = library.getObject(ref);
                }

                // but most likely its an annotations base class
                if (annotObj instanceof Annotation) {
                    a = (Annotation) annotObj;
                }
                // or build annotations from dictionary.
                else if (annotObj instanceof Hashtable) { // Hashtable lacks "Type"->"Annot" entry
                    a = Annotation.buildAnnotation(library, (Hashtable) annotObj);
                }
                // set the object reference, so we can save the state correct
                // and update any references accordingly. 
                if (ref != null) {
                    a.setPObjectReference(ref);
                }

                // add any found annotations to the vector.
                annotations.add(a);
            }
        }
    }

    /**
     * Initialize the Page object.  This method triggers the parsing of a page's
     * child elements.  Once a page has been initialized, it can be painted.
     */
    public synchronized void init() {
        try {
            // make sure we are not revisiting this method
            if (isInited) {
                return;
            }
//try { throw new RuntimeException("Page.init() ****"); } catch(Exception e) { e.printStackTrace(); }


            // get pages resources
            initPageResources();

            // annotations
            initPageAnnotations();

            // Get the value of the page's content entry
            initPageContents();

            /**
             * Finally iterate through the contents vector and concat all of the
             * the resourse streams together so that the content parser can
             * go to town and build all of the page's shapes.
             */

            if (contents != null) {
                Vector<InputStream> inputStreamsVec = new Vector<InputStream>(contents.size());
                for (Stream stream : contents) {
                    //byte[] streamBytes = stream.getBytes();
                    //ByteArrayInputStream input = new ByteArrayInputStream(streamBytes);
                    InputStream input = stream.getInputStreamForDecodedStreamBytes();
                    inputStreamsVec.add(input);
/*
                    InputStream input = stream.getInputStreamForDecodedStreamBytes();
                    InputStream[] inArray = new InputStream[] { input };////
                    String content = Utils.getContentAndReplaceInputStream( inArray, false );
                    input = inArray[0];
                    System.out.println("Page.init()  Stream: " + stream);
                    System.out.println("Page.init()  Content: " + content);
*/
                }
                SequenceInputStream sis = new SequenceInputStream(inputStreamsVec.iterator());

                // push the library and resources to the content parse
                // and return the the shapes vector for the screen elements
                // for the page/resources in question.
                try {
                    ContentParser cp = new ContentParser(library, resources);
                    shapes = cp.parse(sis);
                }
                catch (Exception e) {
                    shapes = new Shapes();
                    logger.log(Level.FINE, "Error initializing Page.", e);
                }
                finally {
                    try {
                        sis.close();
                    }
                    catch (IOException e) {
                        logger.log(Level.FINE, "Error closing page stream.", e);
                    }
                }
            }
            // empty page, nothing to do.
            else {
                shapes = new Shapes();
            }
            // set the initiated flag
            isInited = true;

        } catch (InterruptedException e) {
            // keeps shapes vector so we can paint what we have but make init state as false
            // so we can try to re parse it later.
            isInited = false;
            logger.log(Level.SEVERE, "Page initializing thread interrupted.", e);
        }

    }


    /**
     * Adds an annotation that was previously added to the document.  It is
     * assumed that the annotation has a valid object reference.  This
     * is commonly used with the undo/redo state manager in the RI.  Use
     * the method @link{#createAnnotation} for creating new annotations.
     *
     * @param newAnnotation
     * @return reference to annotaiton that was added.
     */
    public Annotation addAnnotation(Annotation newAnnotation) {

        // make sure the page annotations have been initialized.
        if (!isInited) {
            try {
                initPageAnnotations();
            } catch (InterruptedException e) {
                logger.warning("Annotation Initialization interupted");
            }
        }

        StateManager stateManager = library.getStateManager();

        Object annots = library.getObject(entries, ANNOTS_KEY.getName());
        boolean isAnnotAReference = library.isReference(entries, ANNOTS_KEY.getName());

        // does the page not already have an annotations or if the annots
        // dictionary is indirect.  If so we have to add the page to the state
        // manager
        if (!isAnnotAReference && annots != null) {
            // get annots array from page
            if (annots instanceof Vector) {
                // update annots dictionary with new annotations reference,
                Vector v = (Vector) annots;
                v.add(newAnnotation.getPObjectReference());
                // add the page as state change
                stateManager.addChange(
                        new PObject(this, this.getPObjectReference()));
            }
        } else if (isAnnotAReference && annots != null) {
            // get annots array from page
            if (annots instanceof Vector) {
                // update annots dictionary with new annotations reference,
                Vector v = (Vector) annots;
                v.add(newAnnotation.getPObjectReference());
                // add the annotations reference dictionary as state has changed
                stateManager.addChange(
                        new PObject(annots, library.getObjectReference(
                                entries, ANNOTS_KEY.getName())));
            }
        }
        // we need to add the a new annots reference
        else {
            Vector annotsVector = new Vector(4);
            annotsVector.add(newAnnotation.getPObjectReference());

            // create a new Dictionary of annotaions using an external reference
            PObject annotsPObject = new PObject(annotsVector,
                    stateManager.getNewReferencNumber());

            // add the new dictionary to the page
            entries.put(ANNOTS_KEY, annotsPObject.getReference());
            // add it to the library so we can resolve the reference
            library.addObject(annotsVector, annotsPObject.getReference());

            // add the page and the new dictionary to the state change
            stateManager.addChange(
                    new PObject(this, this.getPObjectReference()));
            stateManager.addChange(annotsPObject);

            annotations = new ArrayList<Annotation>();
        }

        // update parent page reference.
        newAnnotation.getEntries().put(Annotation.PARENT_PAGE_KEY,
                this.getPObjectReference());

        // add the annotations to the parsed annotations list
        annotations.add(newAnnotation);

        // add the new annotations to the library
        library.addObject(newAnnotation, newAnnotation.getPObjectReference());

        // finally add the new annotations to the state manager
        stateManager.addChange(new PObject(newAnnotation, newAnnotation.getPObjectReference()));

        // return to caller for further manipulations.
        return newAnnotation;
    }

    /**
     * Deletes the specified annotation instance from his page.  If the
     * annotation was origional then either the page or the annot ref object
     * is also added to the state maanger.  If the annotation was new then
     * we just have to update the page and or annot reference as the objects
     * will allready be in the state manager.
     */
    public void deleteAnnotation(Annotation annot) {

        // make sure the page annotations have been initialized.
        if (!isInited) {
            try {
                initPageAnnotations();
            } catch (InterruptedException e) {
                logger.warning("Annotation Initialization interupted");
            }
        }

        StateManager stateManager = library.getStateManager();

        Object annots = getObject(ANNOTS_KEY);
        boolean isAnnotAReference =
                library.isReference(entries, ANNOTS_KEY.getName());

        // mark the item as deleted so the state manager can clean up the reference.
        annot.setDeleted(true);

        // check to see if this is an existing annotations, if the annotations
        // is existing then we have to mark either the page or annot ref as chagned.
        if (!annot.isNew() && !isAnnotAReference) {
            // add the page as state change
            stateManager.addChange(
                    new PObject(this, this.getPObjectReference()));
        }
        // if not new and annot is a ref, we have to add annot ref as changed.
        else if (!annot.isNew() && isAnnotAReference) {
            stateManager.addChange(
                    new PObject(annots, library.getObjectReference(
                            entries, ANNOTS_KEY.getName())));
        }
        // removed the annotations from the annots vector
        if (annots instanceof Vector) {
            // update annots dictionary with new annotations reference,
            Vector v = (Vector) annots;
            v.remove(annot.getPObjectReference());
        }

        // remove the annotations form the annotation cache in the page object
        if (annotations != null) {
            annotations.remove(annot);
        }

        // finally remove it from the library, probably not necessary....
//        library.removeObject(annot.getPObjectReference());

    }

    /**
     * Updates the annotation associated with this page.  If the annotation
     * is not in this page then the annotation is no added.
     *
     * @param annotation annotation object that should be updated for this page.
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateAnnotation(Annotation annotation) {
        // bail on null annotations
        if (annotation == null) {
            return false;
        }

        // make sure the page annotations have been initialized.
        if (!isInited) {
            try {
                initPageAnnotations();
            } catch (InterruptedException e) {
                logger.warning("Annotation Initialization interupted");
            }
        }

        StateManager stateManager = library.getStateManager();
        // if we are doing an update we have at least on annot
        Vector<Reference> annots = (Vector)
                library.getObject(entries, ANNOTS_KEY.getName());

        // make sure annotations is in part of page.
        boolean found = false;
        for (Reference ref : annots) {
            if (ref.equals(annotation.getPObjectReference())) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }

        // check the state manager for an instance of this object
        if (stateManager.contains(annotation.getPObjectReference())) {
            // if found we just have to re add the object, foot work around
            // page and annotations creation has already been done.
            stateManager.addChange(
                    new PObject(annotation, annotation.getPObjectReference()));
            return true;
        }
        // we have to do the checks for page and annot dictionary entry.
        else {
            // update parent page reference.
            annotation.getEntries().put(Annotation.PARENT_PAGE_KEY,
                    this.getPObjectReference());

            // add the annotations to the parsed annotations list
            annotations.add(annotation);

            // add the new annotations to the library
            library.addObject(annotation, annotation.getPObjectReference());

            // finally add the new annotations to the state manager
            stateManager.addChange(new PObject(annotation, annotation.getPObjectReference()));

            return true;
        }
    }

    /**
     * Gets a reference to the page's parent page tree.  A reference can be resolved
     * by the Library class.
     *
     * @return reference to parent page tree.
     * @see org.icepdf.core.util.Library
     */
    protected Reference getParentReference() {
        return (Reference) entries.get("Parent");
    }

    /**
     * Gets the page's parent page tree.
     *
     * @return parent page tree.
     */
    public PageTree getParent() {
        // retrieve a pointer to the pageTreeParent
        return (PageTree) library.getObject(entries, "Parent");
    }

 
    /**
     * Returns a summary of the page dictionary entries.
     *
     * @return dictionary entries.
     */
    public String toString() {
        return "PAGE= " + entries.toString();
    }

    /**
     * Gets the total rotation factor of the page after applying a user rotation
     * factor.  This method will normalize rotation factors to be in the range
     * of 0 to 360 degrees.
     *
     * @param userRotation rotation factor to be applied to page
     * @return Total Rotation, representing pageRoation + user rotation
     *         factor applied to the whole document.
     */
    public float getTotalRotation(float userRotation) {
        float totalRotation = getPageRotation() + userRotation;

        // correct to keep in rotation in 360 range.
        totalRotation %= 360;

        if (totalRotation < 0)
            totalRotation += 360;

        // If they calculated the degrees from radians or whatever,
        // then we need to make our even rotation comparisons work
        if (totalRotation >= -0.001f && totalRotation <= 0.001f)
            return 0.0f;
        else if (totalRotation >= 89.99f && totalRotation <= 90.001f)
            return 90.0f;
        else if (totalRotation >= 179.99f && totalRotation <= 180.001f)
            return 180.0f;
        else if (totalRotation >= 269.99f && totalRotation <= 270.001f)
            return 270.0f;

        return totalRotation;
    }

    private float getPageRotation() {
        // Get the pages default orientation if available, if not defined
        // then it is zero.
        Object tmpRotation = library.getObject(entries, "Rotate");
        if (tmpRotation != null) {
            pageRotation = ((Number) tmpRotation).floatValue();
//            System.out.println("Page Rotation  " + pageRotation);
        }
        // check parent to see if value has been set
        else {
            PageTree pageTree = getParent();
            while (pageTree != null) {
                if (pageTree.isRotationFactor) {
                    pageRotation = pageTree.rotationFactor;
                    break;
                }
                pageTree = pageTree.getParent();
            }
        }
        // PDF specifies rotation as clockwise, but Java2D does it
        //  counter-clockwise, so normalise it to Java2D
        pageRotation = 360 - pageRotation;
        pageRotation %= 360;
//        System.out.println("New Page Rotation " + pageRotation);
        return pageRotation;
    }

    /**
     * Gets all annotation information associated with this page.  Each entry
     * in the vector represents one annotation. The size of the vector represents
     * the total number of annotations associated with the page.
     *
     * @return annotation associated with page; null, if there are no annotations.
     */
    public ArrayList<Annotation> getAnnotations() {
        if (!isInited) {
            init();
        }
        return annotations;
    }


    /**
     * Gest the PageText data structure for this page.  PageText is made up
     * of lines, words and glyphs which can be used for searches, text extraction
     * and text highlighting.  The coordinates system has been normalized
     * to page space.
     *
     * @return list of text sprites for the given page.
     */
    public synchronized PageText getViewText() {
        if (!isInited) {
            init();
        }
        return shapes.getPageText();
    }

    /**
     * Gest the PageText data structure for this page using an accelerated
     * parsing technique that ignores some text elements. This method should
     * be used for straight text extraction.
     *
     * @return vector of Strings of all text objects inside the specified page.
     */
    public synchronized PageText getText() {

        // we only do this once per page
        if (isInited) {
            if (shapes != null && shapes.getPageText() != null) {
                return shapes.getPageText();
            }
        }

        Shapes textBlockShapes = null;
        try {
            /**
             * Finally iterate through the contents vector and concat all of the
             * the resouse streams together so that the contant parser can
             * go to town and build all of the pages shapes.
             */
            if (contents == null) {
                // Get the value of the page's content entry
                initPageContents();
            }

            if (resources == null) {
                // get pages resources
                initPageResources();
            }

            if (contents != null) {
                Vector<InputStream> inputStreamsVec =
                        new Vector<InputStream>(contents.size());
                for (int st = 0, max = contents.size(); st < max; st++) {
                    Stream stream = contents.elementAt(st);
                    InputStream input = stream.getInputStreamForDecodedStreamBytes();
                    inputStreamsVec.add(input);
                }
                SequenceInputStream sis = new SequenceInputStream(inputStreamsVec.iterator());

                // push the library and resources to the content parse
                // and return the the shapes vector for the screen elements
                // for the page/resources in question.
                try {
                    ContentParser cp = new ContentParser(library, resources);
                    // custom parsing for text extraction, should be faster
                    textBlockShapes = cp.parseTextBlocks(sis);
                }
                catch (Exception e) {
                    logger.log(Level.FINE, "Error getting page text.", e);
                }
                finally {
                    try {
                        sis.close();
                    }
                    catch (IOException e) {
                        logger.log(Level.FINE, "Error closing page stream.", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            // keeps shapes vector so we can paint what we have but make init state as false
            // so we can try to reparse it later.
            isInited = false;
            logger.log(Level.SEVERE, "Page text extraction thread interrupted.", e);
        }
        if (textBlockShapes != null && textBlockShapes.getPageText() != null) {
            return textBlockShapes.getPageText();
        } else {
            return null;
        }
    }

    /**
     * Gets a vector of Images where each index represents an image  inside
     * this page.
     *
     * @return vector of Images inside the current page
     */
    public synchronized Vector getImages() {
        if (!isInited) {
            init();
        }
        return shapes.getImages();
    }

    public Resources getResources() {
        return resources;
    }

    /**
     * Reduces the amount of memory used by this object.
     */
    public void reduceMemory() {
        dispose(true);
    }

    public synchronized void addPaintPageListener(PaintPageListener listener) {
        // add a listener if it is not already registered
        if (!paintPageListeners.contains(listener)) {
            paintPageListeners.addElement(listener);
        }
    }

    public synchronized void removePaintPageListener(PaintPageListener listener) {
        // remove a listener if it is already registered
        if (paintPageListeners.contains(listener)) {
            paintPageListeners.removeElement(listener);
        }
    }

    public void notifyPaintPageListeners() {
        // create the event object
        PaintPageEvent evt = new PaintPageEvent(this);

        // make a copy of the listener object vector so that it cannot
        // be changed while we are firing events
        // NOTE: this is good practise, but most likely a little to heavy
        //       for this event type
//        Vector v;
//        synchronized (this) {
//            v = (Vector) paintPageListeners.clone();
//        }
//
//        // fire the event to all listeners
//        PaintPageListener client;
//        for (int i = v.size() - 1; i >= 0; i--) {
//            client = (PaintPageListener) v.elementAt(i);
//            client.paintPage(evt);
//        }

        // fire the event to all listeners
        PaintPageListener client;
        for (int i = paintPageListeners.size() - 1; i >= 0; i--) {
            client = paintPageListeners.elementAt(i);
            client.paintPage(evt);
        }
    }
}

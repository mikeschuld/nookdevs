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
package org.icepdf.core.pobjects.fonts;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepdf.core.pobjects.Stream;
import org.icepdf.core.util.Defs;
import org.icepdf.core.util.Library;

/**
 * Simple Factory for loading of font library if present.
 */
public class FontFactory {
    
    private static final Logger logger = Logger.getLogger(FontFactory.class.toString());
    
    // allow scaling of large images to improve clarity on screen
    private static boolean awtFontLoading;
    
    // dynamic property to switch between font engine and awt font substitution.
    private static boolean awtFontSubstitution;
    
    static {
        // turn on font file loading using awt, can cause the jvm to crash
        // if the font file is corrupt.
        awtFontLoading = Defs.sysPropertyBoolean("org.icepdf.core.awtFontLoading", false);
        
    }
    
    public static final int FONT_OPEN_TYPE = 5;
    public static final int FONT_TYPE_0 = 6;
    public static final int FONT_TYPE_3 = 7;
    
    // Singleton instance of class
    private static FontFactory fontFactory;
    
    // NFont class path
    private static final String FONT_CLASS = "org.icepdf.core.pobjects.fonts.nfont.Font";
    private static final String NFONT_CLASS = "org.icepdf.core.pobjects.fonts.nfont.NFont";
    
    private static final String NFONT_OPEN_TYPE = "org.icepdf.core.pobjects.fonts.nfont.NFontOpenType";
    
    private static final String NFONT_TRUE_TYPE = "org.icepdf.core.pobjects.fonts.nfont.NFontTrueType";
    
    private static final String NFONT_TRUE_TYPE_0 = "org.icepdf.core.pobjects.fonts.nfont.NFontType0";
    
    private static final String NFONT_TRUE_TYPE_1 = "org.icepdf.core.pobjects.fonts.nfont.NFontType1";
    
    private static final String NFONT_TRUE_TYPE_3 = "org.icepdf.core.pobjects.fonts.nfont.NFontType3";
    
    static {
        // check class bath for NFont library, and declare results.
        try {
            Class.forName(NFONT_CLASS);
        } catch (ClassNotFoundException e) {
            logger.log(Level.FINE, "NFont font library was not found on the class path");
        }
    }
    
    private static boolean foundNFont;
    
    /**
     * <p>
     * Returns a static instance of the FontManager class.
     * </p>
     * 
     * @return instance of the FontManager.
     */
    public static FontFactory getInstance() {
        // make sure we have initialized the manager
        if (fontFactory == null) {
            fontFactory = new FontFactory();
        }
        return fontFactory;
    }
    
    private FontFactory() {
    }
    
    public Font getFont(Library library, Hashtable entries) {
        
        Font fontDictionary = null;
        
        if (foundFontEngine()) {
            // load each know file type reflectively.
            try {
                Class fontClass = Class.forName(FONT_CLASS);
                Class[] fontArgs = {
                    Library.class, Hashtable.class
                };
                Constructor fontClassConstructor = fontClass.getDeclaredConstructor(fontArgs);
                Object[] fontUrl = {
                    library, entries
                };
                fontDictionary = (Font) fontClassConstructor.newInstance(fontUrl);
            } catch (Throwable e) {
                logger.log(Level.FINE, "Could not load font dictionary class", e);
            }
        } else {
            // create OFont implementation.
            fontDictionary = null;
            // new org.icepdf.core.pobjects.fonts.ofont.Font(library, entries);
        }
        return fontDictionary;
    }
    
    public FontFile createFontFile(Stream fontStream, int fontType) {
        FontFile fontFile = null;
        if (foundFontEngine()) {
            try {
                Class fontClass = getNFontClass(fontType);
                if (fontClass != null) {
                    // convert the stream to byte[]
                    Class[] bytArrayArg = {
                        byte[].class
                    };
                    Constructor fontClassConstructor = fontClass.getDeclaredConstructor(bytArrayArg);
                    Object[] fontStreamBytes = {
                        fontStream.getBytes()
                    };
                    if (fontStream.getBytes().length > 0) {
                        fontFile = (FontFile) fontClassConstructor.newInstance(fontStreamBytes);
                    }
                }
            } catch (Throwable e) {
                logger.log(Level.FINE, "Could not create instance oof font file " + fontType, e);
            }
        }
        return fontFile;
    }
    
    public FontFile createFontFile(File file, int fontType) {
        FontFile fontFile = null;
        if (foundFontEngine()) {
            try {
                Class fontClass = getNFontClass(fontType);
                if (fontClass != null) {
                    // convert the stream to byte[]
                    Class[] urlArg = {
                        URL.class
                    };
                    Constructor fontClassConstructor = fontClass.getDeclaredConstructor(urlArg);
                    Object[] fontUrl = {
                        file.toURL()
                    };
                    fontFile = (FontFile) fontClassConstructor.newInstance(fontUrl);
                }
            } catch (Throwable e) {
                logger.log(Level.FINE, "Could not create instance oof font file " + fontType, e);
            }
        } else {
        }
        return fontFile;
    }
    
    public boolean isAwtFontSubstitution() {
        return awtFontSubstitution;
    }
    
    public void setAwtFontSubstitution(boolean awtFontSubstitution) {
        FontFactory.awtFontSubstitution = awtFontSubstitution;
    }
    
    public void toggleAwtFontSubstitution() {
        awtFontSubstitution = !awtFontSubstitution;
    }
    
    private Class getNFontClass(int fontType) throws ClassNotFoundException {
        Class fontClass = null;
        if (FONT_OPEN_TYPE == fontType) {
            fontClass = Class.forName(NFONT_OPEN_TYPE);
        } else if (FONT_TYPE_0 == fontType) {
            fontClass = Class.forName(NFONT_TRUE_TYPE_0);
        } else if (FONT_TYPE_3 == fontType) {
            fontClass = Class.forName(NFONT_TRUE_TYPE_3);
        }
        return fontClass;
    }
    
    private String fontTypeToString(int fontType) {
        
        if (fontType == FONT_OPEN_TYPE) {
            return "Open Type Font";
        } else if (fontType == FONT_TYPE_0) {
            return "Type 0 Font";
        } else if (fontType == FONT_TYPE_3) {
            return "Type 3 Font";
        } else {
            return "unkown font type: " + fontType;
        }
    }
    
    /**
     * Test if font engine is available on the class path and it has been
     * disabled with the proeprty awtFontSubstitution.
     * 
     * @return true if font engine was found, false otherwise.
     */
    public boolean foundFontEngine() {
        // check class bath for NFont library
        try {
            Class.forName(NFONT_CLASS);
            foundNFont = true;
        } catch (ClassNotFoundException e) {
        }
        
        return foundNFont && !awtFontSubstitution;
    }
    
}

/**
 * write by Li Wenhao. 2009.9
 */

#include <config.h>

#include "GlobalParams.h"
#include "GfxFont.h"
#include "goo/GooHash.h"

//#include <core/SkTypeface.h>

#ifdef USE_GCC_PRAGMAS
#pragma implementation
#endif

#if MULTITHREADED
#  define lockGlobalParams            gLockMutex(&mutex)
#  define lockUnicodeMapCache         gLockMutex(&unicodeMapCacheMutex)
#  define lockCMapCache               gLockMutex(&cMapCacheMutex)
#  define unlockGlobalParams          gUnlockMutex(&mutex)
#  define unlockUnicodeMapCache       gUnlockMutex(&unicodeMapCacheMutex)
#  define unlockCMapCache             gUnlockMutex(&cMapCacheMutex)
#else
#  define lockGlobalParams
#  define lockUnicodeMapCache
#  define lockCMapCache
#  define unlockGlobalParams
#  define unlockUnicodeMapCache
#  define unlockCMapCache
#endif

/* Android implementation of external font matching code */
DisplayFontParam *GlobalParams::getDisplayFont(GfxFont *font) {
    DisplayFontParam *  dfp;
    GooString *         fontName = font->getName();

    if (!fontName) return NULL;

    lockGlobalParams;

    /*
    SkTypeface *typeface = SkTypeface::CreateFromName(fontName->getCString(),
						      SkTypeface::kNormal);
    */
    dfp = (DisplayFontParam *)displayFonts->lookup(fontName);
    if (!dfp) {
        dfp = new DisplayFontParam(fontName, displayFontTT);
	displayFonts->add(fontName, dfp);
    }

    unlockGlobalParams;

    return dfp;
}


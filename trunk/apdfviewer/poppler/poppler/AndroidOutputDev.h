/*
 * Copyright (C) 2009 Li Wenhao <liwenhao.g@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street - Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */
#ifndef ANDROIDOUTPUTDEV_H
#define ANDROIDOUTPUTDEV_H

#ifdef USE_GCC_PRAGMAS
#pragma interface
#endif

#include <goo/GooVector.h>
#include <poppler/GfxState.h>
#include <poppler/OutputDev.h>

#include <SkCanvas.h>
#include <SkPaint.h>


class AndroidOutputDev: public OutputDev {
public:

	// Constructor.
	AndroidOutputDev(SkCanvas *canvas, XRef *xref);

	// Destructor.
	virtual ~AndroidOutputDev();

	//----- info about output device

	// Does this device use upside-down coordinates?
	virtual GBool upsideDown() { return gTrue; }

	// Does this device use drawChar() or drawString()?
	virtual GBool useDrawChar() { return gTrue; }

	// Does this device use beginType3Char/endType3Char?
	virtual GBool interpretType3Chars() { return gFalse; }

	// This device now supports text in pattern colorspace!
	virtual GBool supportTextCSPattern(GfxState *state) 
	{
		return state->getFillColorSpace()->getMode() == csPattern;
	}


	//----- initialization and control

	// Start a page.
	virtual void startPage(int pageNum, GfxState *state);

	// End a page.
	virtual void endPage();

	//----- link borders
	virtual void drawLink(Link *link, Catalog *catalog);

	//----- save/restore graphics state
	virtual void saveState(GfxState *state);
	virtual void restoreState(GfxState *state);

	//----- path painting
	virtual void stroke(GfxState *state);
	virtual void fill(GfxState *state);
	virtual void eoFill(GfxState *state);

	//----- path clipping
	virtual void clip(GfxState *state);
	virtual void eoClip(GfxState *state);

	//----- text drawing
	virtual void drawChar(GfxState *state, double x, double y,
			      double dx, double dy,
			      double originX, double originY,
			      CharCode code, int nBytes, Unicode *u, int uLen);

	//----- image drawing
	virtual void drawImageMask(GfxState *state, Object *ref, Stream *str,
				   int width, int height, GBool invert,
				   GBool interpolate, GBool inlineImg);
	virtual void drawImage(GfxState *state, Object *ref, Stream *str,
			       int width, int height, GfxImageColorMap *colorMap,
			       GBool interpolate, int *maskColors, GBool inlineImg);
   
private:
	SkCanvas *m_canvas;
	SkPaint m_paint;
	XRef *m_xref;			// xref table for current document
};

#endif

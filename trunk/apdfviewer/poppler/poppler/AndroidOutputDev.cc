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
#include <config.h>

#ifdef USE_GCC_PRAGMAS
#pragma implementation
#endif

#include <poppler/GfxState.h>
#include <poppler/GfxFont.h>

#include <goo/GooString.h>
#include <goo/GooHash.h>
#include <goo/GooVector.h>

#include <SkTypeface.h>
#include <SkStream.h>
#include <SkColorPriv.h>

#define LOG_NDEBUG 0
#define LOG_TAG "PDFDocument"
#include <cutils/log.h>

#include "AndroidOutputDev.h"


#define GfxRGB2SkColor(rgb) (SkColorSetRGB(colToByte(rgb.r),colToByte(rgb.g),colToByte(rgb.b)))

// global table to cache fonts
static GooHash g_font_table(gTrue);

// helper functions
static SkPaint::Join getLineJoin(GfxState *state)
{
	SkPaint::Join join = SkPaint::kDefault_Join;

	switch (state->getLineJoin()) {
	case 0:
		join = SkPaint::kMiter_Join;
		break;
	case 1:
		join = SkPaint::kRound_Join;
		break;
	case 2:
		join = SkPaint::kBevel_Join;
		break;
	}

	return join;
}

static SkPaint::Cap getLineCap(GfxState *state)
{
	SkPaint::Cap cap = SkPaint::kDefault_Cap;

	switch (state->getLineCap()) {
	case 0:
		cap = SkPaint::kButt_Cap;
		break;
	case 1:
		cap = SkPaint::kRound_Cap;
		break;
	case 2:
		cap = SkPaint::kSquare_Cap;
		break;
  	}

	return cap;
}

static SkTypeface *getFont(GfxState *state, XRef *xref)
{
	GfxFont *gfxFont;
	GfxFontType fontType;
	GooString *fileName;
	Ref embRef;
	char *tmpBuf = NULL;
	int tmpBufLen;
	SkTypeface *typeface = 0;

	if (!(gfxFont = state->getFont())) {
		return 0;
	}

	// cached?
	GooString *font_id_str = GooString::fromInt(gfxFont->getID()->num);
	typeface = (SkTypeface *)g_font_table.lookupInt(font_id_str);

	// create font
	if (!typeface) {
		if (gfxFont->getEmbeddedFontID(&embRef)) {
			tmpBuf = gfxFont->readEmbFontFile(xref, &tmpBufLen);
			if (!tmpBuf)
				return 0;

			SkMemoryStream *fs = new SkMemoryStream(tmpBuf, tmpBufLen);
			typeface = SkTypeface::CreateFromStream(fs);
		} else {
			LOGV("Not embed font!");
			SkTypeface::Style style = SkTypeface::kNormal;
			if(gfxFont->isItalic())
				style = SkTypeface::kItalic;
			if(gfxFont->isBold())
				style = SkTypeface::kBold;
			if(gfxFont->isBold() && gfxFont->isItalic())
				style = SkTypeface::kBoldItalic;

			typeface = SkTypeface::CreateFromName(gfxFont->getName()->getCString(),style);
		}

		g_font_table.add(font_id_str, (int)typeface);
	} else {
		delete font_id_str;
	}

	return typeface;
}

static SkPath getPath(GfxState *state, GfxPath *path, SkPath::FillType fillType)
{
	GfxSubpath *subpath;
	double x1, y1, x2, y2, x3, y3;
	int i, j;

	SkPath skPath;
	skPath.setFillType(fillType);
	for (i = 0; i < path->getNumSubpaths(); ++i) {
		subpath = path->getSubpath(i);
		if (subpath->getNumPoints() > 0) {
			state->transform(subpath->getX(0), subpath->getY(0), &x1, &y1);
			skPath.moveTo(x1, y1);
			j = 1;
			while (j < subpath->getNumPoints()) {
				if (subpath->getCurve(j)) {
					state->transform(subpath->getX(j), subpath->getY(j), &x1, &y1);
					state->transform(subpath->getX(j+1), subpath->getY(j+1), &x2, &y2);
					state->transform(subpath->getX(j+2), subpath->getY(j+2), &x3, &y3);
					skPath.cubicTo( x1, y1, x2, y2, x3, y3);
					j += 3;
				} else {
					state->transform(subpath->getX(j), subpath->getY(j), &x1, &y1);
					skPath.lineTo(x1, y1);
					++j;
				}
			}
			if (subpath->isClosed()) {
				skPath.close();
			}
		}
	}

	return skPath;
}

static void doFill(SkCanvas *canvas, GfxState *state, SkPath::FillType type)
{
	// path
	SkPath path = getPath(state, state->getPath(), type);

	// paint
	SkPaint paint;
	GfxRGB rgb;
	// color
	state->getFillRGB(&rgb);
	paint.setColor(GfxRGB2SkColor(rgb));
	// style
	paint.setStyle(SkPaint::kFill_Style);
	// pattern
	// TODO: more work here.
	// opacity
	paint.setAlpha((U8CPU)(state->getFillOpacity()*255));

	// draw
	canvas->drawPath(path, paint);
}


// AndroidOutputDev

AndroidOutputDev::AndroidOutputDev(SkCanvas *canvas, XRef *xref):
	m_canvas(canvas), m_xref(xref)
{
}

AndroidOutputDev::~AndroidOutputDev()
{
}

void AndroidOutputDev::startPage(int pageNum, GfxState *state)
{
	// clear screen
	m_canvas->drawColor(SK_ColorWHITE);
}

void AndroidOutputDev::endPage() {
}

void AndroidOutputDev::drawLink(Link *link, Catalog *catalog)
{
}

void AndroidOutputDev::saveState(GfxState *state)
{
	m_canvas->save();
}

void AndroidOutputDev::restoreState(GfxState *state)
{
	m_canvas->restore();
}

void AndroidOutputDev::stroke(GfxState *state)
{
	// path
	SkPath path = getPath(state, state->getPath(), SkPath::kEvenOdd_FillType);

	// paint
	SkPaint paint;
	GfxRGB rgb;
	// color
	state->getStrokeRGB(&rgb);
	paint.setColor(GfxRGB2SkColor(rgb));
	// style
	paint.setStyle(SkPaint::kStroke_Style);
	// pattern
	// TODO: more work here.
	// opacity
	paint.setAlpha((U8CPU)(state->getStrokeOpacity()*255));
	// line width
	paint.setStrokeWidth(state->getLineWidth());
	// cap
	paint.setStrokeCap(getLineCap(state));
	// join
	paint.setStrokeJoin(getLineJoin(state));
	// miter
	paint.setStrokeMiter(state->getMiterLimit());

	// draw
	m_canvas->drawPath(path, paint);
}

void AndroidOutputDev::fill(GfxState *state)
{
	doFill(m_canvas, state, SkPath::kWinding_FillType);
}

void AndroidOutputDev::eoFill(GfxState *state)
{
	doFill(m_canvas, state, SkPath::kEvenOdd_FillType);
}

void AndroidOutputDev::clip(GfxState *state)
{
	// path
	SkPath path = getPath(state, state->getPath(), SkPath::kWinding_FillType);

	// clip
	m_canvas->clipPath(path);
}

void AndroidOutputDev::eoClip(GfxState *state)
{
	// path
	SkPath path = getPath(state, state->getPath(), SkPath::kEvenOdd_FillType);

	// clip
	m_canvas->clipPath(path);
}

void AndroidOutputDev::drawChar(GfxState *state, double x, double y,
				double dx, double dy,
				double originX, double originY,
				CharCode code, int nBytes, Unicode *u, int uLen) {

	double x1, y1;
	SkPaint paint;
	GfxRGB rgb;

//	LOGV("Char: %d, %d, %d, %d", code, nBytes, *u, uLen);

	// font
	paint.setTextEncoding(SkPaint::kUTF16_TextEncoding);
	paint.setTypeface(getFont(state, m_xref));
	paint.setTextSize(state->getTransformedFontSize());
	paint.setAntiAlias(true);

	int render = state->getRender();
	if (render == 3) {
		// invisible text
		return;
	}

	x -= originX;
	y -= originY;
	state->transform(x, y, &x1, &y1);

	// fill
	if (!(render & 1)) {
		// color
		state->getFillRGB(&rgb);
		paint.setColor(GfxRGB2SkColor(rgb));
	}

	// stroke
	if ((render & 3) == 1 || (render & 3) == 2) {
		// color
		state->getStrokeRGB(&rgb);
		paint.setColor(GfxRGB2SkColor(rgb));
	}

	// clip
	if (render & 4) {
		clip(state);
	}

	// draw
	m_canvas->drawText(u, uLen*sizeof(Unicode), x1, y1, paint);
}

void AndroidOutputDev::drawImageMask(GfxState *state, Object *ref, Stream *str,
				     int width, int height, GBool invert,
				     GBool interpolate, GBool inlineImg)
{
}

void AndroidOutputDev::drawImage(GfxState *state, Object *ref, Stream *str,
		int width, int height, GfxImageColorMap *colorMap, GBool interpolate,
		int *maskColors, GBool inlineImg) {
	// create bitmap
	SkBitmap bitmap;
	unsigned int *pixels;

	bitmap.setConfig(SkBitmap::kARGB_8888_Config, width, height);
	if (!bitmap.allocPixels()) {
		//TODO: show error
		return;
	}

	pixels = (unsigned int *)bitmap.getPixels();

	ImageStream *stream = new ImageStream(str, width, colorMap->getNumPixelComps(),
			colorMap->getBits());
	stream->reset();

	int x, y, i;
	Guchar *pix;
	for (y = 0; y < height; y++) {
		pix = stream->getLine();

		for (x = 0; x < width; x++) {
			GfxRGB rgb;
			colorMap->getRGB(pix, &rgb);

			Guchar a = 0xFF;

			Guchar *dest = (Guchar*)(pixels + y*width + x);
			dest[0] = colToByte(rgb.r);
			dest[1] = colToByte(rgb.g);
			dest[2] = colToByte(rgb.b);
			dest[3] = a;

			pix += colorMap->getNumPixelComps();
		}
	}
	delete stream;

	// draw
	double *ctm;
	SkMatrix matrix;
	matrix.reset();

	ctm = state->getCTM();
	matrix.postScale(ctm[0]/width, -ctm[3]/height);
	matrix.postTranslate(ctm[4], ctm[3]+ctm[5]);
	// TODO: rotate

	m_canvas->drawBitmapMatrix(bitmap, matrix);
}


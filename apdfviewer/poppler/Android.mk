LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libpoppler

LOCAL_CPP_EXTENSION := .cc

LOCAL_SRC_FILES :=      \
	fofi/FoFiBase.cc		\
	fofi/FoFiEncodings.cc		\
	fofi/FoFiTrueType.cc		\
	fofi/FoFiType1.cc		\
	fofi/FoFiType1C.cc		\
	goo/gfile.cc			\
	goo/gmempp.cc			\
	goo/GooHash.cc			\
	goo/GooList.cc			\
	goo/GooTimer.cc			\
	goo/GooString.cc		\
	goo/gmem.cc			\
	goo/FixedPoint.cc		\
	goo/gstrtod.cc			\
	poppler/GlobalParams.cc		\
	poppler/GlobalParamsAndroid.cc	\
	poppler/FlateStream.cc		\
	poppler/Annot.cc		\
	poppler/Array.cc 		\
	poppler/BuiltinFont.cc		\
	poppler/BuiltinFontTables.cc	\
	poppler/Catalog.cc 		\
	poppler/CharCodeToUnicode.cc	\
	poppler/CMap.cc			\
	poppler/DateInfo.cc		\
	poppler/Decrypt.cc		\
	poppler/Dict.cc 		\
	poppler/FileSpec.cc		\
	poppler/FontEncodingTables.cc	\
	poppler/Form.cc 		\
	poppler/FontInfo.cc		\
	poppler/Function.cc		\
	poppler/Gfx.cc 			\
	poppler/GfxFont.cc 		\
	poppler/GfxState.cc		\
	poppler/JArithmeticDecoder.cc	\
	poppler/JBIG2Stream.cc		\
	poppler/JPXStream.cc	\
	poppler/Lexer.cc 		\
	poppler/Link.cc 		\
	poppler/Movie.cc                \
	poppler/NameToCharCode.cc	\
	poppler/Object.cc 		\
	poppler/OptionalContent.cc	\
	poppler/Outline.cc		\
	poppler/OutputDev.cc 		\
	poppler/AndroidOutputDev.cc	\
	poppler/Page.cc 		\
	poppler/PageTransition.cc	\
	poppler/Parser.cc 		\
	poppler/PDFDoc.cc 		\
	poppler/PDFDocEncoding.cc	\
	poppler/PopplerCache.cc		\
	poppler/ProfileData.cc		\
	poppler/PSTokenizer.cc		\
	poppler/Stream.cc 		\
	poppler/UnicodeMap.cc		\
	poppler/UnicodeTypeTable.cc	\
	poppler/XRef.cc			\
	poppler/PageLabelInfo.cc	\
	poppler/SecurityHandler.cc	\
	poppler/Sound.cc

LOCAL_C_INCLUDES :=         \
	$(LOCAL_PATH)			\
	$(LOCAL_PATH)/poppler	\
	$(LOCAL_PATH)/goo		\
	external/zlib			\
	external/skia/include/core

LOCAL_CFLAGS += -w -DPLATFORM_ANDROID

#LOCAL_PRELINK_MODULE := false

include $(BUILD_STATIC_LIBRARY)


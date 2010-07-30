/* config.h.  Generated from config.h.cmake by cmake.  */

/* Use libjpeg instead of builtin jpeg decoder. */
/* #define ENABLE_LIBJPEG 1 */

/* Use zlib instead of builtin zlib decoder. */
#define ENABLE_ZLIB 1

/* Use libpng to export images in pdftohtml. */
/* #define ENABLE_LIBPNG 1 */

/* Use fontconfig. */
/* #define ENABLE_FONTCONFIG 1 */

/* Use liblcms for color management. */
/* #define USE_CMS 1 */

/* Use cairo for rendering. */
/* #define HAVE_CAIRO 1 */

/* Define to 1 if you have the <dirent.h> header file, and it defines `DIR'. */
#define HAVE_DIRENT_H 1

/* Define to 1 if you have the <dlfcn.h> header file. */
#define HAVE_DLFCN_H 1

/* Have FreeType2 include files */
#define HAVE_FREETYPE_H 1

/* Define to 1 if you have the `fseek64' function. */
#define HAVE_FSEEK64 1

/* Define to 1 if fseeko (and presumably ftello) exists and is declared. */
#define HAVE_FSEEKO 1

/* Define to 1 if you have the `ftell64' function. */
#define HAVE_FTELL64 1

/* Defines if gettimeofday is available on your system */
#define HAVE_GETTIMEOFDAY 1

/* Define to 1 if you have the <inttypes.h> header file. */
#define HAVE_INTTYPES_H 1

/* Define to 1 if you have the `z' library (-lz). */
#define HAVE_LIBZ 1

/* Define to 1 if you have the <memory.h> header file. */
#define HAVE_MEMORY_H 1

/* Define to 1 if you have the `mkstemp' function. */
#define HAVE_MKSTEMP 1

/* Define to 1 if you have the `mkstemps' function. */
#define HAVE_MKSTEMPS 1

/* Define to 1 if you have the <ndir.h> header file, and it defines `DIR'. */
#define HAVE_NDIR_H 1

/* Define to 1 if you have the `popen' function. */
#define HAVE_POPEN 1

/* Use splash for rendering. */
/* #define HAVE_SPLASH 1 */

/* Define to 1 if you have the <stdint.h> header file. */
#define HAVE_STDINT_H 1

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if you have the <strings.h> header file. */
#define HAVE_STRINGS_H 1

/* Define to 1 if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* Define to 1 if you have the <sys/dir.h> header file, and it defines `DIR'. */
#define HAVE_SYS_DIR_H 1

/* Define to 1 if you have the <sys/ndir.h> header file, and it defines `DIR'. */
#define HAVE_SYS_NDIR_H 1

/* Define to 1 if you have the <sys/stat.h> header file. */
#define HAVE_SYS_STAT_H 1

/* Define to 1 if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* Define to 1 if you have the <unistd.h> header file. */
#define HAVE_UNISTD_H 1

/* Define to 1 if you have the <zlib.h> header file. */
#define HAVE_ZLIB_H 1

/* Define to 1 if gdk was found */
/* #define POPPLER_WITH_GDK 1 */

/* Enable multithreading support. */
/* #define MULTITHREADED 1 */

/* Generate OPI comments in PS output. */
#define OPI_SUPPORT 1

/* Name of package */
#define PACKAGE "poppler"

/* Define to the address where bug reports for this package should be sent. */
#define PACKAGE_BUGREPORT ""

/* Define to the full name of this package. */
#define PACKAGE_NAME "poppler"

/* Define to the full name and version of this package. */
#define PACKAGE_STRING "poppler 0.12.0"

/* Define to the one symbol short name of this package. */
#define PACKAGE_TARNAME "poppler"

/* Define to the version of this package. */
#define PACKAGE_VERSION "0.12.0"

/* Poppler data dir */
#define POPPLER_DATADIR "/sdcard/poppler-data"

/* Define to 1 if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Enable word list support. */
#define TEXTOUT_WORD_LIST 1

/* Throw exceptions to deal with not enough memory and similar problems */
/* #define USE_EXCEPTIONS 1 */

/* Use fixed point arithmetic */
#define USE_FIXEDPOINT 1

/* Version number of package */
#define VERSION "0.12.0"

/* Define to 1 if the X Window System is missing or not being used. */
/* #undef X_DISPLAY_MISSING */

/*
 * jpeg.h needs HAVE_BOOLEAN, when the system uses boolean in system
 * headers and I'm too lazy to write a configure test as long as only
 * unixware is related
 */
#ifdef _UNIXWARE
#define HAVE_BOOLEAN
#endif

/* MS has defined snprintf as deprecated */
#ifdef _MSC_VER
#define snprintf _snprintf
#endif

/* Number of bits in a file offset, on hosts where this is settable. */
/* #undef _FILE_OFFSET_BITS */

/* Define to 1 to make fseeko visible on some hosts (e.g. glibc 2.2). */
/* #undef _LARGEFILE_SOURCE */

/* Define for large files, on AIX-style hosts. */
/* #undef _LARGE_FILES */

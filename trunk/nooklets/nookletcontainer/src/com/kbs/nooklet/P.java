package com.kbs.nooklet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import android.util.Log;

// Some simple pull-parser utils

public class P
{
    public final static String collectText(XmlPullParser p)
        throws IOException, XmlPullParserException
    {
        int type;
        StringBuffer sb = new StringBuffer();
        p.require(XmlPullParser.START_TAG, null, null);
        int nest = 0;
        while ((type = p.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.TEXT) {
                sb.append(p.getText());
            }
            else if (type == XmlPullParser.START_TAG) {
                nest++;
            }
            else if (type == XmlPullParser.END_TAG) {
                if (nest == 0) {
                    break;
                }
                else {
                    nest--;
                }
            }
        }
        p.require(XmlPullParser.END_TAG, null, null);
        p.next();
        return sb.toString();
    }

    public final static boolean skipToStart
        (XmlPullParser p, String tag)
        throws IOException, XmlPullParserException
    {
        int type = p.getEventType();
        while (type != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                if ((tag == null) ||
                    (p.getName().equals(tag))) {
                    return true;
                }
            }
            type = p.next();
        }
        return false;
    }

    public final static boolean skipToStartWithin
        (XmlPullParser p, String tag, String end)
        throws IOException, XmlPullParserException
    {
        int type = p.getEventType();
        while (type != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.START_TAG) {
                if ((tag == null) ||
                    (p.getName().equals(tag))) {
                    return true;
                }
            }
            else if (type == XmlPullParser.END_TAG) {
                if ((end != null) &&
                    (p.getName().equals(end))) {
                    p.next();
                    return false;
                }
            }
            type = p.next();
        }
        return false;
    }

    public final static boolean skipThisBlock(XmlPullParser p)
        throws XmlPullParserException, IOException
    {
        p.require(XmlPullParser.START_TAG, null, null);
        int nest = 0;
        int type = p.next();
        while (type != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.END_TAG) {
                if (nest == 0) {
                    p.next();
                    return true;
                }
                else {
                    nest--;
                }
            }
            else if (type == XmlPullParser.START_TAG) {
                nest++;
            }
            type = p.next();
        }
        return skipToSETag(p);
    }
    public final static boolean skipToSETag(XmlPullParser p)
        throws XmlPullParserException, IOException
    {
        int type = p.getEventType();
        while (type != XmlPullParser.END_DOCUMENT) {
            if ((type == XmlPullParser.START_TAG) ||
                (type == XmlPullParser.END_TAG)) {
                return true;
            }
            type = p.next();
        }
        return false;
    }

    public final static void assertStart(XmlPullParser p, String tag)
        throws IOException, XmlPullParserException
    { p.require(XmlPullParser.START_TAG, null, tag); }

    private final static String TAG = "xml-parse-utils";
}

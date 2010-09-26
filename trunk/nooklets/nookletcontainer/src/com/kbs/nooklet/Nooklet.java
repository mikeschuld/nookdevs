package com.kbs.nooklet;

/*
 * This is really just a file browser -- it simply calls NookletViewer
 */

import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.os.PowerManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.webkit.WebView;
import android.util.Log;
import android.net.Uri;
import com.kbs.util.NookUtils;
import android.view.LayoutInflater;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import java.io.File;
import java.io.IOException;

public class Nooklet extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browsermain);
        Button backB = (Button)
            findViewById(R.id.back);
        backB.setOnClickListener
            (new View.OnClickListener()
                { public void onClick(View x)
                    { finish(); }});

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        m_powerlock = pm.newWakeLock
            (PowerManager.SCREEN_DIM_WAKE_LOCK, "nooklet:"+hashCode());
        m_powerlock.setReferenceCounted(false);
        m_powerdelay = NookUtils.getScreenSaverDelay(this);
    }

    @Override
    public void onUserInteraction()
    {
    	super.onUserInteraction();
    	if (m_powerlock != null) {
            m_powerlock.acquire(m_powerdelay);
        }
    }

    @Override
    public void onResume()
    {
    	super.onResume();
        if (m_powerlock != null) {
            m_powerlock.acquire(m_powerdelay);
        }
        NookUtils.setAppTitle(this, Version.VERSION);

        loadNooklets();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (m_powerlock != null) {
            m_powerlock.release();
        }
    }

    private final synchronized void loadNooklets()
    {
        LinearLayout ll =
            (LinearLayout)
            (findViewById(R.id.nookletcontainer));
        ll.removeAllViews();

        List<NI> nooklets = null;
        try { nooklets = findNooklets(); }
        catch (IOException ioe) {
            Log.d(TAG, "error reading nooklets ", ioe);
            error(ioe.toString());
            return;
        }

        if (nooklets == null) {
            return;
        }

        final LayoutInflater inflater = getLayoutInflater();
        for (NI ninfo: nooklets) {
            if (ninfo.getIcon() == null) {
                TextView v = (TextView)
                    inflater.inflate(R.layout.browsertextitem, ll, false);
                v.setText(ninfo.getName());
                v.setOnClickListener(new L(ninfo));
                ll.addView(v);
                }
            else {
                ImageView iv = (ImageView)
                    inflater.inflate(R.layout.browserimageitem, ll, false);
                // Odd behaviour -- takes a path, not a URI
                iv.setImageURI(Uri.parse(ninfo.getIcon().getPath()));
                iv.setOnClickListener(new L(ninfo));
                ll.addView(iv);
            }
            
        }
    }

    private List<NI> findNooklets()
        throws IOException
    {
        File d = new File(NOOKLET_DIR);
        if (d == null) {
            error("No nooklets found in "+d);
            return null;
        }
        File[] c = d.listFiles();
        if (c == null) {
            error("No nooklets found in "+d);
            return null;
        }

        List<NI> nooklets = new ArrayList<NI>();
        for (int i=0; i<c.length; i++) {
            String cn = c[i].getName();
            if (cn.startsWith(".")) {
                continue;
            }
            if (!c[i].isDirectory()) {
                continue;
            }
            File na = new File(c[i], "nooklet.xml");
            if (!na.canRead()) {
                continue;
            }
            
            Uri target = Uri.fromFile(na);
            Intent ni = new Intent(Intent.ACTION_VIEW);
            ni.setDataAndType(target, MIME_NOOKLET);

            NI nookletdata = new NI(c[i].getName(), ni);
            File ic = new File(c[i], "icon.png");
            if (ic.canRead()) {
                nookletdata.setIcon(ic);
            }
            nooklets.add(nookletdata);
        }
        if (nooklets.size() == 0) {
            error("No nooklets found in "+d);
            return null;
        }
        Collections.sort(nooklets);
        return nooklets;
    }

    private void error(String msg)
    {
        Log.d(TAG, msg);
        Toast.makeText
            (getApplicationContext(),
             msg, Toast.LENGTH_LONG).show();
    }

    private class L
        implements View.OnClickListener
    {
        private L(NI ni)
        { m_ni = ni; }
        public void onClick(View v)
        { startActivity(m_ni.getIntent()); }
        private final NI m_ni;
    }

    private class NI
        implements Comparable<NI>
    {
        NI(String n, Intent i)
        {
            m_name = n;
            m_intent = i;
        }
        void setIcon(File f)
        { m_icon = f; }
        File getIcon()
        { return m_icon; }
        String getName()
        { return m_name; }
        Intent getIntent()
        { return m_intent; }

        public int compareTo(NI other)
        { return m_name.compareTo(other.m_name); }

        private final String m_name;
        private final Intent m_intent;
        private File m_icon;
    }

    private PowerManager.WakeLock m_powerlock = null;
    private long m_powerdelay = NookUtils.DEFAULT_SCREENSAVER_DELAY;
    private final static String NOOKLET_DIR = "/system/media/sdcard/nooklets";
    private final static String TAG = "nooklet-browser";
    public final static String MIME_NOOKLET = "application/nooklet";
}

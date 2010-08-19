package com.nookdevs.taskmanager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import android.widget.LinearLayout;
import android.os.Bundle;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;

import android.content.Context;
import android.widget.Button;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.EditText;

import android.widget.ListView;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.ImageButton;

import android.view.View;
import android.content.DialogInterface;
import android.app.AlertDialog;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.nookdevs.common.nookBaseActivity;

public class NookTaskManager extends nookBaseActivity {
    private static String m_BaseDir="";
		private static String m_Mode = "normal";

		// This filter only exists when in normalMode
		private static String m_Filter[] = 
			{
				"com.android.phone", "com.android.inputmethod.latin", "system", "com.itc.bravo.radiopm", 
				"com.Bravo.UMSServer", "com.bravo.app.screensaver", "com.bravo.ecm.service", 
				"com.bravo.firmwareupdate", "com.bravo.sync:content", "com.bravo.sync", "com.bravo.wifi",
				"com.bravo.ereader.activities", "com.bravo.home",
        "com.bravo.thedaily.Daily", "com.bravo.library.LibraryActivity", "com.bravo.store.StoreFrontActivity",
        "com.bravo.ereader.activities.ReaderActivity", "com.bravo.app.settings.SettingsActivity",
        "com.bravo.app.settings.wifi.WifiActivity",
        "com.nookdevs.launcher.LauncherSettings", "com.bravo.home.HomeActivity",
        "com.nookdevs.launcher.LauncherSelector", "com.bravo.chess.ChessActivity",
        "com.bravo.sudoku.SudokuActivity", "com.bravo.app.browser.BrowserActivity"
			};

		PackageManager m_PM;
		ActivityManager m_ActivityManager;
		MemoryInfo      m_MemoryInfo;
    LinearLayout m_Content;
		List<ActivityManager.RunningAppProcessInfo> m_Activities;

		// Decided to re-tool this to adjust the process filter since there's no button for it.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case NOOK_PAGE_UP_KEY_LEFT:
							m_Mode = "all";
							listProcesses();
							handled = true;
							break;
            case NOOK_PAGE_UP_KEY_RIGHT:
							// Kill all non B&N
							killAll();
							break;
            case NOOK_PAGE_DOWN_KEY_LEFT:
							m_Mode = "normal";	
							listProcesses();
							handled = true;
              break;
            case NOOK_PAGE_UP_SWIPE:
                break;
            case NOOK_PAGE_DOWN_SWIPE:
                break;
            default:
                break;
        }
        return handled;
    }

		private View.OnClickListener appKiller = new View.OnClickListener() 
		{
			public void onClick(final View v) 
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(NookTaskManager.this);
				builder.setTitle(R.string.kill);
				builder.setMessage(R.string.confirm);
				builder.setNegativeButton(android.R.string.no, null).setCancelable(true);
				builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface dialog, int which) 
					{
						if( v.getTag() instanceof RunningAppProcessInfo) 
						{
							final RunningAppProcessInfo RAPI = (RunningAppProcessInfo)v.getTag();
							m_ActivityManager.restartPackage(RAPI.processName);
							listProcesses();
						}
					}
				});

				builder.show();
			}
		};

		public void killAll()
		{
				m_Activities = m_ActivityManager.getRunningAppProcesses();
				RunningAppProcessInfo RAPI;
				String importance;

				for (int i = 0; i < m_Activities.size(); i++)
				{
					RAPI = m_Activities.get(i);

					switch (RAPI.importance)
					{
						case RunningAppProcessInfo.IMPORTANCE_BACKGROUND:
							importance = "Background";
							break;
						case RunningAppProcessInfo.IMPORTANCE_FOREGROUND:
							importance = "Foreground";
							break;
						case RunningAppProcessInfo.IMPORTANCE_SERVICE:
							importance = "Service";
							break;
						case RunningAppProcessInfo.IMPORTANCE_VISIBLE:
							importance = "Visible";
							break;
						case RunningAppProcessInfo.IMPORTANCE_EMPTY:
							importance = "";
							break;
						default:
							importance = "";
							break;
					}

					if (RAPI.importance == RunningAppProcessInfo.IMPORTANCE_SERVICE)
						continue;

					if (RAPI.importance == RunningAppProcessInfo.IMPORTANCE_EMPTY)
						continue;

					boolean filtered = false;

					for (int j = 0; j < m_Filter.length; j++)
					{
						if (RAPI.processName.equals(m_Filter[j]))
						{
							filtered = true;
							break;
						}
					}

					if (filtered == true)
						continue;

					// Not yet
					if (RAPI.processName.equals("com.nookdevs.taskmanager"))
						continue;

					m_ActivityManager.restartPackage(RAPI.processName);

					Log.v( LOGTAG, "Killed " + RAPI.processName + " in killAll()");
				}

				// Suicide
				m_ActivityManager.restartPackage("com.nookdevs.taskmanager");
		}


		public void listProcesses()
		{
			m_MemoryInfo      = new MemoryInfo();
			m_ActivityManager.getMemoryInfo( m_MemoryInfo );

			TextView tv = (TextView) findViewById(R.id.availmem);
			tv.setText(Long.toString(m_MemoryInfo.availMem));

			tv = (TextView) findViewById(R.id.lowmem);
			tv.setText((m_MemoryInfo.lowMemory == true) ? "Yes" : "No");

			tv = (TextView) findViewById(R.id.threshold);
			tv.setText(Long.toString(m_MemoryInfo.threshold));

			m_Activities = m_ActivityManager.getRunningAppProcesses();
			m_Content.removeAllViews();

			String[] names = new String[m_Activities.size()];
			RunningAppProcessInfo RAPI;

			ImageButton button;
			TextView text;
			TextView title1;
			String importance;

			int cnt = 0;

			for (int i = 0; i < m_Activities.size(); i++)
			{
				RAPI = m_Activities.get(i);

				switch (RAPI.importance)
				{
					case RunningAppProcessInfo.IMPORTANCE_BACKGROUND:
						importance = "Background";
						break;
					case RunningAppProcessInfo.IMPORTANCE_FOREGROUND:
						importance = "Foreground";
						break;
					case RunningAppProcessInfo.IMPORTANCE_SERVICE:
						importance = "Service";
						break;
					case RunningAppProcessInfo.IMPORTANCE_VISIBLE:
						importance = "Visible";
						break;
					case RunningAppProcessInfo.IMPORTANCE_EMPTY:
						importance = "";
						break;
					default:
						importance = "";
						break;
				}

				if (m_Mode.equals("normal"))
				{
					if (RAPI.importance == RunningAppProcessInfo.IMPORTANCE_SERVICE)
						continue;

					if (RAPI.importance == RunningAppProcessInfo.IMPORTANCE_EMPTY)
						continue;

					boolean filtered = false;

					for (int j = 0; j < m_Filter.length; j++)
					{
						if (RAPI.processName.equals(m_Filter[j]))
						{
							filtered = true;
							break;
						}
					}

					if (filtered == true)
						continue;
				}

				final LayoutInflater inflater = getLayoutInflater();
				RelativeLayout appdetails = (RelativeLayout) inflater.inflate(R.layout.addapp, m_Content, false);

				button = (ImageButton) appdetails.findViewById(R.id.process);
				text = (TextView) appdetails.findViewById(R.id.desc);
				title1 = (TextView) appdetails.findViewById(R.id.title);

				text.setText(importance);
				title1.setText(RAPI.processName);

				button = (ImageButton) appdetails.findViewById(R.id.process);

				// We want to use their actual icon if it's available
				try
				{
					button.setImageDrawable(m_PM.getApplicationIcon(RAPI.processName));
				}
				catch(Exception ex) 
				{
					button.setImageResource(R.drawable.icon);
				}

				button.setTag(RAPI);
				button.setOnClickListener(appKiller);

				names[cnt] = RAPI.processName;
				Log.v( LOGTAG, " name is " + RAPI.processName );
				m_Content.addView(appdetails, cnt);
				cnt++;
			}
		}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "nookTaskManager";
        NAME = getString(R.string.app_name);
        super.onCreate(savedInstanceState);
				setContentView(R.layout.main);
				m_Content = (LinearLayout) findViewById(R.id.processcontainer);


				m_PM = getPackageManager();
    }

    @Override
    public void onResume() 
		{
        super.onResume();
        init();
    }

    private void init() 
		{
			m_ActivityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
			this.listProcesses();
    }

    @Override
    public void onPause() 
		{
        super.onPause();
    }
}

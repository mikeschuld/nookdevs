package com.nookdevs.launcher;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NookLauncher extends Activity
{
	private final static String TAG = "app-launcher";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		loadApps();
		registerReceivers();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(m_app_receiver);
	}

	private final void registerReceivers()
	{
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
		filter.addDataScheme("package");
		registerReceiver(m_app_receiver, filter);
	}

	private final synchronized void loadApps()
	{
		LinearLayout ll = (LinearLayout) (findViewById(R.id.appcontainer));
		ll.removeAllViews();

		PackageManager manager = getPackageManager();

		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> apps =
			manager.queryIntentActivities(mainIntent, 0);

		Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

		final LayoutInflater inflater = getLayoutInflater();

		// Have a button to get back to the B&N app
		TextView bnv = (TextView)
		inflater.inflate(R.layout.appbutton, ll, false);
		fillBnButton(bnv);
		ll.addView(bnv);        

		for (ResolveInfo ri: apps) {
			TextView v = (TextView)
			inflater.inflate(R.layout.appbutton, ll, false);
			fillButton(v, ri, manager);
			ll.addView(v);
		}
	}

	private final void  fillBnButton(TextView b)
	{
		b.setText(R.string.bn_home);
		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setComponent
		(new ComponentName
				("com.bravo.home", "com.bravo.home.HomeActivity"));
		intent.setFlags
		(Intent.FLAG_ACTIVITY_NEW_TASK |
				Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		b.setOnClickListener(new L(intent));
	}

	private final void fillButton
	(TextView b, ResolveInfo ri, PackageManager manager)
	{
		b.setText(""+ri.loadLabel(manager));
		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent
		(new ComponentName
				(ri.activityInfo.applicationInfo.packageName,
						ri.activityInfo.name));
		intent.setFlags
		(Intent.FLAG_ACTIVITY_NEW_TASK |
				Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		b.setOnClickListener(new L(intent));
	}

	private final class L
	implements View.OnClickListener
	{
		private L(Intent i)
		{ m_intent = i; }
		public void onClick(View v)
		{ startActivity(m_intent); }

		private final Intent m_intent;
	}

	public final class ApplicationsIntentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{ loadApps(); }
	}

	private final ApplicationsIntentReceiver m_app_receiver =
		new ApplicationsIntentReceiver();
}

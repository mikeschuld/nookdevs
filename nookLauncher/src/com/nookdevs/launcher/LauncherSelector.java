package com.nookdevs.launcher;

import java.util.List;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.nookdevs.common.nookBaseActivity;

public class LauncherSelector extends nookBaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eink_display);
        
        WebView wv = (WebView) findViewById(R.id.webview);
        wv.getSettings().setTextSize(WebSettings.TextSize.LARGER);
        wv.loadUrl("file:///android_asset/selector_main.htm");
        
        addPreferredActivity();
    }
    
    private void addPreferredActivity() {
        PackageManager packages = getPackageManager();
        IntentFilter filter = new IntentFilter("android.intent.action.MAIN");
        filter.addCategory("android.intent.category.HOME");
        filter.addCategory("android.intent.category.DEFAULT");
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.addCategory("android.intent.category.DEFAULT");
        
        List<ResolveInfo> resolveInfos = packages.queryIntentActivities(intent, 0);
        ComponentName activity = new ComponentName("com.nookdevs.launcher", ".NookLauncher");
        ComponentName[] components = new ComponentName[resolveInfos.size()];
        int i=0;
        System.out.println("Size = " + resolveInfos.size());
        for( ResolveInfo info:resolveInfos) {
            System.out.println("ActivityInfo =" + info.activityInfo + " " + info.activityInfo.applicationInfo);
            components[i++] = new ComponentName(
                        info.activityInfo.applicationInfo.packageName, info.activityInfo.name); 
        }
        packages.addPreferredActivity(filter, IntentFilter.MATCH_CATEGORY_EMPTY, components, activity);
    }
}

package com.nookdevs.launcher;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.WebView;

import com.nookdevs.common.nookBaseActivity;

public class LauncherSelector extends nookBaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eink_display);
        
        WebView wv = (WebView) findViewById(R.id.webview);
        wv.loadUrl("file:///android_asset/selector_main.htm");
        
        addPreferredActivity();
    }
    
    private void addPreferredActivity() {
        PackageManager packages = getPackageManager();
        
        IntentFilter filter = new IntentFilter("android.intent.action.MAIN");
        filter.addCategory("android.intent.category.HOME");
        filter.addCategory("android.intent.category.DEFAULT");
        
        ComponentName[] components =
            new ComponentName[] {
                new ComponentName("com.bravo.home", ".HomeActivity"),
                new ComponentName("com.nookdevs.launcher", ".NookLauncher")
            };
        
        ComponentName activity = new ComponentName("com.nookdevs.launcher", ".NookLauncher");
        
        packages.addPreferredActivity(filter, IntentFilter.MATCH_CATEGORY_EMPTY, components, activity);
    }
}

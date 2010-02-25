package com.nookdevs.market;

import android.os.Bundle;

import com.nookdevs.common.nookBaseActivity;

public class NookMarket extends nookBaseActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGTAG = "nookMarket";
        NAME = "nook Marketplace";
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}
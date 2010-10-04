package com.nookdevs.mines;

//  This activity does nothing.  But it does draw the screen a solid
//  color before exiting, so we can call it to flash the screen and
//  remove e-ink "ghosts".

import android.os.Bundle;
//import java.lang.Thread;

public class FlashEinkScreenActivity extends MinesActivity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blankscreen);        
        finish();
    }

}

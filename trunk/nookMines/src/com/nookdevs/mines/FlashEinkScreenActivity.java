/*
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *              http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Written by Kevin Vajk
 */


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

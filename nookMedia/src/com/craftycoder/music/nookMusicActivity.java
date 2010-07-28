package com.craftycoder.music;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.nookdevs.common.nookBaseActivity;

public class nookMusicActivity extends nookBaseActivity {
    public final void showSoftKeyboard(View view)
    {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view,InputMethodManager.SHOW_FORCED);
    }
}

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
 */

package com.nookdevs.mtextview;

import android.os.Bundle;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;


public class SearchTermDialog extends BaseActivity {

	protected EditText searchTermText ;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_search);

		searchTermText = (EditText) findViewById(R.id.search_term_input);

		// Button to launch the keyboard in case it's lost on suspend/resume (unusual):
		((Button) findViewById(R.id.launchkeyboard)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showKeyboard(searchTermText);
			}
		});

		Bundle b = getIntent().getExtras();
		String s = (b == null ? "" : b.getString(SEARCHTERM) );
		searchTermText.setText(s);
		searchTermText.requestFocus();
		bringUpKeyboard();

	} // onCreate


	void bringUpKeyboard() {
		InputMethodManager keyboardim = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		try {
			keyboardim.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
		} catch (Exception ex) {
			Log.e( this.toString(), "Error: Exception trying to bring up keyboard: " + ex  );
			return ;
		}
	} // bringUpKeyboard

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent ev)
	{
		if (ev.getAction() == KeyEvent.ACTION_UP) {
			switch (keyCode) {
			case SOFT_KEYBOARD_CLEAR:
				searchTermText.setText("");
				return true;
			case SOFT_KEYBOARD_CANCEL:
				setResult(RESULT_CANCELED);
				finish();
				return true;
			case SOFT_KEYBOARD_SUBMIT:
				Intent result = new Intent();
				result.putExtra(SEARCHTERM, searchTermText.getText().toString() );
				setResult(RESULT_OK, result);
				finish();
				return true;
			default:
				break;
			}
		}
		return false;
	} // onKeyUp

}

package com.nookdevs.crossword;


import android.os.Bundle;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;
//import android.util.Log;


public class InsertRebusActivity extends BaseActivity {

	protected EditText rebusText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_insert_rebus);

		// Button to launch the keyboard in case it's lost on suspend/resume (unusual):
		Button launchkeyboard = (Button) findViewById(R.id.launchkeyboard);
		launchkeyboard.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				bringUpKeyboard();
			}
		});

		rebusText = (EditText) findViewById(R.id.rebus_input);
		Bundle b = getIntent().getExtras();
		String s = (b == null ? "" : b.getString(REBUSTEXTLABEL) );
		rebusText.setText(s);
		rebusText.requestFocus();

		/*
        //  Bring up the keyboard:
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(findViewById(R.layout.dialog_insert_rebus), InputMethodManager.SHOW_FORCED);
		 */
		bringUpKeyboard();

	} // onCreate

	@Override
	public void onResume() {
		super.onResume();
		findViewById(R.id.rebus_input).requestFocus();
	} // onResume


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent ev)
	{
		if (ev.getAction() == KeyEvent.ACTION_UP) {
			switch (keyCode) {
			case SOFT_KEYBOARD_CLEAR:
				rebusText.setText("");
				return true;
			case SOFT_KEYBOARD_CANCEL:
				setResult(RESULT_CANCELED);
				finish();
				return true;
			case SOFT_KEYBOARD_SUBMIT:
				Intent result = new Intent();
				result.putExtra(REBUSTEXTLABEL, rebusText.getText().toString().replaceAll(" *", "") );
				setResult(RESULT_OK, result);
				finish();
				return true;
			default:
				break;
			}
		}
		return false;
	}



} // InsertRebusActivity

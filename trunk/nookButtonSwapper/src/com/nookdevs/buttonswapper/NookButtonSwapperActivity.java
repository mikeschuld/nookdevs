package com.nookdevs.buttonswapper;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewAnimator;

public class NookButtonSwapperActivity extends BaseActivity {
	private TextView tvTopLeft ;
	private TextView tvBottomLeft ;
	private TextView tvTopRight ;
	private TextView tvBottomRight ;
	private int topLeftMapping ;
	private int bottomLeftMapping ;
	private int topRightMapping ;
	private int bottomRightMapping ;
	private ViewAnimator animator;
	private static final int MAIN_VIEWNUM = 0 ;
	private static final int ERROR_VIEWNUM = 1 ;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		animator = (ViewAnimator) findViewById(R.id.main_animator);

		tvTopLeft = (TextView)findViewById(R.id.topleft);
		tvBottomLeft = (TextView)findViewById(R.id.bottomleft);
		tvTopRight = (TextView)findViewById(R.id.topright);
		tvBottomRight = (TextView)findViewById(R.id.bottomright);

		((Button) findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goBack();
			}
		});
		((Button) findViewById(R.id.back2)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goBack();
			}
		});
		((Button) findViewById(R.id.error_ok_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (KeyLayout.layoutIsWriteable()) {
					animator.setDisplayedChild(MAIN_VIEWNUM);
				} else {
					goBack();
				}
			}
		});
		((Button) findViewById(R.id.toggle_topleft_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleTopLeft();
			}
		});
		((Button) findViewById(R.id.toggle_bottomleft_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleBottomLeft();
			}
		});
		((Button) findViewById(R.id.toggle_topright_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleTopRight();
			}
		});
		((Button) findViewById(R.id.toggle_bottomright_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				toggleBottomRight();
			}
		});
		((Button) findViewById(R.id.restore_defaults_button)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				restoreDefaults();
			}
		});

		getMappings();
		setButtonLabels();
		checkPerms();

	} // onCreate

	@Override
	public void onResume() {
		super.onResume();
		getMappings();
		setButtonLabels();
		checkPerms();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	private void checkPerms() {
		if (KeyLayout.layoutIsWriteable()) {
			animator.setDisplayedChild(MAIN_VIEWNUM);
		} else {
			animator.setDisplayedChild(ERROR_VIEWNUM);
		}
	} // checkPerms

	//////////////////////////////////////////////////////////////////////////////////////////////

	private void getMappings() {
		topLeftMapping = KeyLayout.getTopLeftMapping() ;
		bottomLeftMapping = KeyLayout.getBottomLeftMapping() ;
		topRightMapping = KeyLayout.getTopRightMapping() ;
		bottomRightMapping = KeyLayout.getBottomRightMapping() ;
	} // getMappings

	private void setButtonLabels() {
		tvTopLeft.setText( getMappingLabel(topLeftMapping) );
		tvBottomLeft.setText( getMappingLabel(bottomLeftMapping) );
		tvTopRight.setText( getMappingLabel(topRightMapping) );
		tvBottomRight.setText( getMappingLabel(bottomRightMapping) );
	} // setButtonLabels

	private String getMappingLabel(int mapping) {
		if (mapping == KeyLayout.PREV_PAGE) {
			return getString(R.string.prevpage);
		} else if (mapping == KeyLayout.NEXT_PAGE) {
			return getString(R.string.nextpage);
		} else {
			return "";
		}
	} // getMappingLabel

	//////////////////////////////////////////////////////////////////////////////////////////////

	private void toggleTopLeft() {
		if (topLeftMapping == KeyLayout.PREV_PAGE) {
			topLeftMapping = KeyLayout.NEXT_PAGE ;
		} else {
			topLeftMapping =  KeyLayout.PREV_PAGE ;
		}
		KeyLayout.setTopLeftMapping(topLeftMapping);
		setButtonLabels();
	}
	private void toggleBottomLeft() {
		if (bottomLeftMapping == KeyLayout.PREV_PAGE) {
			bottomLeftMapping = KeyLayout.NEXT_PAGE ;
		} else {
			bottomLeftMapping =  KeyLayout.PREV_PAGE ;
		}
		KeyLayout.setBottomLeftMapping(bottomLeftMapping);
		setButtonLabels();
	}
	private void toggleTopRight() {
		if (topRightMapping == KeyLayout.PREV_PAGE) {
			topRightMapping = KeyLayout.NEXT_PAGE ;
		} else {
			topRightMapping = KeyLayout.PREV_PAGE ;
		}
		KeyLayout.setTopRightMapping(topRightMapping);
		setButtonLabels();
	}
	private void toggleBottomRight() {
		if (bottomRightMapping == KeyLayout.PREV_PAGE) {
			bottomRightMapping = KeyLayout.NEXT_PAGE ;
		} else {
			bottomRightMapping = KeyLayout.PREV_PAGE ;
		}
		KeyLayout.setBottomRightMapping(bottomRightMapping);
		setButtonLabels();
	}

	private void restoreDefaults() {
		if (topLeftMapping != KeyLayout.PREV_PAGE) KeyLayout.setTopLeftMapping(KeyLayout.PREV_PAGE);
		if (bottomLeftMapping != KeyLayout.NEXT_PAGE) KeyLayout.setBottomLeftMapping(KeyLayout.NEXT_PAGE);
		if (topRightMapping != KeyLayout.PREV_PAGE) KeyLayout.setTopRightMapping(KeyLayout.PREV_PAGE);
		if (bottomRightMapping != KeyLayout.NEXT_PAGE) KeyLayout.setBottomRightMapping(KeyLayout.NEXT_PAGE);
		getMappings();
		setButtonLabels();
	}

}
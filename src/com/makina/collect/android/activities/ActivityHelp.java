package com.makina.collect.android.activities;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.makina.collect.android.R;
import com.makina.collect.android.dialog.DialogHelp;
import com.makina.collect.android.utilities.Finish;

public class ActivityHelp extends FragmentActivity {
	
	private final int DIALOG_RESULT=1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		
		Window window = getWindow();
	    WindowManager.LayoutParams wlp = window.getAttributes();
	    wlp.gravity = Gravity.CENTER_VERTICAL;
	    wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    wlp.dimAmount = (float) 0.6;
	    window.setAttributes(wlp);
		Finish.activityHelp=this;
		DialogHelp dialog = new DialogHelp(getIntent().getExtras().getInt("position"));
		dialog.show(getSupportFragmentManager(), "");
		dialog.setTargetFragment(dialog.getTargetFragment(),DIALOG_RESULT);
	}
	
}
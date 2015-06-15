package com.makina.collect.activities;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.makina.collect.dialog.DialogHelp;
import com.makina.collect.utilities.Finish;

public class ActivityHelp extends FragmentActivity {
	
	private final int DIALOG_RESULT=1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_help);
		
		Window window = getWindow();
	    WindowManager.LayoutParams wlp = window.getAttributes();
	    wlp.gravity = Gravity.CENTER_VERTICAL;
	    wlp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
	    wlp.dimAmount = (float) 0.6;
	    window.setAttributes(wlp);
		Finish.activityHelp=this;
		DialogHelp.position=getIntent().getExtras().getInt("position");
		DialogHelp dialog = new DialogHelp();
		dialog.show(getSupportFragmentManager(), "");
		dialog.setTargetFragment(dialog.getTargetFragment(),DIALOG_RESULT);
	}
	
	
}
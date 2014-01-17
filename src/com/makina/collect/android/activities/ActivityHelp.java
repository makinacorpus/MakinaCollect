package com.makina.collect.android.activities;
import com.makina.collect.android.R;
import com.makina.collect.android.dialog.DialogHelp;
import com.makina.collect.android.utilities.Finish;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ActivityHelp extends FragmentActivity {
	
	private final int DIALOG_RESULT=1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		
		Finish.activityHelp=this;
		DialogHelp dialog = new DialogHelp(getIntent().getExtras().getInt("position"));
		dialog.show(getSupportFragmentManager(), "");
		dialog.setTargetFragment(dialog.getTargetFragment(),DIALOG_RESULT);
		
	}
	
}
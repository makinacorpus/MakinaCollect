package com.makina.collect.android.dialog;

import com.makina.collect.android.R;


import android.app.Dialog;
import android.content.Context;

public class Help
{
	public static void helpDialog(final Context context, int page)
	{
		final Dialog dialog_help = new Dialog(context);
		dialog_help.setContentView(R.layout.dialog_help);
		
        
		dialog_help.setCanceledOnTouchOutside(false);
		dialog_help.show();
	}
		  
}

package com.makina.collect.android.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import com.makina.collect.android.R;
import com.makina.collect.android.views.CustomFontCheckBox;
import com.makina.collect.android.views.CustomFontTextview;

public class Help
{
	public static CustomFontCheckBox checkbox_help;
	public static void helpDialog(final Context context, String message)
	{
		final Dialog dialog_help = new Dialog(context);
		dialog_help.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog_help.setContentView(R.layout.dialog_help);
		
        
		CustomFontTextview textview_help=(CustomFontTextview)dialog_help.findViewById(R.id.textview_help);
		LinearLayout linearlayout_ok=(LinearLayout) dialog_help.findViewById(R.id.linearlayout_ok);
		
		textview_help.setText(message);
		linearlayout_ok.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				dialog_help.dismiss();
			}
		});
		dialog_help.setCanceledOnTouchOutside(true);
		dialog_help.show();
	}
		  
}

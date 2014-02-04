package com.makina.collect.android.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;

import com.makina.collect.android.R;
import com.makina.collect.android.views.CustomFontCheckBox;
import com.makina.collect.android.views.CustomFontTextview;

public class DialogHelpWithConfirmation
{
	public static CustomFontCheckBox checkbox_help;
	public static CustomFontTextview textview_help_title;
	public static void helpDialog(final Context context, String title, String message)
	{
		final Dialog dialog_help = new Dialog(context);
		dialog_help.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog_help.setContentView(R.layout.dialog_help_with_confirmation);
		
		CustomFontTextview textview_help=(CustomFontTextview)dialog_help.findViewById(R.id.textview_help);
		textview_help_title=(CustomFontTextview)dialog_help.findViewById(R.id.textview_help_title);
        textview_help_title.setText(title);
        checkbox_help=(CustomFontCheckBox) dialog_help.findViewById(R.id.checkbox_help);
		RelativeLayout linearlayout_ok=(RelativeLayout) dialog_help.findViewById(R.id.linearlayout_ok);
		
		textview_help.setText(Html.fromHtml(message));
		linearlayout_ok.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (checkbox_help.isChecked())
				{
					if (textview_help_title.getText().toString().equals(context.getString(R.string.help_title1)))
					{
						SharedPreferences.Editor prefsEditor = context.getSharedPreferences("session", Context.MODE_PRIVATE).edit();
					    prefsEditor.putBoolean("help_download", true);
					    prefsEditor.commit();
					}
					else if (textview_help_title.getText().toString().equals(context.getString(R.string.help_title2)))
					{
						SharedPreferences.Editor prefsEditor = context.getSharedPreferences("session", Context.MODE_PRIVATE).edit();
					    prefsEditor.putBoolean("help_edit", true);
					    prefsEditor.commit();
					}
					else if (textview_help_title.getText().toString().equals(context.getString(R.string.help_title3)))
					{
						SharedPreferences.Editor prefsEditor = context.getSharedPreferences("session", Context.MODE_PRIVATE).edit();
					    prefsEditor.putBoolean("help_saved", true);
					    prefsEditor.commit();
					}
					else if (textview_help_title.getText().toString().equals(context.getString(R.string.help_title4)))
					{
						SharedPreferences.Editor prefsEditor = context.getSharedPreferences("session", Context.MODE_PRIVATE).edit();
					    prefsEditor.putBoolean("help_send", true);
					    prefsEditor.commit();
					}
					else if (textview_help_title.getText().toString().equals(context.getString(R.string.help_title2)))
					{
						SharedPreferences.Editor prefsEditor = context.getSharedPreferences("session", Context.MODE_PRIVATE).edit();
					    prefsEditor.putBoolean("help_form", true);
					    prefsEditor.commit();
					}
				}
				dialog_help.dismiss();
			}
		});
		
		dialog_help.setCanceledOnTouchOutside(false);
		dialog_help.show();
	}
		  
}

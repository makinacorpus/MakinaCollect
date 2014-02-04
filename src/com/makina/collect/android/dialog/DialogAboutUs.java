package com.makina.collect.android.dialog;

import com.makina.collect.android.R;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;

import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class DialogAboutUs
{
	public static void aboutUs(final Context context)
	{
		final Dialog dialog_help = new Dialog(context);
		dialog_help.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog_help.setContentView(R.layout.dialog_about_us);
		
        
        TextView textview_about_us1=(TextView)dialog_help.findViewById(R.id.textview_about_us1);
        TextView textview_about_us2=(TextView)dialog_help.findViewById(R.id.textview_about_us2);
        TextView textview_about_us3=(TextView)dialog_help.findViewById(R.id.textview_about_us3);
        TextView textview_about_us4=(TextView)dialog_help.findViewById(R.id.textview_about_us4);
        
        Typeface type1 = Typeface.createFromAsset(context.getAssets(),"fonts/hero.otf"); 
        Typeface type2 = Typeface.createFromAsset(context.getAssets(),"fonts/hero_light.otf"); 
        Typeface type3 = Typeface.createFromAsset(context.getAssets(),"fonts/journal.ttf"); 
        
        textview_about_us1.setTypeface(type1);
        textview_about_us2.setTypeface(type3);
        textview_about_us4.setTypeface(type2);
        
        textview_about_us3.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View arg0)
			{
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.web_site))));
			}
		});
        
		dialog_help.setCanceledOnTouchOutside(true);
		dialog_help.show();
	}
		  
}

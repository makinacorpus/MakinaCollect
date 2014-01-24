package com.makina.collect.android.views;
import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CustomActionBar
{
	public static void showActionBar(Context cx, TextView textview_title, TextView textview_subTitle, int titleColor, int subTitleColor)
	{
		Typeface typeFace = Typeface.createFromAsset(cx.getAssets(),"fonts/avenir.ttc"); 
        
		if (textview_title!=null)
    	{
			textview_title.setTextColor(titleColor);
			textview_title.setTypeface(typeFace);
	    	LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	    	llp.setMargins(0, 6, 0, 0);
	    	textview_title.setLayoutParams(llp);
    	}
    	
    	if (textview_subTitle!=null)
    	{
    		textview_subTitle.setTextColor(subTitleColor);
    		textview_subTitle.setTypeface(typeFace);
    	}
	}
}

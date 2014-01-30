package com.makina.collect.android.theme;

import java.util.Locale;

import android.app.Activity;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.makina.collect.android.R;
import com.makina.collect.android.preferences.ActivityPreferences;

public class Theme
{
	/**
	 * Set the theme of the Activity, and restart it by creating a new Activity
	 * of the same type.
	 */
	public static void changeTheme(Activity activity)
	{
		String themeChoice = PreferenceManager.getDefaultSharedPreferences(activity).getString(ActivityPreferences.KEY_THEME, ActivityPreferences.KEY_THEME);
		if ( (themeChoice != null) && (themeChoice.contains(activity.getString(R.string.theme2))) )
			activity.setTheme(R.style.CollectThemeLight);
		else
			activity.setTheme(R.style.CollectThemeDark);
		
		
		Resources res = activity.getResources();
	    // Change locale settings in the app.
	    DisplayMetrics dm = res.getDisplayMetrics();
	    android.content.res.Configuration conf = res.getConfiguration();
		String languageChoice = PreferenceManager.getDefaultSharedPreferences(activity).getString(ActivityPreferences.KEY_LANGUAGE, ActivityPreferences.KEY_LANGUAGE);
		if (languageChoice != null) 
		{
			if (languageChoice.equals("1")) 
				conf.locale = new Locale("fr");
			else if (languageChoice.equals("0")) 
				conf.locale = new Locale("en");
			
			res.updateConfiguration(conf, dm);
		}
	    
	}

}

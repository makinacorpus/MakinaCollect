package com.makina.collect.android.theme;

import android.app.Activity;
import android.preference.PreferenceManager;

import com.makina.collect.android.R;
import com.makina.collect.android.preferences.ActivityPreferences;

public class Theme
{

	public final static int THEME_DARK = 0;
	public final static int THEME_LIGHT = 1;

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
	}

}

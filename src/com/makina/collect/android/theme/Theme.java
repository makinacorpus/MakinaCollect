package com.makina.collect.android.theme;

import android.app.Activity;
import com.makina.collect.android.R;

public class Theme
{

	public final static int THEME_DARK = 0;
	public final static int THEME_LIGHT = 1;

	/**
	 * Set the theme of the Activity, and restart it by creating a new Activity
	 * of the same type.
	 */
	public static void changeToTheme(Activity activity, int theme)
	{
		switch (theme)
		{
		default:
		case THEME_LIGHT:
			activity.setTheme(R.style.light);
			break;
		case THEME_DARK:
			activity.setTheme(R.style.Collect);
			break;
		}
	}

}

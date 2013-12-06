package com.makina.collect.android.utilities;

import com.makina.collect.android.activities.ActivityDashBoard;
import com.makina.collect.android.activities.ActivityDownloadForm;

public class Finish
{
	
	public static ActivityDashBoard activityDashBoard;
	public static ActivityDownloadForm activityDownloadForm;
	public static void finish()
	{
		if(activityDashBoard != null) activityDashBoard.finish();
		if(activityDownloadForm != null) activityDownloadForm.finish();
	}
}

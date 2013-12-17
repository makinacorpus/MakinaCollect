package com.makina.collect.android.utilities;

import com.makina.collect.android.activities.ActivityDashBoard;
import com.makina.collect.android.activities.ActivityDownloadForm;
import com.makina.collect.android.activities.ActivityEditForm;
import com.makina.collect.android.activities.ActivityForm;
import com.makina.collect.android.activities.ActivityFormHierarchy;
import com.makina.collect.android.activities.ActivitySaveForm;
import com.makina.collect.android.activities.ActivitySendForm;
import com.makina.collect.android.preferences.ActivityPreferences;

public class Finish
{
	public static ActivityDashBoard activityDashBoard;
	public static ActivityDownloadForm activityDownloadForm;
	public static ActivityEditForm activityEditForm;
	public static ActivitySaveForm activitySaveForm;
	public static ActivitySendForm activitySendForm;
	public static ActivityForm activityForm;
	public static ActivityFormHierarchy activityFormHierarchy;
	public static ActivityPreferences activityPreferences;
	
	public static void finish()
	{
		if(activityDashBoard != null) activityDashBoard.finish();
		if(activityDownloadForm != null) activityDownloadForm.finish();
		if(activityEditForm != null) activityEditForm.finish();
		if(activitySaveForm != null) activitySaveForm.finish();
		if(activitySendForm != null) activitySendForm.finish();
		if(activityForm != null) activityForm.finish();
		if(activityFormHierarchy != null) activityFormHierarchy.finish();
		if(activityPreferences != null) activityPreferences.finish();
	}
}

package com.makina.collect.utilities;

import com.makina.collect.activities.ActivityDownloadForm;
import com.makina.collect.activities.ActivityEditForm;
import com.makina.collect.activities.ActivityForm;
import com.makina.collect.activities.ActivityFormHierarchy;
import com.makina.collect.activities.ActivityHelp;
import com.makina.collect.activities.ActivitySaveForm;
import com.makina.collect.activities.ActivitySendForm;
import com.makina.collect.activity.DashBoardActivity;
import com.makina.collect.preferences.ActivityPreferences;

public class Finish
{
	public static DashBoardActivity dashBoardActivity;
	public static ActivityDownloadForm activityDownloadForm;
	public static ActivityEditForm activityEditForm;
	public static ActivitySaveForm activitySaveForm;
	public static ActivitySendForm activitySendForm;
	public static ActivityForm activityForm;
	public static ActivityFormHierarchy activityFormHierarchy;
	public static ActivityPreferences activityPreferences;
	public static ActivityHelp activityHelp;
	
	public static void finish()
	{
		if(dashBoardActivity != null) dashBoardActivity.finish();
		if(activityDownloadForm != null) activityDownloadForm.finish();
		if(activityEditForm != null) activityEditForm.finish();
		if(activitySaveForm != null) activitySaveForm.finish();
		if(activitySendForm != null) activitySendForm.finish();
		if(activityForm != null) activityForm.finish();
		if(activityFormHierarchy != null) activityFormHierarchy.finish();
		if(activityPreferences != null) activityPreferences.finish();
		if(activityHelp != null) activityHelp.finish();
	}
	
	public static void finishHome()
	{
		if(activityDownloadForm != null) activityDownloadForm.finish();
		if(activityEditForm != null) activityEditForm.finish();
		if(activitySaveForm != null) activitySaveForm.finish();
		if(activitySendForm != null) activitySendForm.finish();
		if(activityForm != null) activityForm.finish();
		if(activityFormHierarchy != null) activityFormHierarchy.finish();
		if(activityPreferences != null) activityPreferences.finish();
		if(activityHelp != null) activityHelp.finish();
	}
}

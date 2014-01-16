/*
 * Copyright (C) 2011 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.makina.collect.android.activities;

import ly.count.android.api.Countly;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.dialog.DialogAboutUs;
import com.makina.collect.android.preferences.ActivityPreferences;
import com.makina.collect.android.utilities.Finish;

public class ActivityDashBoard extends SherlockActivity implements
		OnClickListener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Typeface typeFace = Typeface.createFromAsset(getAssets(),
				"fonts/avenir.ttc");
		getSupportActionBar().setTitle(getString(R.string.app_name));
		int titleId = Resources.getSystem().getIdentifier("action_bar_title",
				"id", "android");
		TextView actionbarTitle = (TextView) findViewById(titleId);
		if (actionbarTitle != null) {
			
			actionbarTitle
					.setText(Html
							.fromHtml("<strong><font color=\"#C2EA60\">Makina</font> <font color=\"#1B8FAA\">Collect</font></strong>"));
			actionbarTitle.setTypeface(typeFace);
		}

		setContentView(R.layout.activity_dashboard);
		Finish.activityDashBoard = this;
		Countly.sharedInstance().init(getApplicationContext(),
				"http://countly.makina-corpus.net",
				"279676abcbba16c3ee5e2b113a990fe579ddc527");

		Button btnDownload = (Button) findViewById(R.id.dashboard_download);
		btnDownload.setOnClickListener(this);
		Button btnEdit = (Button) findViewById(R.id.dashboard_edit);
		btnEdit.setOnClickListener(this);
		Button btnSave = (Button) findViewById(R.id.dashboard_save);
		btnSave.setOnClickListener(this);
		Button btnSend = (Button) findViewById(R.id.dashboard_send);
		btnSend.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.dashboard_download:
			startActivity(new Intent(getApplicationContext(),ActivityDownloadForm.class));
			break;
		case R.id.dashboard_edit:
			startActivity(new Intent(getApplicationContext(),ActivityEditForm.class));
			break;
		case R.id.dashboard_save:
			startActivity(new Intent(getApplicationContext(),ActivitySaveForm.class));
			break;
		case R.id.dashboard_send:
			startActivity(new Intent(getApplicationContext(),
					ActivitySendForm.class));
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.menu_activity_dashboard, menu);

		getLayoutInflater().setFactory(new LayoutInflater.Factory() {
			public View onCreateView(String name, Context context,
					AttributeSet attrs) {
				if (name.equalsIgnoreCase("com.android.internal.view.menu.IconMenuItemView")
						|| name.equalsIgnoreCase("TextView")) {
					try {
						LayoutInflater li = LayoutInflater.from(context);
						final View view = li.createView(name, null, attrs);
						new Handler().post(new Runnable() {
							public void run() {
								((TextView) view)
										.setTextColor(getResources()
												.getColor(
														R.color.actionbarTitleColorGris));
								((TextView) view).setTypeface(Typeface
										.createFromAsset(getAssets(),
												"fonts/avenir.ttc"));
							}
						});
						return view;
					} catch (InflateException e) {
					} catch (ClassNotFoundException e) {
					}
				}
				return null;
			}
		});

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		// Handle action buttons for all fragments
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(this, ActivityPreferences.class));
			return true;
		 case R.id.menu_help:
			 startActivity(new Intent(this, ActivityHelp.class));
			 return true;
		case R.id.menu_about_us:
			DialogAboutUs.aboutUs(this);
			return true;
		case R.id.menu_exit:
			Finish.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Countly.sharedInstance().onStart();
		Collect.getInstance().getActivityLogger().logOnStart(this);
	}

	@Override
	protected void onStop() {
		Countly.sharedInstance().onStop();
		Collect.getInstance().getActivityLogger().logOnStop(this);
		super.onStop();
	}

}

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

import java.io.File;

import ly.count.android.api.Countly;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.dialog.DialogAboutUs;
import com.makina.collect.android.dialog.DialogExit;
import com.makina.collect.android.preferences.ActivityPreferences;
import com.makina.collect.android.provider.FormsProvider;
import com.makina.collect.android.provider.InstanceProvider;
import com.makina.collect.android.theme.Theme;
import com.makina.collect.android.utilities.Finish;
import com.makina.collect.android.views.CustomFontButton;

public class ActivityDashBoard extends SherlockActivity implements
                OnClickListener {
	private Menu menu;
	private final int RESULT_PREFERENCES=1;
        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                Theme.changeTheme(this);
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

                CustomFontButton btnDownload = (CustomFontButton) findViewById(R.id.dashboard_download);
                btnDownload.setOnClickListener(this);
                CustomFontButton btnEdit = (CustomFontButton) findViewById(R.id.dashboard_edit);
                btnEdit.setOnClickListener(this);
                CustomFontButton btnSave = (CustomFontButton) findViewById(R.id.dashboard_save);
                btnSave.setOnClickListener(this);
                CustomFontButton btnSend = (CustomFontButton) findViewById(R.id.dashboard_send);
                btnSend.setOnClickListener(this);
                
                if ( (getIntent().getExtras()!=null) && (getIntent().getExtras().getLong("folder_size")>0))
    	        {
                	final File f = new File(Collect.FORMS_PATH);
    				AlertDialog.Builder adb = new AlertDialog.Builder(this);
    				adb.setTitle(getString(R.string.delete));
    				adb.setMessage(getString(R.string.delete_old_forms));
    				adb.setIconAttribute(android.R.attr.alertDialogIcon);
    				adb.setNegativeButton(getString(android.R.string.cancel),new AlertDialog.OnClickListener()
    				{
    					public void onClick(DialogInterface dialog,int which)
    					{
    						try
    				        {
    				            Collect.createODKDirs();
    				        }
    				        catch (RuntimeException e)
    				        {
    				            return;
    				        }
    					}
    				});
    				adb.setPositiveButton(getString(android.R.string.yes), new AlertDialog.OnClickListener()
    				{
    					public void onClick(DialogInterface dialog,int which)
    					{
    						InstanceProvider.deleteAllInstances();
    						FormsProvider.deleteAllForms();
    						for(File file: f.listFiles()) 
    							file.delete();
    						
    						try
    				        {
    				            Collect.createODKDirs();
    				        }
    				        catch (RuntimeException e)
    				        {
    				            return;
    				        }
    					}
    				});
    				adb.setCancelable(false);
    				adb.show();
    				getIntent().removeExtra("folder_size");
    	        }
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
                this.menu=menu;
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
                                                        	((TextView)view).setTextColor(getResources().getColor(R.color.actionbarTitleColorGris));
                                                            ((TextView)view).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/avenir.ttc"));
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
                        startActivityForResult((new Intent(this, ActivityPreferences.class)),RESULT_PREFERENCES);
                        return true;
                 case R.id.menu_help:
                        Intent mIntent=new Intent(this, ActivityHelp.class);
                Bundle mBundle=new Bundle();
                mBundle.putInt("position", 0);
                mIntent.putExtras(mBundle);
                startActivity(mIntent);
                         return true;
                case R.id.menu_about_us:
                        DialogAboutUs.aboutUs(this);
                        return true;
                case R.id.menu_exit:
                       	DialogExit.show(this);
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
        
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_MENU) {
            	menu.performIdentifierAction(R.id.menu_other, 0);
             }
            super.onKeyUp(keyCode, event);
            return true;
         }
        
        
        protected void onActivityResult(int requestCode, int resultCode, Intent data)
        {
        	if (requestCode == RESULT_PREFERENCES)
        	{
        		Intent i = getIntent();
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
        	}
        }

}
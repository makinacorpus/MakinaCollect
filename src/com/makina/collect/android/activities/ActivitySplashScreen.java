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
import android.app.Activity;
import com.WazaBe.HoloEverywhere.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;

import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.provider.InstanceProvider;

public class ActivitySplashScreen extends Activity {

    private static final int mSplashTimeout = 3000; // milliseconds
    private static final boolean EXIT = true;

    private AlertDialog mAlertDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // this splash screen should be a blank slate
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash_screen);
        
        Countly.sharedInstance().init(getApplicationContext(), "http://countly.makina-corpus.net", "279676abcbba16c3ee5e2b113a990fe579ddc527");
        
        // must be at the beginning of any activity that can be called from an external intent
        try
        {
            Collect.createODKDirs();
        }
        catch (RuntimeException e)
        {
            createErrorDialog(e.getMessage(), EXIT);
            return;
        }

        

        if (!getSharedPreferences("session", MODE_PRIVATE).getBoolean("first_time", false))
		{
        	InstanceProvider.supprimerZgaw();
        	
        	SharedPreferences.Editor prefsEditor = getSharedPreferences("session", MODE_PRIVATE).edit();
		    prefsEditor.putBoolean("first_time", true);
		    prefsEditor.commit();
		}
        
        startSplashScreen();

    }


    private void endSplashScreen() {

        // launch new activity and close splash screen
        startActivity(new Intent(ActivitySplashScreen.this, ActivityDashBoard.class));
        finish();
    }

    private void startSplashScreen() {

        // create a thread that counts up to the timeout
        Thread t = new Thread()
        {
            int count = 0;

            @Override
            public void run()
            {
                try
                {
                    super.run();
                    while (count < mSplashTimeout)
                    {
                        sleep(100);
                        count += 100;
                    }
                }
                catch (Exception e){}
                finally
                {
                    endSplashScreen();
                }
            }
        };
        t.start();
    }


    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
	    Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1:
                	    Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "OK");
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
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

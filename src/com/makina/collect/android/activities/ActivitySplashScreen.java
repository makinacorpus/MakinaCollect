package com.makina.collect.android.activities;
import ly.count.android.api.Countly;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;

import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.provider.InstanceProvider;
import com.makina.collect.android.theme.Theme;

public class ActivitySplashScreen extends Activity {

    private static final int mSplashTimeout = 3000; // milliseconds


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Theme.changeTheme(this);
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
            return;
        }

        
        if (!getSharedPreferences("session", MODE_PRIVATE).getBoolean("first_time", false))
		{
        	InstanceProvider.deleteAllInstances();
        	SharedPreferences.Editor prefsEditor = getSharedPreferences("session", MODE_PRIVATE).edit();
		    prefsEditor.putBoolean("first_time", true);
		    prefsEditor.commit();
		}
        
        startSplashScreen();

    }


    private void endSplashScreen()
    {
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

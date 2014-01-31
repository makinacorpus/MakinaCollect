package com.makina.collect.android.activities;
import java.io.File;

import ly.count.android.api.Countly;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;

import com.WazaBe.HoloEverywhere.app.AlertDialog;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;

import com.makina.collect.android.provider.FormsProvider;
import com.makina.collect.android.provider.InstanceProvider;
import com.makina.collect.android.theme.Theme;

public class ActivitySplashScreen extends Activity {

    private static final int mSplashTimeout = 3000; // milliseconds
    private long folder_size;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Theme.changeTheme(this);
        // this splash screen should be a blank slate
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash_screen);
        Countly.sharedInstance().init(getApplicationContext(), "http://countly.makina-corpus.net", "279676abcbba16c3ee5e2b113a990fe579ddc527");
        
        // must be at the beginning of any activity that can be called from an external intent
        

        String url = PreferenceManager.getDefaultSharedPreferences(this).getString("server_url", "");
		if ((url==null) || (url.equals("")))
		{
			final File f = new File(Collect.FORMS_PATH);
			if(f.isDirectory())
			{
				folder_size = 0;
		        File[] fileList = f.listFiles();
		        for(int i = 0; i < fileList.length; i++)
		        	folder_size += fileList[i].length();
		    }
			
		}
		
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
        
        startSplashScreen();

    }


    private void endSplashScreen()
    {
    	// launch new activity and close splash screen
    	Intent mIntent=new Intent(getApplicationContext(), ActivityDashBoard.class);
    	Bundle mBundle=new Bundle();
    	mBundle.putLong("folder_size", folder_size);
    	mIntent.putExtras(mBundle);
        startActivity(mIntent);
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

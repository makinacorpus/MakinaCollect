package com.makina.collect.android.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.makina.collect.android.R;

import ly.count.android.api.Countly;

/**
 * Base {@code Activity} of this application.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public abstract class AbstractBaseActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Countly.sharedInstance().init(
                this,
                getString(R.string.__config_analytics_server_url),
                getString(R.string.__config_analytics_app_key));
    }

    @Override
    protected void onStart() {
        super.onStart();

        Countly.sharedInstance().onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Countly.sharedInstance().onStop();
    }
}

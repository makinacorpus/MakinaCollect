package com.makina.collect.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.makina.collect.R;
import com.makina.collect.util.ThemeUtils;

import ly.count.android.api.Countly;

/**
 * Base {@code Activity} of this application.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public abstract class AbstractBaseActivity
        extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this,
                         applyTheme());

        super.onCreate(savedInstanceState);

        Countly.sharedInstance()
               .init(this,
                     getString(R.string.__config_analytics_server_url),
                     getString(R.string.__config_analytics_app_key));
    }

    @Override
    protected void onStart() {
        super.onStart();

        Countly.sharedInstance()
               .onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Countly.sharedInstance()
               .onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        ThemeUtils.apply(this,
                         applyTheme());
    }

    /**
     * Override this method to apply a theme variant for this {@code Activity}.
     *
     * @return the theme variant to apply (may be {@code null})
     */
    @Nullable
    protected ThemeUtils.AppThemeVariant applyTheme() {
        return null;
    }
}

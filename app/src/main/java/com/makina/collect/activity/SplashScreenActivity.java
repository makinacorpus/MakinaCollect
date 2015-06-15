package com.makina.collect.activity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;

import com.makina.collect.R;
import com.makina.collect.loader.InitLoader;
import com.makina.collect.util.IntentUtils;
import com.makina.collect.util.ThemeUtils;

/**
 * Splash screen {@code Activity}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class SplashScreenActivity
        extends AbstractBaseActivity
        implements LoaderManager.LoaderCallbacks<Boolean> {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_splash_screen);

        getSupportLoaderManager().initLoader(0,
                                             null,
                                             this);
    }

    @Nullable
    @Override
    protected ThemeUtils.AppThemeVariant applyTheme() {
        return ThemeUtils.AppThemeVariant.BLUE;
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id,
                                       Bundle args) {
        return new InitLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader,
                               Boolean data) {
        PreferenceManager.setDefaultValues(this,
                                           R.xml.preferences,
                                           false);

        // launch DashBoardActivity and close splash screen
        startActivity(IntentUtils.dashBoardActivity(this,
                                                    data));

        finish();
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        // nothing to do ...
    }
}

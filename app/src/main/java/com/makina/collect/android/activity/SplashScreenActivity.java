package com.makina.collect.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;

import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.loader.InitLoader;
import com.makina.collect.android.utilities.ThemeUtils;

import java.io.File;

/**
 * Splash screen {@code Activity}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class SplashScreenActivity
        extends AbstractBaseActivity
        implements LoaderManager.LoaderCallbacks<Void> {

    private static final int INIT_LOADER_ID = 0;
    private long folder_size;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_splash_screen);

        // must be at the beginning of any activity that can be called from an external intent
        // TODO: still useful ?
        String url = PreferenceManager.getDefaultSharedPreferences(this)
                                      .getString("server_url",
                                                 "");

        if ((url == null) || (url.equals(""))) {
            final File f = new File(Collect.FORMS_PATH);

            if (f.isDirectory()) {
                folder_size = 0;
                File[] fileList = f.listFiles();

                for (File aFileList : fileList) {
                    folder_size += aFileList.length();
                }
            }
        }

        PreferenceManager.setDefaultValues(this,
                                           R.xml.preferences,
                                           false);

        getSupportLoaderManager().initLoader(INIT_LOADER_ID,
                                             null,
                                             this);
    }

    @Nullable
    @Override
    protected ThemeUtils.AppThemeVariant applyTheme() {
        return ThemeUtils.AppThemeVariant.BLUE;
    }

    @Override
    public Loader<Void> onCreateLoader(int id,
                                       Bundle args) {
        return new InitLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Void> loader,
                               Void data) {
        // launch DashBoardActivity and close splash screen
        final Intent intent = new Intent(getApplicationContext(),
                                         DashBoardActivity.class);

        final Bundle bundle = new Bundle();
        // TODO: still useful ?
        bundle.putLong("folder_size",
                       folder_size);

        intent.putExtras(bundle);

        startActivity(intent);

        finish();
    }

    @Override
    public void onLoaderReset(Loader<Void> loader) {
        // nothing to do ...
    }
}

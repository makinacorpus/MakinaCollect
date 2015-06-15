package com.makina.collect.loader;

import android.content.Context;
import android.util.Log;

import com.makina.collect.application.Collect;
import com.makina.collect.content.AppSharedPreferences;

import java.io.File;

/**
 * {@code Loader} used to initialize the application.
 * <p>
 * This {@code Loader} does not do much for now:
 * <ul>
 * <li>Check if the current forms path is empty or not</li>
 * </ul>
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class InitLoader
        extends AbstractAsyncTaskLoader<Boolean> {

    private static final String TAG = InitLoader.class.getName();

    private static final int SPLASH_DISPLAY_TIME = 1500;

    public InitLoader(Context context) {
        super(context);
    }

    @Override
    public Boolean loadInBackground() {
        final long start = System.currentTimeMillis();

        boolean appFolderEmpty = true;

        final AppSharedPreferences appSharedPreferences = new AppSharedPreferences(getContext());

        if (!appSharedPreferences.isServerUrlDefined()) {
            final File formsPath = new File(Collect.FORMS_PATH);

            if (formsPath.isDirectory()) {
                appFolderEmpty = formsPath.listFiles().length == 0;
            }
        }

        final long end = System.currentTimeMillis();

        if ((end - start) < SPLASH_DISPLAY_TIME) {
            try {
                Thread.sleep(SPLASH_DISPLAY_TIME - (end - start));
            }
            catch (InterruptedException ie) {
                Log.w(TAG,
                      ie.getMessage());
            }
        }

        return appFolderEmpty;
    }
}

package com.makina.collect.android.loader;

import android.content.Context;
import android.util.Log;

/**
 * {@code Loader} used to initialize the application.
 * <p/>
 * This {@code Loader} do nothing for the moment.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class InitLoader extends AbstractAsyncTaskLoader<Void> {

    private static final String TAG = InitLoader.class.getName();

    private static final int SPLASH_DISPLAY_TIME = 1500;

    public InitLoader(Context context) {
        super(context);
    }

    @Override
    public Void loadInBackground() {

        try {
            Thread.sleep(SPLASH_DISPLAY_TIME);
        }
        catch (InterruptedException ie) {
            Log.w(TAG, ie.getMessage());
        }

        return null;
    }
}

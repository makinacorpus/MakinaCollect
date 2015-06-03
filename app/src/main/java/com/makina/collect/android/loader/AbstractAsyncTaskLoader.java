package com.makina.collect.android.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Base {@code Loader}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public abstract class AbstractAsyncTaskLoader<D> extends AsyncTaskLoader<D> {

    D result;

    protected AbstractAsyncTaskLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        if (result == null) {
            forceLoad();
        } else {
            deliverResult(result);
        }
    }

    @Override
    public void deliverResult(D data) {
        result = data;
        super.deliverResult(data);
    }
}

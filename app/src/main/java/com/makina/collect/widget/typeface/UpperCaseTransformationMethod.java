package com.makina.collect.widget.typeface;

import android.content.Context;
import android.graphics.Rect;
import android.text.method.TransformationMethod;
import android.view.View;

import java.util.Locale;

/**
 * Transforms a given input text into an ALL CAPS string.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class UpperCaseTransformationMethod
        implements TransformationMethod {

    private Locale mLocale;

    public UpperCaseTransformationMethod(Context context) {
        mLocale = context.getResources()
                         .getConfiguration().locale;
    }

    @Override
    public CharSequence getTransformation(CharSequence source,
                                          View view) {
        return source != null ? source.toString()
                                      .toUpperCase(mLocale) : null;
    }

    @Override
    public void onFocusChanged(View view,
                               CharSequence sourceText,
                               boolean focused,
                               int direction,
                               Rect previouslyFocusedRect) {
        // nothing to do ...
    }
}

package com.makina.collect.android.widget.typeface;

import android.support.annotation.NonNull;

/**
 * "Journal" type face.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public enum JournaltypeFaceFamily
        implements ITypefaceFamily {

    JOURNAL("journal",
           "fonts/journal.ttf");

    private String mTypeFaceName;
    private String mTypeFacePath;

    JournaltypeFaceFamily(String typeFaceName,
                          String typeFacePath) {
        mTypeFaceName = typeFaceName;
        mTypeFacePath = typeFacePath;
    }

    @Override
    @NonNull
    public String getTypefaceName() {
        return mTypeFaceName;
    }

    @Override
    @NonNull
    public String getTypefacePath() {
        return mTypeFacePath;
    }
}

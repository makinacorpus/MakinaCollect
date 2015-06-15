package com.makina.collect.widget.typeface;

import android.support.annotation.NonNull;

/**
 * "Avenir" type face.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public enum AvenirTypefaceFamily
        implements ITypefaceFamily {

    AVENIR("avenir",
           "fonts/avenir.ttc");

    private String mTypeFaceName;
    private String mTypeFacePath;

    AvenirTypefaceFamily(String typeFaceName,
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

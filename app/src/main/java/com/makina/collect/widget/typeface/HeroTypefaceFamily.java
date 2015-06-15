package com.makina.collect.widget.typeface;

import android.support.annotation.NonNull;

/**
 * "Hero" type face.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public enum HeroTypefaceFamily
        implements ITypefaceFamily {

    HERO("hero",
         "fonts/hero.otf"),

    HERO_LIGHT("hero_light",
               "fonts/hero_light.otf");

    private String mTypeFaceName;
    private String mTypeFacePath;

    HeroTypefaceFamily(String typeFaceName,
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

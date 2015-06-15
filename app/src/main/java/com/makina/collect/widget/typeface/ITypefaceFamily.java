package com.makina.collect.widget.typeface;

import android.support.annotation.NonNull;

/**
 * Describes a font type face family.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public interface ITypefaceFamily {

    /**
     * Returns the name of the type face for a given font to load.
     *
     * @return name of the type face for the font
     */
    @NonNull
    String getTypefaceName();

    /**
     * Returns the relative path from assets to load the font.
     *
     * @return the relative path from assets of this font
     */
    @NonNull
    String getTypefacePath();
}
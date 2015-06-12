package com.makina.collect.android.widget.typeface;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Default loader about {@link ITypefaceFamily}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class TypefaceLoaderManager {

    private TypefaceLoaderManager() {

    }

    /**
     * Returns a single instance of {@link TypefaceLoaderManager}.
     *
     * @return instance of {@link TypefaceLoaderManager}
     */
    public static TypefaceLoaderManager getInstance() {
        return TypefaceLoaderManagerHolder.sInstance;
    }

    private final HashMap<ITypefaceFamily, Typeface> mTypefaces = new HashMap<>();

    /**
     * Method called from code to apply a custom {@link ITypefaceFamily}.
     *
     * @param textView the {@code TextView} to have the {@link ITypefaceFamily} applied
     * @param style    the {@link ITypefaceFamily} to be applied
     */
    public void applyTypeface(TextView textView,
                              ITypefaceFamily style) {
        if (!textView.isInEditMode()) {
            final Typeface typeface = getTypeface(textView.getContext(),
                                                  style);

            if (typeface != null) {
                textView.setTypeface(typeface);
            }
        }
    }

    /**
     * Apply a type face for a given font family name.
     *
     * @param textView       the given {@code TextView} to have the {@link ITypefaceFamily} applied
     * @param fontFamilyName the font family to apply
     *
     * @see #applyTypeface(TextView, ITypefaceFamily)
     */
    public void applyTypeface(TextView textView,
                              String fontFamilyName) {
        if (!TextUtils.isEmpty(fontFamilyName)) {
            applyTypeface(textView,
                          findFontFamily(fontFamilyName));
        }
    }

    /**
     * Gets the corresponding {@code TypeFace} for a given {@link ITypefaceFamily}.
     *
     * @param context the {@code Context} of the {@link TextView}
     * @param style   the {@link ITypefaceFamily} to be applied
     *
     * @return the {@link Typeface} corresponding to the {@link ITypefaceFamily}, if defined
     */
    private Typeface getTypeface(Context context,
                                 ITypefaceFamily style) {
        if (style == null) {
            return getDefaultFont();
        }

        if (mTypefaces.containsKey(style)) {
            return mTypefaces.get(style);
        }

        final Typeface typeface = Typeface.createFromAsset(context.getAssets(),
                                                           style.getTypefacePath());

        if (typeface == null) {
            return getDefaultFont();
        }

        mTypefaces.put(style,
                       typeface);

        return typeface;
    }

    @NonNull
    private Typeface getDefaultFont() {
        return Typeface.DEFAULT;
    }

    @Nullable
    private ITypefaceFamily findFontFamily(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }

        final List<ITypefaceFamily> typefaceFamilies = new ArrayList<>();
        typefaceFamilies.addAll(Arrays.asList(AvenirTypefaceFamily.values()));
        typefaceFamilies.addAll(Arrays.asList(HeroTypefaceFamily.values()));
        typefaceFamilies.addAll(Arrays.asList(JournaltypeFaceFamily.values()));

        for (ITypefaceFamily typefaceFamily : typefaceFamilies) {
            if (typefaceFamily.getTypefaceName().toLowerCase().equals(name.toLowerCase())) {
                return typefaceFamily;
            }
        }

        return null;
    }

    private static class TypefaceLoaderManagerHolder {
        private final static TypefaceLoaderManager sInstance = new TypefaceLoaderManager();
    }
}
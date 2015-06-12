package com.makina.collect.android.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;

/**
 * Helper class about {@code Spannable}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class SpannableUtils {

    private SpannableUtils() {

    }

    /**
     * Apply text appearance styles to the given text and its substring.
     *
     * @param context              the current context
     * @param firstTextAppearance  the text appearance style to apply to the given text
     * @param secondTextAppearance the text appearance style to apply to the substring
     * @param text                 the global text to use
     * @param substring            the substring of the given global text to use
     *
     * @return the spanned string
     */
    public static SpannableString makeTwoTextAppearanceSpannable(final Context context,
                                                                 @StyleRes final int firstTextAppearance,
                                                                 @StyleRes final int secondTextAppearance,
                                                                 @NonNull final String text,
                                                                 @Nullable final String substring) {
        final SpannableString spannableString = new SpannableString(text);
        final int substringOffsetStart = TextUtils.isEmpty(substring) ? - 1 : text.indexOf(substring);

        spannableString.setSpan(new TextAppearanceSpan(context,
                                                       firstTextAppearance),
                                0,
                                text.length(),
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        if (substringOffsetStart != -1) {
            spannableString.setSpan(new TextAppearanceSpan(context,
                                                           secondTextAppearance),
                                    substringOffsetStart,
                                    substringOffsetStart + substring.length(),
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return spannableString;
    }
}

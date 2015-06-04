package com.makina.collect.android.utilities;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.text.TextUtils;

import com.makina.collect.android.R;
import com.makina.collect.android.preferences.ActivityPreferences;

/**
 * Helper class about application themes.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ThemeUtils {

    private ThemeUtils() {

    }

    /**
     * Resolve and apply the theme according to user preferences.
     *
     * @param context       the current context
     * @param selectedTheme the selected theme variant to apply
     */
    public static void apply(Context context,
                             @Nullable final AppThemeVariant selectedTheme) {
        final String themePreferences = PreferenceManager.getDefaultSharedPreferences(context)
                                                         .getString(ActivityPreferences.KEY_THEME,
                                                                    ActivityPreferences.KEY_THEME);

        context.setTheme(resolveTheme(selectedTheme,
                                      TextUtils.isEmpty(themePreferences) || !themePreferences.equals("0")));
    }

    @StyleRes
    private static int resolveTheme(@Nullable final AppThemeVariant selectedTheme,
                                    boolean isLightTheme) {
        if (selectedTheme == null) {
            if (isLightTheme) {
                return R.style.AppTheme_Light;
            }
            else {
                return R.style.AppTheme_Dark;
            }
        }

        switch (selectedTheme) {
            case GREEN:
                if (isLightTheme) {
                    return R.style.AppTheme_Light_Green;
                }
                else {
                    return R.style.AppTheme_Dark_Green;
                }
            case BLUE:
                if (isLightTheme) {
                    return R.style.AppTheme_Light_Blue;
                }
                else {
                    return R.style.AppTheme_Dark_Blue;
                }
            default:
                if (isLightTheme) {
                    return R.style.AppTheme_Light;
                }
                else {
                    return R.style.AppTheme_Dark;
                }
        }
    }

    public enum AppThemeVariant {
        GREEN,
        BLUE,
    }
}

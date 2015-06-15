package com.makina.collect.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.makina.collect.activity.DashBoardActivity;

/**
 * Helper class about {@code Intent} used in the whole application.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class IntentUtils {

    public static final String FORMS_PATH_EMPTY = "FORMS_PATH_EMPTY";

    private IntentUtils() {

    }

    /**
     * {@code Intent} for {@link DashBoardActivity}.
     *
     * @param context        the current {@code Context}
     * @param formsPathEmpty {@code true} if the current forms path is empty
     *
     * @return the {@code Intent} to start {@link DashBoardActivity}
     */
    @NonNull
    public static Intent dashBoardActivity(final Context context,
                                           boolean formsPathEmpty) {
        final Intent intent = new Intent(context,
                                         DashBoardActivity.class);

        final Bundle bundle = new Bundle();
        bundle.putBoolean(FORMS_PATH_EMPTY,
                          formsPathEmpty);

        intent.putExtras(bundle);

        return intent;
    }
}

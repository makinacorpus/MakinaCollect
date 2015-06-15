package com.makina.collect.content;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.makina.collect.R;

/**
 * {@code SharedPreferences} about global preferences of this application.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class AppSharedPreferences {

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    public AppSharedPreferences(Context pContext) {
        mContext = pContext;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(pContext);
    }

    public boolean isServerUrlDefined() {
        return mSharedPreferences.contains(mContext.getString(R.string.preference_server_url_key)) &&
                !TextUtils.isEmpty(mSharedPreferences.getString(mContext.getString(R.string.preference_server_url_key), ""));
    }

    @NonNull
    public String getServerUrl() {
        final String defaultServerUrl = mContext.getString(R.string.default_server_url);

        if (!isServerUrlDefined()) {
            setServerUrl(defaultServerUrl);
        }

        final String serverUrl = mSharedPreferences.getString(mContext.getString(R.string.preference_server_url_key),
                                                              defaultServerUrl);

        if (TextUtils.isEmpty(serverUrl)) {
            return defaultServerUrl;
        }
        else {
            return serverUrl;
        }
    }

    public void setServerUrl(@NonNull final String serverUrl) {
        mSharedPreferences.edit()
                          .putString(mContext.getString(R.string.preference_server_url_key),
                                     serverUrl)
                          .apply();
    }

    @Nullable
    public String getServerLogin() {
        return mSharedPreferences.getString(mContext.getString(R.string.preference_server_login_key),
                                            null);
    }

    public void setServerLogin(@NonNull final String serverLogin) {
        mSharedPreferences.edit()
                          .putString(mContext.getString(R.string.preference_server_login_key),
                                     serverLogin)
                          .apply();
    }

    @Nullable
    public String getServerPassword() {
        return mSharedPreferences.getString(mContext.getString(R.string.preference_server_password_key),
                                            null);
    }

    public void setServerPassword(@NonNull final String serverPassword) {
        mSharedPreferences.edit()
                          .putString(mContext.getString(R.string.preference_server_password_key),
                                     serverPassword)
                          .apply();
    }

    @NonNull
    public String getFormsListUrlPath() {
        final String defaultFormsListUrlPath = mContext.getString(R.string.default_odk_formlist);

        if (!mSharedPreferences.contains(mContext.getString(R.string.preference_server_forms_list_url_path_key))) {
            setFormsListUrlPath(defaultFormsListUrlPath);
        }

        final String formsListUrlPath = mSharedPreferences.getString(mContext.getString(R.string.preference_server_forms_list_url_path_key),
                                                                     defaultFormsListUrlPath);

        if (TextUtils.isEmpty(formsListUrlPath)) {
            return defaultFormsListUrlPath;
        }
        else {
            return formsListUrlPath;
        }
    }

    public void setFormsListUrlPath(@NonNull final String formsListUrlPath) {
        mSharedPreferences.edit()
                          .putString(mContext.getString(R.string.preference_server_forms_list_url_path_key),
                                     TextUtils.isEmpty(formsListUrlPath) ? mContext.getString(R.string.default_odk_formlist) : formsListUrlPath)
                          .apply();
    }

    @NonNull
    public String getSubmissionUrlPath() {
        final String defaultSubmissionUrlPath = mContext.getString(R.string.default_odk_submission);

        if (!mSharedPreferences.contains(mContext.getString(R.string.preference_server_submission_url_path_key))) {
            setSubmissionUrlPath(defaultSubmissionUrlPath);
        }

        final String submissionUrlPath = mSharedPreferences.getString(mContext.getString(R.string.preference_server_submission_url_path_key),
                                                                      defaultSubmissionUrlPath);

        if (TextUtils.isEmpty(submissionUrlPath)) {
            return defaultSubmissionUrlPath;
        }
        else {
            return submissionUrlPath;
        }
    }

    public void setSubmissionUrlPath(@NonNull final String submissionUrlPath) {
        mSharedPreferences.edit()
                          .putString(mContext.getString(R.string.preference_server_submission_url_path_key),
                                     TextUtils.isEmpty(submissionUrlPath) ? mContext.getString(R.string.default_odk_submission) : submissionUrlPath)
                          .apply();
    }
}

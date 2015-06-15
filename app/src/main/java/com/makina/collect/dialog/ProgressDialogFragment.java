package com.makina.collect.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

/**
 * Basic implementation of a {@code ProgressDialog} as {@code DialogFragment}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ProgressDialogFragment
        extends DialogFragment {

    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_PROGRESS_STYLE = "progress_style";
    private static final String KEY_MAX = "max";

    private ProgressDialog mProgressDialog;

    public static ProgressDialogFragment newInstance(@NonNull final String title,
                                                     int progressStyle,
                                                     int max) {

        return newInstance(title,
                           null,
                           progressStyle,
                           max);
    }

    public static ProgressDialogFragment newInstance(@NonNull final String title,
                                                     @Nullable final String message,
                                                     int progressStyle,
                                                     int max) {

        ProgressDialogFragment dialogFragment = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_TITLE,
                       title);

        if (!TextUtils.isEmpty(message)) {
            args.putString(KEY_MESSAGE,
                           message);
        }

        args.putInt(KEY_PROGRESS_STYLE,
                    progressStyle);
        args.putInt(KEY_MAX,
                    max);
        dialogFragment.setArguments(args);
        dialogFragment.setCancelable(false);

        return dialogFragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle(getArguments().getString(KEY_TITLE));

        if (getArguments().containsKey(KEY_MESSAGE)) {
            mProgressDialog.setMessage(getArguments().getString(KEY_MESSAGE));
        }

        mProgressDialog.setProgressStyle(getArguments().getInt(KEY_PROGRESS_STYLE));
        mProgressDialog.setMax(getArguments().getInt(KEY_MAX));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);

        return mProgressDialog;
    }

    public void setMessage(String message) {

        if (mProgressDialog != null) {
            mProgressDialog.setMessage(message);
        }
    }

    public void setProgress(int progress) {

        if (mProgressDialog != null) {
            mProgressDialog.setProgress(progress);
        }
    }
}
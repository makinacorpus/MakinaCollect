package com.makina.collect.android.service.handler;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.makina.collect.android.BuildConfig;
import com.makina.collect.android.listeners.FormDownloaderListener;
import com.makina.collect.android.model.FormDetails;
import com.makina.collect.android.service.AbstractRequestHandler;
import com.makina.collect.android.service.RequestHandlerStatus;
import com.makina.collect.android.tasks.DownloadFormsTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * {@link AbstractRequestHandler} implementation using {@link com.makina.collect.android.tasks.DownloadFormsTask}
 * {@code AsyncTask}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DownloadFormsRequestHandler
        extends AbstractRequestHandler {

    private static final String TAG = DownloadFormsRequestHandler.class.getSimpleName();

    public static final String KEY_COMMAND = "KEY_COMMAND";
    public static final String KEY_STATUS = "KEY_STATUS";
    public static final String KEY_SELECTED_FORM_DETAILS = "KEY_SELECTED_FORM_DETAILS";
    public static final String KEY_FORMS_LIST = "KEY_FORMS_LIST";
    public static final String KEY_CURRENT_FORM_NAME = "KEY_CURRENT_FORM_NAME";
    public static final String KEY_PROGRESS_VALUE = "KEY_PROGRESS_VALUE";
    public static final String KEY_PROGRESS_SIZE = "KEY_PROGRESS_SIZE";

    protected RequestHandlerStatus mRequestHandlerStatus;
    protected Bundle mMessageData;

    private FormDownloaderListener mFormDownloaderListener = new FormDownloaderListener() {
        @Override
        public void formsDownloadingComplete(HashMap<FormDetails, String> result) {
            if (mMessageData == null) {
                return;
            }

            if (result == null) {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED_WITH_ERRORS);

            }
            else {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED);

                mMessageData.putParcelableArrayList(KEY_FORMS_LIST,
                                                    new ArrayList<>(result.keySet()));
            }

            mMessageData.putParcelable(KEY_STATUS,
                                       mRequestHandlerStatus);

            sendMessage(mMessageData);
        }

        @Override
        public void progressUpdate(String currentFile,
                                   int progress,
                                   int total) {
            if (mMessageData == null) {
                return;
            }

            mMessageData.putString(KEY_CURRENT_FORM_NAME,
                                   currentFile);
            mMessageData.putInt(KEY_PROGRESS_VALUE,
                                progress);
            mMessageData.putInt(KEY_PROGRESS_SIZE,
                                total);

            sendMessage(mMessageData);
        }
    };

    public DownloadFormsRequestHandler(Context pContext) {
        super(pContext);

        this.mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.PENDING);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleMessageFromService(Message message) {

        if (BuildConfig.DEBUG) {
            Log.d(TAG,
                  "handleMessageFromService");
        }

        if (checkMessage(message) && message.getData().containsKey(KEY_COMMAND)) {
            switch ((Command) message.getData()
                                     .getSerializable(KEY_COMMAND)) {
                case START:
                    if (message.getData().containsKey(KEY_SELECTED_FORM_DETAILS)) {
                        mMessageData = message.getData();

                        mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.RUNNING);

                        mMessageData.putParcelable(KEY_STATUS,
                                                   mRequestHandlerStatus);

                        sendMessage(mMessageData);

                        final List<FormDetails> selectedFormDetailsList = message.getData().getParcelableArrayList(KEY_SELECTED_FORM_DETAILS);

                        final DownloadFormsTask downloadFormsTask = new DownloadFormsTask();
                        downloadFormsTask.setDownloaderListener(mFormDownloaderListener);
                        downloadFormsTask.execute(selectedFormDetailsList);
                    }

                    break;
                case GET_STATUS:
                    if (mMessageData == null) {
                        mMessageData = message.getData();
                        mMessageData.putParcelable(KEY_STATUS,
                                                   mRequestHandlerStatus);
                    }

                    sendMessage(mMessageData);

                    break;
            }
        }
    }

    public enum Command {
        START,
        GET_STATUS
    }
}

package com.makina.collect.android.service.handler;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.makina.collect.android.BuildConfig;
import com.makina.collect.android.listeners.FormListDownloaderListener;
import com.makina.collect.android.model.FormDetails;
import com.makina.collect.android.service.AbstractRequestHandler;
import com.makina.collect.android.service.RequestHandlerStatus;
import com.makina.collect.android.tasks.DownloadFormListTask;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * {@link AbstractRequestHandler} implementation using {@link com.makina.collect.android.tasks.DownloadFormListTask}
 * {@code AsyncTask}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DownloadFormsListRequestHandler
        extends AbstractRequestHandler {

    private static final String TAG = DownloadFormsListRequestHandler.class.getSimpleName();

    public static final String KEY_COMMAND = "KEY_COMMAND";
    public static final String KEY_STATUS = "KEY_STATUS";
    public static final String KEY_FORMS_LIST = "KEY_FORMS_LIST";

    protected RequestHandlerStatus mRequestHandlerStatus;
    protected Bundle mMessageData;

    private FormListDownloaderListener mFormListDownloaderListener = new FormListDownloaderListener() {

        @Override
        public void formListDownloadingComplete(HashMap<String, FormDetails> value) {
            if (mMessageData == null) {
                return;
            }

            if (value == null) {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED_WITH_ERRORS);
            }
            else if (value.containsKey(DownloadFormListTask.DL_AUTH_REQUIRED)) {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED_WITH_ERRORS,
                                                                 DownloadFormListTask.DL_AUTH_REQUIRED);
            }
            else if (value.containsKey(DownloadFormListTask.DL_ERROR_MSG)) {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED_WITH_ERRORS,
                                                                 DownloadFormListTask.DL_ERROR_MSG);
            }
            else {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED);

                mMessageData.putParcelableArrayList(KEY_FORMS_LIST,
                                                    new ArrayList<>(value.values()));
            }

            mMessageData.putParcelable(KEY_STATUS,
                                       mRequestHandlerStatus);

            sendMessage(mMessageData);
        }
    };

    public DownloadFormsListRequestHandler(Context pContext) {
        super(pContext);

        this.mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.PENDING);
    }

    @Override
    protected void handleMessageFromService(Message message) {

        if (BuildConfig.DEBUG) {
            Log.d(TAG,
                  "handleMessageFromService");
        }

        if (checkMessage(message) && message.getData().containsKey(KEY_COMMAND)) {
            switch ((Command) message.getData().getSerializable(KEY_COMMAND)) {
                case START:
                    mMessageData = message.getData();

                    mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.RUNNING);

                    mMessageData.putParcelable(KEY_STATUS,
                                               mRequestHandlerStatus);

                    sendMessage(mMessageData);

                    final DownloadFormListTask downloadFormListTask = new DownloadFormListTask();
                    downloadFormListTask.setDownloaderListener(mFormListDownloaderListener);
                    downloadFormListTask.execute();
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

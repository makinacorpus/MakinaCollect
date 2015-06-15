package com.makina.collect.service.handler;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.makina.collect.BuildConfig;
import com.makina.collect.listeners.DiskSyncListener;
import com.makina.collect.service.AbstractRequestHandler;
import com.makina.collect.service.RequestHandlerStatus;
import com.makina.collect.tasks.DiskSyncTask;

/**
 * {@link AbstractRequestHandler} implementation using {@link com.makina.collect.tasks.DiskSyncTask}
 * {@code AsyncTask}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DiskSyncRequestHandler
        extends AbstractRequestHandler {

    private static final String TAG = DiskSyncRequestHandler.class.getSimpleName();

    public static final String KEY_COMMAND = "KEY_COMMAND";
    public static final String KEY_STATUS = "KEY_STATUS";
    public static final String KEY_SYNC_MESSAGE = "KEY_SYNC_MESSAGE";

    protected RequestHandlerStatus mRequestHandlerStatus;
    protected Bundle mMessageData;

    private DiskSyncListener mDiskSyncListener = new DiskSyncListener() {
        @Override
        public void syncComplete(String result) {
            if (mMessageData == null) {
                return;
            }

            if (TextUtils.isEmpty(result)) {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED_WITH_ERRORS);
            }
            else {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED);

                mMessageData.putString(KEY_SYNC_MESSAGE,
                                       result);
            }

            mMessageData.putParcelable(KEY_STATUS,
                                       mRequestHandlerStatus);

            sendMessage(mMessageData);
        }
    };

    public DiskSyncRequestHandler(Context pContext) {
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
            switch ((Command) message.getData()
                                     .getSerializable(KEY_COMMAND)) {
                case START:
                    mMessageData = message.getData();

                    mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.RUNNING);

                    mMessageData.putParcelable(KEY_STATUS,
                                               mRequestHandlerStatus);

                    sendMessage(mMessageData);

                    final DiskSyncTask diskSyncTask = new DiskSyncTask();
                    diskSyncTask.setDiskSyncListener(mDiskSyncListener);
                    diskSyncTask.execute();

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

package com.makina.collect.android.service.handler;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.makina.collect.android.BuildConfig;
import com.makina.collect.android.model.FormDetails;
import com.makina.collect.android.provider.FormsProvider;
import com.makina.collect.android.provider.InstanceProvider;
import com.makina.collect.android.service.AbstractRequestHandler;
import com.makina.collect.android.service.RequestHandlerStatus;

import java.util.List;

/**
 * {@link AbstractRequestHandler} implementation using an {@code AynsTask} to delete a {@code List}
 * of selected {@link FormDetails}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DeleteFormsRequestHandler
        extends AbstractRequestHandler {

    private static final String TAG = DeleteFormsRequestHandler.class.getSimpleName();

    public static final String KEY_COMMAND = "KEY_COMMAND";
    public static final String KEY_STATUS = "KEY_STATUS";
    public static final String KEY_SELECTED_FORM_DETAILS = "KEY_SELECTED_FORM_DETAILS";
    public static final String KEY_SELECTED_FORM_DETAILS_INSTANCES = "KEY_SELECTED_FORM_DETAILS_INSTANCES";
    public static final String KEY_FORMS_DELETED = "KEY_FORMS_DELETED";

    protected RequestHandlerStatus mRequestHandlerStatus;

    public DeleteFormsRequestHandler(Context pContext) {
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
                    if (message.getData().containsKey(KEY_SELECTED_FORM_DETAILS)) {
                        final DeleteFormsAsyncTask deleteFormsAsyncTask = new DeleteFormsAsyncTask(message.getData());
                        deleteFormsAsyncTask.execute();
                    }

                    if (message.getData().containsKey(KEY_SELECTED_FORM_DETAILS_INSTANCES)) {
                        final DeleteFormInstancesAsyncTask deleteFormInstancesAsyncTask = new DeleteFormInstancesAsyncTask(message.getData());
                        deleteFormInstancesAsyncTask.execute();
                    }

                    break;
                case GET_STATUS:
                    message.getData().putParcelable(KEY_STATUS,
                                                    mRequestHandlerStatus);

                    sendMessage(message.getData());

                    break;
            }
        }
    }

    public enum Command {
        START,
        GET_STATUS
    }

    /**
     * Default {@code AsyncTask} about deleting a {@code List} of selected {@link FormDetails}.
     *
     * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
     */
    private class DeleteFormsAsyncTask extends AsyncTask<Void, Void, Integer> {

        private Bundle mData;

        public DeleteFormsAsyncTask(Bundle pData) {
            this.mData = pData;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (BuildConfig.DEBUG) {
                Log.d(getClass().getName(),
                      "doInBackground");
            }

            mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.RUNNING);

            mData.putParcelable(KEY_STATUS,
                                mRequestHandlerStatus);

            sendMessage(mData);

            int deletedForms = 0;
            boolean hasWarnings =false;

            if (mData.containsKey(KEY_SELECTED_FORM_DETAILS)) {
                final List<FormDetails> selectedFormDetailsList = mData.getParcelableArrayList(KEY_SELECTED_FORM_DETAILS);

                for (FormDetails formDetails : selectedFormDetailsList) {

                    // do not delete FormDetails with existing instances
                    if (InstanceProvider.getCountInstancesByFormId(formDetails.formID) == 0) {
                        FormsProvider.deleteFileOrDir(formDetails.filePath);
                        FormsProvider.deleteFileOrDir(formDetails.directoryPath);
                        FormsProvider.deleteForm(formDetails.formID);

                        deletedForms++;
                    }
                    else {
                        hasWarnings = true;
                    }
                }

                mRequestHandlerStatus = new RequestHandlerStatus(hasWarnings ? RequestHandlerStatus.Status.FINISHED_WITH_WARNINGS : RequestHandlerStatus.Status.FINISHED);
            }
            else {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED_WITH_ERRORS);
            }

            return deletedForms;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

            mData.putInt(KEY_FORMS_DELETED,
                                integer);
            mData.putParcelable(KEY_STATUS,
                                mRequestHandlerStatus);

            sendMessage(mData);
        }
    }

    /**
     * Default {@code AsyncTask} about deleting a {@code List} of selected {@link FormDetails} instances.
     *
     * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
     */
    private class DeleteFormInstancesAsyncTask extends AsyncTask<Void, Void, Integer> {

        private Bundle mData;

        public DeleteFormInstancesAsyncTask(Bundle pData) {
            this.mData = pData;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (BuildConfig.DEBUG) {
                Log.d(getClass().getName(),
                      "doInBackground");
            }

            mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.RUNNING);

            mData.putParcelable(KEY_STATUS,
                                mRequestHandlerStatus);

            sendMessage(mData);

            int deletedForms = 0;

            if (mData.containsKey(KEY_SELECTED_FORM_DETAILS_INSTANCES)) {
                final List<FormDetails> selectedFormDetailsList = mData.getParcelableArrayList(KEY_SELECTED_FORM_DETAILS_INSTANCES);

                for (FormDetails formDetails : selectedFormDetailsList) {
                    InstanceProvider.deleteInstance(formDetails.id);
                }

                deletedForms = selectedFormDetailsList.size();
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED);
            }
            else {
                mRequestHandlerStatus = new RequestHandlerStatus(RequestHandlerStatus.Status.FINISHED_WITH_ERRORS);
            }

            return deletedForms;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

            mData.putInt(KEY_FORMS_DELETED,
                         integer);
            mData.putParcelable(KEY_STATUS,
                                mRequestHandlerStatus);

            sendMessage(mData);
        }
    }
}

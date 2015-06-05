package com.makina.collect.android.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.makina.collect.android.BuildConfig;
import com.makina.collect.android.R;
import com.makina.collect.android.adapter.FormDetailsListAdapter;
import com.makina.collect.android.model.FormDetails;
import com.makina.collect.android.service.AbstractRequestHandler;
import com.makina.collect.android.service.RequestHandlerServiceClient;
import com.makina.collect.android.service.RequestHandlerStatus;
import com.makina.collect.android.service.handler.DownloadFormsListRequestHandler;
import com.makina.collect.android.widgets.recyclerview.DividerItemDecoration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@code Fragment} representing a {@code List} of {@link FormDetails}
 * on which we can perform a selection of available forms to download.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class FormDetailsRecyclerViewFragment
        extends RecyclerViewFragment {

    private static final String TAG = FormDetailsRecyclerViewFragment.class.getSimpleName();

    private static final String KEY_REQUEST_HANDLER_SERVICE_CLIENT_TOKEN = "KEY_REQUEST_HANDLER_SERVICE_CLIENT_TOKEN";
    private static final String KEY_SELECTED_FORM_DETAILS = "KEY_SELECTED_FORM_DETAILS";

    private RequestHandlerServiceClient mRequestHandlerServiceClient;

    private FormDetailsListAdapter mFormDetailsListAdapter;

    private Bundle mSavedState;
    private final Set<FormDetails> mSelectedFormDetailsList = new HashSet<>();

    private ActionMode mActionMode;

    private FormDetailsListAdapter.OnFormDetailsItemListener mOnFormDetailsItemListener = new FormDetailsListAdapter.OnFormDetailsItemListener() {

        @Override
        public void onFormDetailsSelected(FormDetails formDetails) {
            Log.d(TAG,
                  "onFormDetailsSelected: " + formDetails.formName);

            if (formDetails.checked) {
                mSelectedFormDetailsList.add(formDetails);
            }
            else {
                mSelectedFormDetailsList.remove(formDetails);
            }

            if (mSelectedFormDetailsList.isEmpty()) {
                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }
            else {
                if (mActionMode == null) {
                    mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
                }

                mActionMode.setTitle(String.valueOf(mSelectedFormDetailsList.size()));
            }
        }
    };

    private RequestHandlerServiceClient.ServiceClientListener mServiceClientListener = new RequestHandlerServiceClient.ServiceClientListener() {

        @Override
        public void onConnected(@NonNull String token) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                      "onConnected: " + token);
            }

            mSavedState.putString(
                    KEY_REQUEST_HANDLER_SERVICE_CLIENT_TOKEN,
                    token
            );

            // send Message to get the current status of DownloadFormsListRequestHandler
            final Bundle data = new Bundle();
            data.putSerializable(DownloadFormsListRequestHandler.KEY_COMMAND,
                                 DownloadFormsListRequestHandler.Command.GET_STATUS);

            mRequestHandlerServiceClient.send(DownloadFormsListRequestHandler.class,
                                              data);
        }

        @Override
        public void onDisconnected() {
            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                      "onDisconnected");
            }
        }

        @Override
        public void onHandleMessage(@NonNull AbstractRequestHandler requestHandler,
                                    @NonNull Bundle data) {
            if (requestHandler instanceof DownloadFormsListRequestHandler) {
                handleMessageForDownloadFormsListRequestHandler(data);
            }
        }
    };

    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode,
                                          Menu menu) {
            final MenuInflater menuInflater = getActivity().getMenuInflater();
            menuInflater.inflate(R.menu.download,
                                 menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode,
                                           Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode,
                                           MenuItem item) {
            switch (item.getItemId()) {
                case R.menu.download:
                    // TODO: perform download selections
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;

            clearSelection();
        }
    };

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment {@link FormDetailsRecyclerViewFragment}.
     */
    public static FormDetailsRecyclerViewFragment newInstance() {
        final FormDetailsRecyclerViewFragment fragment = new FormDetailsRecyclerViewFragment();
        fragment.setArguments(new Bundle());

        return fragment;
    }

    public FormDetailsRecyclerViewFragment() {
        // required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFormDetailsListAdapter = new FormDetailsListAdapter(mOnFormDetailsItemListener);

        if (savedInstanceState == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                      "onCreate, savedInstanceState null");
            }

            mSavedState = new Bundle();
        }
        else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                      "onCreate, savedInstanceState initialized");
            }

            mSavedState = savedInstanceState;

            final List<FormDetails> selectedFormDetailsList = mSavedState.getParcelableArrayList(KEY_SELECTED_FORM_DETAILS);
            mSelectedFormDetailsList.clear();
            mSelectedFormDetailsList.addAll(selectedFormDetailsList);
        }
    }

    @Override
    public void onViewCreated(View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view,
                            savedInstanceState);

        getRecyclerView().addItemDecoration(new DividerItemDecoration(getActivity(),
                                                                      DividerItemDecoration.VERTICAL_LIST));

        setRecyclerViewAdapter(mFormDetailsListAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mRequestHandlerServiceClient == null) {
            mRequestHandlerServiceClient = new RequestHandlerServiceClient(getActivity());
        }

        mRequestHandlerServiceClient.setServiceClientListener(mServiceClientListener);
        mRequestHandlerServiceClient.connect(mSavedState.getString(KEY_REQUEST_HANDLER_SERVICE_CLIENT_TOKEN));
    }

    @Override
    public void onPause() {
        if (mRequestHandlerServiceClient != null) {
            mRequestHandlerServiceClient.disconnect();
        }

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mSavedState.putParcelableArrayList(KEY_SELECTED_FORM_DETAILS,
                                           new ArrayList<>(mSelectedFormDetailsList));

        outState.putAll(mSavedState);

        super.onSaveInstanceState(outState);
    }

    private void clearSelection() {
        for (FormDetails formDetails : mSelectedFormDetailsList) {
            formDetails.checked = false;
            mFormDetailsListAdapter.update(mFormDetailsListAdapter.getItemPosition(formDetails),
                                           formDetails);
        }

        mSelectedFormDetailsList.clear();
    }

    private void handleMessageForDownloadFormsListRequestHandler(@NonNull final Bundle data) {
        if (data.containsKey(DownloadFormsListRequestHandler.KEY_STATUS)) {
            final RequestHandlerStatus.Status status = ((RequestHandlerStatus) data.getParcelable(DownloadFormsListRequestHandler.KEY_STATUS)).getStatus();

            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                      "onHandleMessage: DownloadFormsListRequestHandler status " + status.name());
            }

            switch (status) {
                case PENDING:
                    // send Message to start downloading forms
                    data.putSerializable(DownloadFormsListRequestHandler.KEY_COMMAND,
                                         DownloadFormsListRequestHandler.Command.START);

                    mRequestHandlerServiceClient.send(DownloadFormsListRequestHandler.class,
                                                      data);

                    break;
                case RUNNING:
                    break;
                case FINISHED:
                    final List<FormDetails> formDetailsList = data.getParcelableArrayList(DownloadFormsListRequestHandler.KEY_FORMS_LIST);

                    final Set<FormDetails> formDetailsSet = new HashSet<>();
                    formDetailsSet.addAll(formDetailsList);
                    formDetailsSet.addAll(mSelectedFormDetailsList);

                    mFormDetailsListAdapter.clear();
                    mFormDetailsListAdapter.addAll(formDetailsSet);

                    if (mSelectedFormDetailsList.isEmpty()) {
                        if (mActionMode != null) {
                            mActionMode.finish();
                        }
                    }
                    else {
                        if (mActionMode == null) {
                            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
                        }

                        mActionMode.setTitle(String.valueOf(mSelectedFormDetailsList.size()));
                    }

                    break;
                case FINISHED_WITH_ERRORS:
                    break;
            }
        }
    }
}

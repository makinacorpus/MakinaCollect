package com.makina.collect.android.fragment;

import android.app.ProgressDialog;
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
import android.widget.Toast;

import com.makina.collect.android.BuildConfig;
import com.makina.collect.android.R;
import com.makina.collect.android.adapter.FormDetailsListAdapter;
import com.makina.collect.android.dialog.ProgressDialogFragment;
import com.makina.collect.android.model.FormDetails;
import com.makina.collect.android.service.AbstractRequestHandler;
import com.makina.collect.android.service.RequestHandlerServiceClient;
import com.makina.collect.android.service.RequestHandlerStatus;
import com.makina.collect.android.service.handler.DownloadFormsListRequestHandler;
import com.makina.collect.android.service.handler.DownloadFormsRequestHandler;
import com.makina.collect.android.widget.recyclerview.DividerItemDecoration;

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

    protected static final String PROGRESS_DIALOG_FRAGMENT = "PROGRESS_DIALOG_FRAGMENT";

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

            updateActionMode(true);
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

            final Bundle dataForDownloadFormsListRequestHandler = new Bundle();
            dataForDownloadFormsListRequestHandler.putSerializable(DownloadFormsListRequestHandler.KEY_COMMAND,
                                                                   DownloadFormsListRequestHandler.Command.GET_STATUS);

            // send Message to get the current status of DownloadFormsListRequestHandler
            mRequestHandlerServiceClient.send(DownloadFormsListRequestHandler.class,
                                              dataForDownloadFormsListRequestHandler);

            final Bundle dataForDownloadFormsRequestHandler = new Bundle();
            dataForDownloadFormsRequestHandler.putSerializable(DownloadFormsRequestHandler.KEY_COMMAND,
                                                               DownloadFormsRequestHandler.Command.GET_STATUS);

            // send Message to get the current status of DownloadFormsRequestHandler
            mRequestHandlerServiceClient.send(DownloadFormsRequestHandler.class,
                                              dataForDownloadFormsRequestHandler);
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

            if (requestHandler instanceof DownloadFormsRequestHandler) {
                handleMessageForDownloadFormsRequestHandler(data);
            }
        }
    };

    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode,
                                          Menu menu) {
            final MenuInflater menuInflater = getActivity().getMenuInflater();
            menuInflater.inflate(R.menu.toggle_selection,
                                 menu);
            menuInflater.inflate(R.menu.download,
                                 menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode,
                                           Menu menu) {
            final MenuItem menuItemToggleSelection = menu.findItem(R.id.menu_toggle_selection);
            final MenuItem menuItemDownload = menu.findItem(R.id.menu_download);

            menuItemDownload.setEnabled(!mSelectedFormDetailsList.isEmpty());

            if (mSelectedFormDetailsList.size() < mFormDetailsListAdapter.getItemCount()) {
                menuItemToggleSelection.setTitle(getString(R.string.select_all));
                menuItemToggleSelection.setIcon(R.drawable.ic_action_content_select_all);
            }
            else {
                menuItemToggleSelection.setTitle(getString(R.string.deselect_all));
                menuItemToggleSelection.setIcon(R.drawable.ic_action_content_clear);
            }

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode,
                                           MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_download:
                    // send Message to perform the download of the selected forms
                    final Bundle data = new Bundle();
                    data.putSerializable(DownloadFormsRequestHandler.KEY_COMMAND,
                                         DownloadFormsRequestHandler.Command.START);
                    data.putParcelableArrayList(DownloadFormsRequestHandler.KEY_SELECTED_FORM_DETAILS,
                                                new ArrayList<>(mSelectedFormDetailsList));

                    mRequestHandlerServiceClient.send(DownloadFormsRequestHandler.class,
                                                      data);

                    clearSelection();
                    updateActionMode(true);

                    return true;
                case R.id.menu_toggle_selection:
                    if (mSelectedFormDetailsList.size() < mFormDetailsListAdapter.getItemCount()) {
                        selectAll();
                    }
                    else {
                        clearSelection();
                    }

                    updateActionMode(false);

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

    private void selectAll() {
        for (int i = 0; i < mFormDetailsListAdapter.getItemCount(); i++) {
            final FormDetails formDetails = mFormDetailsListAdapter.getItem(i);

            // should never occur
            if ((formDetails != null) && !formDetails.checked) {
                formDetails.checked = true;

                mFormDetailsListAdapter.update(mFormDetailsListAdapter.getItemPosition(formDetails),
                                               formDetails);
                mSelectedFormDetailsList.add(formDetails);
            }
        }
    }

    private void updateActionMode(boolean finishIfEmpty) {
        if (mSelectedFormDetailsList.isEmpty() && finishIfEmpty) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }
        else {
            if (getActivity() == null) {
                return;
            }

            if (mActionMode == null) {
                mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            }

            mActionMode.setTitle(String.valueOf(mSelectedFormDetailsList.size()));
            mActionMode.invalidate();
        }
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
                case FINISHED:
                    final List<FormDetails> formDetailsList = data.getParcelableArrayList(DownloadFormsListRequestHandler.KEY_FORMS_LIST);

                    final Set<FormDetails> formDetailsSet = new HashSet<>();
                    formDetailsSet.addAll(formDetailsList);
                    formDetailsSet.addAll(mSelectedFormDetailsList);

                    mFormDetailsListAdapter.clear();
                    mFormDetailsListAdapter.addAll(formDetailsSet);

                    updateActionMode(true);

                    break;
                case FINISHED_WITH_ERRORS:
                    // TODO: manage errors
                    break;
            }
        }
    }

    private void handleMessageForDownloadFormsRequestHandler(@NonNull final Bundle data) {
        if (data.containsKey(DownloadFormsRequestHandler.KEY_STATUS)) {
            final RequestHandlerStatus.Status status = ((RequestHandlerStatus) data.getParcelable(DownloadFormsRequestHandler.KEY_STATUS)).getStatus();

            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                      "onHandleMessage: DownloadFormsRequestHandler status " + status.name());
            }

            switch (status) {
                case RUNNING:
                    if (data.containsKey(DownloadFormsRequestHandler.KEY_CURRENT_FORM_NAME) &&
                            data.containsKey(DownloadFormsRequestHandler.KEY_PROGRESS_VALUE) &&
                            data.containsKey(DownloadFormsRequestHandler.KEY_PROGRESS_SIZE)) {

                        if (getActivity() == null) {
                            return;
                        }

                        // find ProgressDialogFragment to update
                        ProgressDialogFragment progressDialogFragment = (ProgressDialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_FRAGMENT);

                        if (progressDialogFragment == null) {
                            progressDialogFragment = ProgressDialogFragment.newInstance(
                                    getString(R.string.downloading),
                                    ProgressDialog.STYLE_HORIZONTAL,
                                    data.getInt(DownloadFormsRequestHandler.KEY_PROGRESS_SIZE));
                            progressDialogFragment.show(getActivity().getSupportFragmentManager(),
                                                        PROGRESS_DIALOG_FRAGMENT);
                        }

                        progressDialogFragment.setProgress(data.getInt(DownloadFormsRequestHandler.KEY_PROGRESS_VALUE));
                    }

                    break;
                case FINISHED:
                    if (data.containsKey(DownloadFormsRequestHandler.KEY_FORMS_LIST)) {
                        int numberOfFormsDownloaded = data.getParcelableArrayList(DownloadFormsRequestHandler.KEY_FORMS_LIST)
                                                          .size();

                        if (getActivity() == null) {
                            return;
                        }

                        // find ProgressDialogFragment to dismiss
                        ProgressDialogFragment progressDialogFragment = (ProgressDialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_FRAGMENT);

                        if (progressDialogFragment != null) {
                            progressDialogFragment.dismiss();
                        }

                        Toast.makeText(getActivity(),
                                       getResources().getQuantityString(R.plurals.download_forms_finish,
                                                                        numberOfFormsDownloaded,
                                                                        numberOfFormsDownloaded),
                                       Toast.LENGTH_LONG)
                             .show();
                    }

                    break;
                case FINISHED_WITH_ERRORS:
                    if (getActivity() == null) {
                        return;
                    }

                    // find ProgressDialogFragment to dismiss
                    ProgressDialogFragment progressDialogFragment = (ProgressDialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_FRAGMENT);

                    if (progressDialogFragment != null) {
                        progressDialogFragment.dismiss();
                    }

                    break;
            }
        }
    }
}

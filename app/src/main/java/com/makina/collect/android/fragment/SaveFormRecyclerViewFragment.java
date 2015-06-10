package com.makina.collect.android.fragment;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import com.makina.collect.android.adapter.EditFormCursorAdapter;
import com.makina.collect.android.model.FormDetails;
import com.makina.collect.android.provider.FormsProviderAPI;
import com.makina.collect.android.provider.InstanceProviderAPI;
import com.makina.collect.android.service.AbstractRequestHandler;
import com.makina.collect.android.service.RequestHandlerServiceClient;
import com.makina.collect.android.service.RequestHandlerStatus;
import com.makina.collect.android.service.handler.DeleteFormsRequestHandler;
import com.makina.collect.android.widget.recyclerview.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code Fragment} representing a {@code List} of available forms to edit.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class SaveFormRecyclerViewFragment
        extends RecyclerViewFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = SaveFormRecyclerViewFragment.class.getSimpleName();

    private static final String KEY_REQUEST_HANDLER_SERVICE_CLIENT_TOKEN = "KEY_REQUEST_HANDLER_SERVICE_CLIENT_TOKEN";
    private static final String KEY_SELECTED_FORM_DETAILS = "KEY_SELECTED_FORM_DETAILS";

    private RequestHandlerServiceClient mRequestHandlerServiceClient;

    private EditFormCursorAdapter mEditFormCursorAdapter;

    private Bundle mSavedState;

    private ActionMode mActionMode;

    private EditFormCursorAdapter.OnEditFormItemListener mOnEditFormItemListener = new EditFormCursorAdapter.OnEditFormItemListener() {

        @Override
        public void onFormDetailsSelected(FormDetails formDetails) {
            Log.d(TAG,
                  "onFormDetailsSelected: " + formDetails.formName);

            updateActionMode(true);
        }

        @Override
        public void onFormDetailsEdited(FormDetails formDetails) {
            Log.d(TAG,
                  "onFormDetailsEdited: " + formDetails.formName);

            final Intent intent = new Intent(Intent.ACTION_EDIT,
                                             ContentUris.withAppendedId(FormsProviderAPI.FormsColumns.CONTENT_URI,
                                                                        formDetails.id));
            intent.putExtra("newForm",
                            true);
            startActivity(intent);
        }
    };

    private RequestHandlerServiceClient.ServiceClientListener mServiceClientListener = new RequestHandlerServiceClient.ServiceClientListener() {

        @Override
        public void onConnected(@NonNull String token) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                      "onConnected: " + token);
            }

            mSavedState.putString(KEY_REQUEST_HANDLER_SERVICE_CLIENT_TOKEN,
                                  token);

            final Bundle dataForDeleteFormsRequestHandler = new Bundle();
            dataForDeleteFormsRequestHandler.putSerializable(DeleteFormsRequestHandler.KEY_COMMAND,
                                                             DeleteFormsRequestHandler.Command.GET_STATUS);

            // send Message to get the current status of DeleteFormsRequestHandler
            mRequestHandlerServiceClient.send(DeleteFormsRequestHandler.class,
                                              dataForDeleteFormsRequestHandler);
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
            if (requestHandler instanceof DeleteFormsRequestHandler) {
                handleMessageForDeleteFormsRequestHandler(data);
            }
        }
    };

    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode,
                                          Menu menu) {
            final MenuInflater menuInflater = getActivity().getMenuInflater();
            menuInflater.inflate(R.menu.delete,
                                 menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode,
                                           Menu menu) {
            final MenuItem menuItemDelete = menu.findItem(R.id.menu_delete);

            menuItemDelete.setEnabled(!mEditFormCursorAdapter.getSelection().isEmpty());

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode,
                                           MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    // send Message to perform the download of the selected forms
                    final Bundle data = new Bundle();
                    data.putSerializable(DeleteFormsRequestHandler.KEY_COMMAND,
                                         DeleteFormsRequestHandler.Command.START);
                    data.putParcelableArrayList(DeleteFormsRequestHandler.KEY_SELECTED_FORM_DETAILS_INSTANCES,
                                                new ArrayList<>(mEditFormCursorAdapter.getSelection()));

                    mRequestHandlerServiceClient.send(DeleteFormsRequestHandler.class,
                                                      data);

                    mEditFormCursorAdapter.clearSelection();

                    updateActionMode(true);

                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;

            mEditFormCursorAdapter.clearSelection();
        }
    };

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment {@link SaveFormRecyclerViewFragment}.
     */
    public static SaveFormRecyclerViewFragment newInstance() {
        final SaveFormRecyclerViewFragment fragment = new SaveFormRecyclerViewFragment();
        fragment.setArguments(new Bundle());

        return fragment;
    }

    public SaveFormRecyclerViewFragment() {
        // required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEditFormCursorAdapter = new EditFormCursorAdapter(null,
                                                           mOnEditFormItemListener);

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
            mEditFormCursorAdapter.updateSelection(selectedFormDetailsList);
        }
    }

    @Override
    public void onViewCreated(View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view,
                            savedInstanceState);

        getRecyclerView().addItemDecoration(new DividerItemDecoration(getActivity(),
                                                                      DividerItemDecoration.VERTICAL_LIST));

        setRecyclerViewAdapter(mEditFormCursorAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(0,
                                      null,
                                      this);
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
                                           new ArrayList<>(mEditFormCursorAdapter.getSelection()));

        outState.putAll(mSavedState);

        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id,
                                         Bundle args) {
        return new CursorLoader(getActivity(),
                                InstanceProviderAPI.InstanceColumns.CONTENT_URI,
                                null,
                                InstanceProviderAPI.InstanceColumns.STATUS + " = ?",
                                new String[]{InstanceProviderAPI.STATUS_INCOMPLETE},
                                InstanceProviderAPI.InstanceColumns.STATUS + " DESC, " + InstanceProviderAPI.InstanceColumns.DISPLAY_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader,
                               Cursor data) {
        mEditFormCursorAdapter.swapCursor(data);

        updateActionMode(true);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // data is not available anymore, delete reference
        if (mEditFormCursorAdapter != null) {
            mEditFormCursorAdapter.swapCursor(null);
        }
    }

    private void updateActionMode(boolean finishIfEmpty) {
        if (mEditFormCursorAdapter.getSelection().isEmpty() && finishIfEmpty) {
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

            mActionMode.setTitle(String.valueOf(mEditFormCursorAdapter.getSelection()
                                                                      .size()));
            mActionMode.invalidate();
        }
    }

    private void handleMessageForDeleteFormsRequestHandler(@NonNull final Bundle data) {
        if (data.containsKey(DeleteFormsRequestHandler.KEY_STATUS)) {
            final RequestHandlerStatus.Status status = ((RequestHandlerStatus) data.getParcelable(DeleteFormsRequestHandler.KEY_STATUS)).getStatus();

            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                      "onHandleMessage: DeleteFormsRequestHandler status " + status.name());
            }

            final int deletedForms = data.getInt(DeleteFormsRequestHandler.KEY_FORMS_DELETED,
                                                 0);

            switch (status) {
                case FINISHED:
                    getLoaderManager().restartLoader(0,
                                                     null,
                                                     this);

                    Toast.makeText(getActivity(),
                                   getResources().getQuantityString(R.plurals.files_deleted_finish,
                                                                    deletedForms,
                                                                    deletedForms),
                                   Toast.LENGTH_LONG)
                         .show();
                    break;
                case FINISHED_WITH_ERRORS:
                    Toast.makeText(getActivity(),
                                   R.string.file_deleted_ko,
                                   Toast.LENGTH_LONG)
                         .show();

                    break;
            }
        }
    }
}

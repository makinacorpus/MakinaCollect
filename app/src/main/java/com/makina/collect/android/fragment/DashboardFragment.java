package com.makina.collect.android.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.makina.collect.android.R;

/**
 * {@code Fragment} about the dashboard.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DashboardFragment extends Fragment implements View.OnClickListener {

    @Nullable
    protected OnDashboardFragmentListener mOnDashboardFragmentListener;

    /**
     * Use this factory method to create a new instance of this {@code Fragment}.
     *
     * @return A new instance of {@link DashboardFragment}.
     */
    @NonNull
    public static DashboardFragment newInstance() {
        final DashboardFragment dashboardFragment = new DashboardFragment();
        dashboardFragment.setArguments(new Bundle());

        return dashboardFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.dashboardFormDownloadButton).setOnClickListener(this);
        view.findViewById(R.id.dashboardFormEditButton).setOnClickListener(this);
        view.findViewById(R.id.dashboardFormFinishButton).setOnClickListener(this);
        view.findViewById(R.id.dashboardFormSendButton).setOnClickListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mOnDashboardFragmentListener = (OnDashboardFragmentListener) activity;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + OnDashboardFragmentListener.class.getName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mOnDashboardFragmentListener = null;
    }

    @Override
    public void onClick(View v) {
        if (mOnDashboardFragmentListener == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.dashboardFormDownloadButton:
                mOnDashboardFragmentListener.onFormDownload();
                break;
            case R.id.dashboardFormEditButton:
                mOnDashboardFragmentListener.onFormEdit();
                break;
            case R.id.dashboardFormFinishButton:
                mOnDashboardFragmentListener.onFormFinish();
                break;
            case R.id.dashboardFormSendButton:
                mOnDashboardFragmentListener.onFormSend();
                break;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * {@code Fragment} to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
     */
    public interface OnDashboardFragmentListener {

        void onFormDownload();

        void onFormEdit();

        void onFormFinish();

        void onFormSend();
    }
}

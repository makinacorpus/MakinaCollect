package com.makina.collect.android.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.makina.collect.android.R;

/**
 * Base {@code Fragment} about {@code RecyclerView}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class RecyclerViewFragment
        extends Fragment {

    protected RecyclerView mRecyclerView;
    protected View mProgressView;
    protected TextView mTextViewEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recycler_view,
                                container,
                                false);
    }

    @Override
    public void onViewCreated(View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view,
                            savedInstanceState);

        mRecyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager as default layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mProgressView = view.findViewById(android.R.id.progress);
        mTextViewEmpty = (TextView) view.findViewById(android.R.id.message);
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void setRecyclerViewAdapter(@Nullable RecyclerView.Adapter adapter) {
        mRecyclerView.setAdapter(adapter);

        if (adapter != null) {
            // start out with a progress indicator
            showProgressBar(true);

            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

                                                    @Override
                                                    public void onChanged() {
                                                        super.onChanged();

                                                        showProgressBar(false);
                                                        showEmptyText(mRecyclerView.getAdapter()
                                                                                   .getItemCount() == 0);
                                                    }

                                                    @Override
                                                    public void onItemRangeInserted(int positionStart,
                                                                                    int itemCount) {
                                                        super.onItemRangeInserted(positionStart,
                                                                                  itemCount);

                                                        showProgressBar(false);
                                                        showEmptyText(false);
                                                    }
                                                });
        }
    }

    public void setEmptyText(CharSequence message) {
        mTextViewEmpty.setText(message);
    }

    public void showProgressBar(boolean show) {
        if ((mProgressView.getVisibility() == View.VISIBLE) == show) {
            return;
        }

        if (show) {
            mTextViewEmpty.setVisibility(View.GONE);
            mProgressView.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                                                                      android.R.anim.fade_in));
            mProgressView.setVisibility(View.VISIBLE);

        }
        else {
            mProgressView.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                                                                      android.R.anim.fade_out));
            mProgressView.setVisibility(View.GONE);
        }
    }

    private void showEmptyText(boolean show) {
        if ((mTextViewEmpty.getVisibility() == View.VISIBLE) == show) {
            return;
        }

        if (show) {
            mTextViewEmpty.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                                                                       android.R.anim.fade_in));
            mTextViewEmpty.setVisibility(View.VISIBLE);

        }
        else {
            mTextViewEmpty.startAnimation(AnimationUtils.loadAnimation(getActivity(),
                                                                       android.R.anim.fade_out));
            mTextViewEmpty.setVisibility(View.GONE);
        }
    }
}

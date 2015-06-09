package com.makina.collect.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.makina.collect.android.R;
import com.makina.collect.android.model.FormDetails;
import com.makina.collect.android.widget.CheckBoxView;

/**
 * Default {@code Adapter} about {@link FormDetails}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class FormDetailsListAdapter
        extends AbstractListAdapter<FormDetails, FormDetailsListAdapter.ViewHolder> {

    private final OnFormDetailsItemListener mOnFormDetailsItemListener;

    public FormDetailsListAdapter(OnFormDetailsItemListener pOnFormDetailsItemListener) {
        this.mOnFormDetailsItemListener = pOnFormDetailsItemListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        // create a new ViewHolder
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.list_item_form_details,
                                                     parent,
                                                     false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder,
                                 int position) {
        holder.bind(getItem(position));
    }

    /**
     * Default {@code ViewHolder} used by {@link FormDetailsListAdapter}.
     *
     * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
     */
    public class ViewHolder
            extends RecyclerView.ViewHolder {

        private final CheckBoxView mCheckBoxView;
        private final TextView mTextViewFormName;

        public ViewHolder(View itemView) {
            super(itemView);

            mCheckBoxView = (CheckBoxView) itemView.findViewById(R.id.selectorView);
            mTextViewFormName = (TextView) itemView.findViewById(android.R.id.text1);

            itemView.findViewById(android.R.id.text2).setVisibility(View.GONE);
        }

        public void bind(final FormDetails formDetails) {
            mCheckBoxView.setChecked(formDetails.checked);
            mTextViewFormName.setText(formDetails.formName);

            if (mOnFormDetailsItemListener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        formDetails.checked = !formDetails.checked;

                        mCheckBoxView.setChecked(formDetails.checked);
                        mOnFormDetailsItemListener.onFormDetailsSelected(formDetails);
                    }
                });
            }
        }
    }

    /**
     * Interface definition for a callback to be invoked when an item in the {@code RecyclerView}
     * has been clicked.
     *
     * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
     */
    public interface OnFormDetailsItemListener {

        void onFormDetailsSelected(FormDetails formDetails);
    }
}

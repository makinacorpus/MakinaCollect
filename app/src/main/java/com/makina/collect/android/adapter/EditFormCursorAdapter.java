package com.makina.collect.android.adapter;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.makina.collect.android.R;
import com.makina.collect.android.model.FormDetails;
import com.makina.collect.android.provider.FormsProvider;
import com.makina.collect.android.widget.CheckBoxView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default {@code Adapter} about form details from {@code Cursor}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class EditFormCursorAdapter
        extends AbstractCursorAdapter<EditFormCursorAdapter.ViewHolder> {

    private final EditFormCursorAdapter.OnEditFormItemListener mOnEditFormItemListener;
    private final Set<FormDetails> mSelectedFormDetailsList = new HashSet<>();

    public EditFormCursorAdapter(Cursor cursor,
                                 EditFormCursorAdapter.OnEditFormItemListener pOnEditFormItemListener) {
        super(cursor);

        this.mOnEditFormItemListener = pOnEditFormItemListener;
    }

    public void clearSelection() {
        mSelectedFormDetailsList.clear();

        notifyDataSetChanged();
    }

    @NonNull
    public List<FormDetails> getSelection() {
        return new ArrayList<>(mSelectedFormDetailsList);
    }

    public void updateSelection(final List<FormDetails> selectedFormDetailsList) {

        if (selectedFormDetailsList == null) {
            return;
        }

        mSelectedFormDetailsList.clear();
        mSelectedFormDetailsList.addAll(selectedFormDetailsList);

        notifyDataSetChanged();
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
                                 Cursor cursor) {
        holder.bind(cursor);
    }

    /**
     * Default {@code ViewHolder} used by {@link EditFormCursorAdapter}.
     *
     * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
     */
    public class ViewHolder
            extends RecyclerView.ViewHolder {

        private final CheckBoxView mCheckBoxView;
        private final TextView mTextViewFormName;
        private final TextView mTextViewFormDescription;

        public ViewHolder(View itemView) {
            super(itemView);

            mCheckBoxView = (CheckBoxView) itemView.findViewById(R.id.selectorView);
            mTextViewFormName = (TextView) itemView.findViewById(android.R.id.text1);
            mTextViewFormDescription = (TextView) itemView.findViewById(android.R.id.text2);
        }

        public void bind(final Cursor cursor) {
            final FormDetails formDetails = FormsProvider.fromCursor(cursor);

            if (formDetails == null) {
                return;
            }

            mCheckBoxView.setChecked(mSelectedFormDetailsList.contains(formDetails));
            mTextViewFormName.setText(formDetails.formName);
            mTextViewFormDescription.setText(formDetails.description);

            if (mOnEditFormItemListener != null) {
                mCheckBoxView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mCheckBoxView.setChecked(!mCheckBoxView.isChecked());

                        if (mCheckBoxView.isChecked()) {
                            mSelectedFormDetailsList.add(formDetails);
                        }
                        else {
                            mSelectedFormDetailsList.remove(formDetails);
                        }

                        mOnEditFormItemListener.onFormDetailsSelected(formDetails);
                    }
                });
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnEditFormItemListener.onFormDetailsEdited(formDetails);
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
    public interface OnEditFormItemListener {

        void onFormDetailsSelected(FormDetails formDetails);

        void onFormDetailsEdited(FormDetails formDetails);
    }
}

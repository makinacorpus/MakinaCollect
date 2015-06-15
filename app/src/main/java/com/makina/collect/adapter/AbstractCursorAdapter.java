package com.makina.collect.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Base {@code RecyclerView.Adapter} that exposes data from a {@code Cursor}.
 * <p/>
 * The {@code Cursor} must include a column named {@code "_id"} or this class will not work.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public abstract class AbstractCursorAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    private boolean mDataValid;
    private Cursor mCursor;
    private int mRowIDColumn;
    private DataSetObserver mDataSetObserver;

    public AbstractCursorAdapter(Cursor cursor) {
        boolean cursorPresent = cursor != null;

        mCursor = cursor;
        mDataValid = cursorPresent;
        mRowIDColumn = cursorPresent ? cursor.getColumnIndexOrThrow("_id") : -1;
        mDataSetObserver = new NotifyDataSetObserver();

        setHasStableIds(true);

        if (cursorPresent) {
            cursor.registerDataSetObserver(mDataSetObserver);
        }
    }

    @Override
    public int getItemCount() {
        if (mDataValid && mCursor != null) {
            return mCursor.getCount();
        }
        else {
            return 0;
        }
    }

    @Override
    public long getItemId(int position) {
        if (mDataValid && mCursor != null) {
            if (mCursor.moveToPosition(position)) {
                return mCursor.getLong(mRowIDColumn);
            }
            else {
                return RecyclerView.NO_ID;
            }
        }
        else {
            return RecyclerView.NO_ID;
        }
    }

    @Override
    public void onBindViewHolder(VH holder,
                                 int position) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }

        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        onBindViewHolder(holder,
                         mCursor);
    }

    /**
     * Called by {@code RecyclerView} to display the data pointed to by cursor. This method
     * should update the contents of the {@link android.support.v7.widget.RecyclerView.ViewHolder#itemView}
     * to reflect the data at the given position.
     *
     * @param holder The {@code ViewHolder} which should be updated to represent the contents of the
     *               item at the given position in the data set.
     * @param cursor The {@code Cursor} from which to get the data. The {@code Cursor} is already
     *               moved to the correct position.
     *
     * @see android.widget.CursorAdapter#bindView(View, Context, Cursor)
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    public abstract void onBindViewHolder(VH holder,
                                          Cursor cursor);

    /**
     * Change the underlying {@code Cursor} to a new {@code Cursor}.
     * If there is an existing {@code Cursor} it will be closed.
     *
     * @param cursor The new {@code Cursor} to be used
     */
    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);

        if (old != null) {
            old.close();
        }
    }

    /**
     * Swap in a new {@code Cursor}, returning the old {@code Cursor}.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old {@code Cursor} is <em>not</em>
     * closed.
     *
     * @param newCursor The new {@code Cursor} to be used.
     *
     * @return Returns the previously set {@code Cursor}, or {@code null} if there was a not one.
     * If the given new {@code Cursor} is the same instance is the previously set
     * {@code Cursor}, {@code null} is also returned.
     */
    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }

        Cursor oldCursor = mCursor;
        int itemCount = getItemCount();

        if (oldCursor != null) {
            if (mDataSetObserver != null) {
                oldCursor.unregisterDataSetObserver(mDataSetObserver);
            }
        }

        mCursor = newCursor;

        if (newCursor != null) {

            if (mDataSetObserver != null) {
                newCursor.registerDataSetObserver(mDataSetObserver);
            }

            mRowIDColumn = newCursor.getColumnIndexOrThrow("_id");
            mDataValid = true;

            // notify the observers about the new cursor
            notifyDataSetChanged();
        }
        else {
            mRowIDColumn = -1;
            mDataValid = false;

            // notify the observers about the lack of a data set
            notifyItemRangeRemoved(0,
                                   itemCount);
        }

        return oldCursor;
    }

    private class NotifyDataSetObserver
            extends DataSetObserver {

        @Override
        public void onChanged() {
            super.onChanged();

            mDataValid = true;

            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();

            int itemCount = getItemCount();

            mDataValid = false;

            notifyItemRangeRemoved(0,
                                   itemCount);
        }
    }
}

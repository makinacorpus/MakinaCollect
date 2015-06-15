package com.makina.collect.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base {@code RecyclerView.Adapter} that is backed by a {@code List} of arbitrary objects.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public abstract class AbstractListAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements Filterable {

    /**
     * Contains the {@code List} of objects that represent the data of this {@code RecyclerView.Adapter}.
     */
    private final List<T> mObjects = new ArrayList<>();

    /**
     * A lock used to modify the content of {@link #mObjects}.
     * Any write operation performed on the array should be synchronized on this lock.
     * This lock is also used by the filter (see {@link #getFilter()} to make a synchronized copy of
     * the original array of data.
     */
    private final Object mLock = new Object();

    /**
     * A copy of the original {@link #mObjects} array, initialized from and then used instead as
     * soon as the {@link #mFilter} {@code Filter} is used.
     * {@link #mObjects} will then only contain the filtered values.
     */
    private ArrayList<T> mOriginalValues;
    private Filter mFilter;

    @Override
    public int getItemCount() {
        return mObjects.size();
    }

    /**
     * Adds the specified object at the end of the array.
     *
     * @param object the object to add at the end of the array
     */
    public void add(T object) {
        if (object == null) {
            return;
        }

        synchronized (mLock) {
            if (mOriginalValues != null) {
                if (mOriginalValues.add(object)) {
                    notifyItemInserted(mOriginalValues.size() - 1);
                }
                else {
                    notifyDataSetChanged();
                }
            }
            else {
                if (mObjects.add(object)) {
                    notifyItemInserted(mObjects.size() - 1);
                }
                else {
                    notifyDataSetChanged();
                }
            }
        }
    }

    /**
     * Adds the specified {@code Collection} at the end of the array.
     *
     * @param collection the {@code Collection} to add at the end of the array.
     */
    public void addAll(Collection<? extends T> collection) {
        if (collection == null) {
            return;
        }

        synchronized (mLock) {
            if (mOriginalValues != null) {
                int itemCount = mOriginalValues.size();

                if (mOriginalValues.addAll(collection)) {
                    notifyItemRangeInserted(itemCount,
                                            collection.size());
                }
                else {
                    notifyDataSetChanged();
                }
            }
            else {
                int itemCount = mObjects.size();

                if (mObjects.addAll(collection)) {
                    notifyItemRangeInserted(itemCount,
                                            collection.size());
                }
                else {
                    notifyDataSetChanged();
                }
            }
        }
    }

    /**
     * Inserts the specified object at the specified index in the array.
     *
     * @param object the object to insert into the array
     * @param index  the index at which the object must be inserted
     */
    public void insert(T object,
                       int index) {
        if (object == null) {
            return;
        }

        if ((index < 0) || ((mOriginalValues != null) && (index > mOriginalValues.size())) || (index > mObjects.size())) {
            add(object);
        }
        else {
            synchronized (mLock) {
                if (mOriginalValues != null) {
                    mOriginalValues.add(index,
                                        object);
                }
                else {
                    mObjects.add(index,
                                 object);
                }

                notifyItemInserted(index);
            }
        }
    }

    /**
     * Replaces the current object at the specified location in the array with the specified object.
     *
     * @param object the object to insert
     */
    public void update(T object,
                       int index) {
        if (object == null) {
            return;
        }

        if ((index < 0) || ((mOriginalValues != null) && (index > mOriginalValues.size())) || (index > mObjects.size())) {
            return;
        }

        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.set(index,
                                    object);
            }
            else {
                mObjects.set(index,
                             object);
            }

            notifyItemChanged(index);
        }
    }

    /**
     * Removes the specified object from the array.
     *
     * @param object the object to remove
     */
    public void remove(T object) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                int index = mOriginalValues.indexOf(object);

                if (mOriginalValues.remove(object)) {
                    notifyItemRemoved(index);

                    if (mOriginalValues.size() > 0) {
                        notifyItemRangeChanged(index,
                                               mOriginalValues.size() - index);
                    }
                }
            }
            else {
                int index = mObjects.indexOf(object);

                if (mObjects.remove(object)) {
                    notifyItemRemoved(index);

                    if (mObjects.size() > 0) {
                        notifyItemRangeChanged(index,
                                               mObjects.size() - index);
                    }
                }
            }
        }
    }

    /**
     * Removes the specified object at the specified index in the array.
     *
     * @param index the index of the object to remove
     */
    public void remove(int index) {
        if ((index < 0) || ((mOriginalValues != null) && (index > mOriginalValues.size())) || (index > mObjects.size())) {
            return;
        }

        synchronized (mLock) {
            if (mOriginalValues != null) {
                if (mOriginalValues.remove(index) != null) {
                    notifyItemRemoved(index);

                    if (mOriginalValues.size() > 0) {
                        notifyItemRangeChanged(index,
                                               mOriginalValues.size() - index);
                    }
                }
            }
            else {
                if (mObjects.remove(index) != null) {
                    notifyItemRemoved(index);

                    if (mObjects.size() > 0) {
                        notifyItemRangeChanged(index,
                                               mObjects.size() - index);
                    }
                }
            }
        }
    }

    /**
     * Remove all elements from the {@code List}.
     */
    public void clear() {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                int itemCount = mOriginalValues.size();
                mOriginalValues.clear();

                if (itemCount > 0) {
                    notifyItemRangeRemoved(0,
                                           itemCount);
                }
            }
            else {
                int itemCount = mObjects.size();
                mObjects.clear();

                if (itemCount > 0) {
                    notifyItemRangeRemoved(0,
                                           itemCount);
                }
            }
        }
    }

    /**
     * Get the object item associated with the specified position in the data set.
     *
     * @param index index of the object to retrieve
     *
     * @return the object at the specified position
     */
    @Nullable
    public T getItem(int index) {
        if ((index < 0) || (index > mObjects.size() - 1)) {
            return null;
        }

        return mObjects.get(index);
    }

    /**
     * Gets the current position of the given object.
     *
     * @param object the object on which to retrieve its position
     *
     * @return the object position or {@code -1} if not found
     */
    public int getItemPosition(T object) {
        return mObjects.indexOf(object);
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }

        return mFilter;
    }

    /**
     * An array {@code Filter} constrains the content of the {@code RecyclerView.Adapter} with a prefix.
     * Each item that does not start with the supplied prefix is removed from the {@code List}.
     */
    private class ArrayFilter
            extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = new ArrayList<>(mObjects);
                }
            }

            if (prefix == null || prefix.length() == 0) {
                ArrayList<T> list;

                synchronized (mLock) {
                    list = new ArrayList<>(mOriginalValues);
                }

                results.values = list;
                results.count = list.size();
            }
            else {
                String prefixString = prefix.toString()
                                            .toLowerCase();

                ArrayList<T> values;

                synchronized (mLock) {
                    values = new ArrayList<>(mOriginalValues);
                }

                final int count = values.size();
                final ArrayList<T> newValues = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    final T value = values.get(i);
                    final String valueText = value.toString()
                                                  .toLowerCase();

                    // first match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(value);
                    }
                    else {
                        final String[] words = valueText.split(" ");

                        // start at index 0, in case valueText starts with space(s)
                        for (String word : words) {
                            if (word.startsWith(prefixString)) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            int itemCount = mObjects.size();

            mObjects.clear();

            if (itemCount > 0) {
                notifyItemRangeRemoved(0,
                                       itemCount);
            }

            // noinspection unchecked
            mObjects.addAll((List<T>) results.values);

            if (mObjects.size() > 0) {
                notifyItemRangeInserted(0,
                                        mObjects.size());
            }
            else {
                notifyDataSetChanged();
            }
        }
    }
}

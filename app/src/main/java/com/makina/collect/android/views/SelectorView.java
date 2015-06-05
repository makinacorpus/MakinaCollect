package com.makina.collect.android.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.makina.collect.android.R;

/**
 * Custom {@code View} acting like a checkbox with animation effects.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class SelectorView
        extends FrameLayout
        implements Checkable {

    private boolean mChecked;

    private ImageView mBackgroundImageView;
    private ImageView mCheckBoxImageView;

    private Drawable mBackgroundIndicatorOff;
    private Drawable mBackgroundIndicatorOn;
    private Drawable mCheckBoxIndicator;

    public SelectorView(Context context) {
        this(context,
             null);
    }

    public SelectorView(Context context,
                        AttributeSet attrs) {
        this(context,
             attrs,
             R.attr.selectorViewStyle);
    }

    public SelectorView(Context context,
                        AttributeSet attrs,
                        int defStyleAttr) {
        super(context,
              attrs,
              defStyleAttr);

        init(attrs,
             defStyleAttr);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState);

        ss.checked = isChecked();

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        final SavedState ss = (SavedState) state;

        super.onRestoreInstanceState(ss.getSuperState());

        setChecked(ss.checked);
        requestLayout();
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;

            updateView();
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    private void init(AttributeSet attrs,
                      int defStyle) {
        inflate(getContext(),
                R.layout.view_selector,
                this);

        mBackgroundImageView = (ImageView) findViewById(android.R.id.background);
        mCheckBoxImageView = (ImageView) findViewById(android.R.id.checkbox);

        // load attributes
        final TypedArray typedArray = getContext().obtainStyledAttributes(attrs,
                                                                          R.styleable.SelectorView,
                                                                          defStyle,
                                                                          0);

        mBackgroundIndicatorOff = typedArray.getDrawable(R.styleable.SelectorView_backgroundIndicatorOff);
        mBackgroundIndicatorOn = typedArray.getDrawable(R.styleable.SelectorView_backgroundIndicatorOn);
        mCheckBoxIndicator = typedArray.getDrawable(R.styleable.SelectorView_checkBoxIndicator);

        mCheckBoxImageView.setImageDrawable(mCheckBoxIndicator);

        typedArray.recycle();

        updateView();
    }

    private void updateView() {
        mBackgroundImageView.setImageDrawable(isChecked() ? mBackgroundIndicatorOn : mBackgroundIndicatorOff);
        mCheckBoxImageView.setVisibility(isChecked() ? VISIBLE : INVISIBLE);
    }

    static class SavedState extends BaseSavedState {
        boolean checked;

        /**
         * Constructor called from {@link SelectorView#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);

            checked = (Boolean) in.readValue(null);
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);

            out.writeValue(checked);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
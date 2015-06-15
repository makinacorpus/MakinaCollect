package com.makina.collect.widget;

import android.animation.Animator;
import android.annotation.SuppressLint;
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

import com.makina.collect.R;
import com.makina.collect.util.DeviceUtils;

/**
 * Custom {@code View} acting like a checkbox with animation effects.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class CheckBoxView
        extends FrameLayout
        implements Checkable {

    private boolean mChecked;

    private ImageView mBackgroundImageView;
    private ImageView mCheckBoxImageView;

    private Drawable mBackgroundIndicatorOff;
    private Drawable mBackgroundIndicatorOn;
    private Drawable mCheckBoxIndicator;

    public CheckBoxView(Context context) {
        this(context,
             null);
    }

    public CheckBoxView(Context context,
                        AttributeSet attrs) {
        this(context,
             attrs,
             R.attr.checkBoxViewStyle);
    }

    public CheckBoxView(Context context,
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
                R.layout.view_checkbox,
                this);

        mBackgroundImageView = (ImageView) findViewById(android.R.id.background);
        mCheckBoxImageView = (ImageView) findViewById(android.R.id.checkbox);

        // load attributes
        final TypedArray typedArray = getContext().obtainStyledAttributes(attrs,
                                                                          R.styleable.CheckBoxView,
                                                                          defStyle,
                                                                          0);

        mBackgroundIndicatorOff = typedArray.getDrawable(R.styleable.CheckBoxView_backgroundIndicatorOff);
        mBackgroundIndicatorOn = typedArray.getDrawable(R.styleable.CheckBoxView_backgroundIndicatorOn);
        mCheckBoxIndicator = typedArray.getDrawable(R.styleable.CheckBoxView_checkBoxIndicator);

        mCheckBoxImageView.setImageDrawable(mCheckBoxIndicator);

        typedArray.recycle();

        mBackgroundImageView.setImageDrawable(isChecked() ? mBackgroundIndicatorOn : mBackgroundIndicatorOff);
        mCheckBoxImageView.setVisibility(isChecked() ? VISIBLE : INVISIBLE);
    }

    @SuppressLint("NewApi")
    private void updateView() {
        if (DeviceUtils.isPostHoneycombMR1()) {
            if (isChecked()) {
                mCheckBoxImageView.setScaleX(0f);
                mCheckBoxImageView.setScaleY(0f);
                mCheckBoxImageView.setVisibility(VISIBLE);
                mCheckBoxImageView.animate()
                                  .scaleX(1f)
                                  .scaleY(1f)
                                  .setDuration(200)
                                  .setStartDelay(400);
            }
            else {
                mCheckBoxImageView.setVisibility(INVISIBLE);
            }

            mBackgroundImageView.animate()
                                .scaleX(0f)
                                .setDuration(200)
                                .setListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                        mBackgroundImageView.setImageDrawable(isChecked() ? mBackgroundIndicatorOff : mBackgroundIndicatorOn);
                                        mBackgroundImageView.setScaleX(1f);
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mBackgroundImageView.animate()
                                                            .scaleX(1f)
                                                            .setDuration(200)
                                                            .setListener(new Animator.AnimatorListener() {
                                                                @Override
                                                                public void onAnimationStart(Animator animation) {
                                                                    mBackgroundImageView.setImageDrawable(isChecked() ? mBackgroundIndicatorOn : mBackgroundIndicatorOff);
                                                                    mBackgroundImageView.setScaleX(0f);
                                                                }

                                                                @Override
                                                                public void onAnimationEnd(Animator animation) {

                                                                }

                                                                @Override
                                                                public void onAnimationCancel(Animator animation) {

                                                                }

                                                                @Override
                                                                public void onAnimationRepeat(Animator animation) {

                                                                }
                                                            });
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {

                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {

                                    }
                                });
        }
        else {
            // no animation for old devices. Sorry !
            mBackgroundImageView.setImageDrawable(isChecked() ? mBackgroundIndicatorOn : mBackgroundIndicatorOff);
            mCheckBoxImageView.setVisibility(isChecked() ? VISIBLE : INVISIBLE);
        }
    }

    static class SavedState
            extends BaseSavedState {
        boolean checked;

        /**
         * Constructor called from {@link CheckBoxView#onSaveInstanceState()}
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
        public void writeToParcel(@NonNull Parcel out,
                                  int flags) {
            super.writeToParcel(out,
                                flags);

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

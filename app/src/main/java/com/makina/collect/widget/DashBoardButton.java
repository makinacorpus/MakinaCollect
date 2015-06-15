package com.makina.collect.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.makina.collect.R;

/**
 * Custom {@code View} as {@code Button} for the dashboard.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DashBoardButton
        extends LinearLayout {

    private ImageView mIconImageView;
    private TextView mTextView1;
    private TextView mTextView2;

    public DashBoardButton(Context context) {
        this(context,
             null);
    }

    public DashBoardButton(Context context,
                           AttributeSet attrs) {
        super(context,
              attrs);

        init(attrs);
    }

    public void setIcon(final Drawable iconDrawable) {
        mIconImageView.setImageDrawable(iconDrawable);
        mIconImageView.setVisibility((iconDrawable == null) ? GONE : VISIBLE);
    }

    public void setText1(final CharSequence text1) {
        mTextView1.setText(text1);
    }

    public void setText2(final CharSequence text2) {
        mTextView2.setText(text2);
    }

    private void init(AttributeSet attrs) {
        inflate(getContext(),
                R.layout.view_dashboard_button,
                this);

        mIconImageView = (ImageView) findViewById(android.R.id.icon);
        mTextView1 = (TextView) findViewById(android.R.id.text1);
        mTextView2 = (TextView) findViewById(android.R.id.text2);

        // load attributes
        final TypedArray typedArray = getContext().obtainStyledAttributes(attrs,
                                                                          R.styleable.DashBoardButton);

        final Drawable iconDrawable = typedArray.getDrawable(R.styleable.DashBoardButton_icon);
        setIcon(iconDrawable);

        int text1ResourceId = typedArray.getResourceId(R.styleable.DashBoardButton_text1,
                                                       0);

        if (text1ResourceId == 0) {
            mTextView1.setText(typedArray.getString(R.styleable.DashBoardButton_text1));
        }
        else {
            mTextView1.setText(text1ResourceId);
        }

        initTextStyleAttributes(mTextView1,
                                typedArray.getResourceId(R.styleable.DashBoardButton_text1Appearance,
                                                         0));

        int text2ResourceId = typedArray.getResourceId(R.styleable.DashBoardButton_text2,
                                                       0);

        if (text2ResourceId == 0) {
            mTextView2.setText(typedArray.getString(R.styleable.DashBoardButton_text2));
        }
        else {
            mTextView2.setText(text2ResourceId);
        }

        initTextStyleAttributes(mTextView2,
                                typedArray.getResourceId(R.styleable.DashBoardButton_text2Appearance,
                                                         0));

        typedArray.recycle();
    }

    private void initTextStyleAttributes(TextView textView, int defStyle) {
        if (defStyle != 0) {
            textView.setTextAppearance(getContext(),
                                       defStyle);
        }
    }
}

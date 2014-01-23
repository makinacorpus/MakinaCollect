package com.makina.collect.android.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.makina.collect.android.logic.HierarchyElement;


public class HierarchyElementView extends RelativeLayout {

    private CustomFontTextview mPrimaryTextView;
    private CustomFontTextview mSecondaryTextView;
    private ImageView mIcon;


    public HierarchyElementView(Context context, HierarchyElement it) {
        super(context);

        setColor(it.getColor());

        mIcon = new ImageView(context);
        mIcon.setImageDrawable(it.getIcon());
        mIcon.setId(1);
        mIcon.setPadding(0, 0, dipToPx(4), 0);

        addView(mIcon, new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        mPrimaryTextView = new CustomFontTextview(context);
        mPrimaryTextView.setTextAppearance(context, android.R.style.TextAppearance_Large);
        mPrimaryTextView.setText(it.getPrimaryText());
        mPrimaryTextView.setId(2);
        mPrimaryTextView.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams l =
            new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        l.addRule(RelativeLayout.RIGHT_OF, mIcon.getId());
        addView(mPrimaryTextView, l);

        mSecondaryTextView = new CustomFontTextview(context);
        mSecondaryTextView.setText(it.getSecondaryText());
        mSecondaryTextView.setTextAppearance(context, android.R.style.TextAppearance_Small);
        mSecondaryTextView.setGravity(Gravity.CENTER_VERTICAL);

        LayoutParams lp =
            new RelativeLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.BELOW, mPrimaryTextView.getId());
        lp.addRule(RelativeLayout.RIGHT_OF, mIcon.getId());
        addView(mSecondaryTextView, lp);

        setPadding(dipToPx(8), dipToPx(4), dipToPx(8), dipToPx(8));

    }


    public void setPrimaryText(String text) {
        mPrimaryTextView.setText(text);
    }


    public void setSecondaryText(String text) {
        mSecondaryTextView.setText(text);
    }


    public void setIcon(Drawable icon) {
        mIcon.setImageDrawable(icon);
    }


    public void setColor(int color) {
        setBackgroundColor(color);
    }


    public void showSecondary(boolean bool) {
        if (bool) {
            mSecondaryTextView.setVisibility(VISIBLE);
            setMinimumHeight(dipToPx(64));

        } else {
            mSecondaryTextView.setVisibility(GONE);
            setMinimumHeight(dipToPx(32));

        }
    }
    
    public int dipToPx(int dip) {
        return (int) (dip * getResources().getDisplayMetrics().density + 0.5f);
    }

}

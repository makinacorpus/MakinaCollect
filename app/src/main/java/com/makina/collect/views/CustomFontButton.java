package com.makina.collect.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;
public class CustomFontButton extends Button {

    public CustomFontButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CustomFontButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomFontButton(Context context) {
        super(context);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/avenir.ttc");
            setTypeface(tf);
        }
    }

}


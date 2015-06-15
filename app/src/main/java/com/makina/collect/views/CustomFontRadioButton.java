package com.makina.collect.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.RadioButton;

public class CustomFontRadioButton extends RadioButton {

    public CustomFontRadioButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CustomFontRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomFontRadioButton(Context context) {
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


package com.makina.collect.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;
public class CustomFontSpinner extends Spinner {

    public CustomFontSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CustomFontSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomFontSpinner(Context context) {
        super(context);
        init();
    }

    private void init() {
        //if (!isInEditMode()) {
            //Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/avenir.ttc");
            //setTypeface(tf);
        //}
    }

}


package com.makina.collect.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

@Deprecated
public class CustomFontTextview extends TextView {

    public CustomFontTextview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CustomFontTextview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomFontTextview(Context context) {
        super(context);
        init();
    }

    private void init() {
        if (!isInEditMode())
        {
            Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/avenir.ttc");
            setTypeface(tf);
        }
    }

}


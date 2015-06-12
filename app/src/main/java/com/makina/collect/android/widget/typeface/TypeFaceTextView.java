package com.makina.collect.android.widget.typeface;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.makina.collect.android.R;

/**
 * Custom {@code TextView} that supports {@code fontFamilyName} attribute from XML.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class TypefaceTextView
        extends TextView {

    public TypefaceTextView(Context context) {
        this(context,
             null);
    }

    public TypefaceTextView(Context context,
                            AttributeSet attrs) {
        this(context,
             attrs,
             R.attr.typefaceViewStyle);
    }

    public TypefaceTextView(Context context,
                            AttributeSet attrs,
                            int defStyle) {
        super(context,
              attrs,
              defStyle);

        init(attrs,
             defStyle);
    }

    public void setTextStyle(ITypefaceFamily textStyle) {
        TypefaceLoaderManager.getInstance()
                             .applyTypeface(this,
                                            textStyle);
    }

    private void init(AttributeSet attrs,
                      int defStyle) {
        final TypedArray typedArray = getContext().getTheme()
                                                  .obtainStyledAttributes(attrs,
                                                                          R.styleable.TypefaceView,
                                                                          defStyle,
                                                                          0);

        if (typedArray.hasValue(R.styleable.TypefaceView_fontFamilyName)) {
            TypefaceLoaderManager.getInstance()
                                 .applyTypeface(this,
                                                typedArray.getString(R.styleable.TypefaceView_fontFamilyName));
        }

        if (typedArray.getBoolean(R.styleable.TypefaceView_textCapitalize,
                                  false)) {
            setTransformationMethod(new UpperCaseTransformationMethod(getContext()));
        }

        typedArray.recycle();
    }
}

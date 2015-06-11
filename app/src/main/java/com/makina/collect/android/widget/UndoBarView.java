package com.makina.collect.android.widget;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.makina.collect.android.R;

/**
 * Custom undo bar {@code View}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class UndoBarView
        extends LinearLayout {

    private TextView mMessage;
    private Button mButton;

    public UndoBarView(Context context) {
        this(context,
             null);
    }

    public UndoBarView(Context context,
                       AttributeSet attrs) {
        super(context,
              attrs);
    }

    public static UndoBarView inflate(Activity activity) {
        ViewGroup decorView = (ViewGroup) activity.getWindow()
                                                  .getDecorView();

        // if we're operating within an Activity, limit ourselves to the content view.
        ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);

        if (rootView == null) {
            rootView = decorView;
        }

        // if it's the first UndoBarView in this window, inflate a new instance
        UndoBarView undoBarView = (UndoBarView) rootView.findViewById(R.id.undoBarView);

        if (undoBarView == null) {
            undoBarView = (UndoBarView) LayoutInflater.from(activity)
                                                      .inflate(R.layout.view_undo_bar,
                                                               rootView,
                                                               false);
            rootView.addView(undoBarView);
        }

        return undoBarView;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMessage = (TextView) findViewById(android.R.id.message);
        mButton = (Button) findViewById(android.R.id.button1);

        setVisibility(GONE);
    }

    public void setMessage(CharSequence message) {
        mMessage.setText(message);
    }

    public void setOnUndoClickListener(OnClickListener onClickListener) {
        mButton.setOnClickListener(onClickListener);
    }

    /**
     * Shows the {@link UndoBarView}.
     */
    public void show() {
        setVisibility(VISIBLE);
    }

    /**
     * Hides the {@link UndoBarView}.
     */
    public void hide() {
        setVisibility(GONE);
    }
}

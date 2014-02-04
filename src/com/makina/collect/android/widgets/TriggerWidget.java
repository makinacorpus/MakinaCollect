package com.makina.collect.android.widgets;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.makina.collect.android.R;
import com.makina.collect.android.listeners.WidgetAnsweredListener;
import com.makina.collect.android.views.CustomFontCheckBox;
import com.makina.collect.android.views.CustomFontTextview;

/**
 * Widget that allows user to scan barcodes and add them to the form.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class TriggerWidget extends QuestionWidget {

    private CustomFontCheckBox mTriggerButton;
    private CustomFontTextview mStringAnswer;
    private static final String mOK = "OK";

    private FormEntryPrompt mPrompt;


    @Override
	public FormEntryPrompt getPrompt() {
        return mPrompt;
    }


    public TriggerWidget(Context context, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
        super(context, widgetAnsweredListener, prompt);
        mPrompt = prompt;

        this.setOrientation(LinearLayout.VERTICAL);

        mTriggerButton = new CustomFontCheckBox(getContext());
        mTriggerButton.setId(QuestionWidget.newUniqueId());
        mTriggerButton.setText(getContext().getString(R.string.trigger));
        mTriggerButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        // mActionButton.setPadding(20, 20, 20, 20);
        mTriggerButton.setEnabled(!prompt.isReadOnly());

        mTriggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTriggerButton.isChecked()) {
                    mStringAnswer.setText(mOK);
                } else {
                    mStringAnswer.setText(null);
                }
            }
        });

        mStringAnswer = new CustomFontTextview(getContext());
        mStringAnswer.setId(QuestionWidget.newUniqueId());
        mStringAnswer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        mStringAnswer.setGravity(Gravity.CENTER);

        String s = prompt.getAnswerText();
        if (s != null) {
            if (s.equals(mOK)) {
                mTriggerButton.setChecked(true);
            } else {
                mTriggerButton.setChecked(false);
            }
            mStringAnswer.setText(s);

        }

        // finish complex layout
        this.addView(mTriggerButton);
        // this.addView(mStringAnswer);
    }


    @Override
    public void clearAnswer() {
        mStringAnswer.setText(null);
        mTriggerButton.setChecked(false);
    }


    @Override
    public IAnswerData getAnswer() {
        String s = mStringAnswer.getText().toString();
        if (s == null || s.equals("")) {
            return null;
        } else {
            return new StringData(s);
        }
    }


    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager =
            (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }


    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mTriggerButton.setOnLongClickListener(l);
        mStringAnswer.setOnLongClickListener(l);
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mTriggerButton.cancelLongPress();
        mStringAnswer.cancelLongPress();
    }

}

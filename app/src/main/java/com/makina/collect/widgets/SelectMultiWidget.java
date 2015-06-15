package com.makina.collect.widgets;

import android.content.Context;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.makina.collect.listeners.WidgetAnsweredListener;
import com.makina.collect.views.CustomFontCheckBox;
import com.makina.collect.views.MediaLayout;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.ArrayList;
import java.util.Vector;

/**
 * SelctMultiWidget handles multiple selection fields using checkboxes.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SelectMultiWidget extends QuestionWidget {
    private boolean mCheckboxInit = true;
    Vector<SelectChoice> mItems;

    private ArrayList<CustomFontCheckBox> mCheckboxes;
    private WidgetAnsweredListener mAnsListener;

    @SuppressWarnings("unchecked")
    public SelectMultiWidget(Context context, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
        super(context, widgetAnsweredListener, prompt);
        mPrompt = prompt;
        mCheckboxes = new ArrayList<CustomFontCheckBox>();
        mItems = prompt.getSelectChoices();
        mAnsListener = widgetAnsweredListener;
        setOrientation(LinearLayout.VERTICAL);

        Vector<Selection> ve = new Vector<Selection>();
        if (prompt.getAnswerValue() != null) {
            ve = (Vector<Selection>) prompt.getAnswerValue().getValue();
        }

        if (mItems != null) {
            for (int i = 0; i < mItems.size(); i++) {
                // no checkbox group so id by answer + offset
                CustomFontCheckBox c = new CustomFontCheckBox(getContext());
                c.setTag(Integer.valueOf(i));
                c.setId(QuestionWidget.newUniqueId());
                c.setText(prompt.getSelectChoiceText(mItems.get(i)));
                c.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
                c.setFocusable(!prompt.isReadOnly());
                c.setEnabled(!prompt.isReadOnly());
                
                for (int vi = 0; vi < ve.size(); vi++) {
                    // match based on value, not key
                    if (mItems.get(i).getValue().equals(ve.elementAt(vi).getValue())) {
                        c.setChecked(true);
                        break;
                    }

                }
                mCheckboxes.add(c);
                // when clicked, check for readonly before toggling
                c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!mCheckboxInit && mPrompt.isReadOnly()) {
                            if (buttonView.isChecked()) {
                                buttonView.setChecked(false);
                            } else {
                                buttonView.setChecked(true);
                            }
                            mAnsListener.setAnswerChange(true);
                    		mAnsListener.updateView();
                    		mAnsListener.setAnswerChange(false);
                        }
                    }
                });

                String audioURI = null;
                audioURI =
                    prompt.getSpecialFormSelectChoiceText(mItems.get(i),
                        FormEntryCaption.TEXT_FORM_AUDIO);

                String imageURI = null;
                imageURI =
                    prompt.getSpecialFormSelectChoiceText(mItems.get(i),
                        FormEntryCaption.TEXT_FORM_IMAGE);

                String videoURI = null;
                videoURI = prompt.getSpecialFormSelectChoiceText(mItems.get(i), "video");

                String bigImageURI = null;
                bigImageURI = prompt.getSpecialFormSelectChoiceText(mItems.get(i), "big-image");

                MediaLayout mediaLayout = new MediaLayout(getContext());
                mediaLayout.setAVT(prompt.getIndex(), "" + Integer.toString(i), c, audioURI, imageURI, videoURI, bigImageURI);
                addView(mediaLayout);

                // Last, add the dividing line between elements (except for the last element)
                ImageView divider = new ImageView(getContext());
                divider.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
                if (i != mItems.size() - 1) {
                    addView(divider);
                }

            }
        }

        mCheckboxInit = false;

    }


    @Override
    public void clearAnswer() {
    	for ( CustomFontCheckBox c : mCheckboxes ) {
    		if ( c.isChecked() ) {
    			c.setChecked(false);
    		}
    	}
    }


    @Override
    public IAnswerData getAnswer() {
        Vector<Selection> vc = new Vector<Selection>();
        for ( int i = 0; i < mCheckboxes.size() ; ++i ) {
        	CustomFontCheckBox c = mCheckboxes.get(i);
        	if ( c.isChecked() ) {
        		vc.add(new Selection(mItems.get(i)));
        	}
        }

        if (vc.size() == 0) {
            return null;
        } else {
            return new SelectMultiData(vc);
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
        for (CustomFontCheckBox c : mCheckboxes) {
            c.setOnLongClickListener(l);
        }
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        for (CustomFontCheckBox c : mCheckboxes) {
            c.cancelLongPress();
        }
    }

}

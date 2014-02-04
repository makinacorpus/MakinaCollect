package com.makina.collect.android.widgets;

import java.util.ArrayList;
import java.util.Vector;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.makina.collect.android.listeners.WidgetAnsweredListener;
import com.makina.collect.android.views.CustomFontRadioButton;
import com.makina.collect.android.views.MediaLayout;

/**
 * SelectOneWidgets handles select-one fields using radio buttons.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SelectOneWidget extends QuestionWidget implements
		OnCheckedChangeListener {

	Vector<SelectChoice> mItems; // may take a while to compute
	ArrayList<CustomFontRadioButton> buttons;
	
	private WidgetAnsweredListener mAnsListener;

	public SelectOneWidget(Context context, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
		super(context, widgetAnsweredListener, prompt);

		mItems = prompt.getSelectChoices();
		buttons = new ArrayList<CustomFontRadioButton>();

		mAnsListener = widgetAnsweredListener;
		
		// Layout holds the vertical list of buttons
		LinearLayout buttonLayout = new LinearLayout(context);

		String s = null;
		if (prompt.getAnswerValue() != null) {
			s = ((Selection) prompt.getAnswerValue().getValue()).getValue();
		}

		if (mItems != null) {
			for (int i = 0; i < mItems.size(); i++) {
				CustomFontRadioButton r = new CustomFontRadioButton(getContext());
				r.setText(prompt.getSelectChoiceText(mItems.get(i)));
				r.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
				r.setTag(Integer.valueOf(i));
				r.setId(QuestionWidget.newUniqueId());
				r.setEnabled(!prompt.isReadOnly());
				r.setFocusable(!prompt.isReadOnly());

				buttons.add(r);

				if (mItems.get(i).getValue().equals(s)) {
					r.setChecked(true);
				}

				r.setOnCheckedChangeListener(this);

				String audioURI = null;
				audioURI = prompt.getSpecialFormSelectChoiceText(mItems.get(i),
						FormEntryCaption.TEXT_FORM_AUDIO);

				String imageURI = null;
				imageURI = prompt.getSpecialFormSelectChoiceText(mItems.get(i),
						FormEntryCaption.TEXT_FORM_IMAGE);

				String videoURI = null;
				videoURI = prompt.getSpecialFormSelectChoiceText(mItems.get(i),
						"video");

				String bigImageURI = null;
				bigImageURI = prompt.getSpecialFormSelectChoiceText(
						mItems.get(i), "big-image");

				MediaLayout mediaLayout = new MediaLayout(getContext());
				mediaLayout.setAVT(prompt.getIndex(), "." + Integer.toString(i), r, audioURI, imageURI,
						videoURI, bigImageURI);

				if (i != mItems.size() - 1) {
					// Last, add the dividing line (except for the last element)
					ImageView divider = new ImageView(getContext());
					divider.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
					mediaLayout.addDivider(divider);
				}
				buttonLayout.addView(mediaLayout);
			}
		}
		buttonLayout.setOrientation(LinearLayout.VERTICAL);

		// The buttons take up the right half of the screen
		LayoutParams buttonParams = new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

		addView(buttonLayout, buttonParams);
	}

	@Override
	public void clearAnswer() {
		for (RadioButton button : this.buttons) {
			if (button.isChecked()) {
				button.setChecked(false);
				return;
			}
		}
	}

	@Override
	public IAnswerData getAnswer() {
		int i = getCheckedId();
		if (i == -1) {
			return null;
		} else {
			SelectChoice sc = mItems.elementAt(i);
			return new SelectOneData(new Selection(sc));
		}
	}

	@Override
	public void setFocus(Context context) {
		// Hide the soft keyboard if it's showing.
		InputMethodManager inputManager = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
	}

	public int getCheckedId() {
		for (int i = 0; i < buttons.size(); ++i) {
			RadioButton button = buttons.get(i);
			if (button.isChecked()) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (!isChecked) {
			// If it got unchecked, we don't care.
			return;
		}

		for (RadioButton button : buttons ) {
			if (button.isChecked() && !(buttonView == button)) {
				button.setChecked(false);
			}
		}
		mAnsListener.setAnswerChange(true);
		mAnsListener.updateView();
		mAnsListener.setAnswerChange(false);
    }

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		for (RadioButton r : buttons) {
			r.setOnLongClickListener(l);
		}
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		for (RadioButton button : this.buttons) {
			button.cancelLongPress();
		}
	}

}

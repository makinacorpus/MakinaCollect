package com.makina.collect.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;

import com.makina.collect.R;
import com.makina.collect.listeners.WidgetAnsweredListener;
import com.makina.collect.views.CustomFontSpinner;
import com.makina.collect.views.CustomFontTextview;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.Vector;

/**
 * SpinnerWidget handles select-one fields. Instead of a list of buttons it uses a spinner, wherein
 * the user clicks a button and the choices pop up in a dialogue box. The goal is to be more
 * compact. If images, audio, or video are specified in the select answers they are ignored.
 * 
 * @author Jeff Beorse (jeff@beorse.net)
 */
public class SpinnerWidget extends QuestionWidget implements OnFocusChangeListener{
    Vector<SelectChoice> mItems;
    CustomFontSpinner spinner;
    String[] choices;
    private boolean mAnswerChanged;
    private boolean mInit;


    public SpinnerWidget(Context context, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
        super(context, widgetAnsweredListener, prompt);
        mAnswerChanged = false;
        mInit = true;
        mAnswerListener.setAnswerChange(false);
        mItems = prompt.getSelectChoices();
        spinner = new CustomFontSpinner(context);
        choices = new String[mItems.size()+1];
        for (int i = 0; i < mItems.size(); i++) {
            choices[i] = prompt.getSelectChoiceText(mItems.get(i));
        }
        choices[mItems.size()] = getContext().getString(R.string.select_one);

        // The spinner requires a custom adapter. It is defined below
        SpinnerAdapter adapter =
            new SpinnerAdapter(getContext(), android.R.layout.simple_spinner_item, choices,
                    TypedValue.COMPLEX_UNIT_DIP, mQuestionFontsize);

        spinner.setAdapter(adapter);
        spinner.setPrompt(prompt.getQuestionText());
        spinner.setEnabled(!prompt.isReadOnly());
        spinner.setFocusable(!prompt.isReadOnly());

        // Fill in previous answer
        String s = null;
        if (prompt.getAnswerValue() != null) {
            s = ((Selection) prompt.getAnswerValue().getValue()).getValue();
        }

        spinner.setSelection(mItems.size());
        if (s != null) {
            for (int i = 0; i < mItems.size(); ++i) {
                String sMatch = mItems.get(i).getValue();
                if (sMatch.equals(s)) {
                    spinner.setSelection(i);
                }
            }
        }

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if ( position == mItems.size() ) {
					if (!mInit){
						mAnswerListener.setAnswerChange(true);
						Log.i(getClass().getName(), "clearValue : answer changed : true");
						updateView();
						mAnswerListener.setAnswerChange(false);
					}
					mInit = false;
				} else {
					if (!mInit){
						mAnswerListener.setAnswerChange(true);
						Log.i(getClass().getName(), "clearValue : answer changed : true");
						updateView();
						mAnswerListener.setAnswerChange(false);
					}
					mInit = false;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}});
        
        addView(spinner);

    }


    @Override
    public IAnswerData getAnswer() {
    	clearFocus();
        int i = spinner.getSelectedItemPosition();
        if (i == -1 || i == mItems.size()) {
            return null;
        } else {
            SelectChoice sc = mItems.elementAt(i);
            return new SelectOneData(new Selection(sc));
        }
    }


    @Override
    public void clearAnswer() {
        // It seems that spinners cannot return a null answer. This resets the answer
        // to its original value, but it is not null.
        spinner.setSelection(mItems.size());
    }


    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager =
            (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);

    }

    // Defines how to display the select answers
    private class SpinnerAdapter extends ArrayAdapter<String> {
        Context context;
        String[] items = new String[] {};
        int textUnit;
        float textSize;


        public SpinnerAdapter(final Context context, final int textViewResourceId,
                final String[] objects, int textUnit, float textSize) {
            super(context, textViewResourceId, objects);
            this.items = objects;
            this.context = context;
            this.textUnit = textUnit;
            this.textSize = textSize;
        }


        @Override
        // Defines the text view parameters for the drop down list entries
        public View getDropDownView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.widget_spinner_item, parent, false);
            }

            CustomFontTextview tv = (CustomFontTextview) convertView.findViewById(android.R.id.text1);
            tv.setTextSize(textUnit, textSize);
            tv.setBackgroundColor(Color.DKGRAY);
        	tv.setPadding(10, 10, 10, 10); // Are these values OK?
            if (position == items.length-1) {
            	tv.setText(parent.getContext().getString(R.string.clear_answer));
            	tv.setTextColor(Color.WHITE);
        		tv.setTypeface(null, Typeface.NORMAL);
            	if (spinner.getSelectedItemPosition() == position) {
            		tv.setTextColor(Color.LTGRAY);
            	}
            } else {
                tv.setText(items[position]);
                tv.setTextColor((spinner.getSelectedItemPosition() == position) 
						? Color.CYAN : Color.WHITE);
                
            	tv.setTypeface(null, (spinner.getSelectedItemPosition() == position) 
            							? Typeface.BOLD : Typeface.NORMAL);
            }
            return convertView;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            }

            CustomFontTextview tv = (CustomFontTextview) convertView.findViewById(android.R.id.text1);
            tv.setText(items[position]);
            tv.setTextSize(textUnit, textSize);
            tv.setTextColor(Color.CYAN);
        	tv.setTypeface(null, Typeface.BOLD);
            if (position == items.length-1) {
            	tv.setTextColor(Color.WHITE);
            	tv.setTypeface(null, Typeface.NORMAL);
            }
            return convertView;
        }

    }


    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        spinner.setOnLongClickListener(l);
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        spinner.cancelLongPress();
    }
    
    @Override
	public void onFocusChange(View v, boolean hasFocus) {
    	Log.i(getClass().getName(), "hasFocus "+hasFocus+" mAnswerChanged "+mAnswerChanged);
		if( mAnswerChanged){
			
		}
		
	}
}

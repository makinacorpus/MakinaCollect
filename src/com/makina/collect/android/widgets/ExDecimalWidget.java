package com.makina.collect.android.widgets;

import java.text.NumberFormat;

import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;

import com.makina.collect.android.application.Collect;
import com.makina.collect.android.listeners.WidgetAnsweredListener;
import com.makina.collect.android.utilities.StaticMethods;


/**
 * Launch an external app to supply a decimal value. If the app
 * does not launch, enable the text area for regular data entry.
 * 
 * See {@link com.makina.collect.android.widgets.ExStringWidget} for usage.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class ExDecimalWidget extends ExStringWidget {

	private Double getDoubleAnswerValue() {
		IAnswerData dataHolder = mPrompt.getAnswerValue();
        Double d = null;
        if (dataHolder != null) {
        	Object dataValue = dataHolder.getValue();
        	if ( dataValue != null ) {
        		if (dataValue instanceof Integer){
	                d =  Double.valueOf(((Integer)dataValue).intValue());
	            } else {
	                d =  (Double) dataValue;
	            }
        	}
        }
        return d;
	}

    public ExDecimalWidget(Activity activity, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
    	super(activity, widgetAnsweredListener, prompt);

        mAnswer.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // only allows numbers and no periods
        mAnswer.setKeyListener(new DigitsKeyListener(true, true));

        // only 15 characters allowed
        InputFilter[] fa = new InputFilter[1];
        fa[0] = new InputFilter.LengthFilter(15);
        mAnswer.setFilters(fa);

        Double d = getDoubleAnswerValue();

        // apparently an attempt at rounding to no more than 15 digit precision???
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(15);
        nf.setMaximumIntegerDigits(15);
        nf.setGroupingUsed(false);
        if (d != null) {
        	// truncate to 15 digits max...
            String dString = nf.format(d);
            d = Double.parseDouble(dString.replace(',', '.')); // in case , is decimal pt
            mAnswer.setText(d.toString());
        }
    }


    @Override
    protected void fireActivity(Intent i) throws ActivityNotFoundException {
        ((Activity) getContext()).startActivityForResult(i,
        		StaticMethods.EX_DECIMAL_CAPTURE);
    }

    
    @Override
    public IAnswerData getAnswer() {
        String s = mAnswer.getText().toString();
        if (s == null || s.equals("")) {
            return null;
        } else {
            try {
                return new DecimalData(Double.valueOf(s).doubleValue());
            } catch (Exception NumberFormatException) {
                return null;
            }
        }
    }


    /**
     * Allows answer to be set externally in {@Link FormEntryActivity}.
     */
    @Override
    public void setBinaryData(Object answer) {
    	mAnswer.setText( answer == null ? null : ((Double) answer).toString());
    	Collect.getInstance().getFormController().setIndexWaitingForData(null);
    }

}

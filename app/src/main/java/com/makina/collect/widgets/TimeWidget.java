package com.makina.collect.widgets;

import android.content.Context;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.TimePicker;

import com.makina.collect.listeners.WidgetAnsweredListener;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.Calendar;
import java.util.Date;

/**
 * Displays a TimePicker widget.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class TimeWidget
        extends QuestionWidget {

    private TimePicker mTimePicker;


    public TimeWidget(Context context,
                      WidgetAnsweredListener widgetAnsweredListener,
                      final FormEntryPrompt prompt) {
        super(context,
              widgetAnsweredListener,
              prompt);

        mTimePicker = new TimePicker(getContext());
        mTimePicker.setId(QuestionWidget.newUniqueId());
        mTimePicker.setFocusable(!prompt.isReadOnly());
        mTimePicker.setEnabled(!prompt.isReadOnly());

        String clockType = android.provider.Settings.System.getString(context.getContentResolver(),
                                                                      android.provider.Settings.System.TIME_12_24);
        if (clockType == null || clockType.equalsIgnoreCase("24")) {
            mTimePicker.setIs24HourView(true);
        }

        // If there's an answer, use it.
        if (prompt.getAnswerValue() != null) {

            // create a new date time from date object using default time zone
            final Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(((Date) prompt.getAnswerValue()
                                                   .getValue()).getTime());
            mTimePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
            mTimePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));

        }
        else {
            // create time widget with current time as of right now
            clearAnswer();
        }

        mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view,
                                      int hourOfDay,
                                      int minute) {
            }
        });

        setGravity(Gravity.LEFT);
        addView(mTimePicker);
    }

    /**
     * Resets time to now.
     */
    @Override
    public void clearAnswer() {
        final Calendar calendar = Calendar.getInstance();

        mTimePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        mTimePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
    }

    @Override
    public IAnswerData getAnswer() {
        clearFocus();

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,
                     mTimePicker.getCurrentHour());
        calendar.set(Calendar.MINUTE,
                     mTimePicker.getCurrentMinute());
        calendar.set(Calendar.SECOND,
                     0);
        calendar.set(Calendar.MILLISECOND,
                     0);

        return new TimeData(calendar.getTime());
    }

    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(),
                                             0);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mTimePicker.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mTimePicker.cancelLongPress();
    }
}

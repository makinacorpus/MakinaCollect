/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.makina.collect.android.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.makina.collect.android.listeners.WidgetAnsweredListener;

import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.Calendar;
import java.util.Date;

/**
 * Displays a DatePicker widget. DateWidget handles leap years and does not allow dates that do not
 * exist.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
@SuppressLint("NewApi")
public class DateTimeWidget
        extends QuestionWidget {

    private DatePicker mDatePicker;
    private TimePicker mTimePicker;
    private DatePicker.OnDateChangedListener mDateListener;

    public DateTimeWidget(Context context,
                          WidgetAnsweredListener widgetAnsweredListener,
                          FormEntryPrompt prompt) {
        super(context,
              widgetAnsweredListener,
              prompt);

        mDatePicker = new DatePicker(getContext());
        mDatePicker.setId(QuestionWidget.newUniqueId());
        mDatePicker.setFocusable(!prompt.isReadOnly());
        mDatePicker.setEnabled(!prompt.isReadOnly());
        mAnswerListener.setAnswerChange(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            mDatePicker.setCalendarViewShown(false);
        }

        mTimePicker = new TimePicker(getContext());
        mTimePicker.setId(QuestionWidget.newUniqueId());
        mTimePicker.setFocusable(!prompt.isReadOnly());
        mTimePicker.setEnabled(!prompt.isReadOnly());
        mTimePicker.setPadding(0,
                               20,
                               0,
                               0);

        String clockType = android.provider.Settings.System.getString(context.getContentResolver(),
                                                                      android.provider.Settings.System.TIME_12_24);
        if (clockType == null || clockType.equalsIgnoreCase("24")) {
            mTimePicker.setIs24HourView(true);
        }

        // If there's an answer, use it.
        setAnswer();

        mDateListener = new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view,
                                      int year,
                                      int month,
                                      int day) {
                if (mPrompt.isReadOnly()) {
                    setAnswer();
                }
                else {
                    // handle leap years and number of days in month
                    // TODO
                    // http://code.google.com/p/android/issues/detail?id=2081
                    // in older versions of android (1.6ish) the datepicker lets you pick bad dates
                    // in newer versions, calling updateDate() calls onDatechangedListener(), causing an
                    // endless loop.
                    Calendar c = Calendar.getInstance();
                    c.set(year,
                          month,
                          1);
                    int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                    if (day > max) {
                        if (!(mDatePicker.getDayOfMonth() == day && mDatePicker.getMonth() == month && mDatePicker.getYear() == year)) {
                            mDatePicker.updateDate(year,
                                                   month,
                                                   max);
                        }
                    }
                    else {
                        if (!(mDatePicker.getDayOfMonth() == day && mDatePicker.getMonth() == month && mDatePicker.getYear() == year)) {
                            mDatePicker.updateDate(year,
                                                   month,
                                                   day);
                        }
                    }
                }
            }
        };

        mTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view,
                                      int hourOfDay,
                                      int minute) {
            }
        });

        setGravity(Gravity.LEFT);
        addView(mDatePicker);
        addView(mTimePicker);
    }

    private void setAnswer() {
        if (mPrompt.getAnswerValue() != null) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(((Date) mPrompt.getAnswerValue()
                                                    .getValue()).getTime());

            mDatePicker.init(calendar.get(Calendar.YEAR),
                             calendar.get(Calendar.MONTH),
                             calendar.get(Calendar.DAY_OF_MONTH),
                             mDateListener);

            mTimePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
            mTimePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));

        }
        else {
            // create time widget with current time as of right now
            clearAnswer();
        }
    }

    /**
     * Resets date to today.
     */
    @Override
    public void clearAnswer() {
        final Calendar calendar = Calendar.getInstance();

        mDatePicker.init(calendar.get(Calendar.YEAR),
                         calendar.get(Calendar.MONTH),
                         calendar.get(Calendar.DAY_OF_MONTH),
                         mDateListener);

        mTimePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        mTimePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
    }


    @Override
    public IAnswerData getAnswer() {
        clearFocus();

        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR,
                     mDatePicker.getYear());
        calendar.set(Calendar.MONTH,
                     mDatePicker.getMonth());
        calendar.set(Calendar.DAY_OF_MONTH,
                     mDatePicker.getDayOfMonth());
        calendar.set(Calendar.HOUR_OF_DAY,
                     mTimePicker.getCurrentHour());
        calendar.set(Calendar.MINUTE,
                     mTimePicker.getCurrentMinute());
        calendar.set(Calendar.SECOND,
                     0);
        calendar.set(Calendar.MILLISECOND,
                     0);

        return new DateTimeData(calendar.getTime());
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
        mDatePicker.setOnLongClickListener(l);
        mTimePicker.setOnLongClickListener(l);
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mDatePicker.cancelLongPress();
        mTimePicker.cancelLongPress();
    }

}

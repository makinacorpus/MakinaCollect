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
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;

import com.makina.collect.android.listeners.WidgetAnsweredListener;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

/**
 * Displays a DatePicker widget. DateWidget handles leap years and does not allow dates that do not
 * exist.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

public class DateWidget
        extends QuestionWidget {

    private DatePicker mDatePicker;
    private DatePicker.OnDateChangedListener mDateListener;
    private boolean hideDay = false;
    private boolean hideMonth = false;


    @SuppressLint("NewApi")
    public DateWidget(Context context,
                      WidgetAnsweredListener widgetAnsweredListener,
                      FormEntryPrompt prompt) {
        super(context,
              widgetAnsweredListener,
              prompt);

        mAnswerListener.setAnswerChange(false);

        mDatePicker = new DatePicker(getContext());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            mDatePicker.setCalendarViewShown(false);
        }

        mDatePicker.setId(QuestionWidget.newUniqueId());
        mDatePicker.setFocusable(!prompt.isReadOnly());
        mDatePicker.setEnabled(!prompt.isReadOnly());

        hideDayFieldIfNotInFormat(prompt);

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
                    // TODO support dates <1900 >2100
                    // handle leap years and number of days in month
                    // http://code.google.com/p/android/issues/detail?id=2081
                    Calendar c = Calendar.getInstance();
                    c.set(year,
                          month,
                          1);
                    int max = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                    // in older versions of android (1.6ish) the datepicker lets you pick bad dates
                    // in newer versions, calling updateDate() calls onDatechangedListener(), causing an
                    // endless loop.
                    if (day > max) {
                        if (!(mDatePicker.getDayOfMonth() == day && mDatePicker.getMonth() == month && mDatePicker.getYear() == year)) {
                            mDatePicker.updateDate(year,
                                                   month,
                                                   max);
                            mAnswerListener.setAnswerChange(true);
                        }
                    }
                    else {
                        if (!(mDatePicker.getDayOfMonth() == day && mDatePicker.getMonth() == month && mDatePicker.getYear() == year)) {
                            mDatePicker.updateDate(year,
                                                   month,
                                                   day);
                            mAnswerListener.setAnswerChange(true);
                        }
                    }
                }
            }
        };

        setGravity(Gravity.LEFT);
        addView(mDatePicker);
    }

    private void hideDayFieldIfNotInFormat(FormEntryPrompt prompt) {
        String appearance = prompt.getQuestion()
                                  .getAppearanceAttr();
        if (appearance == null) {
            return;
        }

        if ("month-year".equals(appearance)) {
            hideDay = true;
        }
        else if ("year".equals(appearance)) {
            hideMonth = true;
        }

        if (hideMonth || hideDay) {
            for (Field datePickerDialogField : this.mDatePicker.getClass()
                                                               .getDeclaredFields()) {
                if ("mDayPicker".equals(datePickerDialogField.getName()) || "mDaySpinner".equals(datePickerDialogField.getName())) {
                    datePickerDialogField.setAccessible(true);
                    Object dayPicker = new Object();
                    try {
                        dayPicker = datePickerDialogField.get(this.mDatePicker);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    ((View) dayPicker).setVisibility(View.GONE);
                }
                if (hideMonth) {
                    if ("mMonthPicker".equals(datePickerDialogField.getName()) || "mMonthSpinner".equals(datePickerDialogField.getName())) {
                        datePickerDialogField.setAccessible(true);
                        Object monthPicker = new Object();
                        try {
                            monthPicker = datePickerDialogField.get(this.mDatePicker);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        ((View) monthPicker).setVisibility(View.GONE);
                    }
                }
            }
        }
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
        }
        else {
            // create date widget with current time as of right now
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

        return new DateData(calendar.getTime());
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
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mDatePicker.cancelLongPress();
    }

}

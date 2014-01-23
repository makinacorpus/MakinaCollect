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

import java.text.DecimalFormat;

import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.makina.collect.android.R;
import com.makina.collect.android.activities.ActivityForm;
import com.makina.collect.android.activities.ActivityGeoPoint;
import com.makina.collect.android.activities.ActivityGeoPointMap;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.listeners.WidgetAnsweredListener;

/**
 * GeoPointWidget is the widget that allows the user to get GPS readings.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class GeoPointWidget extends QuestionWidget implements IBinaryWidget {
	public static final String LOCATION = "gp";
	public static final String ACCURACY_THRESHOLD = "accuracyThreshold";

	public static final double DEFAULT_LOCATION_ACCURACY = 35.0;

	private Button mGetLocationButton;
	private Button mViewButton;
	private Button mOkButton;
	private EditText mLatField;
	private EditText mLongField;
	private TextView mLatInfo;
	private TextView mLongInfo;

	private TextView mStringAnswer;
	private TextView mAnswerDisplay;
	private boolean mUseMaps;
	private boolean mUseGPS;
	private boolean mIsReadOnly;
	private boolean mOfflineMode;
	private double mAccuracyThreshold;

	public GeoPointWidget(Activity activity, WidgetAnsweredListener widgetAnsweredListener, FormEntryPrompt prompt) {
		super(activity, widgetAnsweredListener, prompt);
		mUseGPS = true;
		mUseMaps = true;
		mOfflineMode = true;
		
		String acc = prompt.getQuestion().getAdditionalAttribute(null, ACCURACY_THRESHOLD);
		if ( acc != null && acc.length() != 0 ) {
			mAccuracyThreshold = Double.parseDouble(acc);
		} else {
			mAccuracyThreshold = DEFAULT_LOCATION_ACCURACY;
		}
		
		//First we setup the widgets : 
		setOrientation(LinearLayout.VERTICAL);

		TableLayout.LayoutParams params = new TableLayout.LayoutParams();
		params.setMargins(7, 5, 7, 5);
		
		//CheckBox allow the user to chose whether to use gps or not
		
		CheckBox useGPSinput = new CheckBox(getContext());
		useGPSinput.setId(QuestionWidget.newUniqueId());
		useGPSinput.setChecked(true);
		useGPSinput.setText(R.string.use_gps);
		useGPSinput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		useGPSinput.setGravity(Gravity.BOTTOM);
		useGPSinput.setOnClickListener(new OnClickListener() {
			  @Override
			  public void onClick(View v) {
				mUseGPS = ((CheckBox) v).isChecked();
				refreshWidget ();
			  }
			});
		addView(useGPSinput);
		useGPSinput.setVisibility(View.VISIBLE);

		//setup the get location button
		mGetLocationButton = (Button)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mGetLocationButton.setId(QuestionWidget.newUniqueId());
		mGetLocationButton.setText(getContext()
				.getString(R.string.get_location));
		mGetLocationButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		mGetLocationButton.setEnabled(!prompt.isReadOnly());
		mGetLocationButton.setLayoutParams(params);
		mGetLocationButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_save, 0, 0, 0);

		mIsReadOnly = prompt.isReadOnly();

		// setup play button
		mViewButton = (Button)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mViewButton.setId(QuestionWidget.newUniqueId());
		mViewButton.setText(getContext().getString(R.string.show_location));
		mViewButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		mViewButton.setPadding(20, 20, 20, 20);
		mViewButton.setLayoutParams(params);
		mViewButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_mylocation, 0, 0, 0);
		// on play, launch the appropriate viewer
		mViewButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance()
						.getActivityLogger()
						.logInstanceAction(this, "showLocation", "click",
								mPrompt.getIndex());
				Intent i = new Intent(getContext(), ActivityGeoPointMap.class);
				if (mOfflineMode == true){
					i.putExtra("offLine", true);
				}
				if (mUseMaps && !mUseGPS){
					i.putExtra("noGPS", true);
				}
				if (mStringAnswer != null && mStringAnswer.length() != 0){
					String s = mStringAnswer.getText().toString();
					String[] sa = s.split(" ");
					double gp[] = new double[4];
					gp[0] = Double.valueOf(sa[0]).doubleValue();
					gp[1] = Double.valueOf(sa[1]).doubleValue();
					gp[2] = Double.valueOf(sa[2]).doubleValue();
					gp[3] = Double.valueOf(sa[3]).doubleValue();
					
					i.putExtra(LOCATION, gp);
					i.putExtra(ACCURACY_THRESHOLD, mAccuracyThreshold);
				}
				Collect.getInstance().getFormController()
				.setIndexWaitingForData(mPrompt.getIndex());
				((Activity) getContext()).startActivityForResult(i,
						ActivityForm.LOCATION_CAPTURE);

			}
		});
		
		//Those fields are used to get the location if maps and gps are both unavailable 
		mLatInfo = new TextView(getContext());
		mLatInfo.setId(QuestionWidget.newUniqueId());
		mLatInfo.setText("Latitude : ");
		mLatInfo.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		addView(mLatInfo);
		
		mLatField = new EditText(getContext());
		mLatField.setId(QuestionWidget.newUniqueId());
		mLatField.setText("0");
		mLatField.setRawInputType(InputType.TYPE_CLASS_NUMBER);
		addView(mLatField);
		
		mLongInfo = new TextView(getContext());
		mLongInfo.setId(QuestionWidget.newUniqueId());
		mLongInfo.setText("Longitude : ");
		mLongInfo.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		addView(mLongInfo);
		
		mLongField = new EditText(getContext());
		mLongField.setId(QuestionWidget.newUniqueId());
		mLongField.setText("0");
		mLongField.setRawInputType(InputType.TYPE_CLASS_NUMBER);
		addView(mLongField);
		
		//Ok button to submit the values entered in the fields above
		mOkButton = (Button)activity.getLayoutInflater().inflate(R.layout.widget_button, null);
		mOkButton.setId(QuestionWidget.newUniqueId());
		mOkButton.setText(R.string.ok);
		mOkButton.setEnabled(!prompt.isReadOnly());
		mOkButton.setLayoutParams(params);
		mOkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_save, 0, 0, 0);
		mOkButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mLongField.getText()!=null && mLatField.getText()!=null) {
					mAnswerDisplay.setText(getContext().getString(R.string.latitude) + ": "
							+ formatGps(Double.parseDouble(mLatField.getText().toString()), "lat") + "\n"
							+ getContext().getString(R.string.longitude) + ": "
							+ formatGps(Double.parseDouble(mLongField.getText().toString()), "lon") + "\n"
							+ getContext().getString(R.string.altitude) + ": "
							+ "0" + "m\n"
							+ getContext().getString(R.string.accuracy) + ": "
							+ "0" + "m");
					mStringAnswer.setText(mLatField.getText().toString() + " " + mLongField.getText().toString() + " "
		                    + 0 + " " + 0);
					Collect.getInstance().getFormController().setIndexWaitingForData(null); 
					mViewButton.setEnabled(true);
				}
			}
		});
		addView(mOkButton);

		//Display the answer
		mStringAnswer = new TextView(getContext());
		mStringAnswer.setId(QuestionWidget.newUniqueId());

		mAnswerDisplay = new TextView(getContext());
		mAnswerDisplay.setId(QuestionWidget.newUniqueId());
		mAnswerDisplay
				.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
		mAnswerDisplay.setGravity(Gravity.CENTER);

		
		String s = prompt.getAnswerText();
		if (s != null && !s.equals("")) {
			mGetLocationButton.setText(getContext().getString(
					R.string.replace_location));
			setBinaryData(s);
			mViewButton.setEnabled(true);
		} else {
			mViewButton.setEnabled(false);
		}

		// use maps or not
		try {
			// do google maps exist on the device
			Class.forName("com.google.android.maps.MapActivity");
			mUseMaps = true;
		} catch (ClassNotFoundException e) {
			mUseMaps = false;
		}

		// when you press the button
		mGetLocationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance()
						.getActivityLogger()
						.logInstanceAction(this, "recordLocation", "click",
								mPrompt.getIndex());
				Intent i = null;
				
				if (mUseMaps) {
					i = new Intent(getContext(), ActivityGeoPointMap.class);
					if (mOfflineMode == true){
						i.putExtra("offLine", true);
					}
				} else {
					i = new Intent(getContext(), ActivityGeoPoint.class);
				}
				
				i.putExtra(ACCURACY_THRESHOLD, mAccuracyThreshold);
				Collect.getInstance().getFormController()
						.setIndexWaitingForData(mPrompt.getIndex());
				((Activity) getContext()).startActivityForResult(i,
						ActivityForm.LOCATION_CAPTURE);
			}
		});

		// finish complex layout
		// retrieve answer from data model and update ui

		addView(mGetLocationButton);
		addView(mViewButton);
		addView(mAnswerDisplay);

		refreshWidget ();
	}

	@Override
	public void clearAnswer() {
		mStringAnswer.setText(null);
		mAnswerDisplay.setText(null);
		mGetLocationButton.setText(getContext()
				.getString(R.string.get_location));

	}

	@Override
	public IAnswerData getAnswer() {
		String s = mStringAnswer.getText().toString();
		if (s == null || s.equals("")) {
			return null;
		} else {
			try {
				// segment lat and lon
				String[] sa = s.split(" ");
				double gp[] = new double[4];
				gp[0] = Double.valueOf(sa[0]).doubleValue();
				gp[1] = Double.valueOf(sa[1]).doubleValue();
				gp[2] = Double.valueOf(sa[2]).doubleValue();
				gp[3] = Double.valueOf(sa[3]).doubleValue();

				return new GeoPointData(gp);
			} catch (Exception NumberFormatException) {
				return null;
			}
		}
	}

	private String truncateDouble(String s) {
		DecimalFormat df = new DecimalFormat("#.##");
		return df.format(Double.valueOf(s));
	}

	private String formatGps(double coordinates, String type) {
		String location = Double.toString(coordinates);
		String degreeSign = "\u00B0";
		String degree = location.substring(0, location.indexOf("."))
				+ degreeSign;
		location = "0." + location.substring(location.indexOf(".") + 1);
		double temp = Double.valueOf(location) * 60;
		location = Double.toString(temp);
		String mins = location.substring(0, location.indexOf(".")) + "'";

		location = "0." + location.substring(location.indexOf(".") + 1);
		temp = Double.valueOf(location) * 60;
		location = Double.toString(temp);
		String secs = location.substring(0, location.indexOf(".")) + '"';
		if (type.equalsIgnoreCase("lon")) {
			if (degree.startsWith("-")) {
				degree = "W " + degree.replace("-", "") + mins + secs;
			} else
				degree = "E " + degree.replace("-", "") + mins + secs;
		} else {
			if (degree.startsWith("-")) {
				degree = "S " + degree.replace("-", "") + mins + secs;
			} else
				degree = "N " + degree.replace("-", "") + mins + secs;
		}
		return degree;
	}

	@Override
	public void setFocus(Context context) {
		// Hide the soft keyboard if it's showing.
		InputMethodManager inputManager = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
	}

	@Override
	public void setBinaryData(Object answer) {
		String s = (String) answer;
		mStringAnswer.setText(s);

		String[] sa = s.split(" ");
		mAnswerDisplay.setText(getContext().getString(R.string.latitude) + ": "
				+ formatGps(Double.parseDouble(sa[0]), "lat") + "\n"
				+ getContext().getString(R.string.longitude) + ": "
				+ formatGps(Double.parseDouble(sa[1]), "lon") + "\n"
				+ getContext().getString(R.string.altitude) + ": "
				+ truncateDouble(sa[2]) + "m\n"
				+ getContext().getString(R.string.accuracy) + ": "
				+ truncateDouble(sa[3]) + "m");
		Collect.getInstance().getFormController().setIndexWaitingForData(null);
	}

	@Override
	public boolean isWaitingForBinaryData() {
		return mPrompt.getIndex().equals(
				Collect.getInstance().getFormController()
						.getIndexWaitingForData());
	}

	@Override
	public void cancelWaitingForBinaryData() {
		Collect.getInstance().getFormController().setIndexWaitingForData(null);
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		mViewButton.setOnLongClickListener(l);
		mGetLocationButton.setOnLongClickListener(l);
		mStringAnswer.setOnLongClickListener(l);
		mAnswerDisplay.setOnLongClickListener(l);
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		mViewButton.cancelLongPress();
		mGetLocationButton.cancelLongPress();
		mStringAnswer.cancelLongPress();
		mAnswerDisplay.cancelLongPress();
	}
	
	private void refreshWidget () {
		mUseMaps = mUseMaps && PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("key_use_maps", true);
		mOfflineMode = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("key_offline_mode", true);
		if (!mIsReadOnly){
			if (mUseMaps){
				if (mUseGPS){
					mGetLocationButton.setVisibility(View.VISIBLE);
					mViewButton.setVisibility(View.VISIBLE);
					mLatField.setVisibility(View.GONE);
					mLatInfo.setVisibility(View.GONE);
					mLongInfo.setVisibility(View.GONE);
					mLongField.setVisibility(View.GONE);
					mOkButton.setVisibility(View.GONE);
					mViewButton.setText(R.string.show_location);
				} else {
					mGetLocationButton.setVisibility(View.GONE);
					mViewButton.setVisibility(View.VISIBLE);
					mLatField.setVisibility(View.GONE);
					mLatInfo.setVisibility(View.GONE);
					mLongInfo.setVisibility(View.GONE);
					mLongField.setVisibility(View.GONE);
					mOkButton.setVisibility(View.GONE);
					mViewButton.setEnabled(true);
					mViewButton.setText(R.string.get_location);
				}
			}else{
				if (mUseGPS){
					mGetLocationButton.setVisibility(View.VISIBLE);
					mViewButton.setVisibility(View.GONE);
					mLatField.setVisibility(View.GONE);
					mLatInfo.setVisibility(View.GONE);
					mLongInfo.setVisibility(View.GONE);
					mLongField.setVisibility(View.GONE);
					mOkButton.setVisibility(View.GONE);
				} else {
					mGetLocationButton.setVisibility(View.GONE);
					mViewButton.setVisibility(View.GONE);
					mLatField.setVisibility(View.VISIBLE);
					mLatInfo.setVisibility(View.VISIBLE);
					mLongInfo.setVisibility(View.VISIBLE);
					mLongField.setVisibility(View.VISIBLE);
					mOkButton.setVisibility(View.VISIBLE);
				}
			}
		}else{
			if (mUseMaps){
				mGetLocationButton.setVisibility(View.GONE);
				mViewButton.setVisibility(View.VISIBLE);
				mLatField.setVisibility(View.GONE);
				mLatInfo.setVisibility(View.GONE);
				mLongInfo.setVisibility(View.GONE);
				mLongField.setVisibility(View.GONE);
				mOkButton.setVisibility(View.GONE);
				mViewButton.setText(R.string.show_location);
			}else{
				mGetLocationButton.setVisibility(View.GONE);
				mViewButton.setVisibility(View.GONE);
				mLatField.setVisibility(View.GONE);
				mLatInfo.setVisibility(View.GONE);
				mLongInfo.setVisibility(View.GONE);
				mLongField.setVisibility(View.GONE);
				mOkButton.setVisibility(View.GONE);
			}
		}
	}

}

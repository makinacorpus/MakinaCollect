/*
 * Copyright (C) 2011 University of Washington
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

package com.makina.collect.android.preferences;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore.MediaColumns;
import android.provider.Settings.SettingNotFoundException;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager.LayoutParams;
import android.provider.Settings.System;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.dialog.DialogAboutUs;
import com.makina.collect.android.dialog.DialogExit;
import com.makina.collect.android.theme.Theme;
import com.makina.collect.android.utilities.Finish;
import com.makina.collect.android.utilities.UrlUtils;


public class ActivityPreferences extends SherlockPreferenceActivity implements
		OnPreferenceChangeListener {
	
	protected static final int IMAGE_CHOOSER = 0;

	
	public static final String KEY_INFO = "info";
	public static final String KEY_LAST_VERSION = "lastVersion";
	public static final String KEY_FIRST_RUN = "firstRun";
	public static final String KEY_SHOW_SPLASH = "showSplash";
	public static final String KEY_SPLASH_PATH = "splashPath";
	public static final String KEY_FONT_SIZE = "font_size";
	public static final String KEY_SELECTED_GOOGLE_ACCOUNT = "selected_google_account";
	public static final String KEY_GOOGLE_SUBMISSION = "google_submission_id";

	public static final String KEY_SERVER_URL = "server_url";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";

	public static final String KEY_PROTOCOL = "protocol";
	
	public static final String KEY_USEMAPS = "use_maps";

	// must match /res/arrays.xml
	public static final String PROTOCOL_ODK_DEFAULT = "odk_default";
	public static final String PROTOCOL_GOOGLE = "google";
	public static final String PROTOCOL_OTHER = "";
	
	public static final String NAVIGATION_SWIPE = "swipe";
	public static final String NAVIGATION_BUTTONS = "buttons";
	public static final String NAVIGATION_SWIPE_BUTTONS = "swipe_buttons";

	public static final String KEY_FORMLIST_URL = "formlist_url";
	public static final String KEY_SUBMISSION_URL = "submission_url";
	public static final String KEY_AUTH = "auth";

	public static final String KEY_AUTOSEND_WIFI = "autosend_wifi";
	public static final String KEY_AUTOSEND_NETWORK = "autosend_network";

	public static final String KEY_NAVIGATION = "navigation";
	public static final String KEY_LANGUAGE = "language";
	public static final String KEY_THEME = "theme";
	public static final String KEY_LUMINOSITY="luminosity";

	private PreferenceScreen mSplashPathPreference;
	private EditTextPreference mSubmissionUrlPreference;
	private EditTextPreference mFormListUrlPreference;
	private EditTextPreference mServerUrlPreference;
	private EditTextPreference mUsernamePreference;
	private EditTextPreference mPasswordPreference;
	private Preference mLuminosityPreference;
	private ListPreference mLanguagePreference;
	private ListPreference mThemePreference;
	private ListPreference mSelectedGoogleAccountPreference;
	private ListPreference mFontSizePreference;

	private CheckBoxPreference mAutosendWifiPreference;
	private CheckBoxPreference mAutosendNetworkPreference;
	private CheckBoxPreference mUseMaps;
	private ListPreference mProtocolPreference;
	
	private List<Preference> mPreferences;
	
	private SharedPreferences mAdminPreferences;
	
	private boolean mAdvancedShown;
	
	//Seek bar object
    private SeekBar brightbar;
    //Variable to store brightness value
    private int brightness;
    private TextView txtPerc;
    private ContentResolver cResolver;
    //Window object, that will store a reference to the current window
    private Window window;
    private int old_brightness;
    private float perc ;
    private Menu menu;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
    	 Theme.changeTheme(this);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		Finish.activityPreferences=this;
		
		
		
		Typeface typeFace = Typeface.createFromAsset(getAssets(),"fonts/avenir.ttc"); 
		setTitle(getString(R.string.settings));
		int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
    	TextView actionbarTitle = (TextView)findViewById(titleId);
    	if (actionbarTitle!=null)
    		actionbarTitle.setTypeface(typeFace);
		
		// not super safe, but we're just putting in this mode to help
		// administrate
		// would require code to access it
		boolean adminMode = getIntent().getBooleanExtra("adminMode", false);

		mAdminPreferences = getSharedPreferences(
				AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

		boolean serverAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_SERVER, true);
		boolean urlAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_URL, true);

		PreferenceCategory autosendCategory = (PreferenceCategory) findPreference(getString(R.string.autosend));
		mAutosendWifiPreference = (CheckBoxPreference) findPreference(KEY_AUTOSEND_WIFI);
		boolean autosendWifiAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_AUTOSEND_WIFI, true);
		if (!(autosendWifiAvailable || adminMode)) {
			autosendCategory.removePreference(mAutosendWifiPreference);
		}

		mAutosendNetworkPreference = (CheckBoxPreference) findPreference(KEY_AUTOSEND_NETWORK);
		boolean autosendNetworkAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_AUTOSEND_NETWORK, true);
		if (!(autosendNetworkAvailable || adminMode)) {
			autosendCategory.removePreference(mAutosendNetworkPreference);
		}

		if (!(autosendNetworkAvailable || autosendWifiAvailable || adminMode)) {
			getPreferenceScreen().removePreference(autosendCategory);
		}
		
		PreferenceCategory serverCategory = (PreferenceCategory) findPreference(getString(R.string.server_preferences));

		// declared early to prevent NPE in toggleServerPaths
		mFormListUrlPreference = (EditTextPreference) findPreference(KEY_FORMLIST_URL);
		mSubmissionUrlPreference = (EditTextPreference) findPreference(KEY_SUBMISSION_URL);

		mProtocolPreference = (ListPreference) findPreference(KEY_PROTOCOL);
		mProtocolPreference.setSummary(mProtocolPreference.getEntry());
		if (mProtocolPreference.getValue().equals("odk_default")) {
			mFormListUrlPreference.setEnabled(false);
			mSubmissionUrlPreference.setEnabled(false);
		} else {
			mFormListUrlPreference.setEnabled(true);
			mSubmissionUrlPreference.setEnabled(true);
		}
		mProtocolPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				int index = ((ListPreference) preference)
						.findIndexOfValue(newValue.toString());
				String entry = (String) ((ListPreference) preference)
						.getEntries()[index];
				String value = (String) ((ListPreference) preference)
						.getEntryValues()[index];
				((ListPreference) preference).setSummary(entry);
				if (value.equals("odk_default")) {
					mFormListUrlPreference.setEnabled(false);
					mFormListUrlPreference.setText(getString(R.string.default_odk_formlist));
					mFormListUrlPreference.setSummary(mFormListUrlPreference.getText());
					mSubmissionUrlPreference.setEnabled(false);
					mSubmissionUrlPreference.setText(getString(R.string.default_odk_submission));
					mSubmissionUrlPreference.setSummary(mSubmissionUrlPreference.getText());
				} else {
					mFormListUrlPreference.setEnabled(true);
					mSubmissionUrlPreference.setEnabled(true);
				}
				
				return true;
			}
		});

		mLanguagePreference = (ListPreference) findPreference(KEY_LANGUAGE);
		mLanguagePreference.setSummary(mLanguagePreference.getEntry());
		mLanguagePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference,Object newValue)
			{
				int index = ((ListPreference) preference).findIndexOfValue(newValue.toString());
				String entry = (String) ((ListPreference) preference).getEntries()[index];
				((ListPreference) preference).setSummary(entry);
				
				Intent i = getIntent();
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
				return true;
			}
		});
		
		
		mLuminosityPreference = (Preference) findPreference(KEY_LUMINOSITY);
		//Get the content resolver
        cResolver = getContentResolver();
        //Get the current window
		window = getWindow();
		//Get the current system brightness
		try
        {
            //Get the current system brightness
            brightness = System.getInt(cResolver, System.SCREEN_BRIGHTNESS);
            old_brightness=brightness;
            perc = (brightness /(float)255)*100;
            //Set the brightness percentage 
            mLuminosityPreference.setSummary(getString(R.string.luminosity)+": "+(int)perc+"%");
        } 
        catch (SettingNotFoundException e) 
        {
        }
		
		
		mLuminosityPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
		{
			@Override
			public boolean onPreferenceClick(Preference arg0)
			{
				 LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				    View layout = inflater.inflate(R.layout.dialog_luminosity, (ViewGroup) findViewById(R.id.layoutRoot));
				    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityPreferences.this).setView(layout);
				    builder.setTitle(getString(R.string.luminosity));
				    builder.setIcon(getResources().getDrawable(android.R.drawable.ic_lock_idle_low_battery));
				    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				               @Override
				               public void onClick(DialogInterface dialog, int id) {
				            	   old_brightness=brightness;
				               }
				           })
				           .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				               public void onClick(DialogInterface dialog, int id) {
				            	   System.putInt(cResolver, System.SCREEN_BRIGHTNESS, old_brightness);
				            	   LayoutParams layoutpars = window.getAttributes();
					                //Set the brightness of this window
					               layoutpars.screenBrightness = brightness / (float)255;
					                //Apply attribute changes to this window
					               window.setAttributes(layoutpars);
				            	   perc = (old_brightness /(float)255)*100;
				            	   mLuminosityPreference.setSummary(getString(R.string.luminosity)+": "+(int)perc+"%");
				               }
				           });  
				    Dialog dialog_luminosity = builder.create();
				    dialog_luminosity.show();
				    brightbar = (SeekBar) dialog_luminosity.findViewById(R.id.brightbar);
					txtPerc = (TextView) dialog_luminosity.findViewById(R.id.txtPercentage);
					
					//Set the seekbar range between 0 and 255
			        brightbar.setMax(255);
			        //Set the seek bar progress to 1
			        brightbar.setKeyProgressIncrement(1);
			        try
			        {
			            //Get the current system brightness
			            brightness = System.getInt(cResolver, System.SCREEN_BRIGHTNESS);
			            perc = (brightness /(float)255)*100;
		                //Set the brightness percentage 
		                txtPerc.setText((int)perc +" %");
			        } 
			        catch (SettingNotFoundException e) 
			        {
			        }
			 
			        //Set the progress of the seek bar based on the system's brightness
			        brightbar.setProgress(brightness);
			 
			        //Register OnSeekBarChangeListener, so it can actually change values
			        brightbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
			        {
			            public void onStopTrackingTouch(SeekBar seekBar) 
			            {
			                //Set the system brightness using the brightness variable value
			                System.putInt(cResolver, System.SCREEN_BRIGHTNESS, brightness);
			                //Get the current window attributes
			                LayoutParams layoutpars = window.getAttributes();
			                //Set the brightness of this window
			                layoutpars.screenBrightness = brightness / (float)255;
			                //Apply attribute changes to this window
			                window.setAttributes(layoutpars);
			            }
			 
			            public void onStartTrackingTouch(SeekBar seekBar){}
			 
			            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
			            {
			                //Set the minimal brightness level
			                //if seek bar is 20 or any value below
			                if(progress<=20)
			                {
			                    //Set the brightness to 20
			                    brightness=20;
			                }
			                else //brightness is greater than 20
			                {
			                    //Set brightness variable based on the progress bar 
			                    brightness = progress;
			                }
			                //Calculate the brightness percentage
			                perc = (brightness /(float)255)*100;
			                //Set the brightness percentage 
			                txtPerc.setText((int)perc +" %");
			                mLuminosityPreference.setSummary(getString(R.string.luminosity)+": "+(int)perc+"%");
			            }
			            
			            
			        });  
				    
				return false;
			}
		});
		
		mServerUrlPreference = (EditTextPreference) findPreference(KEY_SERVER_URL);
		mServerUrlPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String url = newValue.toString();

						// remove all trailing "/"s
						while (url.endsWith("/")) {
							url = url.substring(0, url.length() - 1);
						}

						if (UrlUtils.isValidUrl(url)) {
							preference.setSummary(newValue.toString());
							return true;
						} else {
							Toast.makeText(getApplicationContext(),
									R.string.url_error, Toast.LENGTH_SHORT)
									.show();
							return false;
						}
					}
				});
		mServerUrlPreference.setSummary(mServerUrlPreference.getText());
		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });

		

		if (!(serverAvailable || adminMode)) {
			Preference protocol = findPreference(KEY_PROTOCOL);
			serverCategory.removePreference(protocol);
		} else {
			// this just removes the value from protocol, but protocol doesn't
			// exist if we take away access
			disableFeaturesInDevelopment();
		}
		
		if (!(urlAvailable || adminMode)) {
			serverCategory.removePreference(mServerUrlPreference);
		} 

		mUsernamePreference = (EditTextPreference) findPreference(KEY_USERNAME);
		mUsernamePreference.setOnPreferenceChangeListener(this);
		mUsernamePreference.setSummary(mUsernamePreference.getText());
		mUsernamePreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });

		boolean usernameAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_USERNAME, true);
		if (!(usernameAvailable || adminMode)) {
			serverCategory.removePreference(mUsernamePreference);
		}

		mPasswordPreference = (EditTextPreference) findPreference(KEY_PASSWORD);
		mPasswordPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String pw = newValue.toString();

						if (pw.length() > 0) {
							mPasswordPreference.setSummary("********");
						} else {
							mPasswordPreference.setSummary("");
						}
						return true;
					}
				});
		if (mPasswordPreference.getText() != null
				&& mPasswordPreference.getText().length() > 0) {
			mPasswordPreference.setSummary("********");
		}
		mUsernamePreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });

		boolean passwordAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_PASSWORD, true);
		if (!(passwordAvailable || adminMode)) {
			serverCategory.removePreference(mPasswordPreference);
		}

		// get list of google accounts
		final Account[] accounts = AccountManager.get(getApplicationContext())
				.getAccountsByType("com.google");
		ArrayList<String> accountEntries = new ArrayList<String>();
		ArrayList<String> accountValues = new ArrayList<String>();

		for (int i = 0; i < accounts.length; i++) {
			accountEntries.add(accounts[i].name);
			accountValues.add(accounts[i].name);
		}
		accountEntries.add(getString(R.string.no_account));
		accountValues.add("");

		mSelectedGoogleAccountPreference = (ListPreference) findPreference(KEY_SELECTED_GOOGLE_ACCOUNT);
		mSelectedGoogleAccountPreference.setEntries(accountEntries
				.toArray(new String[accountEntries.size()]));
		mSelectedGoogleAccountPreference.setEntryValues(accountValues
				.toArray(new String[accountValues.size()]));
		mSelectedGoogleAccountPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						int index = ((ListPreference) preference)
								.findIndexOfValue(newValue.toString());
						String value = (String) ((ListPreference) preference)
								.getEntryValues()[index];
						((ListPreference) preference).setSummary(value);
						return true;
					}
				});
		mSelectedGoogleAccountPreference
				.setSummary(mSelectedGoogleAccountPreference.getValue());

		boolean googleAccountAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_GOOGLE_ACCOUNT, true);
		if (!(googleAccountAvailable || adminMode)) {
			serverCategory.removePreference(mSelectedGoogleAccountPreference);
		}

		mFormListUrlPreference.setOnPreferenceChangeListener(this);
		mFormListUrlPreference.setSummary(mFormListUrlPreference.getText());
		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter(), getWhitespaceFilter() });
		if (!(serverAvailable || adminMode)) {
			serverCategory.removePreference(mFormListUrlPreference);
		}

		mSubmissionUrlPreference.setOnPreferenceChangeListener(this);
		mSubmissionUrlPreference.setSummary(mSubmissionUrlPreference.getText());
		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter(), getWhitespaceFilter() });
		if (!(serverAvailable || adminMode)) {
			serverCategory.removePreference(mSubmissionUrlPreference);
		}

		if (!(serverAvailable || urlAvailable || usernameAvailable || passwordAvailable
				|| googleAccountAvailable || adminMode)) {
			getPreferenceScreen().removePreference(serverCategory);
		}

		PreferenceCategory clientCategory = (PreferenceCategory) findPreference(getString(R.string.client));

		
		mThemePreference = (ListPreference) findPreference(KEY_THEME);
		mThemePreference.setSummary(mThemePreference.getEntry());
		mThemePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
		{
			@Override
			public boolean onPreferenceChange(Preference preference,Object newValue)
			{
				int index = ((ListPreference) preference).findIndexOfValue(newValue.toString());
				String entry = (String) ((ListPreference) preference).getEntries()[index];
				((ListPreference) preference).setSummary(entry);
				
				Intent i = getIntent();
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
				return true;
			}
		});
		
		
		boolean fontAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_FONT_SIZE, true);
		mFontSizePreference = (ListPreference) findPreference(KEY_FONT_SIZE);
		mFontSizePreference.setSummary(mFontSizePreference.getEntry());
		mFontSizePreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						int index = ((ListPreference) preference)
								.findIndexOfValue(newValue.toString());
						String entry = (String) ((ListPreference) preference)
								.getEntries()[index];
						((ListPreference) preference).setSummary(entry);
						return true;
					}
				});
		if (!(fontAvailable || adminMode)) {
			clientCategory.removePreference(mFontSizePreference);
		}

		boolean defaultAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_DEFAULT_TO_FINALIZED, true);


		mSplashPathPreference = (PreferenceScreen) findPreference(KEY_SPLASH_PATH);
		mSplashPathPreference
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					private void launchImageChooser() {
						Intent i = new Intent(Intent.ACTION_GET_CONTENT);
						i.setType("image/*");
						startActivityForResult(i,
								ActivityPreferences.IMAGE_CHOOSER);
					}

					@Override
					public boolean onPreferenceClick(Preference preference) {
						// if you have a value, you can clear it or select new.
						CharSequence cs = mSplashPathPreference.getSummary();
						if (cs != null && cs.toString().contains("/")) {

							final CharSequence[] items = {
									getString(R.string.select_another_image),
									getString(R.string.use_odk_default) };

							AlertDialog.Builder builder = new AlertDialog.Builder(
									ActivityPreferences.this);
							builder.setTitle(getString(R.string.change_splash_path));
							builder.setNeutralButton(
									getString(R.string.cancel),
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.dismiss();
										}
									});
							builder.setItems(items,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog, int item) {
											if (items[item]
													.equals(getString(R.string.select_another_image))) {
												launchImageChooser();
											} else {
												setSplashPath(getString(R.string.default_splash_path));
											}
										}
									});
							AlertDialog alert = builder.create();
							alert.show();

						} else {
							launchImageChooser();
						}

						return true;
					}
				});
		
		mSplashPathPreference.setSummary(mSplashPathPreference
				.getSharedPreferences().getString(KEY_SPLASH_PATH,
						getString(R.string.default_splash_path)));

		boolean showSplashAvailable = mAdminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_SHOW_SPLASH_SCREEN, true);

		CheckBoxPreference showSplashPreference = (CheckBoxPreference) findPreference(KEY_SHOW_SPLASH);

		if (!(showSplashAvailable || adminMode)) {
			clientCategory.removePreference(showSplashPreference);
			clientCategory.removePreference(mSplashPathPreference);
		}

		//Advanced settings are just useless here
		PreferenceCategory advancedCategory = (PreferenceCategory)findPreference(getString((R.string.advanced_pref)));
		getPreferenceScreen().removePreference(advancedCategory);

	}

	private void disableFeaturesInDevelopment() {
		// remove Google Collections from protocol choices in preferences
		ListPreference protocols = (ListPreference) findPreference(KEY_PROTOCOL);
		int i = protocols.findIndexOfValue(PROTOCOL_GOOGLE);
		if (i != -1) {
			CharSequence[] entries = protocols.getEntries();
			CharSequence[] entryValues = protocols.getEntryValues();

			CharSequence[] newEntries = new CharSequence[entryValues.length - 1];
			CharSequence[] newEntryValues = new CharSequence[entryValues.length - 1];
			for (int k = 0, j = 0; j < entryValues.length; ++j) {
				if (j == i)
					continue;
				newEntries[k] = entries[j];
				newEntryValues[k] = entryValues[j];
				++k;
			}

			protocols.setEntries(newEntries);
			protocols.setEntryValues(newEntryValues);
		}
	}

	private void setSplashPath(String path) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = sharedPreferences.edit();
		editor.putString(KEY_SPLASH_PATH, path);
		editor.commit();

		mSplashPathPreference = (PreferenceScreen) findPreference(KEY_SPLASH_PATH);
		mSplashPathPreference.setSummary(mSplashPathPreference
				.getSharedPreferences().getString(KEY_SPLASH_PATH,
						getString(R.string.default_splash_path)));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == RESULT_CANCELED) {
			// request was canceled, so do nothing
			return;
		}

		switch (requestCode) {
		case IMAGE_CHOOSER:
			String sourceImagePath = null;

			// get gp of chosen file
			Uri uri = intent.getData();
			if (uri.toString().startsWith("file")) {
				sourceImagePath = uri.toString().substring(6);
			} else {
				String[] projection = { MediaColumns.DATA };
				Cursor c = null;
				try {
					c = getContentResolver().query(uri, projection, null, null,
							null);
					int i = c.getColumnIndexOrThrow(MediaColumns.DATA);
					c.moveToFirst();
					sourceImagePath = c.getString(i);
				} finally {
					if (c != null) {
						c.close();
					}
				}
			}
			setSplashPath(sourceImagePath);
			break;
		}
	}

	/**
	 * Disallows whitespace from user entry
	 * 
	 * @return
	 */
	private InputFilter getWhitespaceFilter() {
		InputFilter whitespaceFilter = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.isWhitespace(source.charAt(i))) {
						return "";
					}
				}
				return null;
			}
		};
		return whitespaceFilter;
	}

	/**
	 * Disallows carriage returns from user entry
	 * 
	 * @return
	 */
	private InputFilter getReturnFilter() {
		InputFilter returnFilter = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.getType((source.charAt(i))) == Character.CONTROL) {
						return "";
					}
				}
				return null;
			}
		};
		return returnFilter;
	}

	/**
	 * Generic listener that sets the summary to the newly selected/entered
	 * value
	 */
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		preference.setSummary((CharSequence) newValue);
		return true;
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.menu_activity_preferences, menu);
        this.menu=menu;
        getLayoutInflater().setFactory(new LayoutInflater.Factory()
        {
            public View onCreateView(String name, Context context, AttributeSet attrs)
            {
            	if (name.equalsIgnoreCase("com.android.internal.view.menu.IconMenuItemView")|| name.equalsIgnoreCase("TextView"))
                {
                    try
                    {
                        LayoutInflater li = LayoutInflater.from(context);
                        final View view = li.createView(name, null, attrs);
                        new Handler().post(new Runnable()
                        {
                            public void run()
                            {
                            	((TextView)view).setTextColor(getResources().getColor(R.color.actionbarTitleColorGris));
                                ((TextView)view).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/avenir.ttc"));
                            }
                        });
                        return view;
                    }
                    catch (InflateException e){}
                    catch (ClassNotFoundException e)
                    {}
                }
                return null;
            }
        });
        return true;
    }

	
	
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
    	{
	        case android.R.id.home:
	        	Finish.finishHome();
	        return true;
	        case R.id.menu_about_us:
	        	DialogAboutUs.aboutUs(this);
	        	return true;
	        case R.id.menu_exit:
	        	DialogExit.show(this);
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
    	}
	}
	
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case 1:

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final AlertDialog passwordDialog = builder.create();

			passwordDialog.setTitle(getString(R.string.enter_admin_password));
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			input.setTransformationMethod(PasswordTransformationMethod
					.getInstance());
			passwordDialog.setView(input, 20, 10, 20, 10);

			passwordDialog.setButton(DialogInterface.BUTTON_POSITIVE,
					getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							String value = input.getText().toString();
							String pw = mAdminPreferences.getString(
									AdminPreferencesActivity.KEY_ADMIN_PW, "");
							if (pw.compareTo(value) == 0) {
								Intent i = new Intent(getApplicationContext(),
										AdminPreferencesActivity.class);
								startActivity(i);
								input.setText("");
								passwordDialog.dismiss();
							} else {
								Toast.makeText(
										ActivityPreferences.this,
										getString(R.string.admin_password_incorrect),
										Toast.LENGTH_SHORT).show();
								Collect.getInstance()
										.getActivityLogger()
										.logAction(this, "adminPasswordDialog",
												"PASSWORD_INCORRECT");
							}
						}
					});

			passwordDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
					getString(R.string.cancel),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Collect.getInstance()
									.getActivityLogger()
									.logAction(this, "adminPasswordDialog",
											"cancel");
							input.setText("");
							return;
						}
					});

			passwordDialog.getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			return passwordDialog;

		}
		return null;
	}
	
	 public boolean onKeyUp(int keyCode, KeyEvent event) {
	        if (keyCode == KeyEvent.KEYCODE_MENU) {
	        	menu.performIdentifierAction(R.id.menu_other, 0);
	         }
	        super.onKeyUp(keyCode, event);
	        return true;
	     }


}

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

package com.makina.collect.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;

import com.makina.collect.R;
import com.makina.collect.activity.AbstractBaseActivity;
import com.makina.collect.dialog.DialogAboutUs;
import com.makina.collect.dialog.DialogHelpWithConfirmation;
import com.makina.collect.listeners.FormDownloaderListener;
import com.makina.collect.listeners.FormListDownloaderListener;
import com.makina.collect.model.FormDetails;
import com.makina.collect.preferences.ActivityPreferences;
import com.makina.collect.tasks.DownloadFormListTask;
import com.makina.collect.tasks.DownloadFormsTask;
import com.makina.collect.util.ThemeUtils;
import com.makina.collect.utilities.Finish;
import com.makina.collect.utilities.WebUtils;
import com.makina.collect.views.CroutonView;
import com.makina.collect.views.CustomFontTextview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Responsible for displaying, adding and deleting all the valid forms in the forms directory. One
 * caveat. If the server requires authentication, a dialog will pop up asking when you request the
 * form list. If somehow you manage to wait long enough and then try to download selected forms and
 * your authorization has timed out, it won't again ask for authentication, it will just throw a 401
 * and you'll have to hit 'refresh' where it will ask for credentials again. Technically a server
 * could point at other servers requiring authentication to download the forms, but the current
 * implementation in Collect doesn't allow for that. Mostly this is just because it's a pain in the
 * butt to keep track of which forms we've downloaded and where we're needing to authenticate. I
 * think we do something similar in the instanceuploader task/activity, so should change the
 * implementation eventually.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
@SuppressLint("NewApi")
@Deprecated
public class ActivityDownloadForm
        extends AbstractBaseActivity
        implements FormListDownloaderListener,
                   FormDownloaderListener,
                   SearchView.OnQueryTextListener {

    private static final String TAG = ActivityDownloadForm.class.getName();

    private static final int PROGRESS_DIALOG = 1;
    private static final int AUTH_DIALOG = 2;

    private static final String BUNDLE_TOGGLED_KEY = "toggled";
    private static final String BUNDLE_SELECTED_COUNT = "selectedcount";
    private static final String BUNDLE_FORM_MAP = "formmap";
    private static final String DIALOG_TITLE = "dialogtitle";
    private static final String DIALOG_MSG = "dialogmsg";
    private static final String DIALOG_SHOWING = "dialogshowing";
    private static final String FORMLIST = "formlist";

    public static final String LIST_URL = "listurl";

    private static final String FORMNAME = "formname";
    private static final String FORMDETAIL_KEY = "formdetailkey";
    private static final String FORMID_DISPLAY = "formiddisplay";

    private String mAlertMsg;
    private boolean mAlertShowing = false;
    private String mAlertTitle;

    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;

    private DownloadFormListTask mDownloadFormListTask;
    private DownloadFormsTask mDownloadFormsTask;

    private HashMap<String, FormDetails> mFormNamesAndURLs = new HashMap<>();
    private SimpleAdapter mFormListAdapter;
    private ArrayList<HashMap<String, String>> mFormList;

    private boolean mToggled = false;

    private static final boolean EXIT = true;
    private static final boolean DO_NOT_EXIT = false;
    private boolean mShouldExit;
    private static final String SHOULD_EXIT = "shouldexit";
    private CustomFontTextview textView_pannier;
    private ArrayList<String> mSelected = new ArrayList<>();
    private ListView listView;
    private final int RESULT_PREFERENCES = 1;
    private SearchView mSearchView;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_download_form);

        createDialog(PROGRESS_DIALOG);
        textView_pannier = (CustomFontTextview) findViewById(R.id.textView_pannier);
        Finish.activityDownloadForm = this;

        listView = (ListView) findViewById(R.id.listView);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayUseLogoEnabled(false);
            actionBar.setTitle(R.string.download_action_bar);
            actionBar.setSubtitle(R.string.form);
        }

        if (!getSharedPreferences("session",
                                  MODE_PRIVATE).getBoolean("help_download",
                                                           false)) {
            DialogHelpWithConfirmation.helpDialog(this,
                                                  getString(R.string.help_title1),
                                                  getString(R.string.help_download));
        }


        // need clear background before load
        //getListView().setBackgroundResource(R.drawable.background);


        if (savedInstanceState != null) {
            // If the screen has rotated, the hashmap with the form ids and urls is passed here.
            if (savedInstanceState.containsKey(BUNDLE_FORM_MAP)) {
                mFormNamesAndURLs = (HashMap<String, FormDetails>) savedInstanceState.getSerializable(BUNDLE_FORM_MAP);
            }

            // indicating whether or not select-all is on or off.
            if (savedInstanceState.containsKey(BUNDLE_TOGGLED_KEY)) {
                mToggled = savedInstanceState.getBoolean(BUNDLE_TOGGLED_KEY);
            }

            // to restore alert dialog.
            if (savedInstanceState.containsKey(DIALOG_TITLE)) {
                mAlertTitle = savedInstanceState.getString(DIALOG_TITLE);
            }
            if (savedInstanceState.containsKey(DIALOG_MSG)) {
                mAlertMsg = savedInstanceState.getString(DIALOG_MSG);
            }
            if (savedInstanceState.containsKey(DIALOG_SHOWING)) {
                mAlertShowing = savedInstanceState.getBoolean(DIALOG_SHOWING);
            }
            if (savedInstanceState.containsKey(SHOULD_EXIT)) {
                mShouldExit = savedInstanceState.getBoolean(SHOULD_EXIT);
            }
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(FORMLIST)) {
            mFormList = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable(FORMLIST);
        }
        else {
            mFormList = new ArrayList<>();
        }

        RelativeLayout relativeLayout_check_all = (RelativeLayout) findViewById(R.id.check_all);
        relativeLayout_check_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mToggled) {
                    selectAllOption();
                    ImageView imageView_check_all = (ImageView) findViewById(R.id.imageView_check_all);
                    imageView_check_all.setImageResource(R.drawable.case_off);
                    CustomFontTextview textView_check_all = (CustomFontTextview) findViewById(R.id.textView_check_all);
                    textView_check_all.setText(getString(R.string.select_all));

                }
                else {
                    selectAllOption();
                    ImageView imageView_check_all = (ImageView) findViewById(R.id.imageView_check_all);
                    imageView_check_all.setImageResource(R.drawable.case_on);
                    CustomFontTextview textView_check_all = (CustomFontTextview) findViewById(R.id.textView_check_all);
                    textView_check_all.setText(getString(R.string.deselect_all));
                }
            }
        });

        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> a,
                                    View v,
                                    int position,
                                    long id) {
                Object o = listView.getAdapter()
                                   .getItem(position);
                HashMap<String, String> item = (HashMap<String, String>) o;
                FormDetails detail = mFormNamesAndURLs.get(item.get(FORMDETAIL_KEY));

                if (mSelected.contains(detail.formID)) {
                    mSelected.remove(mSelected.indexOf(detail.formID));
                }
                else {
                    mSelected.add(detail.formID);
                }

                if (mSelected.size() == 0) {
                    textView_pannier.setText(getString(R.string.no_form_selected));
                }
                else if (mSelected.size() == 1) {
                    textView_pannier.setText(getString(R.string.form_selected));
                }
                else if (mSelected.size() > 1) {
                    textView_pannier.setText(mSelected.size() + " " + getString(R.string.forms_selected));
                }
            }
        });

    }

    @Override
    public void onResume() {

        if (this.getLastNonConfigurationInstance() instanceof DownloadFormListTask) {
            mDownloadFormListTask = (DownloadFormListTask) this.getLastNonConfigurationInstance();
            if (mDownloadFormListTask.getStatus() == AsyncTask.Status.FINISHED) {
                try {
                    this.dismissDialog(PROGRESS_DIALOG);
                }
                catch (IllegalArgumentException e) {
                    Log.i(TAG,
                          "Attempting to close a dialog that was not previously opened");
                }
                mDownloadFormsTask = null;
            }
        }
        else if (this.getLastNonConfigurationInstance() instanceof DownloadFormsTask) {
            mDownloadFormsTask = (DownloadFormsTask) this.getLastNonConfigurationInstance();
            if (mDownloadFormsTask.getStatus() == AsyncTask.Status.FINISHED) {
                try {
                    this.dismissDialog(PROGRESS_DIALOG);
                }
                catch (IllegalArgumentException e) {
                    Log.i(TAG,
                          "Attempting to close a dialog that was not previously opened");
                }
                mDownloadFormsTask = null;
            }
        }
        else if (this.getLastNonConfigurationInstance() == null) {
            // first time, so get the formlist
            downloadFormList();
        }

        String[] data = new String[]{FORMNAME, FORMID_DISPLAY, FORMDETAIL_KEY};
        int[] view = new int[]{R.id.text1, R.id.text2};

        mFormListAdapter = new SimpleAdapter(this,
                                             mFormList,
                                             R.layout.listview_item_download_form,
                                             data,
                                             view);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setItemsCanFocus(false);
        listView.setAdapter(mFormListAdapter);

        if (mDownloadFormListTask != null) {
            mDownloadFormListTask.setDownloaderListener(this);
        }
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(this);
        }
        if (mAlertShowing) {
            createAlertDialog(mAlertTitle,
                              mAlertMsg,
                              mShouldExit);
        }

        super.onResume();
    }

    private void clearChoices() {
        listView.clearChoices();

    }

    /**
     * Starts the download task and shows the progress dialog.
     */
    private void downloadFormList() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

        if (ni == null || !ni.isConnected()) {
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
            mProgressDialog.dismiss();
            CroutonView.showBuiltInCrouton(ActivityDownloadForm.this,
                                           getString(R.string.no_connexion),
                                           Style.ALERT);
        }
        else {

            mFormNamesAndURLs = new HashMap<>();
            if (mProgressDialog != null) {
                // This is needed because onPrepareDialog() is broken in 1.6.
                mProgressDialog.setMessage(getString(R.string.please_wait));
            }
            showDialog(PROGRESS_DIALOG);

            if (mDownloadFormListTask != null && mDownloadFormListTask.getStatus() != AsyncTask.Status.FINISHED) {
                return; // we are already doing the download!!!
            }
            else if (mDownloadFormListTask != null) {
                mDownloadFormListTask.setDownloaderListener(null);
                mDownloadFormListTask.cancel(true);
                mDownloadFormListTask = null;
            }

            mDownloadFormListTask = new DownloadFormListTask();
            mDownloadFormListTask.setDownloaderListener(this);
            mDownloadFormListTask.execute();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_TOGGLED_KEY,
                            mToggled);
        outState.putInt(BUNDLE_SELECTED_COUNT,
                        selectedItemCount());
        outState.putSerializable(BUNDLE_FORM_MAP,
                                 mFormNamesAndURLs);
        outState.putString(DIALOG_TITLE,
                           mAlertTitle);
        outState.putString(DIALOG_MSG,
                           mAlertMsg);
        outState.putBoolean(DIALOG_SHOWING,
                            mAlertShowing);
        outState.putBoolean(SHOULD_EXIT,
                            mShouldExit);
        outState.putSerializable(FORMLIST,
                                 mFormList);
    }


    /**
     * returns the number of items currently selected in the list.
     *
     * @return
     */
    private int selectedItemCount() {
        int count = 0;
        SparseBooleanArray sba = listView.getCheckedItemPositions();
        for (int i = 0; i < listView.getCount(); i++) {
            if (sba.get(i,
                        false)) {
                count++;
            }
        }
        return count;
    }

    protected void refreshFormsOption() {
        mToggled = false;
        downloadFormList();
        listView.clearChoices();
        clearChoices();
    }

    protected void getFormsOption() {
        downloadSelectedFiles();
        mToggled = false;
        clearChoices();
    }

    protected void selectAllOption() {
        mToggled = !mToggled;
        mSelected.clear();
        if (mToggled) {
            for (int pos = 0; pos < listView.getCount(); pos++) {
                listView.setItemChecked(pos,
                                        mToggled);
                mSelected.add(mFormList.get(pos)
                                       .get(FORMDETAIL_KEY));
            }
        }
        else {
            for (int pos = 0; pos < listView.getCount(); pos++) {
                listView.setItemChecked(pos,
                                        mToggled);
            }
        }
        if (mSelected.size() == 0) {
            textView_pannier.setText(getString(R.string.no_form_selected));
        }
        else if (mSelected.size() == 1) {
            textView_pannier.setText(getString(R.string.form_selected));
        }
        else if (mSelected.size() > 1) {
            textView_pannier.setText(mSelected.size() + " " + getString(R.string.forms_selected));
        }
    }

    protected void clearAllOption() {
        mToggled = false;
        mSelected.clear();

        for (int pos = 0; pos < listView.getCount(); pos++) {
            listView.setItemChecked(pos,
                                    mToggled);
        }

        textView_pannier.setText(getString(R.string.no_form_selected));
    }

    private void dismissDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void createDialog(int id) {

        switch (id) {
            case PROGRESS_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        dialog.dismiss();
                        // we use the same progress dialog for both
                        // so whatever isn't null is running
                        if (mDownloadFormListTask != null) {
                            mDownloadFormListTask.setDownloaderListener(null);
                            mDownloadFormListTask.cancel(true);
                            mDownloadFormListTask = null;
                        }
                        if (mDownloadFormsTask != null) {
                            mDownloadFormsTask.setDownloaderListener(null);
                            mDownloadFormsTask.cancel(true);
                            mDownloadFormsTask = null;
                        }
                    }
                };
                mProgressDialog.setTitle(getString(R.string.downloading_data));
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIconAttribute(R.attr.dialog_icon_info);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel),
                                          loadingButtonListener);
                mProgressDialog.show();
                break;
            case AUTH_DIALOG:
                AlertDialog.Builder b = new AlertDialog.Builder(this);

                LayoutInflater factory = LayoutInflater.from(this);
                final View dialogView = factory.inflate(R.layout.dialog_server_authentification,
                                                        null);

                // Get the server, username, and password from the settings
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
                String server = settings.getString(ActivityPreferences.KEY_SERVER_URL,
                                                   getString(R.string.default_server_url));

                String formListUrl = getString(R.string.default_odk_formlist);
                final String url = server + settings.getString(ActivityPreferences.KEY_FORMLIST_URL,
                                                               formListUrl);
                Log.i(TAG,
                      "Trying to get formList from: " + url);

                EditText username = (EditText) dialogView.findViewById(R.id.username_edit);
                String storedUsername = settings.getString(ActivityPreferences.KEY_USERNAME,
                                                           null);
                username.setText(storedUsername);

                EditText password = (EditText) dialogView.findViewById(R.id.password_edit);
                String storedPassword = settings.getString(ActivityPreferences.KEY_PASSWORD,
                                                           null);
                password.setText(storedPassword);

                b.setTitle(getString(R.string.server_requires_auth));
                b.setMessage(getString(R.string.server_auth_credentials,
                                       url));
                b.setView(dialogView);
                b.setPositiveButton(getString(R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            EditText username = (EditText) dialogView.findViewById(R.id.username_edit);
                                            EditText password = (EditText) dialogView.findViewById(R.id.password_edit);

                                            Uri u = Uri.parse(url);

                                            WebUtils.addCredentials(username.getText()
                                                                            .toString(),
                                                                    password.getText()
                                                                            .toString(),
                                                                    u.getHost());
                                            downloadFormList();
                                        }
                                    });
                b.setNegativeButton(getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                        }
                                    });

                b.setCancelable(false);
                mAlertShowing = false;
                mProgressDialog.show();
        }
    }


    /**
     * starts the task to download the selected forms, also shows progress dialog
     */
    @SuppressWarnings("unchecked")
    private void downloadSelectedFiles() {
        int totalCount = 0;
        ArrayList<FormDetails> filesToDownload = new ArrayList<>();

        for (int i = 0; i < mSelected.size(); i++) {
            filesToDownload.add(mFormNamesAndURLs.get(mSelected.get(i)));
        }
        totalCount = filesToDownload.size();

        if (totalCount > 0) {
            // show dialog box
            createDialog(PROGRESS_DIALOG);

            mDownloadFormsTask = new DownloadFormsTask();
            mDownloadFormsTask.setDownloaderListener(this);
            mDownloadFormsTask.execute(filesToDownload);
        }
        else {
            CroutonView.showBuiltInCrouton(ActivityDownloadForm.this,
                                           getString(R.string.noselect_error),
                                           Style.ALERT);

        }
    }


    /*@Override
    public Object onRetainNonConfigurationInstance() {
        if (mDownloadFormsTask != null) {
            return mDownloadFormsTask;
        } else {
            return mDownloadFormListTask;
        }
    }*/


    @Override
    public void onDestroy() {
        if (mDownloadFormListTask != null) {
            mDownloadFormListTask.setDownloaderListener(null);
        }
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(null);
        }
        super.onDestroy();
    }

   
   /* @Override
    public void onResume() {
    	
    	if (this.getLastNonConfigurationInstance() instanceof DownloadFormListTask)
    	{
            mDownloadFormListTask = (DownloadFormListTask) this.getLastNonConfigurationInstance();
            if (mDownloadFormListTask.getStatus() == AsyncTask.Status.FINISHED)
            {
                try
                {
                    this.dismissDialog(PROGRESS_DIALOG);
                }
                catch (IllegalArgumentException e)
                {
                    Log.i(TAG, "Attempting to close a dialog that was not previously opened");
                }
                mDownloadFormsTask = null;
            }
        }
    	else if (this.getLastNonConfigurationInstance() instanceof DownloadFormsTask)
    	{
            mDownloadFormsTask = (DownloadFormsTask) this.getLastNonConfigurationInstance();
            if (mDownloadFormsTask.getStatus() == AsyncTask.Status.FINISHED)
            {
                try
                {
                    this.dismissDialog(PROGRESS_DIALOG);
                }
                catch (IllegalArgumentException e)
                {
                    Log.i(TAG, "Attempting to close a dialog that was not previously opened");
                }
                mDownloadFormsTask = null;
            }
        }
    	else if (this.getLastNonConfigurationInstance() == null)
    	{
            // first time, so get the formlist
            downloadFormList();
        }
    	
    	String[] data = new String[] {FORMNAME, FORMID_DISPLAY, FORMDETAIL_KEY};
        int[] view = new int[] {R.id.text1, R.id.text2};

        mFormListAdapter =new SimpleAdapter(this, mFormList, R.layout.listview_item_download_form, data, view);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setItemsCanFocus(false);
        listView.setAdapter(mFormListAdapter);
        
        if (mDownloadFormListTask != null) {
            mDownloadFormListTask.setDownloaderListener(this);
        }
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(this);
        }
        if (mAlertShowing)
        {
            createAlertDialog(mAlertTitle, mAlertMsg, mShouldExit);
        }
        
        super.onResume();
    }*/


    @Override
    public void onPause() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        super.onPause();
    }


    /**
     * Called when the form list has finished downloading. results will either contain a set of
     * <formname, formdetails> tuples, or one tuple of DL.ERROR.MSG and the associated message.
     *
     * @param result
     */
    @Override
    public void formListDownloadingComplete(HashMap<String, FormDetails> result) {
        dismissDialog();
        mDownloadFormListTask.setDownloaderListener(null);
        mDownloadFormListTask = null;

        if (result == null) {
            Log.e(TAG,
                  "Formlist Downloading returned null.  That shouldn't happen");
            // Just displayes "error occured" to the user, but this should never happen.
            createAlertDialog(getString(R.string.load_remote_form_error),
                              getString(R.string.error_occured),
                              EXIT);
            return;
        }

        if (result.containsKey(DownloadFormListTask.DL_AUTH_REQUIRED)) {
            // need authorization
            this.showDialog(AUTH_DIALOG);
        }
        else if (result.containsKey(DownloadFormListTask.DL_ERROR_MSG)) {
            // Download failed
            String dialogMessage = getString(R.string.list_failed_with_error,
                                             result.get(DownloadFormListTask.DL_ERROR_MSG).errorStr);
            String dialogTitle = getString(R.string.load_remote_form_error);
            createAlertDialog(dialogTitle,
                              dialogMessage,
                              DO_NOT_EXIT);
        }
        else {
            // Everything worked. Clear the list and add the results.
            mFormNamesAndURLs = result;

            mFormList.clear();

            ArrayList<String> ids = new ArrayList<>(mFormNamesAndURLs.keySet());
            for (int i = 0; i < result.size(); i++) {
                String formDetailsKey = ids.get(i);
                FormDetails details = mFormNamesAndURLs.get(formDetailsKey);
                HashMap<String, String> item = new HashMap<>();
                item.put(FORMNAME,
                         details.formName);
                item.put(FORMID_DISPLAY,
                         ((details.formVersion == null) ? "" : (getString(R.string.version) + " " + details.formVersion + " ")) + "ID: " + details.formID);
                item.put(FORMDETAIL_KEY,
                         formDetailsKey);
                Log.i(TAG,
                      "nom: " + details.formName + " / version: " + details.formVersion);
                // Insert the new form in alphabetical order.
                if (mFormList.size() == 0) {
                    mFormList.add(item);
                }
                else {
                    int j;
                    for (j = 0; j < mFormList.size(); j++) {
                        HashMap<String, String> compareMe = mFormList.get(j);
                        String name = compareMe.get(FORMNAME);
                        if (name.compareTo(mFormNamesAndURLs.get(ids.get(i)).formName) > 0) {
                            break;
                        }
                    }
                    mFormList.add(j,
                                  item);
                }
            }
            mFormListAdapter.notifyDataSetChanged();
        }

        if ((mFormList == null) || (mFormList.size() == 0)) {
            findViewById(R.id.linearLayout_footer).setVisibility(View.GONE);
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.linearLayout_footer).setVisibility(View.VISIBLE);
            findViewById(R.id.empty).setVisibility(View.GONE);
        }
    }


    /**
     * Creates an alert dialog with the given tite and message. If shouldExit is set to true, the
     * activity will exit when the user clicks "ok".
     *
     * @param title
     * @param message
     * @param shouldExit
     */
    private void createAlertDialog(String title,
                                   String message,
                                   final boolean shouldExit) {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle(title);
        mAlertDialog.setMessage(message);
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,
                                int i) {
                switch (i) {
                    case DialogInterface.BUTTON1: // ok
                        // just close the dialog
                        mAlertShowing = false;
                        finish();
                        startActivity(new Intent(getApplicationContext(),
                                                 ActivityEditForm.class));
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok),
                               quitListener);
        mAlertDialog.setIconAttribute(R.attr.dialog_icon_info);
        mAlertMsg = message;
        mAlertTitle = title;
        mAlertShowing = true;
        mShouldExit = shouldExit;
        mAlertDialog.show();
    }


    @Override
    public void progressUpdate(String currentFile,
                               int progress,
                               int total) {
        mAlertMsg = getString(R.string.fetching_file,
                              currentFile,
                              progress,
                              total);
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(mAlertMsg);
        }
    }


    @Override
    public void formsDownloadingComplete(HashMap<FormDetails, String> result) {
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(null);
        }

        if (mProgressDialog.isShowing()) {
            // should always be true here
            mProgressDialog.dismiss();
        }

        Set<FormDetails> keys = result.keySet();
        StringBuilder b = new StringBuilder();
        for (FormDetails k : keys) {
            b.append(k.formName + " - " + result.get(k));
            b.append("\n\n");
        }
        clearAllOption();
        createAlertDialog(getString(R.string.download_forms_result),
                          b.toString()
                           .trim(),
                          EXIT);
    }

    public boolean onQueryTextChange(String newText) {
        ArrayList<HashMap<String, String>> mFormList = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i < this.mFormList.size(); i++) {
            if (this.mFormList.get(i)
                              .get(FORMNAME)
                              .toUpperCase()
                              .contains(newText.toUpperCase())) {
                mFormList.add(this.mFormList.get(i));
            }
        }

        String[] data = new String[]{FORMNAME, FORMDETAIL_KEY};
        int[] view = new int[]{R.id.text1};

        mFormListAdapter = new SimpleAdapter(this,
                                             mFormList,
                                             R.layout.listview_item_download_form,
                                             data,
                                             view);
        listView.setAdapter(mFormListAdapter);

        for (int i = 0; i < mFormList.size(); i++) {
            if (mSelected.contains(mFormList.get(i)
                                            .get(FORMDETAIL_KEY))) {
                listView.setItemChecked(i,
                                        true);
            }
        }

        return false;
    }

    public boolean onQueryTextSubmit(String query) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchView.getWindowToken(),
                                    0);
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.search,
                             menu);
        menuInflater.inflate(R.menu.download,
                             menu);
        menuInflater.inflate(R.menu.settings,
                             menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        // Handle action buttons for all fragments
        switch (item.getItemId()) {
            case android.R.id.home:
                Finish.finishHome();
                return true;
            case R.id.menu_download:
                getFormsOption();
                return true;
            case R.id.menu_settings:
                startActivityForResult((new Intent(this,
                                                   ActivityPreferences.class)),
                                       RESULT_PREFERENCES);
                return true;
            case R.id.menu_help:
                Intent mIntent = new Intent(this,
                                            ActivityHelp.class);
                Bundle mBundle = new Bundle();
                mBundle.putInt("position",
                               0);
                mIntent.putExtras(mBundle);
                startActivity(mIntent);
                return true;
            case R.id.menu_about_us:
                DialogAboutUs.aboutUs(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        if (requestCode == RESULT_PREFERENCES) {
            Intent i = getIntent();
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }

    @Nullable
    @Override
    protected ThemeUtils.AppThemeVariant applyTheme() {
        return ThemeUtils.AppThemeVariant.GREEN;
    }
}
